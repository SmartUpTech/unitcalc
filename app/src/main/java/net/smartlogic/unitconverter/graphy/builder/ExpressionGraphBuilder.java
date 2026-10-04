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
 * Converts expression evaluation traces into connected Graphy graphs.
 * Each subexpression value exists once; arrows carry results to later operations.
 */
public final class ExpressionGraphBuilder implements GraphBuilder {

    private int nodeCounter;
    private final java.util.function.DoubleFunction<String> formatter;

    public ExpressionGraphBuilder() {
        this(value -> net.smartlogic.unitconverter.utils.NumberUtils.formatCalculator(value, 8));
    }

    public ExpressionGraphBuilder(java.util.function.DoubleFunction<String> formatter) {
        this.formatter = formatter;
    }

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
        for (int i = 0; i < nodes.size(); i++) {
            GraphyNode node = nodes.get(i);
            if (node.type() == GraphyNodeType.RESULT) {
                nodes.set(i, new GraphyNode(node.id(), node.type(), snapshot.getFormattedResult(),
                        snapshot.getFormattedResult(), node.semanticRole(), snapshot.getRawExpression(), null));
            }
        }
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
                    evalNode.getLabel(), role);
        }

        if (evalNode.getKind() == EvalNode.Kind.GROUP) {
            EvalNode child = evalNode.getChild(0);
            if (child != null) {
                return walk(child, nodes, connections, isRoot);
            }
            return addNode(nodes, GraphyNodeType.DERIVED, "(", formatValue(evalNode.getValue()), "group");
        }

        String operationNodeId = addNode(nodes, GraphyNodeType.OPERATION, evalNode.getLabel(),
                evalNode.getLabel(), semanticRoleForOperation(evalNode.getKind()), formulaFor(evalNode));

        for (EvalNode child : evalNode.getChildren()) {
            String childNodeId;
            if (shouldCollapsePercent(evalNode, child)) {
                childNodeId = addPercentOperandNode(nodes, child);
            } else {
                childNodeId = walk(child, nodes, connections, false);
            }
            connections.add(new GraphyConnection(childNodeId, operationNodeId, evalNode.getLabel()));
        }

        GraphyNodeType valueType = isRoot ? GraphyNodeType.RESULT : GraphyNodeType.DERIVED;
        String semanticRole = isRoot ? "result" : "derived";
        String valueNodeId = addNode(nodes, valueType, formatValue(evalNode.getValue()),
                formatValue(evalNode.getValue()), semanticRole, formulaFor(evalNode));
        connections.add(new GraphyConnection(operationNodeId, valueNodeId, null));
        return valueNodeId;
    }

    private static boolean shouldCollapsePercent(@NonNull EvalNode parent, @NonNull EvalNode child) {
        if (child.getKind() != EvalNode.Kind.PERCENT) {
            return false;
        }
        EvalNode.Kind parentKind = parent.getKind();
        EvalNode number = child.getChild(0);
        return number != null && number.getKind() == EvalNode.Kind.NUMBER
                && (parentKind == EvalNode.Kind.MULTIPLY || parentKind == EvalNode.Kind.DIVIDE);
    }

    @NonNull
    private String addPercentOperandNode(@NonNull List<GraphyNode> nodes, @NonNull EvalNode percentNode) {
        EvalNode numberChild = percentNode.getChild(0);
        String display = numberChild != null ? numberChild.getLabel() + "%" : percentNode.getLabel();
        return addNode(nodes, GraphyNodeType.INPUT, display, display, "operand", formulaFor(percentNode));
    }

    private String addNode(@NonNull List<GraphyNode> nodes,
                           @NonNull GraphyNodeType type,
                           @NonNull String label,
                           @NonNull String displayValue,
                           @NonNull String semanticRole) {
        return addNode(nodes, type, label, displayValue, semanticRole, null);
    }

    private String addNode(List<GraphyNode> nodes, GraphyNodeType type, String label,
                           String displayValue, String semanticRole, String formula) {
        String id = "node-" + (++nodeCounter);
        nodes.add(new GraphyNode(id, type, label, displayValue, semanticRole, formula, null));
        return id;
    }

    /** Serialize the existing evaluation trace for disclosure; never evaluate it again. */
    private static String formulaFor(EvalNode node) {
        if (node.getKind() == EvalNode.Kind.NUMBER) return node.getLabel();
        List<String> operands = new ArrayList<>();
        for (EvalNode child : node.getChildren()) operands.add(formulaFor(child));
        if (operands.isEmpty()) return node.getLabel();
        return switch (node.getKind()) {
            case GROUP -> "(" + operands.get(0) + ")";
            case PERCENT -> "(" + operands.get(0) + " ÷ 100)";
            case SQRT -> "√(" + operands.get(0) + ")";
            case UNARY_PLUS, UNARY_MINUS -> node.getLabel() + "(" + operands.get(0) + ")";
            default -> "(" + String.join(" " + node.getLabel() + " ", operands) + ")";
        };
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
    private String formatValue(double value) { return formatter.apply(value); }
}
