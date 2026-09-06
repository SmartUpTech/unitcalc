package net.smartlogic.unitconverter.utils;

import androidx.annotation.NonNull;

/**
 * Recursive-descent expression evaluator with trace capture.
 * Mirrors the calculator parser semantics used by {@code CalculatorFragment}.
 */
public final class ExpressionEvaluator {

    private ExpressionEvaluator() {
    }

    @NonNull
    public static EvaluationResult evaluate(@NonNull String expression) {
        if (expression.trim().isEmpty()) {
            return EvaluationResult.invalid();
        }

        Parser parser = new Parser(expression);
        EvalNode root = parser.parse();
        if (root == null || !root.isValid() || parser.hasRemainingInput()) {
            return EvaluationResult.invalid();
        }
        return EvaluationResult.of(root.getValue(), EvalTrace.of(root));
    }

    private static final class Parser {
        private final String str;
        private int pos = -1;
        private int ch;

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
                    left = EvalNode.operation(EvalNode.Kind.ADD, left.getValue() + right.getValue(), "+", left, right);
                } else if (eat('-')) {
                    EvalNode right = parseTerm();
                    left = EvalNode.operation(EvalNode.Kind.SUBTRACT, left.getValue() - right.getValue(), "-", left, right);
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
                    left = EvalNode.operation(EvalNode.Kind.MULTIPLY, left.getValue() * right.getValue(), "*", left, right);
                } else if (eat('/')) {
                    EvalNode right = parseFactor();
                    double divisor = right.getValue();
                    if (divisor == 0) {
                        return EvalNode.operation(EvalNode.Kind.DIVIDE, Double.NaN, "/", left, right);
                    }
                    left = EvalNode.operation(EvalNode.Kind.DIVIDE, left.getValue() / divisor, "/", left, right);
                } else {
                    return left;
                }
            }
        }

        EvalNode parseFactor() {
            if (eat('+')) {
                EvalNode child = parseFactor();
                return EvalNode.operation(EvalNode.Kind.UNARY_PLUS, child.getValue(), "+", child);
            }
            if (eat('-')) {
                EvalNode child = parseFactor();
                return EvalNode.operation(EvalNode.Kind.UNARY_MINUS, -child.getValue(), "-", child);
            }

            EvalNode node;
            int startPos = pos;
            if (eat('(')) {
                EvalNode inner = parseExpression();
                if (!eat(')')) {
                    return EvalNode.operation(EvalNode.Kind.GROUP, Double.NaN, "(", inner);
                }
                node = EvalNode.operation(EvalNode.Kind.GROUP, inner.getValue(), "(", inner);
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') {
                    nextChar();
                }
                try {
                    String literal = str.substring(startPos, pos);
                    double value = Double.parseDouble(literal);
                    node = EvalNode.number(value, literal);
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
                    node = EvalNode.operation(EvalNode.Kind.SQRT, Math.sqrt(operand.getValue()), "sqrt", operand);
                } else {
                    return EvalNode.operation(EvalNode.Kind.SQRT, Double.NaN, func, operand);
                }
            } else {
                return EvalNode.number(Double.NaN, "");
            }

            if (eat('^')) {
                EvalNode right = parseFactor();
                node = EvalNode.operation(EvalNode.Kind.POWER, Math.pow(node.getValue(), right.getValue()), "^", node, right);
            }
            if (eat('%')) {
                node = EvalNode.operation(EvalNode.Kind.PERCENT, node.getValue() / 100.0, "%", node);
            }
            return node;
        }
    }
}
