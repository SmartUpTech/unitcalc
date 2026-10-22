package net.smartlogic.unitconverter.utils;

import java.math.BigDecimal;
import java.math.MathContext;

/** Shared decimal policy. Exact finite arithmetic; 34 digits/HALF_EVEN for division. */
public final class DecimalMath {
    public static final MathContext CONTEXT = MathContext.DECIMAL128;
    private static final BigDecimal TWO = new BigDecimal("2");

    private DecimalMath() { }

    public static BigDecimal checked(BigDecimal value) {
        int exponent = value.precision() - value.scale() - 1;
        // Bound work and plain-text continuation size on the UI thread.
        if (value.precision() > 1024 || (value.signum() != 0 && Math.abs((long) exponent) > 300)) {
            throw new ArithmeticException("Result outside supported range");
        }
        return value;
    }

    /** Newton iteration uses APIs available on every supported Android version (API 26+). */
    public static BigDecimal sqrt(BigDecimal value) {
        checked(value);
        if (value.signum() < 0) throw new ArithmeticException("Negative square root");
        if (value.signum() == 0) return BigDecimal.ZERO;
        MathContext guard = new MathContext(CONTEXT.getPrecision() + 8, CONTEXT.getRoundingMode());
        int exponent = value.precision() - value.scale() - 1;
        BigDecimal estimate = BigDecimal.ONE.scaleByPowerOfTen(Math.floorDiv(exponent, 2));
        for (int i = 0; i < 100; i++) {
            BigDecimal next = estimate.add(value.divide(estimate, guard)).divide(TWO, guard);
            if (next.round(CONTEXT).compareTo(estimate.round(CONTEXT)) == 0) return next.round(CONTEXT);
            estimate = next;
        }
        throw new ArithmeticException("Square root did not converge");
    }

    public static BigDecimal pow(BigDecimal base, BigDecimal exponent) {
        try {
            int power = exponent.intValueExact();
            if (Math.abs((long) power) > 1000) throw new ArithmeticException("Exponent too large");
            if (base.signum() == 0 && power < 0) throw new ArithmeticException("Division by zero");
            // Reject explosive intermediates before allocating them.
            double magnitude = Math.log10(Math.abs(base.doubleValue())) * power;
            if (Double.isFinite(magnitude) && Math.abs(magnitude) > 301) {
                throw new ArithmeticException("Power outside supported range");
            }
            BigDecimal result = base.pow(Math.abs(power), CONTEXT);
            return checked(power < 0 ? BigDecimal.ONE.divide(result, CONTEXT) : result);
        } catch (ArithmeticException invalidInteger) {
            if (exponent.stripTrailingZeros().scale() <= 0) throw invalidInteger;
            double result = Math.pow(base.doubleValue(), exponent.doubleValue());
            if (!Double.isFinite(result) || (result == 0 && base.signum() != 0)) {
                throw new ArithmeticException("Invalid scientific result");
            }
            return checked(BigDecimal.valueOf(result));
        }
    }

    public static BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.signum() == 0) throw new ArithmeticException("Division by zero");
        BigDecimal result;
        try {
            result = numerator.divide(denominator);
        } catch (ArithmeticException nonTerminating) {
            result = numerator.divide(denominator, CONTEXT);
        }
        return checked(result);
    }

    public static BigDecimal currency(BigDecimal input, BigDecimal fromRate, BigDecimal toRate) {
        if (fromRate.signum() <= 0 || toRate.signum() <= 0) throw new ArithmeticException("Invalid rate");
        return divide(input.multiply(toRate), fromRate);
    }
}
