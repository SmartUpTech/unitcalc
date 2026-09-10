package net.smartlogic.unitconverter.graphy.integration;

import net.smartlogic.unitconverter.graphy.model.GraphyNodeType;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.utils.EvaluationResult;
import net.smartlogic.unitconverter.utils.ExpressionEvaluator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GraphyBridgeTest {

    private final GraphyBridge graphyBridge = new GraphyBridge();

    @Test
    public void build_producesGraphForValidCalculation() {
        EvaluationResult evaluation = ExpressionEvaluator.evaluate("2+3*4");
        CalculationSnapshot snapshot = CalculationSnapshot.create(
                CalculationSnapshot.BASIC_CALCULATOR_ID,
                "2+3*4",
                "2+3*4",
                "14",
                evaluation
        );

        GraphyOutput output = graphyBridge.build(snapshot);

        assertFalse(output.isEmpty());
        assertEquals(CalculationSnapshot.BASIC_CALCULATOR_ID, output.getCalculatorId());
        assertEquals("2+3*4", output.getExpression());
        assertEquals("14", output.getResult());
        assertTrue(output.getNodes().stream().anyMatch(n -> n.type() == GraphyNodeType.RESULT));
    }

    @Test
    public void build_returnsEmptyGraphForInvalidCalculation() {
        EvaluationResult evaluation = ExpressionEvaluator.evaluate("2++");
        CalculationSnapshot snapshot = CalculationSnapshot.create(
                CalculationSnapshot.BASIC_CALCULATOR_ID,
                "2++",
                "2++",
                "0",
                evaluation
        );

        GraphyOutput output = graphyBridge.build(snapshot);
        assertTrue(output.isEmpty());
    }
}
