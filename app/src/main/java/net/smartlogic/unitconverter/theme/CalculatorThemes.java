package net.smartlogic.unitconverter.theme;

import net.smartlogic.unitconverter.theme.generated.ThemePalette;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.smartlogic.unitconverter.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class CalculatorThemes {

    public static final String DEFAULT_ID = "porcelain_white";

    public static final CalculatorTheme PORCELAIN_WHITE = theme(
            DEFAULT_ID, R.string.theme_porcelain_white, 1,
            ThemePalette.PALETTE_F7F6F2, ThemePalette.PALETTE_4A4C4F, ThemePalette.PALETTE_6B6D70, ThemePalette.PALETTE_FFFFFF, ThemePalette.PALETTE_303236);
    public static final CalculatorTheme OYSTER_CREAM = theme(
            "oyster_cream", R.string.theme_oyster_cream, 2,
            ThemePalette.PALETTE_F1ECE2, ThemePalette.PALETTE_4A4C4F, ThemePalette.PALETTE_686A6D, ThemePalette.PALETTE_FFFFFF, ThemePalette.PALETTE_303236);
    public static final CalculatorTheme TITANIUM_GRAY = theme(
            "titanium_gray", R.string.theme_titanium_gray, 3,
            ThemePalette.PALETTE_D6D9DD, ThemePalette.PALETTE_46484B, ThemePalette.PALETTE_595C60, ThemePalette.PALETTE_FFFFFF, ThemePalette.PALETTE_25272A);
    public static final CalculatorTheme GRAPHITE_BLACK = theme(
            "graphite_black", R.string.theme_graphite_black, 4,
            ThemePalette.PALETTE_25272A, ThemePalette.PALETTE_D0D2D4, ThemePalette.PALETTE_9EA1A5, ThemePalette.PALETTE_FFFFFF, ThemePalette.PALETTE_F7F7F7);
    public static final CalculatorTheme PURPLE_MOUNTAIN_MAJESTY = theme(
            "purple_mountain_majesty", R.string.theme_purple_mountain_majesty, 5,
            ThemePalette.PALETTE_9678B6, ThemePalette.PALETTE_18121F, ThemePalette.PALETTE_211A2A, ThemePalette.PALETTE_18121F, ThemePalette.PALETTE_18121F);
    public static final CalculatorTheme GLACIER = theme(
            "glacier", R.string.theme_glacier, 6,
            ThemePalette.PALETTE_B8D7E8, ThemePalette.PALETTE_3E464B, ThemePalette.PALETTE_4C5A63, ThemePalette.PALETTE_FFFFFF, ThemePalette.PALETTE_243F55);
    public static final CalculatorTheme BURGUNDY = theme(
            "burgundy", R.string.theme_burgundy, 7,
            ThemePalette.PALETTE_800020, ThemePalette.PALETTE_F0DDE2, ThemePalette.PALETTE_C5A9B1, ThemePalette.PALETTE_FFFFFF, ThemePalette.PALETTE_FFDCE5);
    public static final CalculatorTheme SAGE_GREEN = theme(
            "sage_green", R.string.theme_sage_green, 8,
            ThemePalette.PALETTE_9CAF88, ThemePalette.PALETTE_303A31, ThemePalette.PALETTE_303D2C, ThemePalette.PALETTE_FFFFFF, ThemePalette.PALETTE_293D2B);
    public static final CalculatorTheme BURNT_ORANGE = theme(
            "burnt_orange", R.string.theme_burnt_orange, 9,
            ThemePalette.PALETTE_AE4300, ThemePalette.PALETTE_FFF0E5, ThemePalette.PALETTE_FFE0C8, ThemePalette.PALETTE_FFFFFF, ThemePalette.PALETTE_FFE8D6);
    public static final CalculatorTheme GOLDEN_POPPY = theme(
            "golden_poppy", R.string.theme_golden_poppy, 10,
            ThemePalette.PALETTE_FCC200, ThemePalette.PALETTE_39372F, ThemePalette.PALETTE_5C512C, ThemePalette.PALETTE_FFFFFF, ThemePalette.PALETTE_483B0A);

    // Permanent unlock order. Never sort by selection, availability or remaining days.
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

    // Never renumber/reuse ranks when themes are reordered or removed. Append new ranks.
    public static Map<String, Integer> unlockRanks() {
        return Collections.unmodifiableMap(ALL.stream().collect(
                Collectors.toMap(CalculatorTheme::id, CalculatorTheme::unlockRank)));
    }

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
            if (theme.id().equals(id)) {
                return theme;
            }
        }
        return defaultTheme();
    }

    @NonNull
    private static CalculatorTheme theme(
            @NonNull String id,
            @StringRes int nameRes,
            int unlockRank,
            int background,
            int mainText,
            int functions,
            int operators,
            int equal
    ) {
        return new CalculatorTheme(
                id,
                nameRes,
                unlockRank,
                background,
                mainText,
                functions,
                operators,
                equal
        );
    }
}
