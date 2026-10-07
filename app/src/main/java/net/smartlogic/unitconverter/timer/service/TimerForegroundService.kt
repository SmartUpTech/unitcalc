package net.smartlogic.unitconverter.timer.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import net.smartlogic.unitconverter.timer.TimerEngine
import net.smartlogic.unitconverter.timer.model.TimerPhase
import net.smartlogic.unitconverter.timer.notification.TimerNotificationHelper

class TimerForegroundService : Service() {
    private val notificationHelper by lazy { TimerNotificationHelper(this) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val engine = TimerEngine.getInstance(this)
        when (val phase = engine.uiState.value.phase) {
            is TimerPhase.Running -> {
                val notification = notificationHelper.buildRunningNotification(
                    label = phase.label,
                    remainingMs = engine.uiState.value.remainingMs,
                    paused = false,
                )
                startForeground(TimerNotificationHelper.NOTIFICATION_ID, notification)
            }
            else -> stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java)
            try {
                context.startForegroundService(intent)
            } catch (denied: IllegalStateException) {
                // Android may reject foreground-service starts from a receiver/background.
                // The scheduled alarm remains responsible for completion.
            } catch (denied: SecurityException) {
                // Device or policy denied this service type; keep the timer/alarm active.
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TimerForegroundService::class.java))
        }
    }
}
