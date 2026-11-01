package net.smartlogic.unitconverter.graphy.builder;

import net.smartlogic.unitconverter.graphy.integration.ConversionSnapshot;
import net.smartlogic.unitconverter.graphy.model.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConversionGraphBuilderTest {
    private GraphyOutput build(double input, double result, ConversionTransformation transformation) {
        return ConversionGraphBuilder.build("test", new ConversionSnapshot(input, result,
                input + " km", "formatted result", "km", "mi", transformation, 1234),
                new ConversionGraphBuilder.Text("Relationship", "1 km = 0.621371 mi", "× factor", "Explanation"));
    }
    @Test public void linearUsesCommonPrimitivesAndPreservesDisplay() {
        GraphyOutput output = build(5, 3.106855, ConversionTransformation.linear(0.621371));
        assertEquals("formatted result", output.getResult());
        assertEquals(GraphyNodeType.INPUT, output.getNodes().get(0).type());
        assertEquals(GraphyNodeType.CONSTANT, output.getNodes().get(1).type());
        assertEquals(GraphyNodeType.OPERATION, output.getNodes().get(2).type());
        assertEquals(GraphyNodeType.RESULT, output.getNodes().get(3).type());
        assertEquals(4, GraphyTopology.orderedNodes(output).size());
        assertTrue(output.getConnections().stream().anyMatch(edge -> edge.fromNodeId().equals("source")
                && edge.toNodeId().equals("operation")));
        assertFalse(output.getMetadata().containsKey("layout"));
    }
    @Test public void dynamicRateIsDerivedAndCarriesIdentityAndTime() {
        GraphyOutput output = build(0, 0, ConversionTransformation.rate(83.5));
        assertFalse(output.isEmpty());
        assertEquals(GraphyNodeType.DERIVED, output.getNodes().get(1).type());
        assertEquals("exchange-rate", output.getNodes().get(1).semanticRole());
        assertEquals(1234L, output.getMetadata().get("rateTimestamp"));
        assertEquals(83.5, (double) output.getMetadata().get("rate"), 0);
        assertEquals("km", output.getMetadata().get("sourceCurrency"));
    }
    @Test public void invalidRateOrNonFiniteResultProducesEmptyGraph() {
        assertTrue(build(1, 0, ConversionTransformation.rate(0)).isEmpty());
        assertTrue(build(1, 0, ConversionTransformation.rate(-1)).isEmpty());
        assertTrue(build(1, Double.POSITIVE_INFINITY, ConversionTransformation.linear(2)).isEmpty());
        assertTrue(build(Double.NaN, 1, ConversionTransformation.linear(2)).isEmpty());
        assertTrue(build(1, 1, ConversionTransformation.linear(Double.NaN)).isEmpty());
    }
    @Test public void directMappingDoesNotPretendToBeConstantMultiplication() {
        GraphyOutput output = build(5, 375, ConversionTransformation.direct());
        assertEquals(GraphyNodeType.EXPLANATION, output.getNodes().get(1).type());
    }
    @Test public void modelsAreImmutable() {
        GraphyOutput output = build(5, 10, ConversionTransformation.linear(2));
        assertThrows(UnsupportedOperationException.class, () -> output.getNodes().clear());
        assertThrows(UnsupportedOperationException.class, () -> output.getMetadata().clear());
    }
}
