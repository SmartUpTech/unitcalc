package net.smartlogic.unitconverter.timer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import net.smartlogic.unitconverter.timer.TimerEngine

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_TIMER_ALARM) {
            return
        }
        val token = intent.getLongExtra(EXTRA_COMPLETION_TOKEN, -1L)
        TimerEngine.getInstance(context).onAlarmFired(token)
    }

    companion object {
        const val ACTION_TIMER_ALARM = "net.smartlogic.unitconverter.timer.ALARM"
        const val EXTRA_COMPLETION_TOKEN = "completion_token"
    }
}
