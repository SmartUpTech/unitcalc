package net.smartlogic.unitconverter.helper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Immutable display/input boundary. Never use displayed values as calculation state. */
public final class NumberFormatSettings {
    public static final String INTERNATIONAL = "international";
    public static final String INDIAN = "indian";
    public final char decimalSeparator;
    public final String groupSeparator;
    public final String groupingPattern;
    public final int decimalPlaces;

    public NumberFormatSettings(char decimal, String group, String pattern, int places) {
        if (group == null || group.length() > 1 || group.indexOf(decimal) >= 0
                || decimal == '-' || Character.isDigit(decimal)
                || !(INTERNATIONAL.equals(pattern) || INDIAN.equals(pattern))
                || places < 0 || places > 10) throw new IllegalArgumentException("number format");
        decimalSeparator = decimal;
        groupSeparator = group;
        groupingPattern = pattern;
        decimalPlaces = places;
    }

    public String format(BigDecimal value) {
        return format(value, decimalPlaces, false);
    }

    public String format(BigDecimal value, int scale, boolean fixedScale) {
        BigDecimal rounded = value.setScale(scale, RoundingMode.HALF_EVEN);
        return displayCanonical((fixedScale ? rounded : rounded.stripTrailingZeros()).toPlainString());
    }

    /** Preserves leading/trailing zeroes, a trailing decimal and sign-only drafts. */
    public String displayCanonical(String canonical) {
        if (!canonical.matches("[+-]?[0-9]*(?:\\.[0-9]*)?"))
            throw new IllegalArgumentException("canonical decimal");
        int start = canonical.startsWith("-") || canonical.startsWith("+") ? 1 : 0;
        int dot = canonical.indexOf('.');
        int end = dot < 0 ? canonical.length() : dot;
        StringBuilder out = new StringBuilder(canonical.substring(0, start));
        int secondary = INDIAN.equals(groupingPattern) ? 2 : 3;
        for (int i = start; i < end; i++) {
            int remaining = end - i;
            if (i > start && !groupSeparator.isEmpty() && remaining >= 3
                    && (remaining - 3) % secondary == 0) out.append(groupSeparator);
            out.append(canonical.charAt(i));
        }
        if (dot >= 0) out.append(decimalSeparator).append(canonical.substring(dot + 1));
        return out.toString();
    }

    /** Strict localized decimal input, intentionally separate from expression grammar. */
    public String normalizeInput(String display) {
        if (display == null || display.length() > 2048) throw new IllegalArgumentException("input");
        StringBuilder western = new StringBuilder();
        display.codePoints().forEach(cp -> {
            int digit = Character.getType(cp) == Character.DECIMAL_DIGIT_NUMBER
                    ? Character.digit(cp, 10) : -1;
            if (digit >= 0) western.append((char) ('0' + digit));
            else if (cp == 0x2212) western.append('-');
            else western.appendCodePoint(cp);
        });
        String text = western.toString();
        int decimal = text.indexOf(decimalSeparator);
        if (decimal != text.lastIndexOf(decimalSeparator)) throw new IllegalArgumentException("decimal");
        String integer = decimal < 0 ? text : text.substring(0, decimal);
        String fraction = decimal < 0 ? "" : text.substring(decimal + 1);
        if (!fraction.matches("[0-9]*")) throw new IllegalArgumentException("fraction");
        if (!groupSeparator.isEmpty() && integer.contains(groupSeparator)) {
            String ungrouped = integer.replace(groupSeparator, "");
            if (!ungrouped.matches("[+-]?[0-9]+")
                    || !displayCanonical(ungrouped).equals(integer))
                throw new IllegalArgumentException("grouping");
            integer = ungrouped;
        }
        if (!integer.matches("[+-]?[0-9]*")) throw new IllegalArgumentException("integer");
        return integer + (decimal < 0 ? "" : "." + fraction);
    }

    /** Map an edit selection without treating inserted grouping marks as canonical data. */
    public int displayCursor(String canonical, int canonicalCursor) {
        if (canonicalCursor < 0 || canonicalCursor > canonical.length())
            throw new IllegalArgumentException("cursor");
        String display = displayCanonical(canonical);
        int consumed = 0;
        for (int i = 0; i < display.length(); i++) {
            if (consumed == canonicalCursor) return i;
            if (groupSeparator.isEmpty() || display.charAt(i) != groupSeparator.charAt(0)) consumed++;
        }
        return display.length();
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof NumberFormatSettings)) return false;
        NumberFormatSettings o = (NumberFormatSettings) other;
        return decimalSeparator == o.decimalSeparator && decimalPlaces == o.decimalPlaces
                && groupSeparator.equals(o.groupSeparator) && groupingPattern.equals(o.groupingPattern);
    }
    @Override public int hashCode() {
        return Objects.hash(decimalSeparator, groupSeparator, groupingPattern, decimalPlaces);
    }
}
