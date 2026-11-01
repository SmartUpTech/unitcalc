package net.smartlogic.unitconverter.graphy.model;

import java.util.ArrayList;
import java.util.List;

/** Mathematical relationship, independent of formatting and renderer styling. */
public record ConversionTransformation(Kind kind, List<Step> steps) {
    public enum Kind { LINEAR, AFFINE, RATE_BASED, RECIPROCAL, COMPOSITE, DIRECT }
    public enum Operator { SCALE, OFFSET, DIVIDE_INPUT }
    public record Step(Operator operator, double operand) {
        public double apply(double value) {
            return switch (operator) {
                case SCALE -> value * operand;
                case OFFSET -> value + operand;
                case DIVIDE_INPUT -> operand / value;
            };
        }
    }

    public ConversionTransformation { steps = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(steps)); }
    public static ConversionTransformation linear(double scale) {
        return new ConversionTransformation(Kind.LINEAR, java.util.Arrays.asList(new Step(Operator.SCALE, scale)));
    }
    public static ConversionTransformation affine(double scale, double offset) {
        return new ConversionTransformation(Kind.AFFINE,
                java.util.Arrays.asList(new Step(Operator.SCALE, scale), new Step(Operator.OFFSET, offset)));
    }
    public static ConversionTransformation rate(double rate) {
        return new ConversionTransformation(Kind.RATE_BASED, java.util.Arrays.asList(new Step(Operator.SCALE, rate)));
    }
    public static ConversionTransformation reciprocal(double numerator) {
        return new ConversionTransformation(Kind.RECIPROCAL,
                java.util.Arrays.asList(new Step(Operator.DIVIDE_INPUT, numerator)));
    }
    public static ConversionTransformation direct() {
        return new ConversionTransformation(Kind.DIRECT, java.util.Collections.emptyList());
    }
    public boolean isValid() {
        if (kind == Kind.DIRECT) return steps.isEmpty();
        if (steps.isEmpty()) return false;
        if (kind == Kind.RATE_BASED && (steps.size() != 1 || steps.get(0).operator() != Operator.SCALE)) return false;
        for (Step step : steps) {
            if (step == null || step.operator() == null || !Double.isFinite(step.operand())) return false;
            if (kind == Kind.RATE_BASED && step.operand() <= 0) return false;
        }
        return true;
    }
    /** Diagnostic/test evaluation only; the UI always consumes the converter's real result. */
    public double apply(double input) {
        if (kind == Kind.DIRECT || !isValid()) return Double.NaN;
        double value = input;
        for (Step step : steps) value = step.apply(value);
        return value;
    }
    public ConversionTransformation reversed() {
        if (kind == Kind.DIRECT) return direct();
        List<Step> inverse = new ArrayList<>();
        for (int i = steps.size() - 1; i >= 0; i--) {
            Step step = steps.get(i);
            inverse.add(new Step(step.operator(), switch (step.operator()) {
                case SCALE -> 1 / step.operand();
                case OFFSET -> -step.operand();
                case DIVIDE_INPUT -> step.operand();
            }));
        }
        return new ConversionTransformation(kind, inverse);
    }
}
