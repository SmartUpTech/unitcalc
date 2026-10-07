package net.smartlogic.unitconverter.theme;

import android.app.Application;
import android.content.SharedPreferences;

import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class ThemePreferencesTest {
    private Preferences preferences;
    private SharedPreferences store;
    private final ThemeProgression progression = new ThemeProgression(CalculatorThemes.unlockRanks());
    private static final LocalDate DAY = LocalDate.of(2026, 10, 4);

    @Before public void setup() {
        preferences = Preferences.getInstance(RuntimeEnvironment.getApplication());
        store = preferences.getPreferences();
        store.edit().clear().commit();
        ThemeManager.init(RuntimeEnvironment.getApplication());
    }

    @Test public void freshInstallationHasExactlyThreeSelectableThemes() {
        assertEquals(3, progression.getUnlockedThemeCount(preferences.getThemeProgression()));
        assertEquals(CalculatorThemes.DEFAULT_ID, preferences.getSelectedThemeId());
        assertFalse(preferences.setSelectedThemeId(CalculatorThemes.GRAPHITE_BLACK.id()));
        assertEquals(CalculatorThemes.DEFAULT_ID, preferences.getSelectedThemeId());
    }

    @Test public void selectionAndUsageSurviveRestoredPreferenceSnapshotAndReboot() {
        preferences.recordThemeUsage(DAY);
        preferences.recordThemeUsage(DAY.plusDays(1));
        preferences.recordThemeUsage(DAY.plusDays(2));
        assertTrue(preferences.setSelectedThemeId(CalculatorThemes.GRAPHITE_BLACK.id()));
        Map<String, ?> backup = new HashMap<>(store.getAll());
        store.edit().clear().commit();
        SharedPreferences.Editor restore = store.edit();
        for (Map.Entry<String, ?> entry : backup.entrySet()) {
            if (entry.getValue() instanceof String) restore.putString(entry.getKey(), (String) entry.getValue());
            else if (entry.getValue() instanceof Integer) restore.putInt(entry.getKey(), (Integer) entry.getValue());
        }
        restore.commit();
        ThemeManager.init(RuntimeEnvironment.getApplication());
        preferences.recordThemeUsage(DAY.plusDays(2));
        preferences.recordThemeUsage(DAY.minusDays(1));
        assertEquals(3, preferences.getThemeProgression().totalUsageDays());
        assertEquals(CalculatorThemes.GRAPHITE_BLACK.id(), ThemeManager.get().id());
    }

    @Test public void unlockingNeverAutomaticallySelectsTheme() {
        preferences.setSelectedThemeId(CalculatorThemes.OYSTER_CREAM.id());
        for (int day = 0; day < 3; day++) preferences.recordThemeUsage(DAY.plusDays(day));
        assertTrue(progression.isThemeUnlocked(CalculatorThemes.GRAPHITE_BLACK.id(), preferences.getThemeProgression()));
        assertEquals(CalculatorThemes.OYSTER_CREAM.id(), preferences.getSelectedThemeId());
    }

    @Test public void duplicateLaunchDoesNotWritePreferences() {
        preferences.recordThemeUsage(DAY);
        AtomicInteger changes = new AtomicInteger();
        SharedPreferences.OnSharedPreferenceChangeListener listener = (prefs, key) -> changes.incrementAndGet();
        store.registerOnSharedPreferenceChangeListener(listener);
        preferences.recordThemeUsage(DAY);
        preferences.recordThemeUsage(DAY.minusDays(1));
        assertEquals(0, changes.get());
        store.unregisterOnSharedPreferenceChangeListener(listener);
    }

    @Test public void overlappingLaunchRegistrationCountsOnlyOnce() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Future<?>> results = new ArrayList<>();
            for (int launch = 0; launch < 8; launch++) {
                results.add(executor.submit(() -> preferences.recordThemeUsage(DAY)));
            }
            for (Future<?> result : results) result.get();
            assertEquals(1, preferences.getThemeProgression().totalUsageDays());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test public void lockedDialogPluralResourcesUseSingularForOneDay() {
        String singular = RuntimeEnvironment.getApplication().getResources()
                .getQuantityString(R.plurals.theme_locked_message, 1, 1);
        String plural = RuntimeEnvironment.getApplication().getResources()
                .getQuantityString(R.plurals.theme_locked_message, 2, 2);
        assertTrue(singular.contains("1 more usage day."));
        assertTrue(plural.contains("2 more usage days."));
        assertTrue(singular.contains("Calculator+"));
        assertTrue(plural.contains("Calculator+"));
    }

    @Test public void updateGrandfathersLegacyThemeEvenAfterSelectingAnotherTheme() {
        store.edit().clear().putString(Preferences.PREFS_SELECTED_THEME, CalculatorThemes.GOLDEN_POPPY.id()).commit();
        ThemeManager.init(RuntimeEnvironment.getApplication());
        assertEquals(CalculatorThemes.GOLDEN_POPPY.id(), ThemeManager.get().id());
        assertEquals(0, preferences.getThemeProgression().totalUsageDays());
        assertTrue(preferences.setSelectedThemeId(CalculatorThemes.DEFAULT_ID));
        assertTrue(preferences.setSelectedThemeId(CalculatorThemes.GOLDEN_POPPY.id()));
        preferences.recordThemeUsage(DAY);
        assertEquals(CalculatorThemes.GOLDEN_POPPY.id(), preferences.getSelectedThemeId());
        assertFalse(preferences.setSelectedThemeId(CalculatorThemes.BURGUNDY.id()));
    }

    @Test public void unknownRemovedAndWrongTypeSelectedIdsFallBackSafely() {
        store.edit().putString(Preferences.PREFS_SELECTED_THEME, "removed_theme").commit();
        ThemeManager.init(RuntimeEnvironment.getApplication());
        assertEquals(CalculatorThemes.DEFAULT_ID, ThemeManager.get().id());
        assertEquals(CalculatorThemes.DEFAULT_ID, store.getString(Preferences.PREFS_SELECTED_THEME, null));
        store.edit().putInt(Preferences.PREFS_SELECTED_THEME, 42).commit();
        ThemeManager.init(RuntimeEnvironment.getApplication());
        assertEquals(CalculatorThemes.DEFAULT_ID, ThemeManager.get().id());
    }

    @Test public void invalidProgressionTypesAndDateDoNotCrash() {
        store.edit().putString(Preferences.PREFS_THEME_USAGE_DAYS, "invalid")
                .putString("theme_last_usage_date", "invalid-date")
                .putString("theme_progression_version", "invalid").commit();
        assertEquals(1, preferences.recordThemeUsage(DAY).totalUsageDays());
        assertEquals(DAY, preferences.getThemeProgression().lastCountedDate());
    }

    @Test public void catalogHasUniqueStableIdsAndUnlockRanks() {
        assertEquals(CalculatorThemes.all().size(), CalculatorThemes.unlockRanks().size());
        assertEquals(CalculatorThemes.all().size(), CalculatorThemes.all().stream()
                .map(CalculatorTheme::unlockRank).distinct().count());
        assertEquals(3, progression.getUnlockedThemeCount(preferences.getThemeProgression()));
    }
}
