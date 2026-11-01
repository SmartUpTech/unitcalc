package net.smartlogic.unitconverter.theme

import android.app.Activity
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.helper.Preferences
import net.smartlogic.unitconverter.utils.Utils

@Immutable
data class ThemeCardState(
    val theme: CalculatorTheme, val selected: Boolean, val unlocked: Boolean,
    val alwaysFree: Boolean, val pro: Boolean, val remainingMillis: Long,
)

@Immutable
data class ThemeSelectorState(
    val cards: List<ThemeCardState>, val lockedThemeNameRes: Int?, val busy: Boolean,
)

/** One derived snapshot. Preview composition never reads preferences or grants access. */
class ThemeSelectorViewModel(private val preferences: Preferences) : ViewModel() {
    private var lockedThemeId: String? = null
    private val mutableState = MutableStateFlow(buildState())
    val uiState: StateFlow<ThemeSelectorState> = mutableState
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key == Preferences.PREFS_SELECTED_THEME ||
            key.startsWith(Preferences.PREFS_THEME_EXPIRY_PREFIX)) refresh()
    }
    private val accessListener = Runnable { refresh() }

    init {
        preferences.preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        ThemeManager.addAccessListener(accessListener)
        viewModelScope.launch { ThemeRewardCoordinator.busy.collect { refresh() } }
    }

    fun tapTheme(activity: Activity, themeId: String) {
        if (ThemeRewardCoordinator.isBusy()) return
        ThemeManager.refreshAccess(activity)
        if (!preferences.isThemeAvailable(themeId, System.currentTimeMillis(), ThemeManager.hasProAccess()) &&
            !Utils().isNetworkAvailable(activity)) {
            dismissLockedTheme()
            Toast.makeText(activity, R.string.theme_unlock_offline, Toast.LENGTH_LONG).show()
            return
        }
        if (requestTheme(themeId)) ThemeManager.select(activity, themeId)
    }

    /** True only when available. The shared manager rechecks before committing selection. */
    fun requestTheme(themeId: String): Boolean {
        if (ThemeRewardCoordinator.isBusy()) return false
        if (!preferences.isThemeAvailable(themeId, System.currentTimeMillis(), ThemeManager.hasProAccess())) {
            lockedThemeId = themeId.takeIf(ThemeAccess::isKnown)
            refresh()
            return false
        }
        lockedThemeId = null
        refresh()
        return true
    }

    fun watchAd(activity: Activity) {
        val id = lockedThemeId ?: return
        dismissLockedTheme()
        ThemeRewardCoordinator.unlock(activity, id)
    }

    fun dismissLockedTheme() { lockedThemeId = null; refresh() }
    fun refresh() { mutableState.value = buildState() }

    private fun buildState(): ThemeSelectorState {
        val now = System.currentTimeMillis()
        val pro = ThemeManager.hasProAccess()
        val selected = preferences.selectedThemeId
        val locked = lockedThemeId?.takeUnless { preferences.isThemeAvailable(it, now, pro) }
        return ThemeSelectorState(
            CalculatorThemes.all().map { theme ->
                ThemeCardState(theme, theme.id() == selected,
                    preferences.isThemeAvailable(theme.id(), now, pro),
                    ThemeAccess.isAlwaysFree(theme.id()), pro,
                    (preferences.getThemeExpiry(theme.id()) - now).coerceAtLeast(0))
            }, locked?.let { CalculatorThemes.fromId(it).nameRes() }, ThemeRewardCoordinator.isBusy(),
        )
    }

    override fun onCleared() {
        preferences.preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        ThemeManager.removeAccessListener(accessListener)
    }

    class Factory(private val preferences: Preferences) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ThemeSelectorViewModel::class.java))
            return ThemeSelectorViewModel(preferences) as T
        }
    }
}
