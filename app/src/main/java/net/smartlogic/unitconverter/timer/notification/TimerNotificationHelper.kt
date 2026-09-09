package net.smartlogic.unitconverter.timer.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.activity.MainActivity
import net.smartlogic.unitconverter.timer.TimerDurationFormatter
import net.smartlogic.unitconverter.timer.service.TimerActionReceiver

class TimerNotificationHelper(private val context: Context) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.timer_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showRunning(label: String, remainingMs: Long, paused: Boolean) {
        val notification = buildRunningNotification(label, remainingMs, paused)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun showCompleted(label: String) {
        val openIntent = openTimerIntent()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(context.getString(R.string.timer_times_up))
            .setContentText(context.getString(R.string.timer_notification_complete, displayLabel(label)))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun buildRunningNotification(label: String, remainingMs: Long, paused: Boolean): Notification {
        val formatted = TimerDurationFormatter.formatCountdown(remainingMs)
        val title = displayLabel(label)
        val content = if (paused) {
            context.getString(R.string.timer_notification_paused, title, formatted)
        } else {
            context.getString(R.string.timer_notification_running, title, formatted)
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(openTimerIntent())
            .setOngoing(!paused)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (paused) {
            builder.addAction(0, context.getString(R.string.timer_resume), actionIntent(ACTION_RESUME))
        } else {
            builder.addAction(0, context.getString(R.string.timer_pause), actionIntent(ACTION_PAUSE))
        }
        builder.addAction(0, context.getString(R.string.timer_reset), actionIntent(ACTION_STOP))
        return builder.build()
    }

    fun cancel() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun openTimerIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_TIMER, true)
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun actionIntent(action: String): PendingIntent {
        val intent = Intent(context, TimerActionReceiver::class.java).apply {
            this.action = action
        }
        val requestCode = when (action) {
            ACTION_PAUSE -> 1
            ACTION_RESUME -> 2
            else -> 3
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun displayLabel(label: String): String {
        return TimerDurationFormatter.displayLabel(label, context.getString(R.string.timer_default_label))
    }

    companion object {
        const val CHANNEL_ID = "graphy_timer"
        const val NOTIFICATION_ID = 51001
        const val ACTION_PAUSE = "net.smartlogic.unitconverter.timer.PAUSE"
        const val ACTION_RESUME = "net.smartlogic.unitconverter.timer.RESUME"
        const val ACTION_STOP = "net.smartlogic.unitconverter.timer.STOP"
    }
}
