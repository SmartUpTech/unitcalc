package net.smartlogic.unitconverter.bmi;

import java.math.BigDecimal;
import java.math.MathContext;
import net.smartlogic.unitconverter.utils.NumberUtils;

/** Adult screening thresholds: CDC adult BMI categories (20+). Classify before rounding. */
public final class BmiCalculator {
    public static final MathContext PRECISION = MathContext.DECIMAL128;
    public static final BigDecimal CM_PER_INCH = new BigDecimal("2.54");
    public static final BigDecimal KG_PER_POUND = new BigDecimal("0.45359237");
    public static final BigDecimal INCHES_PER_FOOT = new BigDecimal("12");
    private static final BigDecimal HEALTHY_MIN = new BigDecimal("18.5");
    private static final BigDecimal HEALTHY_MAX = new BigDecimal("25");

    public enum Category { UNDERWEIGHT, HEALTHY, OVERWEIGHT, OBESITY_I, OBESITY_II, OBESITY_III }
    public record Result(BigDecimal bmi, Category category, BigDecimal minimumKg,
                         BigDecimal upperExclusiveKg, BigDecimal differenceKg) { }

    private BmiCalculator() { }

    public static BigDecimal heightCm(String height, String inches, boolean imperial) {
        BigDecimal first = NumberUtils.parseDecimal(height);
        if (!imperial) return positive(first);
        BigDecimal part = inches.isEmpty() ? BigDecimal.ZERO : NumberUtils.parseDecimal(inches);
        if (first.signum() < 0 || first.stripTrailingZeros().scale() > 0
                || part.signum() < 0 || part.compareTo(INCHES_PER_FOOT) >= 0) {
            throw new IllegalArgumentException("Feet must be whole and inches must be in [0,12)");
        }
        return positive(first.multiply(INCHES_PER_FOOT).add(part).multiply(CM_PER_INCH));
    }

    public static BigDecimal weightKg(String weight, boolean pounds) {
        BigDecimal value = positive(NumberUtils.parseDecimal(weight));
        return pounds ? value.multiply(KG_PER_POUND) : value;
    }

    public static Result calculate(BigDecimal heightCm, BigDecimal weightKg) {
        positive(heightCm); positive(weightKg);
        BigDecimal metres = heightCm.movePointLeft(2);
        BigDecimal square = metres.multiply(metres);
        BigDecimal bmi = weightKg.divide(square, PRECISION);
        Category category = category(bmi);
        BigDecimal minimum = square.multiply(HEALTHY_MIN);
        BigDecimal maximum = square.multiply(HEALTHY_MAX);
        BigDecimal difference = category == Category.UNDERWEIGHT ? weightKg.subtract(minimum)
                : category == Category.HEALTHY ? BigDecimal.ZERO : weightKg.subtract(maximum);
        return new Result(bmi, category, minimum, maximum, difference);
    }

    public static Category category(BigDecimal bmi) {
        positive(bmi);
        if (bmi.compareTo(HEALTHY_MIN) < 0) return Category.UNDERWEIGHT;
        if (bmi.compareTo(HEALTHY_MAX) < 0) return Category.HEALTHY;
        if (bmi.compareTo(new BigDecimal("30")) < 0) return Category.OVERWEIGHT;
        if (bmi.compareTo(new BigDecimal("35")) < 0) return Category.OBESITY_I;
        if (bmi.compareTo(new BigDecimal("40")) < 0) return Category.OBESITY_II;
        return Category.OBESITY_III;
    }

    private static BigDecimal positive(BigDecimal value) {
        if (value == null || value.signum() <= 0 || value.precision() > 30
                || Math.abs((long) value.scale()) > 60) throw new IllegalArgumentException("Invalid measurement");
        return value;
    }
}
