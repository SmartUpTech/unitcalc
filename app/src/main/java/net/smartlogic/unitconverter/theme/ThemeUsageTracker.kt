package net.smartlogic.unitconverter.theme

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.smartlogic.unitconverter.helper.Preferences

/** Foreground sessions only: timer services, receivers and widgets do not earn usage days. */
object ThemeUsageTracker {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    @JvmStatic
    fun install(context: Context) {
        val preferences = Preferences.getInstance(context.applicationContext)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                val launchDate = LocalDate.now()
                scope.launch { preferences.recordThemeUsage(launchDate) }
            }
        })
    }
}
