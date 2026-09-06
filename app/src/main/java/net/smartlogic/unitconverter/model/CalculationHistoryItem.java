package net.smartlogic.unitconverter.model;

/**
 * A persisted calculator history entry.
 */
public class CalculationHistoryItem {

    public final String expression;
    public final String result;

    public CalculationHistoryItem(String expression, String result) {
        this.expression = expression;
        this.result = result;
    }
}
