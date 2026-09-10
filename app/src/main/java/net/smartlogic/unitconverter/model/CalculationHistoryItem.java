package net.smartlogic.unitconverter.model;

import androidx.annotation.NonNull;

public record CalculationHistoryItem(String expression, String result, long createdAt,
                                     @NonNull String calculatorId) {

    public CalculationHistoryItem(@NonNull String expression, @NonNull String result) {
        this(expression, result, 0L, CalculatorCatalog.ID_BASIC);
    }

    public CalculationHistoryItem(@NonNull String expression, @NonNull String result, long createdAt) {
        this(expression, result, createdAt, CalculatorCatalog.ID_BASIC);
    }

    public CalculationHistoryItem(
            @NonNull String expression,
            @NonNull String result,
            long createdAt,
            @NonNull String calculatorId
    ) {
        this.expression = expression;
        this.result = result;
        this.createdAt = createdAt;
        this.calculatorId = calculatorId;
    }
}
