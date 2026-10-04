package net.smartlogic.unitconverter.theme;

import org.junit.Test;

import java.time.LocalDate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ThemeProgressionTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 4);
    private final ThemeProgression progression = new ThemeProgression(catalog(10));

    private static Map<String, Integer> catalog(int size) {
        Map<String, Integer> themes = new LinkedHashMap<>();
        for (int rank = 1; rank <= size; rank++) themes.put("theme_" + rank, rank);
        return themes;
    }

    private static ThemeProgression.State state(int days) {
        return new ThemeProgression.State(days, null, ThemeProgression.VERSION, null);
    }

    @Test public void unlockThresholds() {
        int[][] examples = {{0, 3}, {1, 3}, {2, 3}, {3, 4}, {5, 4}, {6, 5}, {9, 6}};
        for (int[] example : examples) {
            assertEquals("days=" + example[0], example[1], progression.getUnlockedThemeCount(state(example[0])));
        }
    }

    @Test public void cycleProgressAndRemainingDays() {
        for (int days = 0; days < 9; days++) {
            assertEquals(days % 3, progression.getCurrentProgress(state(days)));
            assertEquals(3 - days % 3, progression.getDaysUntilNextUnlock(state(days)));
        }
        assertEquals(20, progression.getDaysUntilUnlock("theme_10", state(1)));
    }

    @Test public void duplicateLaunchActivityRecreationAndProcessRestartDoNotCountAgain() {
        ThemeProgression.State counted = progression.registerUsage(state(0), DAY);
        assertEquals(1, counted.totalUsageDays());
        assertSame(counted, progression.registerUsage(counted, DAY));
        ThemeProgression.State restored = new ThemeProgression.State(counted.totalUsageDays(),
                counted.lastCountedDate(), counted.version(), counted.grandfatheredThemeId());
        assertSame(restored, progression.registerUsage(restored, DAY));
    }

    @Test public void consecutiveLaunchesUnlockFourthTheme() {
        ThemeProgression.State counted = state(0);
        for (int day = 0; day < 3; day++) counted = progression.registerUsage(counted, DAY.plusDays(day));
        assertEquals(3, counted.totalUsageDays());
        assertTrue(progression.isThemeUnlocked("theme_4", counted));
        assertFalse(progression.isThemeUnlocked("theme_5", counted));
    }

    @Test public void skippedDaysAndForwardClockOnlyCountOneActualLaunch() {
        ThemeProgression.State counted = progression.registerUsage(state(0), DAY);
        counted = progression.registerUsage(counted, DAY.plusYears(1));
        assertEquals(2, counted.totalUsageDays());
    }

    @Test public void backwardClockAndReturnToPreviouslyCountedDayCannotEarnUsage() {
        ThemeProgression.State counted = progression.registerUsage(state(0), DAY);
        assertSame(counted, progression.registerUsage(counted, DAY.minusDays(1)));
        assertSame(counted, progression.registerUsage(counted, DAY));
        assertEquals(2, progression.registerUsage(counted, DAY.plusDays(1)).totalUsageDays());
    }

    @Test public void crossingLocalMidnightCountsNextLaunch() {
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        LocalDate before = LocalDate.now(Clock.fixed(Instant.parse("2026-10-04T18:29:59Z"), zone));
        LocalDate after = LocalDate.now(Clock.fixed(Instant.parse("2026-10-04T18:30:00Z"), zone));
        ThemeProgression.State counted = progression.registerUsage(state(0), before);
        assertEquals(2, progression.registerUsage(counted, after).totalUsageDays());
    }

    @Test public void allThemesUnlockedAndMaximumCountHaveNoNextUnlock() {
        assertEquals(10, progression.getUnlockedThemeCount(state(21)));
        assertEquals(10, progression.getUnlockedThemeCount(state(Integer.MAX_VALUE)));
        assertNull(progression.getNextLockedThemeId(state(21)));
        assertEquals(0, progression.getDaysUntilNextUnlock(state(21)));
        assertEquals(Integer.MAX_VALUE,
                progression.registerUsage(state(Integer.MAX_VALUE), DAY).totalUsageDays());
    }

    @Test public void catalogSmallerThanDefaultCount() {
        assertEquals(2, new ThemeProgression(catalog(2)).getUnlockedThemeCount(state(0)));
        assertEquals(0, new ThemeProgression(catalog(0)).getUnlockedThemeCount(state(100)));
    }

    @Test public void newlyAddedThemeUsesEarnedUsageIncludingDaysAfterAllUnlocked() {
        ThemeProgression expanded = new ThemeProgression(catalog(11));
        assertFalse(expanded.isThemeUnlocked("theme_11", state(21)));
        assertEquals(3, expanded.getDaysUntilNextUnlock(state(21)));
        assertTrue(expanded.isThemeUnlocked("theme_11", state(24)));
    }

    @Test public void reorderingOrRemovingThemesDoesNotChangeOtherThemesAvailability() {
        Map<String, Integer> reordered = new LinkedHashMap<>();
        for (int rank = 10; rank >= 1; rank--) reordered.put("theme_" + rank, rank);
        ThemeProgression rearranged = new ThemeProgression(reordered);
        assertEquals("theme_5", rearranged.getNextLockedThemeId(state(3)));
        reordered.remove("theme_4");
        ThemeProgression removed = new ThemeProgression(reordered);
        assertTrue(removed.isThemeUnlocked("theme_5", state(6)));
        assertFalse(removed.isThemeUnlocked("theme_6", state(6)));
        assertFalse(removed.isThemeUnlocked("theme_4", state(6)));
    }

    @Test public void migrationGrandfathersSelectionWithoutInventingDays() {
        ThemeProgression.State legacy = new ThemeProgression.State(0, null, 0, null);
        ThemeProgression.State migrated = progression.migrate(legacy, "theme_10", true);
        assertEquals(0, migrated.totalUsageDays());
        assertEquals(4, progression.getUnlockedThemeCount(migrated));
        assertTrue(progression.isThemeUnlocked("theme_10", migrated));
        assertFalse(progression.isThemeUnlocked("theme_4", migrated));
        assertEquals("theme_4", progression.getNextLockedThemeId(migrated));
        assertSame(migrated, progression.migrate(migrated, "theme_1", true));
    }

    @Test public void freshOrUnknownLegacyThemeDoesNotGrantAdditionalAccess() {
        ThemeProgression.State legacy = new ThemeProgression.State(0, null, 0, null);
        assertEquals(3, progression.getUnlockedThemeCount(progression.migrate(legacy, "theme_10", false)));
        assertNull(progression.migrate(legacy, "removed_theme", true).grandfatheredThemeId());
        assertFalse(progression.isThemeUnlocked("unknown", state(100)));
    }

    @Test public void negativeCountIsSanitizedAndNewerVersionIsPreserved() {
        assertEquals(3, progression.getUnlockedThemeCount(state(-1)));
        ThemeProgression.State future = new ThemeProgression.State(4, DAY, 2, "theme_10");
        assertSame(future, progression.migrate(future, "theme_1", true));
    }
}
