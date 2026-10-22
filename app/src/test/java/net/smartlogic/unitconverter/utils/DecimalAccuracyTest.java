package net.smartlogic.unitconverter.utils;

import java.math.BigDecimal;
import org.junit.Test;
import static org.junit.Assert.*;

public class DecimalAccuracyTest {
    private void expression(String input, String expected) {
        EvaluationResult result = ExpressionEvaluator.evaluate(input);
        assertTrue(input, result.isValid());
        assertEquals(input, 0, new BigDecimal(expected).compareTo(result.getDecimalValue()));
    }

    @Test public void decimalOperationsAndLargeIntegersAreExact() {
        expression("99*12", "1188");
        expression("0.1+0.2", "0.3");
        expression("0.3-0.2-0.1", "0");
        expression("1.005*100", "100.5");
        expression("9007199254740992+1", "9007199254740993");
        expression("123456789012345678901234567890123456789/1", "123456789012345678901234567890123456789");
        expression("(2+3)*4", "20");
        expression("2+3*4", "14");
        expression("-5*-3", "15");
        expression("50%", "0.5");
        expression("2^-3", "0.125");
    }

    @Test public void rootsHandlePerfectSquaresNestedAndTinyOperands() {
        expression("sqrt(16)", "4");
        expression("sqrt(0)", "0");
        expression("sqrt(sqrt(256))", "4");
        expression("sqrt(0.0000000000000000000000000000000000000001)", "0.00000000000000000001");
        expression("4^0.5", "2");
        BigDecimal root = ExpressionEvaluator.evaluate("sqrt(2)").getDecimalValue();
        assertTrue(root.subtract(new BigDecimal("1.414213562373095048801688724209698")).abs()
                .compareTo(new BigDecimal("1e-33")) <= 0);
    }

    @Test public void incompleteInvalidAndExcessiveExpressionsFailWithoutThrowing() {
        String[] invalid = {"sqrt(", "sqrt()", "sqrt(-1)", "1/0", "0^-1", "2++", "2^1000000000",
                "(".repeat(40) + "1" + ")".repeat(40), "-".repeat(100) + "1",
                "1".repeat(1025), "1/0*0", "2+unknown(1)"};
        for (String input : invalid) assertFalse(input, ExpressionEvaluator.evaluate(input).isValid());
    }

    @Test public void roundingAndLocalesDoNotChangeCanonicalContinuation() {
        BigDecimal value = ExpressionEvaluator.evaluate("1/8").getDecimalValue();
        assertEquals("0,12", NumberUtils.formatCalculator(value, 2, ',', '.', true));
        expression(value.toPlainString() + "*8", "1");
        assertEquals("9,007,199,254,740,993", NumberUtils.formatCalculator(
                new BigDecimal("9007199254740993"), 2, '.', ',', true));
        assertEquals("0", NumberUtils.formatCalculator(new BigDecimal("-0.0001"), 2, '.', ',', false));
        BigDecimal third = ExpressionEvaluator.evaluate("1/3").getDecimalValue();
        assertEquals("1", NumberUtils.formatCalculator(third.multiply(new BigDecimal("3")), 8, '.', ',', false));
    }

    @Test public void currencyUsesDecimalRatesAndInputs() {
        assertEquals(0, new BigDecimal("8325").compareTo(DecimalMath.currency(
                new BigDecimal("100"), BigDecimal.ONE, new BigDecimal("83.25"))));
        assertEquals(0, new BigDecimal("100").compareTo(DecimalMath.currency(
                new BigDecimal("8325"), new BigDecimal("83.25"), BigDecimal.ONE)));
        assertEquals(0, new BigDecimal("9007199254740993").compareTo(DecimalMath.currency(
                new BigDecimal("9007199254740993"), new BigDecimal("83.25"), new BigDecimal("83.25"))));
        BigDecimal longInput = new BigDecimal("123456789012345678901234567890123456789");
        assertEquals(0, longInput.compareTo(DecimalMath.currency(longInput, BigDecimal.ONE, BigDecimal.ONE)));
        try {
            DecimalMath.currency(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE);
            fail("Accepted zero rate");
        } catch (ArithmeticException expected) { }
    }

    @Test public void numericInputDoesNotSilentlyStripMalformedCharacters() {
        assertEquals(0, new BigDecimal("12.5").compareTo(NumberUtils.parseDecimal("12.5")));
        for (String input : new String[]{"1,5", "12oops3", "1.2.3", "Infinity", "NaN"}) {
            try { NumberUtils.parseDecimal(input); fail(input); }
            catch (IllegalArgumentException expected) { }
        }
    }
}
