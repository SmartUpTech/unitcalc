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
    private final net.smartlogic.unitconverter.theme.CalculatorTheme fixedPalette;

    private GraphyViewTheme(Context context, net.smartlogic.unitconverter.theme.CalculatorTheme palette) {
        this.context = context;
        this.fixedPalette = palette;
    }

    /** White paper without changing the user's selected theme, density or text scaling. */
    public static GraphyViewTheme forExport(Context context) {
        var config = new android.content.res.Configuration(context.getResources().getConfiguration());
        config.densityDpi = android.util.DisplayMetrics.DENSITY_DEFAULT;
        config.fontScale = 1f;
        Context paperContext = context.createConfigurationContext(config);
        var base = net.smartlogic.unitconverter.theme.CalculatorThemes.PORCELAIN_WHITE;
        var paper = new net.smartlogic.unitconverter.theme.CalculatorTheme(base.id(), base.nameRes(), base.unlockRank(),
                paperContext.getColor(R.color.graphy_export_paper), base.mainText(), base.functions(), base.operators(), base.equal());
        return new GraphyViewTheme(paperContext, paper);
    }

    private net.smartlogic.unitconverter.theme.CalculatorTheme palette() {
        return fixedPalette != null ? fixedPalette : ThemeManager.get();
    }

    public GraphyViewTheme(@NonNull Context context) {
        this.context = context.getApplicationContext();
        fixedPalette = null;
    }

    @ColorInt
    public int getBackgroundColor() {
        return palette().background();
    }

    @ColorInt
    public int getPrimaryTextColor() {
        return palette().primaryContentColor();
    }

    @ColorInt
    public int getConnectorColor() {
        return GraphySemanticStyle.connector(palette());
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

    public float getFoldIconSize() { return dimen(R.dimen.graphy_fold_icon_size); }
    public float getTouchTarget() { return dimen(R.dimen.graphy_touch_target); }
    public float getNodeVerticalPadding() { return dimen(R.dimen.graphy_node_vertical_padding); }

    public float getNodeMaxWidth() { return dimen(R.dimen.graphy_node_max_width); }
    public float getNodeBorderStroke() { return dimen(R.dimen.graphy_node_border_stroke); }
    public float getNodeFocusStroke() { return dimen(R.dimen.graphy_node_focus_stroke); }
    public float getNodeDashLength() { return dimen(R.dimen.graphy_node_dash_length); }
    public float getNodeDashGap() { return dimen(R.dimen.graphy_node_dash_gap); }

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
        return GraphySemanticStyle.fill(palette(), type);
    }

    public int nodeContent(GraphyNodeType type) {
        return GraphySemanticStyle.content(palette(), type);
    }

    public int nodeBorder(GraphyNodeType type) {
        return GraphySemanticStyle.border(palette(), type);
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
        return net.smartlogic.unitconverter.theme.AppearanceTypography.typeface(context);
    }

    public android.content.res.Resources getResources() { return context.getResources(); }

    public float getNodePadding() { return dimen(R.dimen.graphy_node_padding); }
    public float getLabelTextSize() { return dimen(R.dimen.graphy_label_text_size); }
    public float getConnectorClearance() { return dimen(R.dimen.graphy_connector_clearance); }
    public float getArrowSize() { return dimen(R.dimen.graphy_arrow_size); }
    public float getArrowLength() { return dimen(R.dimen.graphy_arrow_length); }

    private float dimen(@DimenRes int dimenRes) {
        return context.getResources().getDimension(dimenRes);
    }
}
