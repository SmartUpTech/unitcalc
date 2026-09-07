package net.smartlogic.unitconverter.graphy.builder;

import androidx.annotation.NonNull;

import net.smartlogic.unitconverter.graphy.model.GraphyConnection;
import net.smartlogic.unitconverter.graphy.model.GraphyNode;
import net.smartlogic.unitconverter.graphy.model.GraphyNodeType;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds a vertical conversion graph: input value → operation → result.
 */
public final class ConversionGraphBuilder {

    public static final String UNIT_CONVERTER_ID = "unit-converter";
    public static final String CURRENCY_CONVERTER_ID = "currency-converter";

    private ConversionGraphBuilder() {
    }

    @NonNull
    public static GraphyOutput build(@NonNull String calculatorId,
                                     @NonNull String inputDisplay,
                                     @NonNull String operationLabel,
                                     @NonNull String resultDisplay,
                                     @NonNull String explanation) {
        if (inputDisplay.trim().isEmpty()) {
            return GraphyOutput.empty(calculatorId, inputDisplay, resultDisplay);
        }

        List<GraphyNode> nodes = new ArrayList<>();
        List<GraphyConnection> connections = new ArrayList<>();

        String inputId = addNode(nodes, GraphyNodeType.INPUT, inputDisplay, inputDisplay, "input");
        String operationId = addNode(nodes, GraphyNodeType.OPERATION, operationLabel,
                operationLabel, "conversion");
        String resultId = addNode(nodes, GraphyNodeType.RESULT, resultDisplay, resultDisplay, "result");

        connections.add(new GraphyConnection(inputId, operationId, null));
        connections.add(new GraphyConnection(operationId, resultId, null));

        return GraphyOutput.create(
                calculatorId,
                inputDisplay + " → " + resultDisplay,
                resultDisplay,
                nodes,
                connections,
                explanation,
                Collections.emptyMap()
        );
    }

    private static String addNode(@NonNull List<GraphyNode> nodes,
                                  @NonNull GraphyNodeType type,
                                  @NonNull String label,
                                  @NonNull String displayValue,
                                  @NonNull String semanticRole) {
        String id = "node-" + (nodes.size() + 1);
        nodes.add(new GraphyNode(id, type, label, displayValue, semanticRole));
        return id;
    }
}
