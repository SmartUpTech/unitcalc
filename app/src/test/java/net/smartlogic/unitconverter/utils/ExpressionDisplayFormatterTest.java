package net.smartlogic.unitconverter.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExpressionDisplayFormatterTest {

    @Test
    public void formatsSimpleExpressionWithSemanticSpacing() {
        assertPlainText("6-5×(4-2)", "6 - 5 × (4 - 2) =");
    }

    @Test
    public void formatsComplexExpression() {
        assertPlainText(
                "500+600×6%/(2+3)-sqrt(144)+1250",
                "500 + 600 × 6% ÷ (2 + 3) - √144 + 1,250 =",
                ',',
                4,
                true
        );
    }

    @Test
    public void keepsUnaryNegativeAttachedInsideParentheses() {
        assertPlainText("(-25+10)×3", "(-25 + 10) × 3 =");
    }

    @Test
    public void formatsGroupedSquareRootExpression() {
        assertPlainText("sqrt(25+16)×12.5", "√(25 + 16) × 12.5 =");
    }

    @Test
    public void formatsDivisionAndMultiplicationChain() {
        assertPlainText("100/5-8", "100 ÷ 5 - 8 =");
        assertPlainText("50*(12-4)/2", "50 × (12 - 4) ÷ 2 =");
    }

    @Test
    public void keepsUnaryNegativeWithoutInteriorSpaces() {
        assertPlainText("-5", "-5 =");
    }

    @Test
    public void distinguishesSubtractionFromUnaryNegative() {
        assertPlainText("8-5", "8 - 5 =");
    }

    @Test
    public void keepsDecimalValuesAsSingleNumberToken() {
        assertPlainText("1234.567", "1,234.567 =", ',', 4, true);
    }

    @Test public void enteredDecimalsAreNeverRoundedOrHidden() {
        assertPlainText("1.005", "1.005 =", ',', 2, true);
        assertPlainText("1.", "1. =");
        assertPlainText("1.00", "1.00 =");
        assertPlainText("-0.001", "-0.001 =");
    }

    @Test public void incompleteSquareRootsAreSafeAtEveryTypingStep() {
        assertPlainText("sqrt(", "√( =");
        assertPlainText("sqrt(1", "√(1 =");
        assertPlainText("sqrt(16", "√(16 =");
        assertPlainText("sqrt(16)", "√16 =");
        assertPlainText("sqrt(sqrt(", "√(√( =");
        assertPlainText("sqrt()", "√() =");
        assertPlainText("sqrt(9007199254740993)", "√9007199254740993 =");
    }

    private void assertPlainText(String rawExpression, String expected) {
        assertPlainText(rawExpression, expected, '.', 4, false);
    }

    private void assertPlainText(
            String rawExpression,
            String expected,
            char groupSeparator,
            int maxDecimals,
            boolean useGrouping
    ) {
        ExpressionDisplayFormatter.FormattedExpression formatted =
                ExpressionDisplayFormatter.formatWithOptions(
                        rawExpression,
                        '.',
                        groupSeparator,
                        maxDecimals,
                        useGrouping
                );
        assertEquals(expected, formatted.plainText);
    }
}
