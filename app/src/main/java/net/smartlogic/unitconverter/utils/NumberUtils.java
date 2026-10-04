package net.smartlogic.unitconverter.utils;

public class NumberUtils {

    /** Shared calculator/Graphy display boundary. Does not change internal precision. */
    public static String formatCalculator(double value, int decimals) {
        java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###.########");
        formatter.setMaximumFractionDigits(decimals);
        return formatter.format(value);
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
