package net.smartlogic.unitconverter.fuel;

import android.content.Context;
import java.util.Arrays;
import java.util.Map;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.emi.EmiFormatter;
import net.smartlogic.unitconverter.graphy.model.GraphyConnection;
import net.smartlogic.unitconverter.graphy.model.GraphyNode;
import net.smartlogic.unitconverter.graphy.model.GraphyNodeType;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.model.CalculatorCatalog;

public final class FuelEfficiencyGraphBuilder {
    private FuelEfficiencyGraphBuilder() {
    }

    public static GraphyOutput build(Context context, FuelEfficiencyCalculator.Result result) {
        EmiFormatter money = new EmiFormatter(context);
        String rule = context.getString(R.string.fuel_graphy_rule);
        String distance = context.getString(
                R.string.fuel_graphy_distance,
                result.inputs().distance().stripTrailingZeros().toPlainString(),
                result.inputs().distanceUnit().getSymbol(),
                result.inputs().roundTrip() ? context.getString(R.string.fuel_round_trip_on) : "");
        String fuel = context.getString(R.string.fuel_graphy_fuel, formatVolume(context, result.fuelLitres()));
        String cost = context.getString(
                R.string.fuel_graphy_cost,
                money.money(result.tripCost(), result.inputs().currencyCode()));
        String share = context.getString(
                R.string.fuel_graphy_share,
                result.inputs().passengers(),
                money.money(result.costPerPassenger(), result.inputs().currencyCode()));
        String summary = money.money(result.costPerPassenger(), result.inputs().currencyCode());
        return GraphyOutput.create(
                CalculatorCatalog.ID_FUEL_EFFICIENCY,
                distance,
                summary,
                Arrays.asList(
                        node("distance", GraphyNodeType.INPUT, context.getString(R.string.fuel_trip_distance), result.distanceKm().stripTrailingZeros().toPlainString() + " km", "input"),
                        node("efficiency", GraphyNodeType.INPUT, context.getString(R.string.fuel_efficiency_label),
                                result.litresPer100Km().stripTrailingZeros().toPlainString() + " L/100 km",
                                "input"),
                        node("price", GraphyNodeType.INPUT, context.getString(R.string.fuel_price_label),
                                money.money(result.inputs().pricePerLitre(), result.inputs().currencyCode()) + "/L",
                                "input"),
                        node("fuel_op", GraphyNodeType.OPERATION, context.getString(R.string.fuel_graphy_step_fuel),
                                context.getString(R.string.fuel_graphy_step_fuel), "operation", fuel, rule),
                        node("fuel", GraphyNodeType.DERIVED, context.getString(R.string.fuel_needed_label), fuel, "derived", fuel, rule),
                        node("cost_op", GraphyNodeType.OPERATION, context.getString(R.string.fuel_graphy_step_cost),
                                context.getString(R.string.fuel_graphy_step_cost), "operation", cost, rule),
                        node("cost", GraphyNodeType.DERIVED, context.getString(R.string.fuel_trip_cost_label), cost, "derived", cost, rule),
                        node("passengers", GraphyNodeType.INPUT, context.getString(R.string.fuel_passengers),
                                Integer.toString(result.inputs().passengers()), "input"),
                        node("share_op", GraphyNodeType.OPERATION, context.getString(R.string.fuel_graphy_step_share),
                                context.getString(R.string.fuel_graphy_step_share), "operation", share, rule),
                        node("result", GraphyNodeType.RESULT, context.getString(R.string.fuel_cost_per_passenger), summary, "result", null, rule)),
                Arrays.asList(
                        edge("distance", "fuel_op"),
                        edge("efficiency", "fuel_op"),
                        edge("fuel_op", "fuel"),
                        edge("price", "cost_op"),
                        edge("fuel", "cost_op"),
                        edge("cost_op", "cost"),
                        edge("cost", "share_op"),
                        edge("passengers", "share_op"),
                        edge("share_op", "result")),
                rule,
                Map.of(GraphyOutput.METADATA_CONTEXT, distance));
    }

    static String formatVolume(Context context, java.math.BigDecimal litres) {
        var preferences = net.smartlogic.unitconverter.helper.Preferences.getInstance(context);
        String decimal = preferences.getDecimalSeparator(), group = preferences.getGroupSeparator();
        char dec = decimal.isEmpty() ? '.' : decimal.charAt(0);
        char sep = group.isEmpty() ? ',' : group.charAt(0);
        boolean grouping = !group.isEmpty() && !"None".equalsIgnoreCase(group)
                && !group.equals(context.getString(R.string.group_separator_none)) && dec != sep;
        String number = net.smartlogic.unitconverter.utils.NumberUtils.formatCalculator(litres,
                preferences.getNumberDecimals(), dec, sep, grouping);
        return context.getString(R.string.fuel_volume, number);
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
