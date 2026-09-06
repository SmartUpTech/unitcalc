package net.smartlogic.unitconverter.graphy.theme;

import android.content.Context;
import androidx.annotation.ColorInt;
import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import net.smartlogic.unitconverter.R;

/**
 * Typed accessor for Graphy design tokens.
 */
public final class GraphyTheme {

    private final Context context;

    public GraphyTheme(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @ColorInt
    public int getPrimaryColor() {
        return color(R.color.graphy_primary);
    }

    @ColorInt
    public int getInputNodeColor() {
        return color(R.color.graphy_input);
    }

    @ColorInt
    public int getOperationNodeColor() {
        return color(R.color.graphy_operation);
    }

    @ColorInt
    public int getResultNodeColor() {
        return color(R.color.graphy_result);
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
        return color(R.color.graphy_secondary);
    }

    @ColorInt
    public int getConnectorColor() {
        return color(R.color.graphy_connector);
    }

    @ColorInt
    public int getConnectorHighlightColor() {
        return color(R.color.graphy_connector_highlight);
    }

    @ColorInt
    public int getSurfaceColor() {
        return color(R.color.graphy_surface);
    }

    @ColorInt
    public int getSurfaceElevatedColor() {
        return color(R.color.graphy_surface_elevated);
    }

    @ColorInt
    public int getDerivedNodeColor() {
        return color(R.color.graphy_surface_elevated);
    }

    @ColorInt
    public int getOnInputColor() {
        return color(R.color.graphy_on_input);
    }

    @ColorInt
    public int getOnOperationColor() {
        return color(R.color.graphy_on_operation);
    }

    @ColorInt
    public int getOnResultColor() {
        return color(R.color.graphy_on_result);
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
