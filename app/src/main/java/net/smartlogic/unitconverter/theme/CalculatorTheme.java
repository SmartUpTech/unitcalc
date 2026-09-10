package net.smartlogic.unitconverter.theme;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

/**
 * Central calculator appearance. UI code must read these fields rather than
 * branching on theme id or name.
 */
public final class CalculatorTheme {

    @NonNull
    public final String id;
    @NonNull
    public final String name;
    @ColorInt
    public final int background;
    @ColorInt
    public final int mainText;
    @ColorInt
    public final int functions;
    @ColorInt
    public final int operators;
    @ColorInt
    public final int equal;

    public CalculatorTheme(
            @NonNull String id,
            @NonNull String name,
            @ColorInt int background,
            @ColorInt int mainText,
            @ColorInt int functions,
            @ColorInt int operators,
            @ColorInt int equal
    ) {
        this.id = id;
        this.name = name;
        this.background = background;
        this.mainText = mainText;
        this.functions = functions;
        this.operators = operators;
        this.equal = equal;
    }

    public boolean isLightBackground() {
        return ColorUtils.calculateLuminance(background) >= 0.5d;
    }

    /**
     * Operator glyph color: use the catalog operators hex when it contrasts
     * with the background; otherwise main text (operators is white in every
     * catalog theme, which is unreadable on light pads).
     */
    @ColorInt
    public int operatorContentColor() {
        if (ColorUtils.calculateContrast(operators, background) >= 3.0d) {
            return operators;
        }
        return mainText;
    }

    @ColorInt
    public int onChromeColor() {
        return mainText;
    }
}
