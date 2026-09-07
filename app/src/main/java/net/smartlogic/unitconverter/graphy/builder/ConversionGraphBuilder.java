package net.smartlogic.unitconverter.graphy.builder;

import androidx.annotation.NonNull;

import net.smartlogic.unitconverter.graphy.model.GraphyConnection;
import net.smartlogic.unitconverter.graphy.model.GraphyNode;
import net.smartlogic.unitconverter.graphy.model.GraphyNodeType;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds conversion graphs with a primary branch and up to four related unit branches.
 */
public final class ConversionGraphBuilder {

    public static final String UNIT_CONVERTER_ID = "unit-converter";
    public static final String CURRENCY_CONVERTER_ID = "currency-converter";
    public static final int MAX_RELATED_BRANCHES = 4;

    private ConversionGraphBuilder() {
    }

    /**
     * Single conversion path (legacy / currency).
     */
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
        BranchIds branch = addBranch(nodes, connections, operationLabel, resultDisplay, true, true);
        connections.add(new GraphyConnection(inputId, branch.constantId, null));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("layout", "branch");
        metadata.put("primaryBranchId", branch.resultId);

        return GraphyOutput.create(
                calculatorId,
                inputDisplay + " → " + resultDisplay,
                resultDisplay,
                nodes,
                connections,
                explanation,
                metadata
        );
    }

    /**
     * Primary selected conversion plus up to four clearly related unit branches.
     */
    @NonNull
    public static GraphyOutput buildMultiBranch(@NonNull String calculatorId,
                                                @NonNull String inputDisplay,
                                                @NonNull BranchSpec primaryBranch,
                                                @NonNull List<BranchSpec> relatedBranches,
                                                @NonNull String explanation) {
        if (inputDisplay.trim().isEmpty()) {
            return GraphyOutput.empty(calculatorId, inputDisplay, primaryBranch.resultDisplay);
        }

        List<GraphyNode> nodes = new ArrayList<>();
        List<GraphyConnection> connections = new ArrayList<>();

        String inputId = addNode(nodes, GraphyNodeType.INPUT, inputDisplay, inputDisplay, "input");

        BranchIds primary = addBranch(nodes, connections, primaryBranch.constantLabel,
                primaryBranch.resultDisplay, true, true);
        connections.add(new GraphyConnection(inputId, primary.constantId, null));

        int relatedCount = Math.min(relatedBranches.size(), MAX_RELATED_BRANCHES);
        for (int i = 0; i < relatedCount; i++) {
            BranchSpec spec = relatedBranches.get(i);
            BranchIds related = addBranch(nodes, connections, spec.constantLabel,
                    spec.resultDisplay, false, false);
            connections.add(new GraphyConnection(inputId, related.constantId, null));
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("layout", "branch");
        metadata.put("primaryBranchId", primary.resultId);

        return GraphyOutput.create(
                calculatorId,
                inputDisplay,
                primaryBranch.resultDisplay,
                nodes,
                connections,
                explanation,
                metadata
        );
    }

    @NonNull
    private static BranchIds addBranch(@NonNull List<GraphyNode> nodes,
                                       @NonNull List<GraphyConnection> connections,
                                       @NonNull String constantLabel,
                                       @NonNull String resultDisplay,
                                       boolean primary,
                                       boolean finalResult) {
        String constantId = addNode(nodes, GraphyNodeType.CONSTANT, constantLabel,
                constantLabel, primary ? "primary-constant" : "related-constant");

        String operatorSymbol = extractOperatorSymbol(constantLabel);
        String operationId = addNode(nodes, GraphyNodeType.OPERATION, operatorSymbol,
                operatorSymbol, "conversion");

        GraphyNodeType resultType = finalResult ? GraphyNodeType.RESULT : GraphyNodeType.DERIVED;
        String resultRole = primary ? "primary" : "related";
        String resultId = addNode(nodes, resultType, resultDisplay, resultDisplay, resultRole);

        connections.add(new GraphyConnection(constantId, operationId, null));
        connections.add(new GraphyConnection(operationId, resultId, null));

        return new BranchIds(constantId, operationId, resultId);
    }

    @NonNull
    private static String extractOperatorSymbol(@NonNull String constantLabel) {
        if (constantLabel.startsWith("×") || constantLabel.startsWith("x") || constantLabel.startsWith("*")) {
            return "*";
        }
        if (constantLabel.startsWith("÷") || constantLabel.startsWith("/")) {
            return "/";
        }
        if (constantLabel.contains("→")) {
            return "→";
        }
        return "×";
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

    public static final class BranchSpec {
        public final String constantLabel;
        public final String resultDisplay;

        public BranchSpec(@NonNull String constantLabel, @NonNull String resultDisplay) {
            this.constantLabel = constantLabel;
            this.resultDisplay = resultDisplay;
        }
    }

    private static final class BranchIds {
        final String constantId;
        final String operationId;
        final String resultId;

        BranchIds(@NonNull String constantId, @NonNull String operationId, @NonNull String resultId) {
            this.constantId = constantId;
            this.operationId = operationId;
            this.resultId = resultId;
        }
    }
}
