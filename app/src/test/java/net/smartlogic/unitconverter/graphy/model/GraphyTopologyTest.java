package net.smartlogic.unitconverter.graphy.model;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class GraphyTopologyTest {
    private GraphyNode node(String id) { return new GraphyNode(id, GraphyNodeType.DERIVED, id, id, null); }
    private GraphyOutput output(List<GraphyNode> nodes, GraphyConnection... edges) {
        return GraphyOutput.create("test", "", "", nodes, List.of(edges), null, null);
    }
    @Test public void branchAndMergeAreOrderedByDependencies() {
        var graph = output(List.of(node("result"), node("a"), node("b"), node("input")),
                new GraphyConnection("input", "a", null), new GraphyConnection("input", "b", null),
                new GraphyConnection("a", "result", null), new GraphyConnection("b", "result", null));
        var order = GraphyTopology.orderedNodes(graph);
        assertEquals("input", order.get(0).id());
        assertEquals("result", order.get(3).id());
    }
    @Test public void cycleDuplicateAndDanglingEdgesAreRejected() {
        assertTrue(GraphyTopology.orderedNodes(output(List.of(node("a"), node("b")),
                new GraphyConnection("a", "b", null), new GraphyConnection("b", "a", null))).isEmpty());
        assertTrue(GraphyTopology.orderedNodes(output(List.of(node("a"), node("a")))).isEmpty());
        assertTrue(GraphyTopology.orderedNodes(output(List.of(node("a")),
                new GraphyConnection("a", "missing", null))).isEmpty());
    }
}
