package net.smartlogic.unitconverter.graphy.theme;

import android.content.Context;
import androidx.annotation.ColorInt;
import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.theme.ThemeManager;
import net.smartlogic.unitconverter.graphy.model.GraphyNode;
import net.smartlogic.unitconverter.graphy.model.GraphyNodeType;

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
        return ThemeManager.get().background();
    }

    @ColorInt
    public int getPrimaryTextColor() {
        return ThemeManager.get().mainText();
    }

    @ColorInt
    public int getConnectorColor() {
        return GraphySemanticStyle.connector(ThemeManager.get());
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

    public int nodeFill(GraphyNodeType type) {
        return GraphySemanticStyle.fill(ThemeManager.get(), type);
    }

    public int nodeContent(GraphyNodeType type) {
        return GraphySemanticStyle.content(ThemeManager.get(), type);
    }

    public String nodeRole(GraphyNodeType type) {
        return context.getString(switch (type) {
            case INPUT -> R.string.graphy_role_input;
            case CONSTANT -> R.string.graphy_role_constant;
            case OPERATION -> R.string.graphy_role_operation;
            case DERIVED -> R.string.graphy_role_derived;
            case RESULT -> R.string.graphy_role_result;
            case DECISION -> R.string.graphy_role_decision;
            case VISUALIZATION -> R.string.graphy_role_visualization;
            case EXPLANATION -> R.string.graphy_role_explanation;
        });
    }

    public String nodeCaption(GraphyNode node) {
        return node.label().equals(node.displayValue()) || node.type() == GraphyNodeType.OPERATION
                ? nodeRole(node.type()) : node.label();
    }

    public android.graphics.Typeface getTypeface() {
        return androidx.core.content.res.ResourcesCompat.getFont(context, R.font.app_text);
    }

    public float getNodePadding() { return dimen(R.dimen.graphy_spacing_sm); }
    public float getLabelTextSize() { return dimen(R.dimen.graphy_label_text_size); }
    public float getConnectorClearance() { return dimen(R.dimen.graphy_connector_clearance); }
    public float getArrowSize() { return dimen(R.dimen.graphy_arrow_size); }
    public float getArrowLength() { return dimen(R.dimen.graphy_arrow_length); }

    private float dimen(@DimenRes int dimenRes) {
        return context.getResources().getDimension(dimenRes);
    }
}
