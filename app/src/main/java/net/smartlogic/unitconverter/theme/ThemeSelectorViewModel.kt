package net.smartlogic.unitconverter.theme

import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.smartlogic.unitconverter.helper.Preferences

@Immutable
data class ThemeCardState(val theme: CalculatorTheme, val selected: Boolean, val unlocked: Boolean)

@Immutable
data class ThemeSelectorState(
    val cards: List<ThemeCardState>,
    val progress: Int,
    val cycleDays: Int,
    val nextThemeNameRes: Int?,
    val daysUntilNextUnlock: Int,
    val lockedThemeNameRes: Int?,
    val daysUntilTappedUnlock: Int,
)

/** One derived snapshot for the entire row. Cards never read preferences. */
class ThemeSelectorViewModel(private val preferences: Preferences) : ViewModel() {
    private val progression = ThemeProgression(CalculatorThemes.unlockRanks())
    private var lockedThemeId: String? = null
    private val mutableState = MutableStateFlow(buildState())
    val uiState: StateFlow<ThemeSelectorState> = mutableState
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key == Preferences.PREFS_SELECTED_THEME || key == Preferences.PREFS_THEME_USAGE_DAYS) {
            refresh()
        }
    }

    init {
        preferences.preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        refresh()
    }

    /** True only for an available theme. Selection is committed through the shared ThemeManager. */
    fun requestTheme(themeId: String): Boolean {
        val state = preferences.themeProgression
        if (!progression.isThemeUnlocked(themeId, state)) {
            lockedThemeId = themeId.takeIf { id -> CalculatorThemes.all().any { it.id() == id } }
            refresh()
            return false
        }
        lockedThemeId = null
        refresh()
        return true
    }

    fun dismissLockedTheme() {
        lockedThemeId = null
        refresh()
    }

    private fun refresh() {
        mutableState.value = buildState()
    }

    private fun buildState(): ThemeSelectorState {
        val progressState = preferences.themeProgression
        val selectedId = preferences.selectedThemeId
        val nextId = progression.getNextLockedThemeId(progressState)
        val lockedId = lockedThemeId?.takeUnless { progression.isThemeUnlocked(it, progressState) }
        return ThemeSelectorState(
            cards = CalculatorThemes.all().map { theme ->
                ThemeCardState(theme, theme.id() == selectedId, progression.isThemeUnlocked(theme.id(), progressState))
            },
            progress = progression.getCurrentProgress(progressState),
            cycleDays = ThemeProgression.DAYS_PER_UNLOCK,
            nextThemeNameRes = nextId?.let { CalculatorThemes.fromId(it).nameRes() },
            daysUntilNextUnlock = progression.getDaysUntilNextUnlock(progressState),
            lockedThemeNameRes = lockedId?.let { CalculatorThemes.fromId(it).nameRes() },
            daysUntilTappedUnlock = lockedId?.let { progression.getDaysUntilUnlock(it, progressState) } ?: 0,
        )
    }

    override fun onCleared() {
        preferences.preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
    }

    class Factory(private val preferences: Preferences) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ThemeSelectorViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return ThemeSelectorViewModel(preferences) as T
        }
    }
}
