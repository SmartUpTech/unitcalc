package net.smartlogic.unitconverter.graphy.renderer;

import net.smartlogic.unitconverter.graphy.model.GraphyNode;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.graphy.model.GraphyTopology;
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One dependency-layer layout for linear, merge, branch and multi-stage graphs. */
final class GraphLayoutEngine {
    LayoutResult layout(GraphyOutput output, GraphyViewTheme theme, int maxWidthPx) {
        List<GraphyNode> ordered = GraphyTopology.orderedNodes(output);
        if (ordered.isEmpty()) return LayoutResult.empty();
        float gutter = theme.getHorizontalGap();
        float available = Math.max(1, maxWidthPx - gutter * 2);
        Map<String, Integer> depth = new LinkedHashMap<>();
        Map<String, GraphyNodeContent> content = new LinkedHashMap<>();
        Map<Integer, List<GraphyNode>> layers = new LinkedHashMap<>();
        for (GraphyNode node : ordered) {
            int level = 0;
            for (var edge : output.getConnections()) {
                if (edge.toNodeId().equals(node.id())) level = Math.max(level, depth.get(edge.fromNodeId()) + 1);
            }
            depth.put(node.id(), level);
            content.put(node.id(), GraphyNodeContent.measure(node, theme, available));
        }
        // Independent inputs/constants appear immediately before their earliest consumer,
        // instead of forcing every late-stage input into a crowded first row.
        for (GraphyNode node : ordered) {
            boolean leaf = output.getConnections().stream().noneMatch(edge -> edge.toNodeId().equals(node.id()));
            if (leaf) {
                int consumerDepth = Integer.MAX_VALUE;
                for (var edge : output.getConnections()) {
                    if (edge.fromNodeId().equals(node.id())) consumerDepth = Math.min(consumerDepth, depth.get(edge.toNodeId()));
                }
                if (consumerDepth != Integer.MAX_VALUE) depth.put(node.id(), Math.max(0, consumerDepth - 1));
            }
            layers.computeIfAbsent(depth.get(node.id()), ignored -> new ArrayList<>()).add(node);
        }
        Map<String, LayoutBox> bounds = new LinkedHashMap<>();
        float y = theme.getNodePadding();
        // Layers are traversed by depth, independently of definition insertion order.
        int maxDepth = depth.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        for (int level = 0; level <= maxDepth; level++) {
            List<GraphyNode> row = new ArrayList<>();
            float rowWidth = 0;
            for (GraphyNode node : layers.getOrDefault(level, java.util.Collections.emptyList())) {
                float width = content.get(node.id()).width();
                if (!row.isEmpty() && rowWidth + theme.getHorizontalGap() + width > available) {
                    y = placeRow(row, rowWidth, y, available, gutter, theme, content, bounds);
                    row.clear();
                    rowWidth = 0;
                }
                if (!row.isEmpty()) rowWidth += theme.getHorizontalGap();
                row.add(node);
                rowWidth += width;
            }
            if (!row.isEmpty()) y = placeRow(row, rowWidth, y, available, gutter, theme, content, bounds);
        }
        return new LayoutResult(bounds, content, Math.max(1, maxWidthPx),
                (int) Math.ceil(y - theme.getVerticalGap() + theme.getNodePadding()));
    }

    private float placeRow(List<GraphyNode> row, float rowWidth, float y, float available,
                           float gutter, GraphyViewTheme theme, Map<String, GraphyNodeContent> content,
                           Map<String, LayoutBox> bounds) {
        float height = 0;
        for (var node : row) height = Math.max(height, content.get(node.id()).height());
        float x = gutter + (available - rowWidth) / 2;
        for (var node : row) {
            var text = content.get(node.id());
            bounds.put(node.id(), new LayoutBox(x, y + (height - text.height()) / 2,
                    text.width(), text.height()));
            x += text.width() + theme.getHorizontalGap();
        }
        return y + height + theme.getVerticalGap();
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
    record LayoutResult(Map<String, LayoutBox> nodeBounds, Map<String, GraphyNodeContent> content,
                        int contentWidth, int contentHeight) {
        static LayoutResult empty() { return new LayoutResult(java.util.Collections.emptyMap(), java.util.Collections.emptyMap(), 0, 0); }
    }
}
