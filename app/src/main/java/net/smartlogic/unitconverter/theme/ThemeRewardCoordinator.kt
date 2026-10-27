package net.smartlogic.unitconverter.theme

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.helper.AdMobManager
import net.smartlogic.unitconverter.helper.Preferences
import net.smartlogic.unitconverter.utils.Utils
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.Executors

/** Process-owned transaction; no Activity/ViewModel is retained across the ad. */
object ThemeRewardCoordinator {
    internal fun interface AdPresenter {
        fun show(activity: Activity, eligible: () -> Boolean, earned: (Long) -> Unit, closed: (Boolean) -> Unit): Boolean
    }
    internal var presenter = AdPresenter { activity, eligible, earned, closed ->
        AdMobManager.getInstance(activity).showThemeAd(activity, eligible, earned, closed)
    }
    internal var connected: (android.content.Context) -> Boolean = { Utils().isNetworkAvailable(it) }
    private val main = Handler(Looper.getMainLooper())
    private val writer = Executors.newSingleThreadExecutor { task -> Thread(task, "theme-reward-store") }
    private val mutableBusy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = mutableBusy
    private var attempt: Attempt? = null

    private class Attempt(val themeId: String, val id: String = UUID.randomUUID().toString()) {
        var earned = false
        var saved = false
        var writeComplete = false
        var closed = false
        var failed = false
    }

    @JvmStatic fun isBusy(): Boolean = mutableBusy.value

    fun unlock(activity: Activity, themeId: String) {
        if (isBusy()) return
        ThemeManager.refreshAccess(activity)
        val preferences = Preferences.getInstance(activity)
        if (preferences.isThemeAvailable(themeId, System.currentTimeMillis(), ThemeManager.hasProAccess())) {
            ThemeManager.select(activity, themeId)
            return
        }
        if (!connected(activity)) {
            message(activity, R.string.theme_unlock_offline)
            return
        }
        if (!ThemeAccess.isKnown(themeId)) return
        val transaction = Attempt(themeId)
        attempt = transaction
        mutableBusy.value = true
        val context = activity.applicationContext
        val host = WeakReference(activity)
        val accepted = try { presenter.show(activity, {
            val target = host.get()
            target != null && !target.isFinishing && !target.isDestroyed &&
                (target !is LifecycleOwner || target.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) &&
                connected(context) &&
                !preferences.isThemeAvailable(themeId, System.currentTimeMillis(), ThemeManager.hasProAccess())
        }, { rewardTime ->
            // The SDK may deliver more than one callback or close before reward delivery.
            if (!transaction.earned) {
                transaction.earned = true
                if (attempt === transaction) mutableBusy.value = true
                writer.execute {
                    val saved = try { preferences.saveThemeReward(themeId, transaction.id, rewardTime) }
                        catch (error: RuntimeException) { false }
                    main.post {
                        transaction.saved = saved
                        transaction.writeComplete = true
                        if (!saved) message(context, R.string.theme_reward_save_failed)
                        finish(context, transaction)
                    }
                }
            }
        }, { failed ->
            if (transaction.closed) return@show
            transaction.closed = true
            transaction.failed = failed
            finish(context, transaction)
        }) } catch (error: RuntimeException) { false }
        if (!accepted) {
            transaction.closed = true
            transaction.failed = true
            finish(context, transaction)
        }
    }

    private fun finish(context: android.content.Context, transaction: Attempt) {
        if (!transaction.closed || (transaction.earned && !transaction.writeComplete)) return
        if (attempt !== transaction) return
        mutableBusy.value = false
        if (transaction.saved) {
            // Foreground recovery uses the same pending-reward path as startup.
            ThemeManager.refreshAccess(context)
        } else if (!transaction.earned) {
            val preferences = Preferences.getInstance(context)
            if (preferences.isThemeAvailable(transaction.themeId, System.currentTimeMillis(), ThemeManager.hasProAccess())) {
                ThemeManager.select(context, transaction.themeId)
            } else main.post {
                if (!transaction.earned) message(context,
                    if (transaction.failed) R.string.theme_ad_unavailable else R.string.theme_ad_not_earned)
            }
        }
    }

    internal fun awaitWrites() { writer.submit {}.get() }

    private fun message(context: android.content.Context, resId: Int) {
        Toast.makeText(context, resId, Toast.LENGTH_LONG).show()
    }
}
