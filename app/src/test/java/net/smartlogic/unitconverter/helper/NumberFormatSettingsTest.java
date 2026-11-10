package net.smartlogic.unitconverter.helper;

import org.junit.Test;
import java.math.BigDecimal;
import static org.junit.Assert.*;

public class NumberFormatSettingsTest {
    private NumberFormatSettings format(char decimal, String group, String pattern) {
        return new NumberFormatSettings(decimal, group, pattern, 4);
    }
    @Test public void independentRegionalExpectations() {
        BigDecimal value = new BigDecimal("1234567.89");
        assertEquals("1,234,567.89", format('.', ",", "international").format(value));
        assertEquals("12,34,567.89", format('.', ",", "indian").format(value));
        assertEquals("1.234.567,89", format(',', ".", "international").format(value));
        assertEquals("1234567,89", format(',', "", "indian").format(value));
        assertEquals(new BigDecimal("1234567.89"), value);
    }
    @Test public void roundingIsOnlyAtDisplayBoundary() {
        NumberFormatSettings f = format('.', ",", "international");
        assertEquals("0", f.format(new BigDecimal("0.00005")));
        assertEquals("0.0002", f.format(new BigDecimal("0.00015")));
        assertEquals("-1,000", f.format(new BigDecimal("-999.99999")));
        assertEquals("1.00", f.format(BigDecimal.ONE, 2, true));
    }
    @Test public void editsAndUnicodeDigitsArePreserved() {
        NumberFormatSettings f = format(',', ".", "international");
        assertEquals("-12,00", f.displayCanonical("-12.00"));
        assertEquals("12,", f.displayCanonical("12."));
        assertEquals("-", f.normalizeInput("−"));
        assertEquals("-1234.00", f.normalizeInput("−١.٢٣٤,٠٠"));
        assertEquals(".5", f.normalizeInput(",५"));
    }
    @Test public void groupingMustBeCompleteAndUnambiguous() {
        NumberFormatSettings us = format('.', ",", "international");
        for (String input : new String[]{"1,23", "12,34,567", "1.234,56", "1,,234", " 123", "1e3"}) {
            assertThrows(input, IllegalArgumentException.class, () -> us.normalizeInput(input));
        }
        assertEquals("1234567.89", format('.', ",", "indian").normalizeInput("12,34,567.89"));
        for (String separator : new String[]{",", ".", " ", "\u00a0", "\u202f", "'"}) {
            NumberFormatSettings f = format(separator.equals(".") ? ',' : '.', separator, "international");
            assertEquals("1234", f.normalizeInput("1" + separator + "234"));
        }
        assertThrows(IllegalArgumentException.class, () -> format(',', ",", "international"));
    }
    @Test public void cursorMappingAccountsForGrouping() {
        NumberFormatSettings f = format(',', ".", "international");
        assertEquals(0, f.displayCursor("1234.00", 0));
        assertEquals(4, f.displayCursor("1234.00", 3));
        assertEquals(8, f.displayCursor("1234.00", 7));
    }
}
