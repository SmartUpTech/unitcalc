package net.smartlogic.unitconverter.graphy.theme;

import android.graphics.Color;
import androidx.core.graphics.ColorUtils;
import net.smartlogic.unitconverter.graphy.model.GraphyNodeType;
import net.smartlogic.unitconverter.theme.CalculatorTheme;

/** One semantic color contract for Canvas and Compose, resolved from the app theme. */
public final class GraphySemanticStyle {
    private GraphySemanticStyle() { }

    public static int fill(CalculatorTheme theme, GraphyNodeType type) {
        return switch (type) {
            case INPUT -> ColorUtils.blendARGB(theme.background(), theme.mainText(), 0.08f);
            case CONSTANT -> ColorUtils.blendARGB(theme.background(), theme.functions(), 0.12f);
            case OPERATION -> ColorUtils.blendARGB(theme.background(), theme.operatorContentColor(), 0.08f);
            case DERIVED, DECISION -> ColorUtils.blendARGB(theme.background(), theme.functions(), 0.20f);
            case RESULT -> theme.equal();
            default -> theme.background();
        };
    }

    public static int content(CalculatorTheme theme, GraphyNodeType type) {
        int surface = fill(theme, type);
        int preferred = type == GraphyNodeType.RESULT ? theme.background() : theme.mainText();
        return readable(preferred, surface);
    }

    /** Preserve a theme role when it has normal-text contrast; otherwise use readable ink. */
    public static int readable(int preferred, int surface) {
        if (ColorUtils.calculateContrast(preferred, surface) >= 4.5d) return preferred;
        return ColorUtils.calculateContrast(Color.BLACK, surface)
                >= ColorUtils.calculateContrast(Color.WHITE, surface) ? Color.BLACK : Color.WHITE;
    }

    public static int connector(CalculatorTheme theme) {
        return readable(theme.functions(), theme.background());
    }
}
