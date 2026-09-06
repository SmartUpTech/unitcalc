package net.smartlogic.unitconverter.graphy.integration;

import androidx.annotation.NonNull;

import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.utils.EvaluationResult;

/**
 * Immutable record of one calculator evaluation used to build Graphy output.
 */
public final class CalculationSnapshot {

    public static final String BASIC_CALCULATOR_ID = "basic-calculator";

    private final String calculatorId;
    private final String rawExpression;
    private final String sanitizedExpression;
    private final String formattedResult;
    private final EvaluationResult evaluationResult;
    private final long timestampMs;

    private CalculationSnapshot(@NonNull String calculatorId,
                                @NonNull String rawExpression,
                                @NonNull String sanitizedExpression,
                                @NonNull String formattedResult,
                                @NonNull EvaluationResult evaluationResult,
                                long timestampMs) {
        this.calculatorId = calculatorId;
        this.rawExpression = rawExpression;
        this.sanitizedExpression = sanitizedExpression;
        this.formattedResult = formattedResult;
        this.evaluationResult = evaluationResult;
        this.timestampMs = timestampMs;
    }

    @NonNull
    public static CalculationSnapshot create(@NonNull String calculatorId,
                                             @NonNull String rawExpression,
                                             @NonNull String sanitizedExpression,
                                             @NonNull String formattedResult,
                                             @NonNull EvaluationResult evaluationResult) {
        return new CalculationSnapshot(calculatorId, rawExpression, sanitizedExpression,
                formattedResult, evaluationResult, System.currentTimeMillis());
    }

    @NonNull
    public String getCalculatorId() {
        return calculatorId;
    }

    @NonNull
    public String getRawExpression() {
        return rawExpression;
    }

    @NonNull
    public String getSanitizedExpression() {
        return sanitizedExpression;
    }

    @NonNull
    public String getFormattedResult() {
        return formattedResult;
    }

    @NonNull
    public EvaluationResult getEvaluationResult() {
        return evaluationResult;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    public boolean isValid() {
        return evaluationResult.isValid();
    }
}
