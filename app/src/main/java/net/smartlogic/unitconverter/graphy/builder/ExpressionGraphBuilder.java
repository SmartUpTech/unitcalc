package net.smartlogic.unitconverter.graphy.builder;

import androidx.annotation.NonNull;

import net.smartlogic.unitconverter.graphy.integration.CalculationSnapshot;
import net.smartlogic.unitconverter.graphy.model.GraphyConnection;
import net.smartlogic.unitconverter.graphy.model.GraphyNode;
import net.smartlogic.unitconverter.graphy.model.GraphyNodeType;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.utils.EvalNode;
import net.smartlogic.unitconverter.utils.EvalTrace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Converts expression evaluation traces into Graphy nodes and connections.
 */
public final class ExpressionGraphBuilder implements GraphBuilder {

    private int nodeCounter;

    @NonNull
    @Override
    public GraphyOutput build(@NonNull CalculationSnapshot snapshot, @NonNull EvalTrace trace) {
        nodeCounter = 0;
        List<GraphyNode> nodes = new ArrayList<>();
        List<GraphyConnection> connections = new ArrayList<>();

        EvalNode root = trace.getRoot();
        if (root == null || !root.isValid()) {
            return GraphyOutput.empty(
                    snapshot.getCalculatorId(),
                    snapshot.getRawExpression(),
                    snapshot.getFormattedResult()
            );
        }

        walk(root, nodes, connections, true);
        String explanation = snapshot.getRawExpression() + " = " + snapshot.getFormattedResult();

        return GraphyOutput.create(
                snapshot.getCalculatorId(),
                snapshot.getRawExpression(),
                snapshot.getFormattedResult(),
                nodes,
                connections,
                explanation,
                new HashMap<>()
        );
    }

    private String walk(@NonNull EvalNode evalNode,
                        @NonNull List<GraphyNode> nodes,
                        @NonNull List<GraphyConnection> connections,
                        boolean isRoot) {
        if (evalNode.getKind() == EvalNode.Kind.NUMBER) {
            GraphyNodeType type = isRoot ? GraphyNodeType.RESULT : GraphyNodeType.INPUT;
            String role = isRoot ? "result" : "operand";
            return addNode(nodes, type, evalNode.getLabel(),
                    formatValue(evalNode.getValue()), role);
        }

        if (evalNode.getKind() == EvalNode.Kind.GROUP) {
            EvalNode child = evalNode.getChild(0);
            if (child != null) {
                return walk(child, nodes, connections, isRoot);
            }
            return addNode(nodes, GraphyNodeType.DERIVED, "(", formatValue(evalNode.getValue()), "group");
        }

        String operationNodeId = addNode(nodes, GraphyNodeType.OPERATION, evalNode.getLabel(),
                evalNode.getLabel(), semanticRoleForOperation(evalNode.getKind()));

        for (EvalNode child : evalNode.getChildren()) {
            String childNodeId = walk(child, nodes, connections, false);
            connections.add(new GraphyConnection(childNodeId, operationNodeId, evalNode.getLabel()));
        }

        GraphyNodeType valueType = isRoot ? GraphyNodeType.RESULT : GraphyNodeType.DERIVED;
        String semanticRole = isRoot ? "result" : "derived";
        String valueNodeId = addNode(nodes, valueType, formatValue(evalNode.getValue()),
                formatValue(evalNode.getValue()), semanticRole);
        connections.add(new GraphyConnection(operationNodeId, valueNodeId, null));
        return valueNodeId;
    }

    private String addNode(@NonNull List<GraphyNode> nodes,
                           @NonNull GraphyNodeType type,
                           @NonNull String label,
                           @NonNull String displayValue,
                           @NonNull String semanticRole) {
        String id = "node-" + (++nodeCounter);
        nodes.add(new GraphyNode(id, type, label, displayValue, semanticRole));
        return id;
    }

    @NonNull
    private static String semanticRoleForOperation(@NonNull EvalNode.Kind kind) {
        switch (kind) {
            case ADD:
                return "addition";
            case SUBTRACT:
                return "subtraction";
            case MULTIPLY:
                return "multiplication";
            case DIVIDE:
                return "division";
            case POWER:
                return "power";
            case PERCENT:
                return "percent";
            case SQRT:
                return "square-root";
            case UNARY_PLUS:
                return "unary-plus";
            case UNARY_MINUS:
                return "unary-minus";
            default:
                return "operation";
        }
    }

    @NonNull
    private static String formatValue(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
