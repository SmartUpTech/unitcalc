package net.smartlogic.unitconverter.bmi;

import java.math.BigDecimal;
import org.junit.Test;
import static org.junit.Assert.*;

public class BmiCalculatorTest {
    private BmiCalculator.Result metric(String height, String weight) {
        return BmiCalculator.calculate(new BigDecimal(height), new BigDecimal(weight));
    }
    @Test public void referenceExamplesAndDifferences() {
        assertEquals("17.34", metric("186", "60").bmi().setScale(2, java.math.RoundingMode.HALF_EVEN).toPlainString());
        assertEquals(BmiCalculator.Category.UNDERWEIGHT, metric("186", "60").category());
        assertEquals("21.10", metric("186", "73").bmi().setScale(2, java.math.RoundingMode.HALF_EVEN).toPlainString());
        assertEquals(BmiCalculator.Category.HEALTHY, metric("186", "73").category());
        assertEquals("26.01", metric("186", "90").bmi().setScale(2, java.math.RoundingMode.HALF_EVEN).toPlainString());
        assertEquals(0, new BigDecimal("64.0026").compareTo(metric("186", "60").minimumKg()));
        assertEquals(0, new BigDecimal("86.49").compareTo(metric("186", "90").upperExclusiveKg()));
        assertEquals(0, new BigDecimal("3.51").compareTo(metric("186", "90").differenceKg()));
        assertEquals(0, metric("186", "73").differenceKg().signum());
    }
    @Test public void thresholdsUseUnroundedBmiAndContinuousIntervals() {
        String[] values = {"18.4999", "18.5", "24.9999", "25", "29.9999", "30", "34.9999", "35", "39.9999", "40"};
        int[] categories = {0,1,1,2,2,3,3,4,4,5};
        for (int i = 0; i < values.length; i++) assertEquals(values[i], categories[i], metric("100", values[i]).category().ordinal());
    }
    @Test public void exactImperialDefinitionsAndMixedUnits() {
        assertEquals(0, new BigDecimal("182.88").compareTo(BmiCalculator.heightCm("6", "0", true)));
        assertEquals(0, new BigDecimal("180.34").compareTo(BmiCalculator.heightCm("5", "11", true)));
        assertEquals(0, new BigDecimal("45.359237").compareTo(BmiCalculator.weightKg("100", true)));
        assertEquals(0, new BigDecimal("2.54").compareTo(BmiCalculator.heightCm("0", "1", true)));
    }
    @Test public void classificationDoesNotRoundOntoAnInternalPrecisionBoundary() {
        // Height squared includes a positive 1e-40 term; mass omits that term.
        assertEquals(BmiCalculator.Category.HEALTHY,
                metric("100.000000000000000001", "25.0000000000000000005").category());
        assertEquals(BmiCalculator.Category.UNDERWEIGHT,
                metric("100.000000000000000001", "18.50000000000000000037").category());
    }
    @Test public void invalidAndIncompleteInputsAreRejected() {
        for (String value : new String[]{"", ".", "0", "-1", "NaN", "Infinity", "1,234", "1e5"}) {
            try { BmiCalculator.heightCm(value, "", false); fail(value); } catch (IllegalArgumentException expected) { }
            try { BmiCalculator.weightKg(value, false); fail(value); } catch (IllegalArgumentException expected) { }
        }
        for (String[] value : new String[][]{{"5.5","0"},{"5","12"},{"0","0"},{"5","-1"}}) {
            try { BmiCalculator.heightCm(value[0], value[1], true); fail(); } catch (IllegalArgumentException expected) { }
        }
    }
}
