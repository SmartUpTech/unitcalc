package net.smartlogic.unitconverter.theme;

import java.util.Set;

/** Selection and entitlement are independent. All times are epoch milliseconds. */
public final class ThemeAccess {
    public static final long REWARD_DURATION_MILLIS = 7L * 24 * 60 * 60 * 1000;
    public static final int VERSION = 1;
    private static final Set<String> FREE_IDS = Set.of("porcelain_white", "oyster_cream", "titanium_gray");

    private ThemeAccess() { }

    public static boolean isKnown(String id) {
        return id != null && CalculatorThemes.all().stream().anyMatch(theme -> theme.id().equals(id));
    }

    public static boolean isAlwaysFree(String id) { return FREE_IDS.contains(id == null ? "" : id); }

    public static boolean isAvailable(String id, boolean pro, long expiry, long now) {
        return isKnown(id) && (isAlwaysFree(id) || pro || expiry > now);
    }
}
