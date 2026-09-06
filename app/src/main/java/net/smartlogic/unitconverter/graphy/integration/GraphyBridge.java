package net.smartlogic.unitconverter.graphy.integration;

import androidx.annotation.NonNull;

import net.smartlogic.unitconverter.graphy.builder.ExpressionGraphBuilder;
import net.smartlogic.unitconverter.graphy.builder.GraphBuilder;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;

/**
 * Single entry point between calculator screens and the Graphy engine.
 */
public final class GraphyBridge {

    private final GraphBuilder expressionGraphBuilder;

    public GraphyBridge() {
        this(new ExpressionGraphBuilder());
    }

    public GraphyBridge(@NonNull GraphBuilder expressionGraphBuilder) {
        this.expressionGraphBuilder = expressionGraphBuilder;
    }

    @NonNull
    public GraphyOutput build(@NonNull CalculationSnapshot snapshot) {
        if (!snapshot.isValid()) {
            return GraphyOutput.empty(
                    snapshot.getCalculatorId(),
                    snapshot.getRawExpression(),
                    snapshot.getFormattedResult()
            );
        }
        return expressionGraphBuilder.build(snapshot, snapshot.getEvaluationResult().getTrace());
    }
}
