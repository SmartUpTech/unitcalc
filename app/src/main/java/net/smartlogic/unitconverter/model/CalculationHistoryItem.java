package net.smartlogic.unitconverter.model;

public class CalculationHistoryItem {

    public final String expression;
    public final String result;
    public final long createdAt;

    public CalculationHistoryItem(String expression, String result) {
        this(expression, result, 0L);
    }

    public CalculationHistoryItem(String expression, String result, long createdAt) {
        this.expression = expression;
        this.result = result;
        this.createdAt = createdAt;
    }
}
