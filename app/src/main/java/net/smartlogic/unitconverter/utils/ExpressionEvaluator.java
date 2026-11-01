package net.smartlogic.unitconverter.utils;

import androidx.annotation.NonNull;
import java.math.BigDecimal;

/**
 * Recursive-descent expression evaluator with trace capture.
 * Mirrors the calculator parser semantics used by {@code CalculatorFragment}.
 */
public final class ExpressionEvaluator {

    private ExpressionEvaluator() {
    }

    @NonNull
    public static EvaluationResult evaluate(@NonNull String expression) {
        if (expression.trim().isEmpty() || expression.length() > 1024) {
            return EvaluationResult.invalid();
        }

        int nesting = 0;
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (ch == '(' && ++nesting > 32) return EvaluationResult.invalid();
            if (ch == ')') nesting--;
        }
        try {
            Parser parser = new Parser(expression);
            EvalNode root = parser.parse();
            if (root == null || !root.isValid() || parser.hasRemainingInput()) {
                return EvaluationResult.invalid();
            }
            return EvaluationResult.of(root.getValue(), EvalTrace.of(root));
        } catch (ArithmeticException | IllegalArgumentException invalid) {
            return EvaluationResult.invalid();
        }
    }

    private static final class Parser {
        private final String str;
        private int pos = -1;
        private int ch;
        private int depth;

        Parser(String str) {
            this.str = str;
        }

        boolean hasRemainingInput() {
            return pos < str.length();
        }

        void nextChar() {
            ch = (++pos < str.length()) ? str.charAt(pos) : -1;
        }

        boolean eat(int charToEat) {
            while (ch == ' ') {
                nextChar();
            }
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        EvalNode parse() {
            nextChar();
            return parseExpression();
        }

        EvalNode parseExpression() {
            EvalNode left = parseTerm();
            for (;;) {
                if (eat('+')) {
                    EvalNode right = parseTerm();
                    left = EvalNode.decimalOperation(EvalNode.Kind.ADD, left.getDecimalValue().add(right.getDecimalValue()), "+", left, right);
                } else if (eat('-')) {
                    EvalNode right = parseTerm();
                    left = EvalNode.decimalOperation(EvalNode.Kind.SUBTRACT, left.getDecimalValue().subtract(right.getDecimalValue()), "-", left, right);
                } else {
                    return left;
                }
            }
        }

        EvalNode parseTerm() {
            EvalNode left = parseFactor();
            for (;;) {
                if (eat('*')) {
                    EvalNode right = parseFactor();
                    left = EvalNode.decimalOperation(EvalNode.Kind.MULTIPLY, left.getDecimalValue().multiply(right.getDecimalValue()), "*", left, right);
                } else if (eat('/')) {
                    EvalNode right = parseFactor();
                    BigDecimal divisor = right.getDecimalValue();
                    if (divisor.signum() == 0) {
                        return EvalNode.operation(EvalNode.Kind.DIVIDE, Double.NaN, "/", left, right);
                    }
                    left = EvalNode.decimalOperation(EvalNode.Kind.DIVIDE, DecimalMath.divide(left.getDecimalValue(), divisor), "/", left, right);
                } else {
                    return left;
                }
            }
        }

        EvalNode parseFactor() {
            if (++depth > 64) throw new IllegalArgumentException("Expression too deeply nested");
            try { return parseFactorValue(); }
            finally { depth--; }
        }

        EvalNode parseFactorValue() {
            if (eat('+')) {
                EvalNode child = parseFactor();
                return EvalNode.decimalOperation(EvalNode.Kind.UNARY_PLUS, child.getDecimalValue(), "+", child);
            }
            if (eat('-')) {
                EvalNode child = parseFactor();
                return EvalNode.decimalOperation(EvalNode.Kind.UNARY_MINUS, child.getDecimalValue().negate(), "-", child);
            }

            EvalNode node;
            int startPos = pos;
            if (eat('(')) {
                EvalNode inner = parseExpression();
                if (!eat(')')) {
                    return EvalNode.operation(EvalNode.Kind.GROUP, Double.NaN, "(", inner);
                }
                node = EvalNode.decimalOperation(EvalNode.Kind.GROUP, inner.getDecimalValue(), "(", inner);
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') {
                    nextChar();
                }
                try {
                    String literal = str.substring(startPos, pos);
                    node = EvalNode.decimalNumber(new BigDecimal(literal), literal);
                } catch (NumberFormatException e) {
                    return EvalNode.number(Double.NaN, str.substring(startPos, pos));
                }
            } else if (ch >= 'a' && ch <= 'z') {
                while (ch >= 'a' && ch <= 'z') {
                    nextChar();
                }
                String func = str.substring(startPos, pos);
                EvalNode operand;
                if (eat('(')) {
                    operand = parseExpression();
                    if (!eat(')')) {
                        return EvalNode.operation(EvalNode.Kind.SQRT, Double.NaN, func, operand);
                    }
                } else {
                    operand = parseFactor();
                }
                if ("sqrt".equals(func)) {
                    node = EvalNode.decimalOperation(EvalNode.Kind.SQRT, DecimalMath.sqrt(operand.getDecimalValue()), "sqrt", operand);
                } else {
                    return EvalNode.operation(EvalNode.Kind.SQRT, Double.NaN, func, operand);
                }
            } else {
                throw new IllegalArgumentException("Missing operand");
            }

            if (eat('^')) {
                EvalNode right = parseFactor();
                node = EvalNode.decimalOperation(EvalNode.Kind.POWER, DecimalMath.pow(node.getDecimalValue(), right.getDecimalValue()), "^", node, right);
            }
            if (eat('%')) {
                node = EvalNode.decimalOperation(EvalNode.Kind.PERCENT, node.getDecimalValue().movePointLeft(2), "%", node);
            }
            return node;
        }
    }
}
