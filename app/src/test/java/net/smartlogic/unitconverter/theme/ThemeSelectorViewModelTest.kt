package net.smartlogic.unitconverter.theme

import android.app.Application
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
        ThemeManager.init(RuntimeEnvironment.getApplication())
        model = ThemeSelectorViewModel(preferences)
        store = ViewModelStore().apply { put("theme", model) }
    }

    @After fun teardown() = store.clear()

    @Test fun everyInitialUnlockedThemeCanBeSelectedAndEveryLockedTapIsRejected() {
        val initialCards = model.uiState.value.cards
        assertEquals(3, initialCards.count { it.unlocked })
        initialCards.filter { it.unlocked }.forEach { card ->
            assertTrue(model.requestTheme(card.theme.id()))
            ThemeManager.select(RuntimeEnvironment.getApplication(), card.theme.id())
            assertEquals(card.theme.id(), model.uiState.value.cards.single { it.selected }.theme.id())
        }
        val selectedId = preferences.selectedThemeId
        initialCards.filterNot { it.unlocked }.forEach { card ->
            assertFalse(model.requestTheme(card.theme.id()))
            assertEquals(card.theme.nameRes(), model.uiState.value.lockedThemeNameRes)
            assertEquals((card.theme.unlockRank() - 3) * 3, model.uiState.value.daysUntilTappedUnlock)
            assertEquals(selectedId, preferences.selectedThemeId)
            assertEquals(selectedId, model.uiState.value.cards.single { it.selected }.theme.id())
        }
    }

    @Test fun dialogCountdownUsesTappedThemeAndUpdatesWhileOpen() {
        val day = java.time.LocalDate.of(2026, 10, 4)
        assertFalse(model.requestTheme(CalculatorThemes.GRAPHITE_BLACK.id()))
        preferences.recordThemeUsage(day)
        assertEquals(2, model.uiState.value.daysUntilTappedUnlock)
        preferences.recordThemeUsage(day.plusDays(1))
        assertEquals(1, model.uiState.value.daysUntilTappedUnlock)
        preferences.recordThemeUsage(day.plusDays(2))
        assertNull(model.uiState.value.lockedThemeNameRes)
        assertTrue(model.uiState.value.cards.single { it.theme.id() == CalculatorThemes.GRAPHITE_BLACK.id() }.unlocked)
        assertEquals(CalculatorThemes.DEFAULT_ID, preferences.selectedThemeId)

        assertFalse(model.requestTheme(CalculatorThemes.PURPLE_MOUNTAIN_MAJESTY.id()))
        assertEquals(3, model.uiState.value.daysUntilTappedUnlock)
    }

    @Test fun lockedTapShowsCorrectDialogWithoutChangingSelection() {
        assertFalse(model.requestTheme(CalculatorThemes.GRAPHITE_BLACK.id()))
        assertEquals(CalculatorThemes.GRAPHITE_BLACK.nameRes(), model.uiState.value.lockedThemeNameRes)
        assertEquals(3, model.uiState.value.daysUntilTappedUnlock)
        assertEquals(CalculatorThemes.DEFAULT_ID, preferences.selectedThemeId)
        model.dismissLockedTheme()
        assertNull(model.uiState.value.lockedThemeNameRes)
    }

    @Test fun liveUsageUpdatesCardsAndSingularRemainingDay() {
        val day = java.time.LocalDate.of(2026, 10, 4)
        preferences.recordThemeUsage(day)
        preferences.recordThemeUsage(day.plusDays(1))
        assertFalse(model.requestTheme(CalculatorThemes.GRAPHITE_BLACK.id()))
        assertEquals(1, model.uiState.value.daysUntilTappedUnlock)
        assertEquals(2, model.uiState.value.progress)
        preferences.recordThemeUsage(day.plusDays(2))
        assertTrue(model.requestTheme(CalculatorThemes.GRAPHITE_BLACK.id()))
        assertNull(model.uiState.value.lockedThemeNameRes)
        assertEquals(4, model.uiState.value.cards.count { it.unlocked })
        assertEquals(CalculatorThemes.DEFAULT_ID, model.uiState.value.cards.single { it.selected }.theme.id())
    }

    @Test fun selectingAvailableThemeUpdatesSnapshotAndSharedManager() {
        assertTrue(model.requestTheme(CalculatorThemes.OYSTER_CREAM.id()))
        ThemeManager.select(RuntimeEnvironment.getApplication(), CalculatorThemes.OYSTER_CREAM.id())
        assertEquals(CalculatorThemes.OYSTER_CREAM.id(), model.uiState.value.cards.single { it.selected }.theme.id())
        assertEquals(CalculatorThemes.OYSTER_CREAM.id(), ThemeManager.get().id())
        ThemeManager.select(RuntimeEnvironment.getApplication(), CalculatorThemes.GRAPHITE_BLACK.id())
        assertEquals(CalculatorThemes.OYSTER_CREAM.id(), ThemeManager.get().id())
    }
}
