package net.smartlogic.unitconverter.theme;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.graphics.ColorUtils;
import net.smartlogic.unitconverter.theme.generated.ThemePalette;

/**
 * Central calculator appearance. UI code must read these fields rather than
 * branching on theme id or name.
 */
public record CalculatorTheme(@NonNull String id, @StringRes int nameRes, int unlockRank, @ColorInt int background,
                              @ColorInt int mainText, @ColorInt int functions,
                              @ColorInt int operators, @ColorInt int equal) {

    public boolean isLightBackground() {
        return ColorUtils.calculateContrast(ThemePalette.PALETTE_000000, background)
                >= ColorUtils.calculateContrast(ThemePalette.PALETTE_FFFFFF, background);
    }

    @ColorInt public int primaryContentColor() { return readableColor(mainText, background); }

    @ColorInt public int secondaryContentColor() { return readableColor(functions, background); }

    public enum StatusTone { LOW, POSITIVE, CAUTION, WARNING, DANGER, CRITICAL }

    /** Shared status ink retains its hue while meeting text contrast on every app palette. */
    @ColorInt public int statusContentColor(StatusTone tone) {
        int preferred = switch (tone) {
            case LOW -> ThemePalette.PALETTE_1565C0;
            case POSITIVE -> ThemePalette.PALETTE_137D50;
            case CAUTION -> ThemePalette.PALETTE_B45A00;
            case WARNING -> ThemePalette.PALETTE_C43E24;
            case DANGER -> ThemePalette.PALETTE_AD244B;
            case CRITICAL -> ThemePalette.PALETTE_8228A8;
        };
        int endpoint = isLightBackground() ? ThemePalette.PALETTE_000000 : ThemePalette.PALETTE_FFFFFF;
        for (int step = 0; step <= 20; step++) {
            int candidate = ColorUtils.blendARGB(preferred, endpoint, step / 20f);
            if (ColorUtils.calculateContrast(candidate, background) >= 4.5d) return candidate;
        }
        return readableColor(preferred, background);
    }

    /** Foreground accent for equals, selected tabs, navigation and action text. */
    @ColorInt public int accentContentColor() { return readableColor(equal, background); }

    /** Content on an accent-filled segment, button or result. */
    @ColorInt public int onAccentColor() { return readableColor(background, accentContentColor()); }

    /** Also covers smaller operator tokens in expressions, not only large keypad glyphs. */
    @ColorInt
    public int operatorContentColor() {
        if (ColorUtils.calculateContrast(operators, background) >= 4.5d) {
            return operators;
        }
        return primaryContentColor();
    }

    @ColorInt
    public int onChromeColor() {
        return primaryContentColor();
    }

    /** One contrast policy for XML, Compose and Graphy, including tinted surfaces. */
    @ColorInt
    public static int readableColor(@ColorInt int preferred, @ColorInt int surface) {
        if (ColorUtils.calculateContrast(preferred, surface) >= 4.5d) return preferred;
        return ColorUtils.calculateContrast(ThemePalette.PALETTE_000000, surface)
                >= ColorUtils.calculateContrast(ThemePalette.PALETTE_FFFFFF, surface)
                ? ThemePalette.PALETTE_000000 : ThemePalette.PALETTE_FFFFFF;
    }
}
