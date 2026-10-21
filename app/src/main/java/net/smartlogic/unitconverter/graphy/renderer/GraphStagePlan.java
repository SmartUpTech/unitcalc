package net.smartlogic.unitconverter.graphy.renderer;

import net.smartlogic.unitconverter.graphy.model.*;
import java.util.*;

/** Dependency stages and folding are presentation only: no evaluation or value copies. */
final class GraphStagePlan {
    record Stage(int number, List<String> operations, List<String> outputs) { }
    final List<Stage> stages = new ArrayList<>();
    final Map<String, Integer> rows = new LinkedHashMap<>();
    final Map<String, GraphyNode> originals = new LinkedHashMap<>();
    final GraphyOutput presentation;

    GraphStagePlan(GraphyOutput output, Set<Integer> collapsed) {
        List<GraphyNode> ordered = GraphyTopology.orderedNodes(output);
        Map<String, List<String>> parents = new LinkedHashMap<>(), children = new LinkedHashMap<>();
        for (GraphyNode node : ordered) {
            originals.put(node.id(), node);
            parents.put(node.id(), new ArrayList<>());
            children.put(node.id(), new ArrayList<>());
        }
        for (GraphyConnection edge : output.getConnections()) {
            parents.get(edge.toNodeId()).add(edge.fromNodeId());
            children.get(edge.fromNodeId()).add(edge.toNodeId());
        }
        Map<String, Boolean> operationAncestor = new HashMap<>();
        Map<Integer, List<String>> operations = new TreeMap<>();
        for (GraphyNode node : ordered) {
            int row = 0;
            boolean ancestor = false;
            for (String parent : parents.get(node.id())) {
                row = Math.max(row, rows.get(parent) + 1);
                ancestor |= operationAncestor.get(parent) || originals.get(parent).type() == GraphyNodeType.OPERATION;
            }
            rows.put(node.id(), row);
            operationAncestor.put(node.id(), ancestor);
            if (node.type() == GraphyNodeType.OPERATION && !children.get(node.id()).isEmpty()) {
                operations.computeIfAbsent(row, ignored -> new ArrayList<>()).add(node.id());
            }
        }
        for (List<String> ids : operations.values()) {
            List<String> outputs = new ArrayList<>();
            for (String id : ids) for (String child : children.get(id)) if (!outputs.contains(child)) outputs.add(child);
            stages.add(new Stage(stages.size() + 1, Collections.unmodifiableList(new ArrayList<>(ids)), Collections.unmodifiableList(new ArrayList<>(outputs))));
        }
        // Introduce original operands just before their first consumer, rather than at the top.
        for (GraphyNode node : ordered) if (parents.get(node.id()).isEmpty() && !children.get(node.id()).isEmpty()) {
            int firstConsumer = Integer.MAX_VALUE;
            for (String child : children.get(node.id())) firstConsumer = Math.min(firstConsumer, rows.get(child));
            rows.put(node.id(), firstConsumer - 1);
        }
        Set<String> hidden = new HashSet<>();
        for (Stage stage : stages) if (collapsed.contains(stage.number())) hidden.addAll(stage.operations());
        // Hide only exclusive input material. Outputs of other calculations remain connection points.
        for (int i = ordered.size() - 1; i >= 0; i--) {
            GraphyNode node = ordered.get(i);
            if (node.type() != GraphyNodeType.RESULT && !operationAncestor.get(node.id())
                    && !children.get(node.id()).isEmpty() && hidden.containsAll(children.get(node.id()))) {
                hidden.add(node.id());
            }
        }
        if (hidden.isEmpty()) { presentation = output; return; }
        List<GraphyNode> visible = new ArrayList<>();
        List<GraphyConnection> edges = new ArrayList<>();
        for (GraphyNode node : ordered) if (!hidden.contains(node.id())) {
            visible.add(node);
            // Contract hidden operations, retaining every visible boundary dependency.
            ArrayDeque<String> pending = new ArrayDeque<>();
            List<String> incoming = parents.get(node.id());
            for (int i = incoming.size() - 1; i >= 0; i--) pending.push(incoming.get(i));
            Set<String> visited = new HashSet<>();
            while (!pending.isEmpty()) {
                String parent = pending.pop();
                if (!visited.add(parent)) continue;
                if (hidden.contains(parent)) {
                    List<String> ancestors = parents.get(parent);
                    for (int i = ancestors.size() - 1; i >= 0; i--) pending.push(ancestors.get(i));
                } else {
                    String label = null;
                    for (GraphyConnection edge : output.getConnections()) {
                        if (edge.fromNodeId().equals(parent) && edge.toNodeId().equals(node.id())) { label = edge.label(); break; }
                    }
                    edges.add(new GraphyConnection(parent, node.id(), label));
                }
            }
        }
        presentation = GraphyOutput.create(output.getCalculatorId(), output.getExpression(), output.getResult(),
                visible, edges, output.getExplanationTemplate(), output.getMetadata());
    }
}
