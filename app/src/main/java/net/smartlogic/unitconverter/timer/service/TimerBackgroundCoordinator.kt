package net.smartlogic.unitconverter.timer.service

import android.content.Context
import net.smartlogic.unitconverter.timer.notification.TimerNotificationHelper

interface TimerBackgroundCoordinator {
    fun startRunning(label: String, remainingMs: Long, completionToken: Long, triggerAtMillis: Long)
    fun stopRunning()
    fun showPaused(label: String, remainingMs: Long)
    fun showCompleted(label: String)
    fun cancelNotification()
}

class AndroidTimerBackgroundCoordinator(context: Context) : TimerBackgroundCoordinator {
    private val appContext = context.applicationContext
    private val alarmScheduler = TimerAlarmScheduler(appContext)
    private val notificationHelper = TimerNotificationHelper(appContext)

    override fun startRunning(
        label: String,
        remainingMs: Long,
        completionToken: Long,
        triggerAtMillis: Long,
    ) {
        alarmScheduler.schedule(completionToken, triggerAtMillis)
        TimerForegroundService.start(appContext)
        notificationHelper.showRunning(label, remainingMs, paused = false)
    }

    override fun stopRunning() {
        alarmScheduler.cancel()
        TimerForegroundService.stop(appContext)
        notificationHelper.cancel()
    }

    override fun showPaused(label: String, remainingMs: Long) {
        notificationHelper.showRunning(label, remainingMs, paused = true)
    }

    override fun showCompleted(label: String) {
        notificationHelper.showCompleted(label)
    }

    override fun cancelNotification() {
        notificationHelper.cancel()
    }
}

class NoOpTimerBackgroundCoordinator : TimerBackgroundCoordinator {
    override fun startRunning(
        label: String,
        remainingMs: Long,
        completionToken: Long,
        triggerAtMillis: Long,
    ) = Unit

    override fun stopRunning() = Unit
    override fun showPaused(label: String, remainingMs: Long) = Unit
    override fun showCompleted(label: String) = Unit
    override fun cancelNotification() = Unit
}
