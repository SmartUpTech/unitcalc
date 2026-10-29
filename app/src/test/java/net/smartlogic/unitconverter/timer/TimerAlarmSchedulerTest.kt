package net.smartlogic.unitconverter.timer

import android.app.AlarmManager
import android.content.Context
import net.smartlogic.unitconverter.timer.service.TimerAlarmScheduler
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 29, 34])
class TimerAlarmSchedulerTest {
    @Test fun scheduledElapsedAlarmIsCancelledByTheSameIdentity() {
        val app = RuntimeEnvironment.getApplication()
        val alarms = shadowOf(app.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        val scheduler = TimerAlarmScheduler(app)
        scheduler.schedule(7L, 120_000L)
        assertEquals(1, alarms.scheduledAlarms.size)
        assertEquals(AlarmManager.ELAPSED_REALTIME_WAKEUP, alarms.scheduledAlarms.single().type)
        assertEquals(120_000L, alarms.scheduledAlarms.single().triggerAtTime)
        scheduler.cancel()
        assertTrue(alarms.scheduledAlarms.isEmpty())
    }

    @Test @Config(sdk = [34]) fun deniedExactAlarmAccessStillSchedulesCancelableFallback() {
        val app = RuntimeEnvironment.getApplication()
        val alarms = shadowOf(app.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        val scheduler = TimerAlarmScheduler(app)
        scheduler.schedule(8L, 180_000L)
        assertEquals(1, alarms.scheduledAlarms.size)
        scheduler.cancel()
        assertTrue(alarms.scheduledAlarms.isEmpty())
    }
}
