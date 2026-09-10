package net.smartlogic.unitconverter.timer

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.smartlogic.unitconverter.timer.alarm.TimerAlarmPlayer
import net.smartlogic.unitconverter.timer.model.RecentTimer
import net.smartlogic.unitconverter.timer.model.TimerDraft
import net.smartlogic.unitconverter.timer.model.TimerPhase
import net.smartlogic.unitconverter.timer.model.TimerUiState
import net.smartlogic.unitconverter.timer.notification.TimerNotificationHelper
import net.smartlogic.unitconverter.timer.service.AndroidTimerBackgroundCoordinator
import net.smartlogic.unitconverter.timer.service.TimerBackgroundCoordinator
import net.smartlogic.unitconverter.timer.widget.TimerWidgetUpdater

class TimerEngine private constructor(
    appContext: Context,
    private var clock: TimerClock = SystemTimerClock,
) {
    private val context = appContext.applicationContext
    private val preferences = TimerPreferences(context)
    private var backgroundCoordinator: TimerBackgroundCoordinator =
        AndroidTimerBackgroundCoordinator(context)
    private val alarmPlayer = TimerAlarmPlayer(context)
    private val widgetUpdater = TimerWidgetUpdater(context)

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var completionToken: Long = 0L
    private var lastFiredCompletionToken: Long = -1L
    private var alarmPlayerEnabled: Boolean = true

    init {
        restoreFromStorage()
    }

    fun setClockForTests(clock: TimerClock) {
        this.clock = clock
    }

    fun setBackgroundCoordinatorForTests(coordinator: TimerBackgroundCoordinator) {
        backgroundCoordinator = coordinator
    }

    fun setAlarmPlayerEnabledForTests(enabled: Boolean) {
        alarmPlayerEnabled = enabled
    }

    fun refreshDisplay() {
        reconcileActivePhase()
    }

    fun updateDraft(draft: TimerDraft) {
        val normalized = draft.copy(label = TimerDurationFormatter.normalizeLabel(draft.label))
        if (_uiState.value.phase is TimerPhase.Setting) {
            preferences.saveSettingDraft(normalized)
            publishSetting(normalized)
        }
    }

    fun startFromDraft() {
        val draft = currentDraft()
        if (draft.isZero) {
            return
        }
        startTimer(draft.durationMs, draft.label, draft.durationMs)
    }

    fun startRecent(recent: RecentTimer, forceReplace: Boolean = false) {
        if (!forceReplace && requiresReplaceConfirmation()) {
            _uiState.update { it.copy(pendingRecent = recent) }
            return
        }
        _uiState.update { it.copy(pendingRecent = null) }
        startTimer(recent.durationMs, recent.label, recent.durationMs)
    }

    fun confirmReplaceRecent() {
        val pending = _uiState.value.pendingRecent ?: return
        startRecent(pending, forceReplace = true)
    }

    fun dismissReplaceRecent() {
        _uiState.update { it.copy(pendingRecent = null) }
    }

    fun pause() {
        val phase = _uiState.value.phase as? TimerPhase.Running ?: return
        val remainingMs = remainingForRunning(phase).coerceAtLeast(0L)
        backgroundCoordinator.stopRunning()
        val paused = TimerPhase.Paused(
            label = phase.label,
            originalDurationMs = phase.originalDurationMs,
            remainingMs = remainingMs,
        )
        publishPhase(paused, remainingMs)
        backgroundCoordinator.showPaused(phase.label, remainingMs)
    }

    fun resume() {
        val phase = _uiState.value.phase as? TimerPhase.Paused ?: return
        if (phase.remainingMs <= 0L) {
            completeTimer(phase.label, phase.originalDurationMs)
            return
        }
        val running = TimerPhase.Running(
            label = phase.label,
            originalDurationMs = phase.originalDurationMs,
            endElapsedRealtime = clock.elapsedRealtime() + phase.remainingMs,
        )
        publishPhase(running, phase.remainingMs)
        startBackgroundExecution(running, phase.remainingMs)
    }

    fun restart() {
        when (val phase = _uiState.value.phase) {
            is TimerPhase.Running,
            is TimerPhase.Paused,
            is TimerPhase.Completed,
            -> {
                val label = phaseLabel(phase)
                val original = phaseOriginalDuration(phase)
                stopAlarmIfNeeded()
                startTimer(original, label, original)
            }
            is TimerPhase.Setting -> startFromDraft()
        }
    }

    fun reset() {
        stopAlarmIfNeeded()
        stopBackgroundExecution()
        val draft = currentDraft()
        publishSetting(draft)
    }

    fun dismissCompleted() {
        val phase = _uiState.value.phase
        if (phase !is TimerPhase.Completed) {
            return
        }
        stopAlarmIfNeeded()
        val draft = TimerDurationFormatter.draftFromDuration(phase.originalDurationMs, phase.label)
        publishSetting(draft)
    }

    fun onAlarmFired(token: Long) {
        if (token != completionToken || token == lastFiredCompletionToken) {
            return
        }
        lastFiredCompletionToken = token
        val phase = _uiState.value.phase
        if (phase is TimerPhase.Running) {
            completeTimer(phase.label, phase.originalDurationMs, playAlarm = true)
        }
    }

    fun handleNotificationAction(action: String) {
        when (action) {
            TimerNotificationHelper.ACTION_PAUSE -> pause()
            TimerNotificationHelper.ACTION_RESUME -> resume()
            TimerNotificationHelper.ACTION_STOP -> reset()
        }
    }

    private fun startTimer(durationMs: Long, label: String, originalDurationMs: Long) {
        if (durationMs <= 0L) {
            return
        }
        stopAlarmIfNeeded()
        val normalizedLabel = TimerDurationFormatter.normalizeLabel(label)
        addRecent(RecentTimer(durationMs, normalizedLabel))
        completionToken += 1L
        val running = TimerPhase.Running(
            label = normalizedLabel,
            originalDurationMs = originalDurationMs,
            endElapsedRealtime = clock.elapsedRealtime() + durationMs,
        )
        publishPhase(running, durationMs)
        startBackgroundExecution(running, durationMs)
    }

    private fun completeTimer(label: String, originalDurationMs: Long, playAlarm: Boolean = true) {
        stopBackgroundExecution()
        val completed = TimerPhase.Completed(
            label = label,
            originalDurationMs = originalDurationMs,
            completionToken = completionToken,
        )
        publishPhase(completed, 0L)
        backgroundCoordinator.showCompleted(label)
        widgetUpdater.update(this)
        if (playAlarm && alarmPlayerEnabled) {
            alarmPlayer.start()
        }
    }

    private fun reconcileActivePhase() {
        when (val phase = _uiState.value.phase) {
            is TimerPhase.Running -> {
                val remaining = remainingForRunning(phase)
                if (remaining <= 0L) {
                    if (completionToken != lastFiredCompletionToken) {
                        lastFiredCompletionToken = completionToken
                        completeTimer(phase.label, phase.originalDurationMs)
                    }
                } else {
                    _uiState.update { it.copy(remainingMs = remaining) }
                    widgetUpdater.update(this)
                }
            }
            is TimerPhase.Paused -> {
                _uiState.update { it.copy(remainingMs = phase.remainingMs) }
                backgroundCoordinator.showPaused(phase.label, phase.remainingMs)
                widgetUpdater.update(this)
            }
            is TimerPhase.Completed -> {
                _uiState.update { it.copy(remainingMs = 0L) }
                widgetUpdater.update(this)
            }
            is TimerPhase.Setting -> widgetUpdater.update(this)
        }
    }

    private fun remainingForRunning(phase: TimerPhase.Running): Long {
        return phase.endElapsedRealtime - clock.elapsedRealtime()
    }

    private fun requiresReplaceConfirmation(): Boolean {
        return _uiState.value.phase is TimerPhase.Running || _uiState.value.phase is TimerPhase.Paused
    }

    private fun currentDraft(): TimerDraft {
        return when (val phase = _uiState.value.phase) {
            is TimerPhase.Setting -> phase.draft
            is TimerPhase.Running -> TimerDurationFormatter.draftFromDuration(
                phase.originalDurationMs,
                phase.label,
            )
            is TimerPhase.Paused -> TimerDurationFormatter.draftFromDuration(
                phase.originalDurationMs,
                phase.label,
            )
            is TimerPhase.Completed -> TimerDurationFormatter.draftFromDuration(
                phase.originalDurationMs,
                phase.label,
            )
        }
    }

    private fun publishSetting(draft: TimerDraft) {
        preferences.saveSettingDraft(draft)
        preferences.clearActiveSnapshot()
        publishPhase(TimerPhase.Setting(draft), 0L)
    }

    private fun publishPhase(phase: TimerPhase, remainingMs: Long) {
        preferences.saveActiveSnapshot(phase, completionToken)
        _uiState.update {
            it.copy(
                phase = phase,
                remainingMs = remainingMs.coerceAtLeast(0L),
                pendingRecent = null,
            )
        }
        widgetUpdater.update(this)
    }

    private fun addRecent(recent: RecentTimer) {
        val recents = preferences.loadRecents().toMutableList()
        recents.removeAll { TimerDurationFormatter.recentKey(it) == TimerDurationFormatter.recentKey(recent) }
        recents.add(0, recent)
        val trimmed = recents.take(TimerDurationFormatter.MAX_RECENTS)
        preferences.saveRecents(trimmed)
        _uiState.update { it.copy(recents = trimmed) }
    }

    private fun startBackgroundExecution(running: TimerPhase.Running, remainingMs: Long) {
        backgroundCoordinator.startRunning(
            label = running.label,
            remainingMs = remainingMs,
            completionToken = completionToken,
            triggerAtMillis = clock.currentTimeMillis() + remainingMs,
        )
    }

    private fun stopBackgroundExecution() {
        backgroundCoordinator.stopRunning()
    }

    private fun stopAlarmIfNeeded() {
        alarmPlayer.stop()
    }

    private fun restoreFromStorage() {
        val recents = preferences.loadRecents()
        val draft = preferences.loadSettingDraft()
        val snapshot = preferences.loadActiveSnapshot()
        val phase: TimerPhase
        val remaining: Long
        if (snapshot == null) {
            phase = TimerPhase.Setting(draft)
            remaining = 0L
        } else {
            completionToken = snapshot.completionToken
            when (snapshot.state) {
                "running" -> {
                    val running = TimerPhase.Running(
                        label = snapshot.label,
                        originalDurationMs = snapshot.originalDurationMs,
                        endElapsedRealtime = snapshot.endElapsedRealtime,
                    )
                    remaining = running.endElapsedRealtime - clock.elapsedRealtime()
                    phase = if (remaining <= 0L) {
                        TimerPhase.Completed(
                            label = snapshot.label,
                            originalDurationMs = snapshot.originalDurationMs,
                            completionToken = snapshot.completionToken,
                        )
                    } else {
                        running
                    }
                }
                "paused" -> {
                    phase = TimerPhase.Paused(
                        label = snapshot.label,
                        originalDurationMs = snapshot.originalDurationMs,
                        remainingMs = snapshot.remainingMs,
                    )
                    remaining = snapshot.remainingMs
                }
                "completed" -> {
                    phase = TimerPhase.Completed(
                        label = snapshot.label,
                        originalDurationMs = snapshot.originalDurationMs,
                        completionToken = snapshot.completionToken,
                    )
                    remaining = 0L
                }
                else -> {
                    phase = TimerPhase.Setting(draft)
                    remaining = 0L
                }
            }
        }
        _uiState.value = TimerUiState(
            phase = phase,
            recents = recents,
            remainingMs = remaining.coerceAtLeast(0L),
        )
        if (phase is TimerPhase.Running && remaining > 0L) {
            startBackgroundExecution(phase, remaining)
        } else if (phase is TimerPhase.Running && remaining <= 0L) {
            completeTimer(phase.label, phase.originalDurationMs, playAlarm = false)
        }
        widgetUpdater.update(this)
    }

    private fun phaseLabel(phase: TimerPhase): String = when (phase) {
        is TimerPhase.Running -> phase.label
        is TimerPhase.Paused -> phase.label
        is TimerPhase.Completed -> phase.label
        is TimerPhase.Setting -> phase.draft.label
    }

    private fun phaseOriginalDuration(phase: TimerPhase): Long = when (phase) {
        is TimerPhase.Running -> phase.originalDurationMs
        is TimerPhase.Paused -> phase.originalDurationMs
        is TimerPhase.Completed -> phase.originalDurationMs
        is TimerPhase.Setting -> phase.draft.durationMs
    }

    fun snapshotForWidget(): WidgetSnapshot {
        val state = _uiState.value
        return WidgetSnapshot(
            phase = state.phase,
            remainingMs = state.remainingMs,
        )
    }

    data class WidgetSnapshot(
        val phase: TimerPhase,
        val remainingMs: Long,
    )

    companion object {
        @Volatile
        private var instance: TimerEngine? = null

        fun getInstance(context: Context): TimerEngine {
            return instance ?: synchronized(this) {
                instance ?: TimerEngine(context.applicationContext).also { instance = it }
            }
        }

        fun resetForTests() {
            instance = null
        }
    }
}
