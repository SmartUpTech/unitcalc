package net.smartlogic.unitconverter.graphy.builder;

import net.smartlogic.unitconverter.graphy.model.GraphyNodeType;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.graphy.renderer.FlowchartRenderer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConversionGraphBuilderTest {

    private final FlowchartRenderer renderer = new FlowchartRenderer();

    @Test
    public void build_createsVerticalConversionGraph() {
        GraphyOutput output = ConversionGraphBuilder.build(
                ConversionGraphBuilder.UNIT_CONVERTER_ID,
                "100 km",
                "× 0.621371",
                "62.14 mi",
                "100 km × 0.621371 = 62.14 mi"
        );

        assertTrue(renderer.supports(output));
        assertEquals(3, output.getNodes().size());
        assertEquals(2, output.getConnections().size());
        assertEquals(GraphyNodeType.INPUT, output.getNodes().get(0).getType());
        assertEquals(GraphyNodeType.OPERATION, output.getNodes().get(1).getType());
        assertEquals(GraphyNodeType.RESULT, output.getNodes().get(2).getType());
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
}
