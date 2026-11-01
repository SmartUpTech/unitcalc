package net.smartlogic.unitconverter.graphy.theme;

import androidx.core.graphics.ColorUtils;
import net.smartlogic.unitconverter.graphy.model.GraphyNodeType;
import net.smartlogic.unitconverter.theme.CalculatorTheme;

/** One semantic color contract for Canvas and Compose, resolved from the app theme. */
public final class GraphySemanticStyle {
    private GraphySemanticStyle() { }

    public static boolean hasDashedBorder(GraphyNodeType type) {
        return type == GraphyNodeType.DERIVED;
    }

    public static int fill(CalculatorTheme theme, GraphyNodeType type) {
        return switch (type) {
            case INPUT, CONSTANT -> theme.background();
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

    public static int border(CalculatorTheme theme, GraphyNodeType type) {
        return readable(connector(theme), fill(theme, type));
    }

    /** Preserve a theme role when it has normal-text contrast; otherwise use readable ink. */
    public static int readable(int preferred, int surface) {
        return CalculatorTheme.readableColor(preferred, surface);
    }

    public static int connector(CalculatorTheme theme) {
        return theme.secondaryContentColor();
    }
}
