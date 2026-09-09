package net.smartlogic.unitconverter.timer.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.activity.MainActivity
import net.smartlogic.unitconverter.timer.TimerDurationFormatter
import net.smartlogic.unitconverter.timer.TimerEngine
import net.smartlogic.unitconverter.timer.model.TimerPhase

class TimerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        TimerWidgetUpdater(context).update(TimerEngine.getInstance(context))
    }

    override fun onEnabled(context: Context) {
        TimerWidgetUpdater(context).update(TimerEngine.getInstance(context))
    }
}

object TimerWidgetRenderer {
    fun render(context: Context, snapshot: TimerEngine.WidgetSnapshot): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.timer_widget)
        val defaultLabel = context.getString(R.string.timer_default_label)
        val label = when (val phase = snapshot.phase) {
            is TimerPhase.Setting -> TimerDurationFormatter.displayLabel(phase.draft.label, defaultLabel)
            is TimerPhase.Running -> TimerDurationFormatter.displayLabel(phase.label, defaultLabel)
            is TimerPhase.Paused -> TimerDurationFormatter.displayLabel(phase.label, defaultLabel)
            is TimerPhase.Completed -> TimerDurationFormatter.displayLabel(phase.label, defaultLabel)
        }
        val timeText = when (snapshot.phase) {
            is TimerPhase.Setting -> TimerDurationFormatter.formatDuration(
                (snapshot.phase as TimerPhase.Setting).draft.durationMs,
            )
            is TimerPhase.Completed -> context.getString(R.string.timer_times_up)
            else -> TimerDurationFormatter.formatCountdown(snapshot.remainingMs)
        }
        views.setTextViewText(R.id.timer_widget_label, label)
        views.setTextViewText(R.id.timer_widget_time, timeText)
        val openIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            android.content.Intent(context, MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_TIMER, true)
            },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.timer_widget_label, openIntent)
        views.setOnClickPendingIntent(R.id.timer_widget_time, openIntent)
        return views
    }
}

class TimerWidgetUpdater(private val context: Context) {
    fun update(engine: TimerEngine) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            android.content.ComponentName(context, TimerWidgetProvider::class.java),
        )
        if (ids.isEmpty()) {
            return
        }
        val views = TimerWidgetRenderer.render(context, engine.snapshotForWidget())
        ids.forEach { id -> manager.updateAppWidget(id, views) }
    }
}
