package net.smartlogic.unitconverter.model;

import androidx.annotation.NonNull;

public class CalculationHistoryItem {

    public final String expression;
    public final String result;
    public final long createdAt;
    @NonNull public final String calculatorId;

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
