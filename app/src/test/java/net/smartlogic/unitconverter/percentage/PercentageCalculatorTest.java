package net.smartlogic.unitconverter.percentage;

import java.math.BigDecimal;
import org.junit.Test;
import static org.junit.Assert.*;

public class PercentageCalculatorTest {
    @Test public void goldenIncrease() {
        PercentageCalculator.Result result = PercentageCalculator.calculate(
                new PercentageCalculator.Inputs(
                        PercentageCalculator.Mode.INCREASE,
                        new BigDecimal("250"),
                        new BigDecimal("12")));
        assertEquals(0, new BigDecimal("280").compareTo(result.result()));
        assertEquals(0, new BigDecimal("30").compareTo(result.amount()));
    }

    @Test public void percentOf() {
        PercentageCalculator.Result result = PercentageCalculator.calculate(
                new PercentageCalculator.Inputs(
                        PercentageCalculator.Mode.PERCENT_OF,
                        new BigDecimal("250"),
                        new BigDecimal("12")));
        assertEquals(0, new BigDecimal("30").compareTo(result.result()));
    }

    @Test public void decrease() {
        PercentageCalculator.Result result = PercentageCalculator.calculate(
                new PercentageCalculator.Inputs(
                        PercentageCalculator.Mode.DECREASE,
                        new BigDecimal("250"),
                        new BigDecimal("12")));
        assertEquals(0, new BigDecimal("220").compareTo(result.result()));
        assertEquals(0, new BigDecimal("30").compareTo(result.amount()));
    }

    @Test public void whatPercent() {
        PercentageCalculator.Result result = PercentageCalculator.calculate(
                new PercentageCalculator.Inputs(
                        PercentageCalculator.Mode.WHAT_PERCENT,
                        new BigDecimal("30"),
                        new BigDecimal("250")));
        assertEquals(0, new BigDecimal("12").compareTo(result.result()));
    }

    @Test public void whatPercentRejectsZeroWhole() {
        assertThrows(IllegalArgumentException.class, () -> PercentageCalculator.calculate(
                new PercentageCalculator.Inputs(
                        PercentageCalculator.Mode.WHAT_PERCENT,
                        new BigDecimal("30"),
                        BigDecimal.ZERO)));
    }

    @Test public void rejectsNegativePercent() {
        assertThrows(IllegalArgumentException.class, () -> PercentageCalculator.calculate(
                new PercentageCalculator.Inputs(
                        PercentageCalculator.Mode.INCREASE,
                        new BigDecimal("100"),
                        new BigDecimal("-1"))));
    }
}
