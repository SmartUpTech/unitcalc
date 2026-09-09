package net.smartlogic.unitconverter.timer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import net.smartlogic.unitconverter.timer.TimerEngine
import net.smartlogic.unitconverter.timer.notification.TimerNotificationHelper

class TimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        TimerEngine.getInstance(context).handleNotificationAction(action)
    }

    companion object {
        const val ACTION_OPEN_TIMER = "net.smartlogic.unitconverter.timer.OPEN"
    }
}
