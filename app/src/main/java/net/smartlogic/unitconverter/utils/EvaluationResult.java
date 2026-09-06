package net.smartlogic.unitconverter.utils;

import androidx.annotation.NonNull;

/**
 * Result of evaluating a calculator expression.
 */
public final class EvaluationResult {

    private final double value;
    private final EvalTrace trace;
    private final boolean valid;

    private EvaluationResult(double value, EvalTrace trace, boolean valid) {
        this.value = value;
        this.trace = trace == null ? EvalTrace.empty() : trace;
        this.valid = valid;
    }

    public static EvaluationResult invalid() {
        return new EvaluationResult(Double.NaN, EvalTrace.empty(), false);
    }

    public static EvaluationResult of(double value, EvalTrace trace) {
        boolean valid = !Double.isNaN(value) && !Double.isInfinite(value) && trace.isValid();
        return new EvaluationResult(value, trace, valid);
    }

    public double getValue() {
        return value;
    }

    @NonNull
    public EvalTrace getTrace() {
        return trace;
    }

    public boolean isValid() {
        return valid;
    }
}
