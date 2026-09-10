package net.smartlogic.unitconverter.graphy.builder;

import net.smartlogic.unitconverter.graphy.model.GraphyNode;
import net.smartlogic.unitconverter.graphy.model.GraphyNodeType;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.graphy.renderer.FlowchartRenderer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConversionGraphBuilderTest {

    private final FlowchartRenderer renderer = new FlowchartRenderer();

    @Test
    public void build_createsBranchConversionGraph() {
        GraphyOutput output = ConversionGraphBuilder.build(
                ConversionGraphBuilder.UNIT_CONVERTER_ID,
                "100 km",
                "× 0.621371",
                "62.14 mi",
                "100 km × 0.621371 = 62.14 mi"
        );

        assertEquals("100 km → 62.14 mi", output.getExpression());
        assertEquals("62.14 mi", output.getResult());
        assertTrue(renderer.supports(output));
        assertEquals(4, output.getNodes().size());
        assertEquals(3, output.getConnections().size());
        assertEquals("branch", output.getMetadata().get("layout"));
        assertTrue(hasNodeType(output, GraphyNodeType.INPUT));
        assertTrue(hasNodeType(output, GraphyNodeType.CONSTANT));
        assertTrue(hasNodeType(output, GraphyNodeType.OPERATION));
        assertTrue(hasNodeType(output, GraphyNodeType.RESULT));
    }

    @Test
    public void buildMultiBranch_includesPrimaryAndRelatedBranches() {
        GraphyOutput output = ConversionGraphBuilder.buildMultiBranch(
                ConversionGraphBuilder.UNIT_CONVERTER_ID,
                "12.5 km",
                new ConversionGraphBuilder.BranchSpec("× 1,000", "12,500 m"),
                java.util.Arrays.asList(
                        new ConversionGraphBuilder.BranchSpec("× 1,000,000", "12,500,000 mm"),
                        new ConversionGraphBuilder.BranchSpec("÷ 1.609", "7.767 mi")
                ),
                "12.5 km conversion"
        );

        assertEquals(10, output.getNodes().size());
        assertEquals(9, output.getConnections().size());
        assertEquals("branch", output.getMetadata().get("layout"));
        assertEquals(1, countNodeType(output, GraphyNodeType.RESULT));
        assertEquals(2, countNodeType(output, GraphyNodeType.DERIVED));
    }

    @Test
    public void build_returnsEmptyForBlankInput() {
        GraphyOutput output = ConversionGraphBuilder.build(
                ConversionGraphBuilder.UNIT_CONVERTER_ID,
                " ",
                "× 1",
                "0",
                ""
        );

        assertTrue(output.isEmpty());
    }

    private static boolean hasNodeType(GraphyOutput output, GraphyNodeType type) {
        for (GraphyNode node : output.getNodes()) {
            if (node.type() == type) {
                return true;
            }
        }
        return false;
    }

    private static int countNodeType(GraphyOutput output, GraphyNodeType type) {
        int count = 0;
        for (GraphyNode node : output.getNodes()) {
            if (node.type() == type) {
                count++;
            }
        }
        return count;
    }
}
