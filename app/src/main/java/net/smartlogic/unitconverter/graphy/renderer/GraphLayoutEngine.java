package net.smartlogic.unitconverter.graphy.renderer;

import net.smartlogic.unitconverter.graphy.model.*;
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme;
import java.util.*;

/** A shared layered graph: every semantic value has exactly one visual occurrence. */
final class GraphLayoutEngine {
    LayoutResult layout(GraphyOutput output, GraphyViewTheme theme, int viewportWidth) {
        return layout(output, theme, viewportWidth, Collections.emptySet());
    }

    LayoutResult layout(GraphyOutput output, GraphyViewTheme theme, int viewportWidth, Set<Integer> collapsed) {
        return layout(output, theme, viewportWidth, collapsed, true);
    }

    LayoutResult layout(GraphyOutput output, GraphyViewTheme theme, int viewportWidth, Set<Integer> collapsed, boolean showGutter) {
        if (GraphyTopology.orderedNodes(output).isEmpty()) return LayoutResult.empty(output);
        GraphStagePlan plan = new GraphStagePlan(output, collapsed);
        List<GraphyNode> ordered = GraphyTopology.orderedNodes(plan.presentation);
        Map<String, GraphyNodeContent> content = new LinkedHashMap<>();
        Map<String, List<String>> parents = new LinkedHashMap<>();
        Map<String, Double> order = new HashMap<>();
        Map<Integer, List<GraphyNode>> rows = new TreeMap<>();
        float gutter = !showGutter || plan.stages.isEmpty() ? 0 : Math.max(theme.getTouchTarget(),
                String.valueOf(plan.stages.size()).length() * theme.getLabelTextSize() + theme.getFoldIconSize() + theme.getNodePadding() * 3);
        for (GraphyNode node : ordered) {
            content.put(node.id(), GraphyNodeContent.measure(node, theme, theme.getNodeMaxWidth()));
            parents.put(node.id(), new ArrayList<>());
        }
        for (GraphyConnection edge : plan.presentation.getConnections()) parents.get(edge.toNodeId()).add(edge.fromNodeId());
        // Stable subtree order comes from original operand edges, never from numeric values.
        Map<String, List<String>> originalParents = new HashMap<>();
        for (GraphyNode node : output.getNodes()) originalParents.put(node.id(), new ArrayList<>());
        for (GraphyConnection edge : output.getConnections()) originalParents.get(edge.toNodeId()).add(edge.fromNodeId());
        Set<String> consumed = new HashSet<>();
        for (GraphyConnection edge : output.getConnections()) consumed.add(edge.fromNodeId());
        ArrayDeque<String> traversal = new ArrayDeque<>();
        for (int i = output.getNodes().size() - 1; i >= 0; i--) {
            String id = output.getNodes().get(i).id();
            if (!consumed.contains(id)) traversal.push(id);
        }
        Set<String> visited = new HashSet<>();
        Map<String, Integer> leafOrder = new HashMap<>();
        while (!traversal.isEmpty()) {
            String id = traversal.pop();
            if (!visited.add(id)) continue;
            List<String> incoming = originalParents.get(id);
            if (incoming.isEmpty()) leafOrder.put(id, leafOrder.size());
            else for (int i = incoming.size() - 1; i >= 0; i--) traversal.push(incoming.get(i));
        }
        int leaf = leafOrder.size();
        for (GraphyNode node : GraphyTopology.orderedNodes(output)) {
            List<String> incoming = originalParents.get(node.id());
            double position = 0;
            if (incoming.isEmpty()) position = leafOrder.get(node.id());
            else { for (String id : incoming) position += order.get(id); position /= incoming.size(); }
            order.put(node.id(), position);
        }
        float graphWidth = 0;
        for (GraphyNode node : ordered) rows.computeIfAbsent(plan.rows.get(node.id()), ignored -> new ArrayList<>()).add(node);
        for (List<GraphyNode> row : rows.values()) {
            row.sort(Comparator.comparingDouble(node -> order.get(node.id())));
            float rowWidth = 0;
            for (GraphyNode node : row) rowWidth += Math.max(content.get(node.id()).width(), theme.getTouchTarget());
            rowWidth += Math.max(0, row.size() - 1) * theme.getHorizontalGap();
            graphWidth = Math.max(graphWidth, rowWidth);
        }
        Map<Integer, Integer> visibleRow = new HashMap<>();
        for (int row : rows.keySet()) visibleRow.put(row, visibleRow.size());
        Set<String> longSources = new HashSet<>();
        for (GraphyConnection edge : plan.presentation.getConnections()) {
            if (visibleRow.get(plan.rows.get(edge.toNodeId())) - visibleRow.get(plan.rows.get(edge.fromNodeId())) > 1) longSources.add(edge.fromNodeId());
        }
        float side = theme.getHorizontalGap() * (longSources.size() + 1);
        int width = (int) Math.ceil(Math.max(viewportWidth, gutter + side * 2 + graphWidth));
        float left = gutter + side, right = width - side;
        Map<String, LayoutBox> bounds = new LinkedHashMap<>();
        float outer = Math.max(theme.getNodePadding(), (theme.getTouchTarget() - theme.getNodeMinHeight()) / 2);
        float y = outer;
        List<GraphyNode> reading = new ArrayList<>();
        for (List<GraphyNode> row : rows.values()) {
            float height = 0;
            for (GraphyNode node : row) height = Math.max(height, content.get(node.id()).height());
            float[] positions = new float[row.size()];
            float previousRight = left - theme.getHorizontalGap();
            for (int i = 0; i < row.size(); i++) {
                GraphyNode node = row.get(i);
                float reserved = Math.max(content.get(node.id()).width(), theme.getTouchTarget());
                float desired = left + (right - left) * (float) ((order.get(node.id()) + 0.5) / Math.max(1, leaf));
                List<String> incoming = parents.get(node.id());
                if (!incoming.isEmpty()) {
                    desired = 0;
                    for (String parent : incoming) {
                        LayoutBox box = bounds.get(parent);
                        desired += box.x() + box.width() / 2;
                    }
                    desired /= incoming.size();
                } else if (row.size() == 1) desired = (left + right) / 2;
                positions[i] = Math.max(desired - reserved / 2, previousRight + theme.getHorizontalGap());
                previousRight = positions[i] + reserved;
            }
            float nextLeft = right + theme.getHorizontalGap();
            for (int i = row.size() - 1; i >= 0; i--) {
                GraphyNode node = row.get(i);
                GraphyNodeContent text = content.get(node.id());
                float reserved = Math.max(text.width(), theme.getTouchTarget());
                positions[i] = Math.min(positions[i], nextLeft - theme.getHorizontalGap() - reserved);
                nextLeft = positions[i];
                bounds.put(node.id(), new LayoutBox(positions[i] + (reserved - text.width()) / 2,
                        y + height - text.height(), text.width(), text.height()));
            }
            reading.addAll(row);
            y += height + theme.getVerticalGap();
        }
        List<StageMarker> markers = new ArrayList<>();
        for (GraphStagePlan.Stage stage : plan.stages) {
            boolean folded = collapsed.contains(stage.number());
            List<String> anchors = folded ? stage.outputs() : stage.operations();
            float top = Float.MAX_VALUE;
            for (String id : anchors) {
                LayoutBox box = bounds.get(id);
                if (box != null) top = Math.min(top, box.y() + box.height() / 2);
            }
            if (top != Float.MAX_VALUE) markers.add(new StageMarker(stage.number(), stage.operations().size(), folded,
                    new LayoutBox(0, Math.max(0, top - theme.getTouchTarget() / 2), gutter, theme.getTouchTarget())));
        }
        GraphyOutput presentation = GraphyOutput.create(output.getCalculatorId(), output.getExpression(), output.getResult(),
                reading, plan.presentation.getConnections(), output.getExplanationTemplate(), output.getMetadata());
        return new LayoutResult(bounds, content, width,
                (int) Math.ceil(y - theme.getVerticalGap() + outer), presentation, plan.originals, markers, gutter);
    }

    static String formatOperator(String symbol) {
        return switch (symbol) {
            case "*" -> "×";
            case "/" -> "÷";
            case "-" -> "−";
            case "sqrt" -> "√";
            default -> symbol;
        };
    }
    record LayoutBox(float x, float y, float width, float height) { }
    record StageMarker(int number, int calculationCount, boolean collapsed, LayoutBox bounds) { }
    record LayoutResult(Map<String, LayoutBox> nodeBounds, Map<String, GraphyNodeContent> content,
                        int contentWidth, int contentHeight, GraphyOutput presentation,
                        Map<String, GraphyNode> originals, List<StageMarker> stages, float gutterWidth) {
        static LayoutResult empty(GraphyOutput output) {
            return new LayoutResult(Collections.emptyMap(), Collections.emptyMap(), 0, 0,
                    GraphyOutput.empty(output.getCalculatorId(), output.getExpression(), output.getResult()),
                    Collections.emptyMap(), Collections.emptyList(), 0);
        }
    }
}
