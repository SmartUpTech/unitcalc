package net.smartlogic.unitconverter.bmi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads both compact history descriptions and the earlier labeled format. */
public record BmiHistoryInputs(String years, int months, boolean female, String height,
        String inches, boolean imperial, String weight, boolean pounds) {
    private static final String NUMBER = "([0-9]+(?:\\.[0-9]+)?)";
    private static final Pattern INPUT = Pattern.compile(
            "^(?:A:|Age:\\s*)([0-9]{1,3})(?:Y| years?)?(?:\\s*([0-9]{1,2})(?:M| months?))?[,;]\\s*"
            + "(?:G:|Gender:\\s*)(Male|Female|M|F)[,;]\\s*"
            + "(?:H:|Height:\\s*)" + NUMBER + "\\s*(cm|ft)(?:\\s*" + NUMBER + "\\s*in)?[,;]\\s*"
            + "(?:W:|Weight:\\s*)" + NUMBER + "\\s*(kg|lb)$");
    public static BmiHistoryInputs parse(String description) {
        if (description == null) return null;
        Matcher m = INPUT.matcher(description.trim());
        if (!m.matches()) return null;
        int months = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
        boolean imperial = "ft".equals(m.group(5));
        if (months > 11 || !imperial && m.group(6) != null) return null;
        return new BmiHistoryInputs(m.group(1), months, m.group(3).startsWith("F"), m.group(4),
                m.group(6) == null ? "0" : m.group(6), imperial, m.group(7), "lb".equals(m.group(8)));
    }
}
