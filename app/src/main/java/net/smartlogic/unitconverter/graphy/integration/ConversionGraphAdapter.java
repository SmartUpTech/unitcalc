package net.smartlogic.unitconverter.graphy.integration;

import android.content.Context;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.graphy.builder.ConversionGraphBuilder;
import net.smartlogic.unitconverter.graphy.model.ConversionTransformation;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.model.Conversion;
import net.smartlogic.unitconverter.model.Unit;
import net.smartlogic.unitconverter.utils.Conversions;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;

/** Domain metadata → localized explanation → common Graphy renderer. No result calculation. */
public final class ConversionGraphAdapter {
    private ConversionGraphAdapter() { }

    public static ConversionTransformation transformation(Conversions conversions, int category,
                                                           Unit from, Unit to) {
        if (from.getId() == to.getId()) return ConversionTransformation.linear(1);
        if (category == Conversion.TEMPERATURE) {
            if (from.getId() == Unit.GAS_MARK || to.getId() == Unit.GAS_MARK) {
                return ConversionTransformation.direct();
            }
            // Reuse the established affine converter itself. This avoids maintaining a second
            // temperature formula table, including the less common scales.
            double offset = conversions.convertTemperatureValue(0, from, to);
            double scale = (conversions.convertTemperatureValue(100, from, to) - offset) / 100;
            return ConversionTransformation.affine(scale, offset);
        }
        if (category == Conversion.FUEL) {
            if (from.getId() == Unit.L_100K) return ConversionTransformation.reciprocal(
                    from.getConversionToBaseUnit() * to.getConversionFromBaseUnit());
            if (to.getId() == Unit.L_100K) return ConversionTransformation.reciprocal(
                    to.getConversionFromBaseUnit() / from.getConversionToBaseUnit());
        }
        return ConversionTransformation.linear(from.getConversionToBaseUnit() * to.getConversionFromBaseUnit());
    }

    public static GraphyOutput unit(Context context, Conversions conversions, int category,
                                    Unit from, Unit to, double input, double result,
                                    String inputDisplay, String resultDisplay, String source, String target) {
        var transformation = transformation(conversions, category, from, to);
        String note = "";
        if (transformation.kind() == ConversionTransformation.Kind.DIRECT) {
            note = context.getString(R.string.graphy_piecewise);
        } else if (transformation.kind() == ConversionTransformation.Kind.RECIPROCAL) {
            note = context.getString(input == 0 ? R.string.graphy_zero_fuel : R.string.graphy_fuel_rounding);
            // Preserve the legacy converter's explicit zero behavior without implying division by zero.
            if (input == 0) transformation = ConversionTransformation.direct();
        }
        return build(context, ConversionGraphBuilder.UNIT_CONVERTER_ID,
                new ConversionSnapshot(input, result, inputDisplay, resultDisplay, source, target,
                        transformation, 0), note);
    }

    public static GraphyOutput currency(Context context, double input, double result, double rate,
                                        String inputDisplay, String resultDisplay, String source,
                                        String target, long timestamp) {
        String note = timestamp > 0 ? context.getString(R.string.graphy_rate_cached,
                DateFormat.getDateTimeInstance().format(new Date(timestamp)))
                : context.getString(R.string.graphy_rate_unknown_time);
        return build(context, ConversionGraphBuilder.CURRENCY_CONVERTER_ID,
                new ConversionSnapshot(input, result, inputDisplay, resultDisplay, source, target,
                        ConversionTransformation.rate(rate), timestamp), note);
    }

    private static GraphyOutput build(Context context, String id, ConversionSnapshot snapshot, String note) {
        var transformation = snapshot.transformation();
        // Use the entered text for presentation; widening a float input would expose binary
        // artifacts (for example 1.2f → 1.200000047...). This never re-parses numeric results.
        String shownInput = snapshot.sourceDisplay();
        String unitSuffix = " " + snapshot.sourceUnit();
        if (shownInput.endsWith(unitSuffix)) shownInput = shownInput.substring(0, shownInput.length() - unitSuffix.length());
        String formula = formula(context, snapshot, shownInput);
        String relationship;
        if (transformation.kind() == ConversionTransformation.Kind.DIRECT) {
            relationship = note;
        } else if (transformation.kind() == ConversionTransformation.Kind.LINEAR
                || transformation.kind() == ConversionTransformation.Kind.RATE_BASED) {
            relationship = context.getString(R.string.graphy_unit_relationship, snapshot.sourceUnit(),
                    value(context, transformation.steps().get(0).operand(), transformation.kind()), snapshot.targetUnit());
        } else {
            relationship = context.getString(R.string.graphy_equation, snapshot.targetUnit(),
                    formula(context, snapshot, snapshot.sourceUnit()));
        }
        String explanation = context.getString(R.string.graphy_equation, formula, snapshot.targetDisplay());
        if (!note.isEmpty()) explanation += "\n\n" + note;
        return ConversionGraphBuilder.build(id, snapshot, new ConversionGraphBuilder.Text(
                context.getString(transformation.kind() == ConversionTransformation.Kind.RATE_BASED
                        ? R.string.graphy_exchange_rate : R.string.graphy_relationship),
                relationship, formula, explanation, note));
    }

    private static String formula(Context context, ConversionSnapshot snapshot, String input) {
        if (snapshot.transformation().kind() == ConversionTransformation.Kind.DIRECT) {
            return context.getString(R.string.graphy_mapping);
        }
        String expression = input;
        boolean compound = false;
        for (var step : snapshot.transformation().steps()) {
            expression = switch (step.operator()) {
                case SCALE -> context.getString(R.string.graphy_linear_formula, compound ? "(" + expression + ")" : expression, value(context, step.operand(), snapshot.transformation().kind()));
                case OFFSET -> context.getString(R.string.graphy_offset_formula, expression,
                        step.operand() < 0 ? "−" : "+", value(context, Math.abs(step.operand()), snapshot.transformation().kind()));
                case DIVIDE_INPUT -> context.getString(R.string.graphy_reciprocal_formula, value(context, step.operand(), snapshot.transformation().kind()), expression);
            };
            compound = true;
        }
        return expression;
    }

    /** Explanatory factors retain significant digits, even below display rounding precision. */
    private static String value(Context context, double number, ConversionTransformation.Kind kind) {
        double magnitude = Math.abs(number);
        String pattern = magnitude != 0 && (magnitude < 0.000001 || magnitude >= 1e12)
                ? "0.################E0" : "0.################";
        DecimalFormat format = new DecimalFormat(pattern);
        if (kind != ConversionTransformation.Kind.RATE_BASED) {
            var preferences = net.smartlogic.unitconverter.helper.Preferences.getInstance(context);
            var symbols = format.getDecimalFormatSymbols();
            symbols.setDecimalSeparator(preferences.getDecimalSeparator().charAt(0));
            String grouping = preferences.getGroupSeparator();
            if (!grouping.equals(context.getString(R.string.group_separator_none)) && !pattern.contains("E")) {
                symbols.setGroupingSeparator(grouping.charAt(0));
                format.setGroupingSize(3);
                format.setGroupingUsed(true);
            }
            format.setDecimalFormatSymbols(symbols);
        }
        return format.format(number);
    }
}
