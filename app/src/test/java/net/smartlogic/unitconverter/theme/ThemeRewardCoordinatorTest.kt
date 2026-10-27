package net.smartlogic.unitconverter.theme

import android.app.Activity
import android.app.Application
import android.os.Looper
import net.smartlogic.unitconverter.helper.Preferences
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
@LooperMode(LooperMode.Mode.PAUSED)
class ThemeRewardCoordinatorTest {
    private lateinit var preferences: Preferences
    private lateinit var activity: Activity
    private lateinit var earned: (Long) -> Unit
    private lateinit var closed: (Boolean) -> Unit
    private lateinit var eligible: () -> Boolean
    private lateinit var original: ThemeRewardCoordinator.AdPresenter
    private var presentations = 0

    @Before fun setup() {
        preferences = Preferences.getInstance(RuntimeEnvironment.getApplication())
        preferences.preferences.edit().clear().commit()
        ThemeManager.setProAccessProvider { false }
        ThemeManager.init(RuntimeEnvironment.getApplication())
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        original = ThemeRewardCoordinator.presenter
        ThemeRewardCoordinator.connected = { true }
        ThemeRewardCoordinator.presenter = ThemeRewardCoordinator.AdPresenter { _, check, reward, close ->
            presentations++
            eligible = check; earned = reward; closed = close
            true
        }
    }

    @After fun teardown() {
        if (::closed.isInitialized) closed(true)
        flush()
        ThemeRewardCoordinator.presenter = original
        ThemeRewardCoordinator.connected = { net.smartlogic.unitconverter.utils.Utils().isNetworkAvailable(it) }
        ThemeManager.setProAccessProvider { false }
        activity.finish()
    }

    private fun flush() {
        ThemeRewardCoordinator.awaitWrites()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test fun onlyEarnedRewardPersistsAndApplicationWaitsForClosure() {
        ThemeRewardCoordinator.unlock(activity, "glacier")
        assertTrue(eligible())
        assertEquals(0, preferences.getThemeExpiry("glacier"))
        val now = System.currentTimeMillis()
        earned(now)
        earned(now + 1000)
        flush()
        assertTrue(ThemeRewardCoordinator.isBusy())
        assertEquals(now + ThemeAccess.REWARD_DURATION_MILLIS, preferences.getThemeExpiry("glacier"))
        assertEquals("porcelain_white", preferences.selectedThemeId)
        assertEquals("glacier", preferences.preferences.getString(Preferences.PREFS_PENDING_REWARDED_THEME, null))
        closed(false)
        flush()
        assertEquals("glacier", preferences.selectedThemeId)
        assertEquals("glacier", ThemeManager.get().id())
        assertFalse(preferences.preferences.contains(Preferences.PREFS_PENDING_REWARDED_THEME))
        assertFalse(ThemeRewardCoordinator.isBusy())
    }

    @Test fun rapidTapsCannotStartAnotherRewardAttempt() {
        ThemeRewardCoordinator.unlock(activity, "glacier")
        ThemeRewardCoordinator.unlock(activity, "sage_green")
        assertEquals(1, presentations)
        earned(System.currentTimeMillis())
        closed(false)
        flush()
        assertEquals(0, preferences.getThemeExpiry("sage_green"))
    }

    @Test fun dismissalAndPresentationFailureGrantNothingAndAllowRetry() {
        ThemeRewardCoordinator.unlock(activity, "glacier")
        closed(false)
        flush()
        assertEquals(0, preferences.getThemeExpiry("glacier"))
        assertFalse(ThemeRewardCoordinator.isBusy())
        ThemeRewardCoordinator.unlock(activity, "glacier")
        closed(true)
        flush()
        assertEquals(2, presentations)
        assertEquals(0, preferences.getThemeExpiry("glacier"))
    }

    @Test fun closeBeforeRewardDeliveryDoesNotLoseTheReward() {
        ThemeRewardCoordinator.unlock(activity, "glacier")
        closed(false)
        earned(System.currentTimeMillis())
        flush()
        assertEquals("glacier", preferences.selectedThemeId)
        assertFalse(ThemeRewardCoordinator.isBusy())
    }

    @Test fun offlineAndAlreadyUnlockedThemesNeverStartAnAd() {
        ThemeRewardCoordinator.connected = { false }
        ThemeRewardCoordinator.unlock(activity, "glacier")
        assertEquals(0, presentations)
        assertEquals(0, preferences.getThemeExpiry("glacier"))
        ThemeRewardCoordinator.unlock(activity, "oyster_cream")
        assertEquals("oyster_cream", preferences.selectedThemeId)
        assertEquals(0, presentations)
    }

    @Test fun accessAndConnectivityAreRecheckedBeforePresentation() {
        ThemeRewardCoordinator.unlock(activity, "glacier")
        ThemeRewardCoordinator.connected = { false }
        assertFalse(eligible())
        ThemeRewardCoordinator.connected = { true }
        ThemeManager.setProAccessProvider { true }
        assertFalse(eligible())
        closed(true)
        flush()
        assertEquals("glacier", preferences.selectedThemeId)
        assertEquals(0, preferences.getThemeExpiry("glacier"))
    }

    @Test fun recreationDoesNotLoseEarnedAccess() {
        ThemeRewardCoordinator.unlock(activity, "glacier")
        earned(System.currentTimeMillis())
        activity.finish()
        flush()
        assertTrue(preferences.getThemeExpiry("glacier") > System.currentTimeMillis())
        closed(false)
        flush()
        ThemeManager.init(RuntimeEnvironment.getApplication())
        assertEquals("glacier", ThemeManager.get().id())
    }
}
