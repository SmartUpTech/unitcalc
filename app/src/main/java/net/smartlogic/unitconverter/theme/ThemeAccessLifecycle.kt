package net.smartlogic.unitconverter.theme

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.activity.MainActivity
import net.smartlogic.unitconverter.activity.SettingsActivity
import net.smartlogic.unitconverter.helper.Preferences
import java.lang.ref.WeakReference

/** Foreground boundaries only; no expiry alarm or appearance-changing countdown. */
object ThemeAccessLifecycle {
    private var resumed = WeakReference<Activity>(null)
    private val main = Handler(Looper.getMainLooper())

    @JvmStatic fun dispatchPendingNotice(context: android.content.Context) {
        val application = context.applicationContext
        main.post {
            val activity = resumed.get() ?: return@post
            if (activity.isFinishing || activity.isDestroyed || ThemeRewardCoordinator.isBusy()) return@post
            val message = when (Preferences.getInstance(application).consumeThemeNotice()) {
                Preferences.NOTICE_EXPIRED -> R.string.theme_access_expired
                Preferences.NOTICE_MIGRATED -> R.string.theme_access_migrated
                else -> return@post
            }
            Toast.makeText(application, message, Toast.LENGTH_LONG).show()
        }
    }

    @JvmStatic fun install(application: Application) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { ThemeManager.refreshAccess(application) }
        })
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (activity !is MainActivity && activity !is SettingsActivity) return
                resumed = WeakReference(activity)
                ThemeManager.refreshAccess(application)
            }
            override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) {
                if (resumed.get() === activity) resumed.clear()
            }
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
