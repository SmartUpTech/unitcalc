package net.smartlogic.unitconverter.graphy.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stable dependency order shared by layout, validation and accessibility. */
public final class GraphyTopology {
    private GraphyTopology() { }

    /** Empty for malformed graphs: duplicate ids, dangling edges or cycles. */
    public static List<GraphyNode> orderedNodes(GraphyOutput output) {
        Map<String, GraphyNode> nodes = new LinkedHashMap<>();
        Map<String, Integer> degrees = new LinkedHashMap<>();
        Map<String, List<String>> children = new LinkedHashMap<>();
        for (GraphyNode node : output.getNodes()) {
            if (node == null || node.id() == null || node.type() == null || node.label() == null
                    || node.displayValue() == null) return Collections.emptyList();
            if (node.id().isEmpty() || nodes.put(node.id(), node) != null) return Collections.emptyList();
            degrees.put(node.id(), 0);
            children.put(node.id(), new ArrayList<>());
        }
        for (GraphyConnection edge : output.getConnections()) {
            if (!nodes.containsKey(edge.fromNodeId()) || !nodes.containsKey(edge.toNodeId())) {
                return Collections.emptyList();
            }
            children.get(edge.fromNodeId()).add(edge.toNodeId());
            degrees.put(edge.toNodeId(), degrees.get(edge.toNodeId()) + 1);
        }
        ArrayDeque<String> ready = new ArrayDeque<>();
        degrees.forEach((id, degree) -> { if (degree == 0) ready.add(id); });
        List<GraphyNode> ordered = new ArrayList<>();
        while (!ready.isEmpty()) {
            String id = ready.remove();
            ordered.add(nodes.get(id));
            for (String next : children.get(id)) {
                int remaining = degrees.get(next) - 1;
                degrees.put(next, remaining);
                if (remaining == 0) ready.add(next);
            }
        }
        return ordered.size() == nodes.size() ? Collections.unmodifiableList(ordered) : Collections.emptyList();
    }
}
