package net.smartlogic.unitconverter.bmi;
import org.junit.Test;
import static org.junit.Assert.*;
public class BmiHistoryInputsTest {
    @Test public void restoresCompactAndEarlierRecords() {
        var metric = BmiHistoryInputs.parse("A:40Y 1M, G:M, H:186cm, W:90kg");
        assertNotNull(metric); assertEquals("40", metric.years()); assertEquals(1, metric.months());
        assertFalse(metric.female()); assertFalse(metric.imperial()); assertEquals("90", metric.weight());
        var imperial = BmiHistoryInputs.parse("A:9Y 6M, G:F, H:4ft 2.5in, W:73lb");
        assertNotNull(imperial); assertTrue(imperial.female()); assertTrue(imperial.imperial());
        assertTrue(imperial.pounds()); assertEquals("2.5", imperial.inches());
        var old = BmiHistoryInputs.parse("Age: 25 years 0 months; Gender: Female; Height: 186 cm; Weight: 73 kg");
        assertNotNull(old); assertTrue(old.female()); assertEquals("186", old.height());
        assertNotNull(BmiHistoryInputs.parse("Age: 25; Gender: Male; Height: 6 ft 1 in; Weight: 160 lb"));
        assertNull(BmiHistoryInputs.parse("A:40Y 12M, G:M, H:186cm, W:90kg"));
        assertNull(BmiHistoryInputs.parse("18690"));
    }
}
