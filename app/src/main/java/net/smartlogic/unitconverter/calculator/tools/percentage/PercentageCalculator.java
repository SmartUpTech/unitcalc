package net.smartlogic.unitconverter.calculator.tools.percentage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.smartlogic.unitconverter.utils.DecimalMath;

/** Four-mode percentage arithmetic with a single BigDecimal calculation path. */
public final class PercentageCalculator {
    public static final BigDecimal MAX_VALUE = new BigDecimal("1000000000");
    public static final BigDecimal MAX_PERCENT = new BigDecimal("1000000");
    public static final int DISPLAY_SCALE = 10;

    public enum Mode {
        PERCENT_OF,
        INCREASE,
        DECREASE,
        WHAT_PERCENT
    }

    public record Inputs(Mode mode, BigDecimal first, BigDecimal second) {
    }

    public record Result(
            Mode mode,
            BigDecimal result,
            BigDecimal amount,
            Inputs inputs) {
    }

    private PercentageCalculator() {
    }

    public static Result calculate(Inputs inputs) {
        if (inputs == null || inputs.mode() == null || inputs.first() == null || inputs.second() == null) {
            throw new IllegalArgumentException("inputs");
        }
        Mode mode = inputs.mode();
        BigDecimal first = inputs.first();
        BigDecimal second = inputs.second();
        if (first.abs().compareTo(MAX_VALUE) > 0 || second.abs().compareTo(MAX_VALUE) > 0) {
            throw new IllegalArgumentException("range");
        }
        return switch (mode) {
            case PERCENT_OF -> percentOf(first, second, inputs);
            case INCREASE -> increase(first, second, inputs);
            case DECREASE -> decrease(first, second, inputs);
            case WHAT_PERCENT -> whatPercent(first, second, inputs);
        };
    }

    private static Result percentOf(BigDecimal base, BigDecimal percent, Inputs inputs) {
        requireNonNegativePercent(percent);
        BigDecimal amount = percentAmount(base, percent);
        return new Result(Mode.PERCENT_OF, display(amount), display(amount), inputs);
    }

    private static Result increase(BigDecimal base, BigDecimal percent, Inputs inputs) {
        requireNonNegativePercent(percent);
        BigDecimal amount = percentAmount(base, percent);
        BigDecimal result = DecimalMath.checked(base.add(amount));
        return new Result(Mode.INCREASE, display(result), display(amount), inputs);
    }

    private static Result decrease(BigDecimal base, BigDecimal percent, Inputs inputs) {
        requireNonNegativePercent(percent);
        BigDecimal amount = percentAmount(base, percent);
        BigDecimal result = DecimalMath.checked(base.subtract(amount));
        return new Result(Mode.DECREASE, display(result), display(amount), inputs);
    }

    private static Result whatPercent(BigDecimal part, BigDecimal whole, Inputs inputs) {
        if (whole.signum() == 0) throw new IllegalArgumentException("whole");
        BigDecimal percent = DecimalMath.divide(part.multiply(new BigDecimal("100")), whole);
        return new Result(Mode.WHAT_PERCENT, display(percent), display(percent), inputs);
    }

    private static BigDecimal percentAmount(BigDecimal base, BigDecimal percent) {
        if (percent.compareTo(MAX_PERCENT) > 0) throw new IllegalArgumentException("percent");
        return DecimalMath.divide(base.multiply(percent), new BigDecimal("100"));
    }

    private static void requireNonNegativePercent(BigDecimal percent) {
        if (percent.signum() < 0) throw new IllegalArgumentException("percent");
        if (percent.compareTo(MAX_PERCENT) > 0) throw new IllegalArgumentException("percent");
    }

    /** Presentation boundary only; calculation consumers use the same Result values. */
    public static BigDecimal display(BigDecimal value) {
        return value.setScale(DISPLAY_SCALE, RoundingMode.HALF_EVEN).stripTrailingZeros();
    }
}
