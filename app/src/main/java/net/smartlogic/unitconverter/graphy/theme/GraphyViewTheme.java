package net.smartlogic.unitconverter.graphy.theme;

import android.content.Context;
import androidx.annotation.ColorInt;
import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.theme.ThemeManager;

/**
 * Typed accessor for Graphy design tokens.
 */
public final class GraphyViewTheme {

    private final Context context;

    public GraphyViewTheme(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @ColorInt
    public int getBackgroundColor() {
        return ThemeManager.get().background;
    }

    @ColorInt
    public int getPrimaryTextColor() {
        return ThemeManager.get().mainText;
    }

    @ColorInt
    public int getInputColor() {
        return color(R.color.graphy_input);
    }

    @ColorInt
    public int getConstantColor() {
        return color(R.color.graphy_constant);
    }

    @ColorInt
    public int getOperationColor() {
        return color(R.color.graphy_operation);
    }

    @ColorInt
    public int getDerivedColor() {
        return color(R.color.graphy_derived);
    }

    @ColorInt
    public int getResultFillColor() {
        return ThemeManager.get().equal;
    }

    @ColorInt
    public int getResultOnColor() {
        return ThemeManager.get().background;
    }

    @ColorInt
    public int getInputNodeColor() {
        return color(R.color.graphy_input_fill);
    }

    @ColorInt
    public int getOperationNodeColor() {
        return color(R.color.graphy_operation_fill);
    }

    @ColorInt
    public int getOperationStrokeColor() {
        return color(R.color.graphy_operation_stroke);
    }

    @ColorInt
    public int getResultNodeColor() {
        return ThemeManager.get().equal;
    }

    @ColorInt
    public int getDerivedNodeColor() {
        return color(R.color.graphy_derived);
    }

    @ColorInt
    public int getConnectorColor() {
        return ThemeManager.get().functions;
    }

    @ColorInt
    public int getConnectorHighlightColor() {
        return ThemeManager.get().functions;
    }

    @ColorInt
    public int getSurfaceColor() {
        return ThemeManager.get().background;
    }

    @ColorInt
    public int getSurfaceElevatedColor() {
        return ThemeManager.get().background;
    }

    @ColorInt
    public int getOnInputColor() {
        return color(R.color.graphy_on_input);
    }

    @ColorInt
    public int getOnConstantColor() {
        return color(R.color.graphy_on_constant);
    }

    @ColorInt
    public int getOnDerivedColor() {
        return color(R.color.graphy_on_derived);
    }

    @ColorInt
    public int getOnOperationColor() {
        return color(R.color.graphy_on_operation);
    }

    @ColorInt
    public int getOnResultColor() {
        return ThemeManager.get().background;
    }

    @ColorInt
    public int getPrimaryColor() {
        return color(R.color.graphy_primary);
    }

    @ColorInt
    public int getWarningColor() {
        return color(R.color.graphy_warning);
    }

    @ColorInt
    public int getTimeColor() {
        return color(R.color.graphy_time);
    }

    @ColorInt
    public int getSecondaryColor() {
        return ThemeManager.get().functions;
    }

    public float getNodeCornerRadius() {
        return dimen(R.dimen.graphy_node_corner_radius);
    }

    public float getConnectorStrokeWidth() {
        return dimen(R.dimen.graphy_connector_stroke);
    }

    public float getOperationMarkerSize() {
        return dimen(R.dimen.graphy_operation_marker_size);
    }

    public float getNodeMinWidth() {
        return dimen(R.dimen.graphy_node_min_width);
    }

    public float getNodeMinHeight() {
        return dimen(R.dimen.graphy_node_min_height);
    }

    public float getValueTextSize() {
        return dimen(R.dimen.graphy_value_text_size);
    }

    public float getOperationTextSize() {
        return dimen(R.dimen.graphy_operation_text_size);
    }

    public float getResultTextSize() {
        return dimen(R.dimen.graphy_result_text_size);
    }

    public float getVerticalGap() {
        return dimen(R.dimen.graphy_vertical_gap);
    }

    public float getHorizontalGap() {
        return dimen(R.dimen.graphy_horizontal_gap);
    }

    public float getBranchGap() {
        return dimen(R.dimen.graphy_branch_gap);
    }

    @StyleRes
    public int getNodeLabelStyle() {
        return R.style.GraphyText_NodeLabel;
    }

    @StyleRes
    public int getNodeValueStyle() {
        return R.style.GraphyText_NodeValue;
    }

    @StyleRes
    public int getOperationLabelStyle() {
        return R.style.GraphyText_OperationLabel;
    }

    @StyleRes
    public int getExplanationStyle() {
        return R.style.GraphyText_Explanation;
    }

    @ColorInt
    private int color(int colorRes) {
        return context.getColor(colorRes);
    }

    private float dimen(@DimenRes int dimenRes) {
        return context.getResources().getDimension(dimenRes);
    }
}
