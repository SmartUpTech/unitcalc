package net.smartlogic.unitconverter.theme;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Pure usage policy. Theme ranks are permanent catalog metadata, never display indexes. */
public final class ThemeProgression {
    public static final int DEFAULT_UNLOCKED_COUNT = 3;
    public static final int DAYS_PER_UNLOCK = 3;
    public static final int VERSION = 1;

    public record State(int totalUsageDays, LocalDate lastCountedDate, int version,
                        String grandfatheredThemeId) {
        public State {
            totalUsageDays = Math.max(0, totalUsageDays);
        }
    }

    private final Map<String, Integer> unlockRanks;

    public ThemeProgression(Map<String, Integer> unlockRanks) {
        this.unlockRanks = Collections.unmodifiableMap(new HashMap<>(unlockRanks));
    }

    /** Preserve the legacy selection without inventing usage days or unlocking other themes. */
    public State migrate(State state, String selectedThemeId, boolean hasLegacySelection) {
        if (state.version() >= VERSION) return state;
        String grandfathered = state.grandfatheredThemeId();
        if (hasLegacySelection && unlockRanks.containsKey(selectedThemeId)) {
            grandfathered = selectedThemeId;
        }
        return new State(state.totalUsageDays(), state.lastCountedDate(), VERSION, grandfathered);
    }

    /** A high-water date rejects duplicates and clock rollback, including after restart/restore. */
    public State registerUsage(State state, LocalDate localDate) {
        Objects.requireNonNull(localDate);
        if (state.lastCountedDate() != null && !localDate.isAfter(state.lastCountedDate())) {
            return state;
        }
        int days = state.totalUsageDays() == Integer.MAX_VALUE
                ? Integer.MAX_VALUE : state.totalUsageDays() + 1;
        return new State(days, localDate, state.version(), state.grandfatheredThemeId());
    }

    public boolean isThemeUnlocked(String themeId, State state) {
        Integer rank = unlockRanks.get(themeId);
        return rank != null && (themeId.equals(state.grandfatheredThemeId())
                || rank <= DEFAULT_UNLOCKED_COUNT + (long) state.totalUsageDays() / DAYS_PER_UNLOCK);
    }

    public int getUnlockedThemeCount(State state) {
        return (int) unlockRanks.keySet().stream().filter(id -> isThemeUnlocked(id, state)).count();
    }

    public int getCurrentProgress(State state) {
        return state.totalUsageDays() % DAYS_PER_UNLOCK;
    }

    public int getDaysUntilUnlock(String themeId, State state) {
        if (!unlockRanks.containsKey(themeId) || isThemeUnlocked(themeId, state)) return 0;
        long requiredDays = (long) (unlockRanks.get(themeId) - DEFAULT_UNLOCKED_COUNT) * DAYS_PER_UNLOCK;
        return (int) Math.min(Integer.MAX_VALUE, requiredDays - state.totalUsageDays());
    }

    public String getNextLockedThemeId(State state) {
        return unlockRanks.entrySet().stream()
                .filter(entry -> !isThemeUnlocked(entry.getKey(), state))
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);
    }

    public int getDaysUntilNextUnlock(State state) {
        String next = getNextLockedThemeId(state);
        return next == null ? 0 : getDaysUntilUnlock(next, state);
    }
}
