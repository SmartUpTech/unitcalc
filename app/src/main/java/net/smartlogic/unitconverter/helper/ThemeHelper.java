package net.smartlogic.unitconverter.helper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Locks the process to a single resource configuration so OEM force-dark /
 * DayNight cannot override {@link net.smartlogic.unitconverter.theme.CalculatorTheme}.
 */
public final class ThemeHelper {

    private ThemeHelper() {
    }

    public static void lockResourceNightMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    /**
     * @deprecated Appearance is controlled only by {@code selected_theme}.
     */
    @Deprecated
    public static void applyTheme(@NonNull String ignored) {
        lockResourceNightMode();
    }
}
