package net.smartlogic.unitconverter.theme

import android.app.Application
import net.smartlogic.unitconverter.BuildConfig
import androidx.lifecycle.ViewModelStore
import net.smartlogic.unitconverter.helper.Preferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class ThemeSelectorViewModelTest {
    private lateinit var preferences: Preferences
    private lateinit var model: ThemeSelectorViewModel
    private lateinit var store: ViewModelStore
    @Before fun setup() {
        preferences = Preferences.getInstance(RuntimeEnvironment.getApplication())
        preferences.preferences.edit().clear().commit()
        ThemeManager.setProAccessProvider { false }
        ThemeManager.init(RuntimeEnvironment.getApplication())
        model = ThemeSelectorViewModel(preferences)
        store = ViewModelStore().apply { put("theme", model) }
    }
    @After fun teardown() { store.clear(); ThemeManager.setProAccessProvider { false } }
    @Test fun freeThemesHaveNoCountdownAndLockedTapOnlyOpensConfirmation() {
        val cards = model.uiState.value.cards
        assertEquals(3, cards.count { it.unlocked && it.alwaysFree })
        cards.filter { it.alwaysFree }.forEach {
            assertEquals(0L, it.remainingMillis)
            assertTrue(model.requestTheme(it.theme.id()))
        }
        assertEquals(BuildConfig.DEBUG, model.requestTheme("glacier"))
        assertEquals(if (BuildConfig.DEBUG) null else CalculatorThemes.GLACIER.nameRes(), model.uiState.value.lockedThemeNameRes)
        assertEquals(0L, preferences.getThemeExpiry("glacier"))
        assertEquals("porcelain_white", preferences.selectedThemeId)
        model.dismissLockedTheme()
        assertNull(model.uiState.value.lockedThemeNameRes)
    }
    @Test fun rewardedAccessUpdatesCardsWithoutChangingSelectionUntilRecovery() {
        assertTrue(preferences.saveThemeReward("glacier", "a", System.currentTimeMillis()))
        model.refresh()
        assertTrue(model.uiState.value.cards.single { it.theme.id() == "glacier" }.unlocked)
        assertEquals("porcelain_white", preferences.selectedThemeId)
        ThemeManager.refreshAccess(RuntimeEnvironment.getApplication())
        assertEquals("glacier", model.uiState.value.cards.single { it.selected }.theme.id())
    }
    @Test fun foregroundRefreshUpdatesUnusedExpiredCardsAndProStatus() {
        val context = RuntimeEnvironment.getApplication()
        preferences.saveThemeReward("glacier", "a", System.currentTimeMillis())
        ThemeManager.refreshAccess(context)
        ThemeManager.select(context, "oyster_cream")
        preferences.preferences.edit().putLong(Preferences.PREFS_THEME_EXPIRY_PREFIX + "glacier", 1).commit()
        ThemeManager.refreshAccess(context)
        assertEquals(BuildConfig.DEBUG, model.uiState.value.cards.single { it.theme.id() == "glacier" }.unlocked)
        assertEquals("oyster_cream", preferences.selectedThemeId)
        assertNull(preferences.consumeThemeNotice())
        ThemeManager.setProAccessProvider { true }
        ThemeManager.refreshAccess(context)
        assertTrue(model.uiState.value.cards.all { it.unlocked })
        assertTrue(model.uiState.value.cards.all { it.pro })
    }
}
