package net.smartlogic.unitconverter.timer

import android.content.Context

import net.smartlogic.unitconverter.timer.model.RecentTimer
import net.smartlogic.unitconverter.timer.model.TimerDraft
import net.smartlogic.unitconverter.timer.model.TimerPhase
import net.smartlogic.unitconverter.timer.service.NoOpTimerBackgroundCoordinator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TimerEngineTest {
    private lateinit var clock: FakeTimerClock
    private lateinit var engine: TimerEngine

    @Before
    fun setUp() {
        TimerEngine.resetForTests()
        RuntimeEnvironment.getApplication()
            .getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        clock = FakeTimerClock()
        engine = TimerEngine.getInstance(RuntimeEnvironment.getApplication())
        engine.setClockForTests(clock)
        engine.setBackgroundCoordinatorForTests(NoOpTimerBackgroundCoordinator())
        engine.setAlarmPlayerEnabledForTests(false)
        engine.reset()
        engine.updateDraft(TimerDraft(hours = 0, minutes = 0, seconds = 10, label = "Study"))
    }

    @Test
    fun start_countsDownToCompleted() {
        engine.startFromDraft()
        clock.advance(10_000L)
        engine.refreshDisplay()
        assertTrue(engine.uiState.value.phase is TimerPhase.Completed)
        assertEquals(0L, engine.uiState.value.remainingMs)
    }

    @Test
    fun pause_freezesRemaining() {
        engine.startFromDraft()
        clock.advance(4_000L)
        engine.refreshDisplay()
        engine.pause()
        val pausedRemaining = engine.uiState.value.remainingMs
        clock.advance(5_000L)
        engine.refreshDisplay()
        assertTrue(engine.uiState.value.phase is TimerPhase.Paused)
        assertEquals(pausedRemaining, engine.uiState.value.remainingMs)
    }

    @Test
    fun resume_continuesFromRemaining() {
        engine.startFromDraft()
        clock.advance(4_000L)
        engine.refreshDisplay()
        engine.pause()
        engine.resume()
        clock.advance(6_000L)
        engine.refreshDisplay()
        assertTrue(engine.uiState.value.phase is TimerPhase.Completed)
    }

    @Test
    fun restart_restoresOriginalDuration() {
        engine.startFromDraft()
        clock.advance(4_000L)
        engine.refreshDisplay()
        engine.restart()
        assertTrue(engine.uiState.value.phase is TimerPhase.Running)
        assertEquals(10_000L, engine.uiState.value.remainingMs)
    }

    @Test
    fun reset_returnsToSetting() {
        engine.startFromDraft()
        engine.reset()
        assertTrue(engine.uiState.value.phase is TimerPhase.Setting)
    }

    @Test
    fun zeroDurationCannotStart() {
        engine.updateDraft(TimerDraft())
        engine.startFromDraft()
        assertTrue(engine.uiState.value.phase is TimerPhase.Setting)
    }

    @Test
    fun reentry_keepsOneRunningTimerAndUsesElapsedTime() {
        engine.startFromDraft()
        clock.advance(3_500L)
        val reopened = TimerEngine.getInstance(RuntimeEnvironment.getApplication())
        assertTrue(engine === reopened)
        reopened.refreshDisplay()
        assertEquals(6_500L, reopened.uiState.value.remainingMs)
        reopened.pause()
        clock.advance(20_000L)
        reopened.refreshDisplay()
        assertEquals(6_500L, reopened.uiState.value.remainingMs)
        reopened.resume()
        clock.advance(6_500L)
        reopened.refreshDisplay()
        assertTrue(reopened.uiState.value.phase is TimerPhase.Completed)
    }

    @Test
    fun reset_cancelsRunningStateAndAllowsFreshStart() {
        engine.startFromDraft()
        clock.advance(2_000L)
        engine.reset()
        clock.advance(20_000L)
        engine.refreshDisplay()
        assertTrue(engine.uiState.value.phase is TimerPhase.Setting)
        engine.startFromDraft()
        assertEquals(10_000L, engine.uiState.value.remainingMs)
    }

    @Test
    fun recents_mergeDuplicatesAndKeepOrder() {
        engine.startRecent(RecentTimer(5_000L, "Study"))
        engine.reset()
        engine.startRecent(RecentTimer(5_000L, "Study"))
        engine.reset()
        assertEquals(1, engine.uiState.value.recents.size)
        assertEquals("Study", engine.uiState.value.recents.first().label)
    }

    @Test fun doubleStartDoesNotRestartActiveTimer() {
        engine.startFromDraft()
        clock.advance(2_000L)
        engine.startFromDraft()
        engine.refreshDisplay()
        assertEquals(8_000L, engine.uiState.value.remainingMs)
    }

    @Test fun wallClockChangesDoNotChangeDuration() {
        engine.startFromDraft()
        clock.currentTimeMillisMs += 3_600_000L
        engine.refreshDisplay()
        assertEquals(10_000L, engine.uiState.value.remainingMs)
        clock.currentTimeMillisMs -= 7_200_000L
        clock.advance(1000L)
        engine.refreshDisplay()
        assertEquals(9_000L, engine.uiState.value.remainingMs)
    }

    @Test fun largestWheelDurationRemainsExact() {
        engine.updateDraft(TimerDraft(hours = 99, minutes = 59, seconds = 59))
        engine.startFromDraft()
        assertEquals(359_999_000L, engine.uiState.value.remainingMs)
        clock.advance(359_999_000L)
        engine.refreshDisplay()
        assertTrue(engine.uiState.value.phase is TimerPhase.Completed)
    }

    @Test fun latePauseCompletesRatherThanFreezingAtZero() {
        engine.startFromDraft()
        clock.advance(10_001L)
        engine.pause()
        assertTrue(engine.uiState.value.phase is TimerPhase.Completed)
    }

    @Test fun oldAlarmCannotCompleteReplacementOrResumedTimerEarly() {
        engine.startFromDraft()
        engine.onAlarmFired(1L)
        assertTrue(engine.uiState.value.phase is TimerPhase.Running)
        engine.pause()
        clock.advance(30_000L)
        engine.resume()
        engine.onAlarmFired(1L)
        assertTrue(engine.uiState.value.phase is TimerPhase.Running)
        engine.restart()
        clock.advance(10_000L)
        engine.onAlarmFired(1L)
        assertTrue(engine.uiState.value.phase is TimerPhase.Running)
        engine.onAlarmFired(2L)
        assertTrue(engine.uiState.value.phase is TimerPhase.Completed)
        engine.onAlarmFired(2L)
        assertTrue(engine.uiState.value.phase is TimerPhase.Completed)
    }

    @Test fun recentReplacementRequiresConfirmationAndCanBeCancelled() {
        engine.startFromDraft()
        engine.startRecent(RecentTimer(60_000L, "Tea"))
        assertEquals(10_000L, engine.uiState.value.remainingMs)
        assertEquals("Tea", engine.uiState.value.pendingRecent?.label)
        engine.dismissReplaceRecent()
        assertEquals(null, engine.uiState.value.pendingRecent)
        engine.startRecent(RecentTimer(60_000L, "Tea"))
        engine.confirmReplaceRecent()
        assertEquals(60_000L, engine.uiState.value.remainingMs)
    }

    @Test fun notificationActionsFollowTheSameStateMachine() {
        val helper = net.smartlogic.unitconverter.timer.notification.TimerNotificationHelper
        engine.startFromDraft()
        clock.advance(3_000L)
        engine.handleNotificationAction(helper.ACTION_PAUSE)
        assertEquals(7_000L, engine.uiState.value.remainingMs)
        engine.handleNotificationAction(helper.ACTION_RESUME)
        assertTrue(engine.uiState.value.phase is TimerPhase.Running)
        engine.handleNotificationAction(helper.ACTION_STOP)
        assertTrue(engine.uiState.value.phase is TimerPhase.Setting)
    }

    @Test fun pausedTimerAndDraftSurviveEngineRecreation() {
        engine.startFromDraft()
        clock.advance(4_000L)
        engine.pause()
        TimerEngine.resetForTests()
        val restored = TimerEngine.getInstance(RuntimeEnvironment.getApplication())
        restored.setBackgroundCoordinatorForTests(NoOpTimerBackgroundCoordinator())
        restored.setAlarmPlayerEnabledForTests(false)
        assertTrue(restored.uiState.value.phase is TimerPhase.Paused)
        assertEquals(6_000L, restored.uiState.value.remainingMs)
        restored.reset()
        assertEquals(TimerDraft(seconds = 10, label = "Study"),
            (restored.uiState.value.phase as TimerPhase.Setting).draft)
    }

    @Test fun expiredPersistedRunningTimerCompletesThroughReceiverOnlyOnce() {
        val app = RuntimeEnvironment.getApplication()
        TimerPreferences(app).saveActiveSnapshot(TimerPhase.Running("Tea", 1000L, 0L), 42L)
        TimerEngine.resetForTests()
        val restored = TimerEngine.getInstance(app)
        restored.setBackgroundCoordinatorForTests(NoOpTimerBackgroundCoordinator())
        restored.setAlarmPlayerEnabledForTests(false)
        restored.onAlarmFired(42L)
        assertTrue(restored.uiState.value.phase is TimerPhase.Completed)
        restored.dismissCompleted()
        restored.onAlarmFired(42L)
        assertTrue(restored.uiState.value.phase is TimerPhase.Setting)
    }
}
