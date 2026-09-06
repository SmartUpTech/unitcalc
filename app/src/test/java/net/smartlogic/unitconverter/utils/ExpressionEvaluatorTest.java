package net.smartlogic.unitconverter.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExpressionEvaluatorTest {

    @Test
    public void evaluate_simpleAddition() {
        EvaluationResult result = ExpressionEvaluator.evaluate("2+3");
        assertTrue(result.isValid());
        assertEquals(5.0, result.getValue(), 0.0001);
    }

    @Test
    public void evaluate_respectsOperatorPrecedence() {
        EvaluationResult result = ExpressionEvaluator.evaluate("2+3*4");
        assertTrue(result.isValid());
        assertEquals(14.0, result.getValue(), 0.0001);
    }

    @Test
    public void evaluate_sqrt() {
        EvaluationResult result = ExpressionEvaluator.evaluate("sqrt(16)");
        assertTrue(result.isValid());
        assertEquals(4.0, result.getValue(), 0.0001);
    }

    @Test
    public void evaluate_divisionByZeroIsInvalid() {
        EvaluationResult result = ExpressionEvaluator.evaluate("1/0");
        assertFalse(result.isValid());
    }

    @Test
    public void evaluate_parentheses() {
        EvaluationResult result = ExpressionEvaluator.evaluate("(2+3)*4");
        assertTrue(result.isValid());
        assertEquals(20.0, result.getValue(), 0.0001);
    }

    @Test
    public void evaluate_percent() {
        EvaluationResult result = ExpressionEvaluator.evaluate("50%");
        assertTrue(result.isValid());
        assertEquals(0.5, result.getValue(), 0.0001);
    }

    @Test
    public void evaluate_power() {
        EvaluationResult result = ExpressionEvaluator.evaluate("2^3");
        assertTrue(result.isValid());
        assertEquals(8.0, result.getValue(), 0.0001);
    }

    @Test
    public void evaluate_invalidExpression() {
        EvaluationResult result = ExpressionEvaluator.evaluate("2++");
        assertFalse(result.isValid());
    }

    @Test
    public void trace_containsRootNode() {
        EvaluationResult result = ExpressionEvaluator.evaluate("2+3");
        assertTrue(result.getTrace().isValid());
        assertEquals(5.0, result.getTrace().getRoot().getValue(), 0.0001);
    }
}
