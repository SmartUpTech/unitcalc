package net.smartlogic.unitconverter.utils;

import java.math.BigDecimal;

public class NumberUtils {

    /** Shared calculator/Graphy display boundary. Does not change internal precision. */
    public static String formatCalculator(double value, int decimals) {
        return formatCalculator(BigDecimal.valueOf(value), decimals);
    }

    public static String formatCalculator(BigDecimal value, int decimals) {
        java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###.########");
        formatter.setMaximumFractionDigits(decimals);
        return formatter.format(value.setScale(decimals, java.math.RoundingMode.HALF_EVEN));
    }

    public static String formatCalculator(BigDecimal value, int decimals, char decimalSeparator,
                                          char groupSeparator, boolean grouping) {
        java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols(java.util.Locale.US);
        symbols.setDecimalSeparator(decimalSeparator);
        symbols.setGroupingSeparator(groupSeparator);
        java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,##0.########", symbols);
        formatter.setGroupingUsed(grouping);
        formatter.setMaximumFractionDigits(decimals);
        return formatter.format(value.setScale(decimals, java.math.RoundingMode.HALF_EVEN));
    }

    /** Input fields store ungrouped canonical decimals; reject malformed input, never strip it. */
    public static BigDecimal parseDecimal(String text) {
        if (text == null || text.length() > 1024
                || !text.matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)")) {
            throw new NumberFormatException("Invalid decimal input");
        }
        return DecimalMath.checked(new BigDecimal(text));
    }

    public static Float parseFloat(String str) {
        if (!StringUtils.isNotBlank(str)) return 0f;
        String clean = str.replaceAll("[^0-9.\\-]", "");
        return clean.isEmpty() || clean.equals("-") ? 0f : Float.parseFloat(clean);
    }

    public static Double parseDouble(String str) {
        if (!StringUtils.isNotBlank(str) || str.equals("-")) return 0.0;
        String clean = str.replaceAll("[^0-9.\\-]", "");
        return clean.isEmpty() || clean.equals("-") ? 0.0 : Double.parseDouble(clean);
    }
}
