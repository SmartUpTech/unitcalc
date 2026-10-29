package net.smartlogic.unitconverter.timer

import android.app.Application
import android.app.Notification
import android.widget.FrameLayout
import android.widget.TextView
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.activity.MainActivity
import net.smartlogic.unitconverter.timer.model.TimerDraft
import net.smartlogic.unitconverter.timer.model.TimerPhase
import net.smartlogic.unitconverter.timer.notification.TimerNotificationHelper
import net.smartlogic.unitconverter.timer.widget.TimerWidgetRenderer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class TimerEntryPointsTest {
    @Test fun runningNotificationCountsDownAndActionsUseTimerReceiver() {
        val app = RuntimeEnvironment.getApplication()
        val notification = TimerNotificationHelper(app).buildRunningNotification("Focus", 60_000L, false)
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertEquals(TimerNotificationHelper.ACTION_PAUSE, shadowOf(notification.actions[0].actionIntent).savedIntent.action)
        assertEquals(TimerNotificationHelper.ACTION_STOP, shadowOf(notification.actions[1].actionIntent).savedIntent.action)
        val open = shadowOf(notification.contentIntent).savedIntent
        assertEquals(MainActivity::class.java.name, open.component!!.className)
        assertTrue(open.getBooleanExtra(MainActivity.EXTRA_OPEN_TIMER, false))
    }

    @Test fun pausedAndCompletedNotificationsOfferResumeOrDismiss() {
        val helper = TimerNotificationHelper(RuntimeEnvironment.getApplication())
        val paused = helper.buildRunningNotification("Study", 10_000L, true)
        assertFalse(paused.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertEquals(TimerNotificationHelper.ACTION_RESUME, shadowOf(paused.actions[0].actionIntent).savedIntent.action)
        val completed = helper.buildCompletedNotification("Focus")
        assertEquals(TimerNotificationHelper.ACTION_STOP, shadowOf(completed.actions.single().actionIntent).savedIntent.action)
        assertTrue(completed.flags and Notification.FLAG_ONGOING_EVENT != 0)
    }

    @Test fun widgetRendersEveryPhaseAndOpensTheExistingTimer() {
        val app = RuntimeEnvironment.getApplication()
        val phases = listOf(
            TimerPhase.Setting(TimerDraft(minutes = 5)) to "5:00",
            TimerPhase.Running("Focus", 60_000L, 60_000L) to "0:01",
            TimerPhase.Paused("Focus", 60_000L, 999L) to "0:01",
            TimerPhase.Completed("Focus", 60_000L, 1L) to app.getString(R.string.timer_times_up),
        )
        for ((phase, expected) in phases) {
            val root = TimerWidgetRenderer.render(app, TimerEngine.WidgetSnapshot(phase, 999L))
                .apply(app, FrameLayout(app))
            assertEquals(expected, root.findViewById<TextView>(R.id.timer_widget_time).text.toString())
            assertEquals("Focus", root.findViewById<TextView>(R.id.timer_widget_label).text.toString())
            assertTrue(root.findViewById<TextView>(R.id.timer_widget_time).performClick())
            val intent = shadowOf(app).nextStartedActivity
            assertEquals(MainActivity::class.java.name, intent.component!!.className)
            assertTrue(intent.getBooleanExtra(MainActivity.EXTRA_OPEN_TIMER, false))
        }
    }
}
