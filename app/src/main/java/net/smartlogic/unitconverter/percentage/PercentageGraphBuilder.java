package net.smartlogic.unitconverter.percentage;

import android.content.Context;
import java.util.Arrays;
import java.util.Map;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.graphy.model.GraphyConnection;
import net.smartlogic.unitconverter.graphy.model.GraphyNode;
import net.smartlogic.unitconverter.graphy.model.GraphyNodeType;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.model.CalculatorCatalog;

public final class PercentageGraphBuilder {
    private PercentageGraphBuilder() {
    }

    public static GraphyOutput build(Context context, PercentageCalculator.Result result, String formattedResult) {
        String rule = context.getString(result.mode() == PercentageCalculator.Mode.WHAT_PERCENT
                ? R.string.percentage_graphy_step_what : R.string.percentage_graphy_rule);
        String firstLabel;
        String secondLabel;
        String firstValue = plain(result.inputs().first());
        String secondValue = plain(result.inputs().second());
        String step;
        switch (result.mode()) {
            case PERCENT_OF -> {
                firstLabel = context.getString(R.string.percentage_base_amount);
                secondLabel = context.getString(R.string.percentage_percent_label);
                secondValue = secondValue + "%";
                step = context.getString(R.string.percentage_graphy_step_of);
            }
            case INCREASE -> {
                firstLabel = context.getString(R.string.percentage_base_amount);
                secondLabel = context.getString(R.string.percentage_percent_label);
                secondValue = secondValue + "%";
                step = context.getString(R.string.percentage_graphy_step_increase);
            }
            case DECREASE -> {
                firstLabel = context.getString(R.string.percentage_base_amount);
                secondLabel = context.getString(R.string.percentage_percent_label);
                secondValue = secondValue + "%";
                step = context.getString(R.string.percentage_graphy_step_decrease);
            }
            case WHAT_PERCENT -> {
                firstLabel = context.getString(R.string.percentage_part_label);
                secondLabel = context.getString(R.string.percentage_whole_label);
                step = context.getString(R.string.percentage_graphy_step_what);
            }
            default -> throw new IllegalStateException();
        }
        String amount = plain(result.amount()) + (result.mode() == PercentageCalculator.Mode.WHAT_PERCENT ? "%" : "");
        String summary = formattedResult;
        String contextLine = firstValue + " · " + secondValue;
        boolean adjustment = result.mode() == PercentageCalculator.Mode.INCREASE
                || result.mode() == PercentageCalculator.Mode.DECREASE;
        var nodes = new java.util.ArrayList<GraphyNode>(Arrays.asList(
                node("first", GraphyNodeType.INPUT, firstLabel, firstValue, "input"),
                node("second", GraphyNodeType.INPUT, secondLabel, secondValue, "input"),
                node("op", GraphyNodeType.OPERATION,
                        adjustment ? context.getString(R.string.percentage_graphy_step_of) : step,
                        adjustment ? context.getString(R.string.percentage_graphy_step_of) : step,
                        "operation", amount, rule),
                node("derived", GraphyNodeType.DERIVED, context.getString(R.string.percentage_amount_label),
                        amount, "derived", amount, rule)));
        var edges = new java.util.ArrayList<GraphyConnection>(Arrays.asList(
                edge("first", "op"), edge("second", "op"), edge("op", "derived")));
        if (adjustment) {
            nodes.add(node("adjust", GraphyNodeType.OPERATION, step, step, "operation", summary, rule));
            edges.add(edge("first", "adjust"));
            edges.add(edge("derived", "adjust"));
            edges.add(edge("adjust", "result"));
        } else {
            edges.add(edge("derived", "result"));
        }
        nodes.add(node("result", GraphyNodeType.RESULT, context.getString(R.string.percentage_result_label),
                summary, "result", null, rule));
        return GraphyOutput.create(
                CalculatorCatalog.ID_PERCENTAGE,
                contextLine,
                summary,
                nodes,
                edges,
                rule,
                Map.of(GraphyOutput.METADATA_CONTEXT, contextLine));
    }

    private static String plain(java.math.BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static GraphyNode node(String id, GraphyNodeType type, String label, String value, String role) {
        return new GraphyNode(id, type, label, value, role);
    }

    private static GraphyNode node(String id, GraphyNodeType type, String label, String value, String role,
            String formula, String description) {
        return new GraphyNode(id, type, label, value, role, formula, description);
    }

    private static GraphyConnection edge(String from, String to) {
        return new GraphyConnection(from, to, null);
    }
}
