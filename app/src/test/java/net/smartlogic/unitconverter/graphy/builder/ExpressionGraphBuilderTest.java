package net.smartlogic.unitconverter.graphy.builder;

import net.smartlogic.unitconverter.graphy.integration.CalculationSnapshot;
import net.smartlogic.unitconverter.graphy.model.GraphyNode;
import net.smartlogic.unitconverter.graphy.model.GraphyNodeType;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.utils.EvaluationResult;
import net.smartlogic.unitconverter.utils.ExpressionEvaluator;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

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
        assertNoDuplicateDisplayValues(output);
    }

    @Test
    public void build_collapsesPercentInMultiplication() {
        EvaluationResult evaluation = ExpressionEvaluator.evaluate("600*6%");
        CalculationSnapshot snapshot = CalculationSnapshot.create(
                CalculationSnapshot.BASIC_CALCULATOR_ID,
                "600*6%",
                "600*6%",
                "36",
                evaluation
        );

        GraphyOutput output = builder.build(snapshot, evaluation.getTrace());

        assertTrue(hasDisplayValue(output, "6%"));
        assertEquals(0, countDisplayValue(output, "0.06"));
    }

    @Test
    public void build_complexExpressionHasSingleResultNode() {
        EvaluationResult evaluation = ExpressionEvaluator.evaluate("(500+600*6%)/2+1250");
        CalculationSnapshot snapshot = CalculationSnapshot.create(
                CalculationSnapshot.BASIC_CALCULATOR_ID,
                "(500+600*6%)/2+1250",
                "(500+600*6%)/2+1250",
                "1518",
                evaluation
        );

        GraphyOutput output = builder.build(snapshot, evaluation.getTrace());

        assertEquals("1518", output.getResult());
        assertEquals(1, countNodeType(output, GraphyNodeType.RESULT));
        assertNoDuplicateDisplayValues(output);
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

    @Test
    public void build_sqrtExpression() {
        EvaluationResult evaluation = ExpressionEvaluator.evaluate("sqrt(16)");
        CalculationSnapshot snapshot = CalculationSnapshot.create(
                CalculationSnapshot.BASIC_CALCULATOR_ID,
                "sqrt(16)",
                "sqrt(16)",
                "4",
                evaluation
        );

        GraphyOutput output = builder.build(snapshot, evaluation.getTrace());

        assertEquals("4", output.getResult());
        assertTrue(hasNodeType(output, GraphyNodeType.OPERATION));
        assertTrue(hasNodeType(output, GraphyNodeType.RESULT));
    }

    @Test
    public void build_parenthesesExpression() {
        EvaluationResult evaluation = ExpressionEvaluator.evaluate("(2+3)*4");
        CalculationSnapshot snapshot = CalculationSnapshot.create(
                CalculationSnapshot.BASIC_CALCULATOR_ID,
                "(2+3)*4",
                "(2+3)*4",
                "20",
                evaluation
        );

        GraphyOutput output = builder.build(snapshot, evaluation.getTrace());

        assertEquals("20", output.getResult());
        assertTrue(output.getConnections().size() >= 3);
    }

    private static boolean hasNodeType(GraphyOutput output, GraphyNodeType type) {
        for (GraphyNode node : output.getNodes()) {
            if (node.getType() == type) {
                return true;
            }
        }
        return false;
    }

    private static int countNodeType(GraphyOutput output, GraphyNodeType type) {
        int count = 0;
        for (GraphyNode node : output.getNodes()) {
            if (node.getType() == type) {
                count++;
            }
        }
        return count;
    }

    private static boolean hasDisplayValue(GraphyOutput output, String value) {
        for (GraphyNode node : output.getNodes()) {
            if (value.equals(node.getDisplayValue())) {
                return true;
            }
        }
        return false;
    }

    private static int countDisplayValue(GraphyOutput output, String value) {
        int count = 0;
        for (GraphyNode node : output.getNodes()) {
            if (value.equals(node.getDisplayValue())) {
                count++;
            }
        }
        return count;
    }

    private static void assertNoDuplicateDisplayValues(GraphyOutput output) {
        Set<String> derivedAndResults = new HashSet<>();
        for (GraphyNode node : output.getNodes()) {
            if (node.getType() == GraphyNodeType.DERIVED || node.getType() == GraphyNodeType.RESULT) {
                assertTrue("Duplicate result node for " + node.getDisplayValue(),
                        derivedAndResults.add(node.getDisplayValue()));
            }
        }
    }
}
