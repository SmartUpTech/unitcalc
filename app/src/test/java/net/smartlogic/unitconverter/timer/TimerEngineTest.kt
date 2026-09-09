package net.smartlogic.unitconverter.timer

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
    fun recents_mergeDuplicatesAndKeepOrder() {
        engine.startRecent(RecentTimer(5_000L, "Study"))
        engine.reset()
        engine.startRecent(RecentTimer(5_000L, "Study"))
        engine.reset()
        assertEquals(1, engine.uiState.value.recents.size)
        assertEquals("Study", engine.uiState.value.recents.first().label)
    }
}
