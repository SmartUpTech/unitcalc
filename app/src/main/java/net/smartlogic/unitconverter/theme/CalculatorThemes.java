package net.smartlogic.unitconverter.theme;

import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class CalculatorThemes {

    public static final String DEFAULT_ID = "porcelain_white";

    public static final CalculatorTheme PORCELAIN_WHITE = theme(
            DEFAULT_ID, "Porcelain White",
            "#F7F6F2", "#4A4C4F", "#77797C", "#FFFFFF", "#303236");
    public static final CalculatorTheme OYSTER_CREAM = theme(
            "oyster_cream", "Oyster Cream",
            "#F1ECE2", "#4A4C4F", "#77797C", "#FFFFFF", "#303236");
    public static final CalculatorTheme TITANIUM_GRAY = theme(
            "titanium_gray", "Titanium Gray",
            "#D6D9DD", "#46484B", "#737579", "#FFFFFF", "#25272A");
    public static final CalculatorTheme GRAPHITE_BLACK = theme(
            "graphite_black", "Graphite Black",
            "#25272A", "#D0D2D4", "#9EA1A5", "#FFFFFF", "#F7F7F7");
    public static final CalculatorTheme PURPLE_MOUNTAIN_MAJESTY = theme(
            "purple_mountain_majesty", "Purple Mountain Majesty",
            "#9678B6", "#343238", "#64616A", "#FFFFFF", "#F1EAF7");
    public static final CalculatorTheme GLACIER = theme(
            "glacier", "Glacier",
            "#B8D7E8", "#3E464B", "#707980", "#FFFFFF", "#F3FAFD");
    public static final CalculatorTheme BURGUNDY = theme(
            "burgundy", "Burgundy",
            "#800020", "#F0DDE2", "#C5A9B1", "#FFFFFF", "#FFDCE5");
    public static final CalculatorTheme SAGE_GREEN = theme(
            "sage_green", "Sage Green",
            "#9CAF88", "#384039", "#687168", "#FFFFFF", "#F0F6EC");
    public static final CalculatorTheme BURNT_ORANGE = theme(
            "burnt_orange", "Burnt Orange",
            "#CC5500", "#FFF0E5", "#D8BBA7", "#FFFFFF", "#FFE8D6");
    public static final CalculatorTheme GOLDEN_POPPY = theme(
            "golden_poppy", "Golden Poppy",
            "#FCC200", "#39372F", "#706D62", "#FFFFFF", "#FFF7D6");

    private static final List<CalculatorTheme> ALL = Collections.unmodifiableList(Arrays.asList(
            PORCELAIN_WHITE,
            OYSTER_CREAM,
            TITANIUM_GRAY,
            GRAPHITE_BLACK,
            PURPLE_MOUNTAIN_MAJESTY,
            GLACIER,
            BURGUNDY,
            SAGE_GREEN,
            BURNT_ORANGE,
            GOLDEN_POPPY
    ));

    private CalculatorThemes() {
    }

    @NonNull
    public static List<CalculatorTheme> all() {
        return ALL;
    }

    @NonNull
    public static CalculatorTheme defaultTheme() {
        return PORCELAIN_WHITE;
    }

    @NonNull
    public static CalculatorTheme fromId(@Nullable String id) {
        if (id == null || id.isEmpty()) {
            return defaultTheme();
        }
        for (CalculatorTheme theme : ALL) {
            if (theme.id.equals(id)) {
                return theme;
            }
        }
        return defaultTheme();
    }

    @NonNull
    private static CalculatorTheme theme(
            @NonNull String id,
            @NonNull String name,
            @NonNull String background,
            @NonNull String mainText,
            @NonNull String functions,
            @NonNull String operators,
            @NonNull String equal
    ) {
        return new CalculatorTheme(
                id,
                name,
                Color.parseColor(background),
                Color.parseColor(mainText),
                Color.parseColor(functions),
                Color.parseColor(operators),
                Color.parseColor(equal)
        );
    }
}
