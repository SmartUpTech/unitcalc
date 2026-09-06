package net.smartlogic.unitconverter.graphy.builder;

import net.smartlogic.unitconverter.graphy.integration.CalculationSnapshot;
import net.smartlogic.unitconverter.graphy.model.GraphyNode;
import net.smartlogic.unitconverter.graphy.model.GraphyNodeType;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.utils.EvaluationResult;
import net.smartlogic.unitconverter.utils.ExpressionEvaluator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExpressionGraphBuilderTest {

    private final ExpressionGraphBuilder builder = new ExpressionGraphBuilder();

    @Test
    public void build_createsResultNodeForSimpleExpression() {
        EvaluationResult evaluation = ExpressionEvaluator.evaluate("2+3");
        CalculationSnapshot snapshot = CalculationSnapshot.create(
                CalculationSnapshot.BASIC_CALCULATOR_ID,
                "2+3",
                "2+3",
                "5",
                evaluation
        );

        GraphyOutput output = builder.build(snapshot, evaluation.getTrace());

        assertFalse(output.isEmpty());
        assertEquals("5", output.getResult());
        assertTrue(hasNodeType(output, GraphyNodeType.RESULT));
        assertTrue(hasNodeType(output, GraphyNodeType.OPERATION));
        assertTrue(hasNodeType(output, GraphyNodeType.INPUT));
    }

    @Test
    public void build_respectsOperatorPrecedence() {
        EvaluationResult evaluation = ExpressionEvaluator.evaluate("2+3*4");
        CalculationSnapshot snapshot = CalculationSnapshot.create(
                CalculationSnapshot.BASIC_CALCULATOR_ID,
                "2+3*4",
                "2+3*4",
                "14",
                evaluation
        );

        GraphyOutput output = builder.build(snapshot, evaluation.getTrace());

        assertEquals("14", output.getResult());
        assertTrue(output.getConnections().size() >= 3);
        assertEquals("2+3*4 = 14", output.getExplanationTemplate());
    }

    @Test
    public void build_returnsEmptyGraphForInvalidExpression() {
        EvaluationResult evaluation = ExpressionEvaluator.evaluate("2++");
        CalculationSnapshot snapshot = CalculationSnapshot.create(
                CalculationSnapshot.BASIC_CALCULATOR_ID,
                "2++",
                "2++",
                "0",
                evaluation
        );

        GraphyOutput output = builder.build(snapshot, evaluation.getTrace());
        assertTrue(output.isEmpty());
    }

    @Test
    public void build_singleNumberProducesResultNode() {
        EvaluationResult evaluation = ExpressionEvaluator.evaluate("42");
        CalculationSnapshot snapshot = CalculationSnapshot.create(
                CalculationSnapshot.BASIC_CALCULATOR_ID,
                "42",
                "42",
                "42",
                evaluation
        );

        GraphyOutput output = builder.build(snapshot, evaluation.getTrace());

        assertEquals(1, output.getNodes().size());
        assertEquals(GraphyNodeType.RESULT, output.getNodes().get(0).getType());
        assertEquals("42", output.getNodes().get(0).getDisplayValue());
    }

    private static boolean hasNodeType(GraphyOutput output, GraphyNodeType type) {
        for (GraphyNode node : output.getNodes()) {
            if (node.getType() == type) {
                return true;
            }
        }
        return false;
    }
}
