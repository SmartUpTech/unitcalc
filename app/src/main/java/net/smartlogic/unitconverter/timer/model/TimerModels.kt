package net.smartlogic.unitconverter.timer.model

data class TimerDraft(
    val hours: Int = 0,
    val minutes: Int = 0,
    val seconds: Int = 0,
    val label: String = "",
) {
    val durationMs: Long
        get() = ((hours * 3600L) + (minutes * 60L) + seconds) * 1000L

    val isZero: Boolean
        get() = durationMs <= 0L
}

data class RecentTimer(
    val durationMs: Long,
    val label: String,
)

sealed interface TimerPhase {
    data class Setting(val draft: TimerDraft) : TimerPhase

    data class Running(
        val label: String,
        val originalDurationMs: Long,
        val endElapsedRealtime: Long,
    ) : TimerPhase

    data class Paused(
        val label: String,
        val originalDurationMs: Long,
        val remainingMs: Long,
    ) : TimerPhase

    data class Completed(
        val label: String,
        val originalDurationMs: Long,
        val completionToken: Long,
    ) : TimerPhase
}

data class TimerUiState(
    val phase: TimerPhase = TimerPhase.Setting(TimerDraft()),
    val recents: List<RecentTimer> = emptyList(),
    val remainingMs: Long = 0L,
    val pendingRecent: RecentTimer? = null,
)
