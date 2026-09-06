package net.smartlogic.unitconverter.graphy.renderer;

import net.smartlogic.unitconverter.graphy.integration.CalculationSnapshot;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.utils.EvaluationResult;
import net.smartlogic.unitconverter.utils.ExpressionEvaluator;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FlowchartRendererTest {

    private final FlowchartRenderer renderer = new FlowchartRenderer();

    @Test
    public void supports_returnsFalseForEmptyOutput() {
        GraphyOutput empty = GraphyOutput.empty(
                CalculationSnapshot.BASIC_CALCULATOR_ID,
                "2++",
                "0"
        );
        assertFalse(renderer.supports(empty));
    }

    @Test
    public void supports_returnsTrueForValidGraph() {
        EvaluationResult evaluation = ExpressionEvaluator.evaluate("2+3*4");
        CalculationSnapshot snapshot = CalculationSnapshot.create(
                CalculationSnapshot.BASIC_CALCULATOR_ID,
                "2+3*4",
                "2+3*4",
                "14",
                evaluation
        );
        GraphyOutput output = new net.smartlogic.unitconverter.graphy.integration.GraphyBridge()
                .build(snapshot);

        assertTrue(renderer.supports(output));
    }
}
