package net.smartlogic.unitconverter.timer.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import net.smartlogic.unitconverter.timer.TimerEngine
import net.smartlogic.unitconverter.timer.model.TimerPhase
import net.smartlogic.unitconverter.timer.notification.TimerNotificationHelper

class TimerForegroundService : Service() {
    private val notificationHelper by lazy { TimerNotificationHelper(this) }
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            val engine = TimerEngine.getInstance(this@TimerForegroundService)
            engine.refreshDisplay()
            if (engine.uiState.value.phase is TimerPhase.Running) {
                handler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.removeCallbacks(tick)
        val engine = TimerEngine.getInstance(this)
        when (val phase = engine.uiState.value.phase) {
            is TimerPhase.Running -> {
                startForeground(TimerNotificationHelper.NOTIFICATION_ID,
                    notificationHelper.buildRunningNotification(phase.label,
                        (phase.endElapsedRealtime - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0L), false))
                handler.post(tick)
            }
            is TimerPhase.Completed -> {
                startForeground(TimerNotificationHelper.NOTIFICATION_ID,
                    notificationHelper.buildCompletedNotification(phase.label))
                engine.ensureCompletedAlarm()
            }
            else -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java)
            try {
                context.startForegroundService(intent)
            } catch (_: IllegalStateException) {
                // A background start may be denied. The scheduled alarm remains active.
            } catch (_: SecurityException) {
                // Preserve timer state when a device disallows this service type.
            }
        }
        fun stop(context: Context) {
            context.stopService(Intent(context, TimerForegroundService::class.java))
        }
    }
}
