package net.smartlogic.unitconverter.utils;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.theme.CalculatorTheme;
import net.smartlogic.unitconverter.theme.ThemeManager;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tokenizes calculator expressions and produces display text with semantic coloring.
 * Spacing is derived from token boundaries, not blind character replacement.
 */
public final class ExpressionDisplayFormatter {

    public enum SemanticRole {
        NUMBER,
        OPERATOR,
        FUNCTION,
        EQUAL
    }

    public static final class DisplaySegment {
        public final String text;
        public final SemanticRole role;

        DisplaySegment(@NonNull String text, @NonNull SemanticRole role) {
            this.text = text;
            this.role = role;
        }
    }

    public static final class FormattedExpression {
        public final List<DisplaySegment> segments;
        public final String plainText;

        FormattedExpression(@NonNull List<DisplaySegment> segments, @NonNull String plainText) {
            this.segments = segments;
            this.plainText = plainText;
        }
    }

    private enum TokenKind {
        NUMBER,
        BINARY_OPERATOR,
        OPEN_PAREN,
        CLOSE_PAREN,
        PERCENT,
        POWER,
        SQRT_SIMPLE,
        SQRT_GROUP
    }

    private static final class Token {
        final TokenKind kind;
        final String value;
        final List<Token> children;

        Token(@NonNull TokenKind kind, @NonNull String value) {
            this(kind, value, null);
        }

        Token(@NonNull TokenKind kind, @NonNull String value, @Nullable List<Token> children) {
            this.kind = kind;
            this.value = value;
            this.children = children;
        }
    }

    private ExpressionDisplayFormatter() {
    }

    @NonNull
    public static FormattedExpression format(@NonNull String rawExpression, @NonNull Preferences prefs) {
        String groupSeparator = prefs.getGroupSeparator();
        boolean useGrouping = groupSeparator.length() > 0
                && !"None".equals(groupSeparator)
                && groupSeparator.charAt(0) != 'N';
        char groupChar = useGrouping ? firstChar(groupSeparator, ',') : ',';
        return formatWithOptions(
                rawExpression,
                firstChar(prefs.getDecimalSeparator(), '.'),
                groupChar,
                prefs.getNumberDecimals(),
                useGrouping
        );
    }

    @NonNull
    static FormattedExpression formatWithOptions(
            @NonNull String rawExpression,
            char decimalSeparator,
            char groupSeparator,
            int maxDecimals,
            boolean useGrouping
    ) {
        if (rawExpression.isEmpty()) {
            return new FormattedExpression(new ArrayList<>(), "");
        }

        List<Token> tokens = tokenize(rawExpression);
        NumberFormatter numberFormatter = new NumberFormatter(
                decimalSeparator,
                groupSeparator,
                maxDecimals,
                useGrouping
        );
        List<DisplaySegment> segments = new ArrayList<>();
        buildDisplaySegments(tokens, numberFormatter, segments, true);

        StringBuilder plain = new StringBuilder();
        for (DisplaySegment segment : segments) {
            plain.append(segment.text);
        }
        return new FormattedExpression(segments, plain.toString());
    }

    @NonNull
    public static CharSequence toSpannable(
            @NonNull FormattedExpression formatted,
            @NonNull GraphySemanticTheme theme
    ) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (DisplaySegment segment : formatted.segments) {
            int start = builder.length();
            builder.append(segment.text);
            builder.setSpan(
                    new ForegroundColorSpan(theme.colorFor(segment.role)),
                    start,
                    builder.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        return builder;
    }

    @NonNull
    public static CharSequence formatSpannable(
            @NonNull String rawExpression,
            @NonNull Preferences prefs,
            @NonNull GraphySemanticTheme theme
    ) {
        FormattedExpression formatted = format(rawExpression, prefs);
        if (formatted.plainText.isEmpty()) {
            return "";
        }
        return toSpannable(formatted, theme);
    }

    @NonNull
    private static List<Token> tokenize(@NonNull String expression) {
        List<Token> tokens = new ArrayList<>();
        int index = 0;
        while (index < expression.length()) {
            if (expression.startsWith("sqrt(", index)) {
                int closeIndex = findMatchingCloseParen(expression, index + 4);
                String inner = expression.substring(index + 5, closeIndex);
                List<Token> innerTokens = tokenize(inner);
                if (innerTokens.size() == 1 && innerTokens.get(0).kind == TokenKind.NUMBER) {
                    tokens.add(new Token(TokenKind.SQRT_SIMPLE, innerTokens.get(0).value));
                } else {
                    tokens.add(new Token(TokenKind.SQRT_GROUP, "", innerTokens));
                }
                index = closeIndex + 1;
                continue;
            }

            char current = expression.charAt(index);
            if (Character.isDigit(current) || current == '.') {
                int start = index;
                index++;
                while (index < expression.length()) {
                    char next = expression.charAt(index);
                    if (Character.isDigit(next) || next == '.') {
                        index++;
                    } else {
                        break;
                    }
                }
                tokens.add(new Token(TokenKind.NUMBER, expression.substring(start, index)));
                continue;
            }

            if (current == '-') {
                if (isUnaryMinus(expression, index)) {
                    int start = index;
                    index++;
                    while (index < expression.length()) {
                        char next = expression.charAt(index);
                        if (Character.isDigit(next) || next == '.') {
                            index++;
                        } else {
                            break;
                        }
                    }
                    if (index == start + 1) {
                        tokens.add(new Token(TokenKind.BINARY_OPERATOR, "-"));
                    } else {
                        tokens.add(new Token(TokenKind.NUMBER, expression.substring(start, index)));
                    }
                } else {
                    tokens.add(new Token(TokenKind.BINARY_OPERATOR, "-"));
                    index++;
                }
                continue;
            }

            if (current == '+') {
                tokens.add(new Token(TokenKind.BINARY_OPERATOR, "+"));
            } else if (current == '×' || current == '*') {
                tokens.add(new Token(TokenKind.BINARY_OPERATOR, "×"));
            } else if (current == '÷' || current == '/') {
                tokens.add(new Token(TokenKind.BINARY_OPERATOR, "÷"));
            } else if (current == '(') {
                tokens.add(new Token(TokenKind.OPEN_PAREN, "("));
            } else if (current == ')') {
                tokens.add(new Token(TokenKind.CLOSE_PAREN, ")"));
            } else if (current == '%') {
                tokens.add(new Token(TokenKind.PERCENT, "%"));
            } else if (current == '^') {
                tokens.add(new Token(TokenKind.POWER, "^"));
            }
            index++;
        }
        return tokens;
    }

    private static void buildDisplaySegments(
            @NonNull List<Token> tokens,
            @NonNull NumberFormatter numberFormatter,
            @NonNull List<DisplaySegment> out,
            boolean appendEqual
    ) {
        Token previous = null;
        for (Token token : tokens) {
            appendTokenSegments(token, previous, numberFormatter, out);
            previous = token;
        }
        if (appendEqual) {
            if (needsSpaceBeforeEqual(previous)) {
                out.add(new DisplaySegment(" ", SemanticRole.NUMBER));
            }
            out.add(new DisplaySegment("=", SemanticRole.EQUAL));
        }
    }

    private static void appendTokenSegments(
            @NonNull Token token,
            @Nullable Token previous,
            @NonNull NumberFormatter numberFormatter,
            @NonNull List<DisplaySegment> out
    ) {
        switch (token.kind) {
            case NUMBER:
                if (needsSpaceBefore(previous, token)) {
                    out.add(new DisplaySegment(" ", SemanticRole.NUMBER));
                }
                out.add(new DisplaySegment(
                        numberFormatter.format(token.value),
                        SemanticRole.NUMBER
                ));
                break;
            case BINARY_OPERATOR:
                if (previous != null) {
                    out.add(new DisplaySegment(" ", SemanticRole.NUMBER));
                }
                out.add(new DisplaySegment(displayOperator(token.value), SemanticRole.OPERATOR));
                out.add(new DisplaySegment(" ", SemanticRole.NUMBER));
                break;
            case OPEN_PAREN:
                if (needsSpaceBefore(previous, token)) {
                    out.add(new DisplaySegment(" ", SemanticRole.NUMBER));
                }
                out.add(new DisplaySegment("(", SemanticRole.FUNCTION));
                break;
            case CLOSE_PAREN:
                out.add(new DisplaySegment(")", SemanticRole.FUNCTION));
                break;
            case PERCENT:
                out.add(new DisplaySegment("%", SemanticRole.FUNCTION));
                break;
            case POWER:
                out.add(new DisplaySegment("^", SemanticRole.FUNCTION));
                break;
            case SQRT_SIMPLE:
                if (needsSpaceBefore(previous, token)) {
                    out.add(new DisplaySegment(" ", SemanticRole.NUMBER));
                }
                out.add(new DisplaySegment("√", SemanticRole.FUNCTION));
                out.add(new DisplaySegment(
                        numberFormatter.format(token.value),
                        SemanticRole.NUMBER
                ));
                break;
            case SQRT_GROUP:
                if (needsSpaceBefore(previous, token)) {
                    out.add(new DisplaySegment(" ", SemanticRole.NUMBER));
                }
                out.add(new DisplaySegment("√(", SemanticRole.FUNCTION));
                buildDisplaySegments(token.children, numberFormatter, out, false);
                out.add(new DisplaySegment(")", SemanticRole.FUNCTION));
                break;
            default:
                break;
        }
    }

    private static boolean needsSpaceBefore(@Nullable Token previous, @NonNull Token current) {
        if (previous == null) {
            return false;
        }
        switch (current.kind) {
            case BINARY_OPERATOR:
                return true;
            case OPEN_PAREN:
                return previous.kind == TokenKind.NUMBER
                        || previous.kind == TokenKind.CLOSE_PAREN
                        || previous.kind == TokenKind.PERCENT;
            case SQRT_SIMPLE:
            case SQRT_GROUP:
                return previous.kind == TokenKind.NUMBER
                        || previous.kind == TokenKind.CLOSE_PAREN
                        || previous.kind == TokenKind.PERCENT;
            case NUMBER:
                return previous.kind == TokenKind.CLOSE_PAREN;
            default:
                return false;
        }
    }

    private static boolean needsSpaceBeforeEqual(@Nullable Token previous) {
        return previous != null;
    }

    @NonNull
    private static String displayOperator(@NonNull String operator) {
        return operator;
    }

    private static boolean isUnaryMinus(@NonNull String expression, int index) {
        if (expression.charAt(index) != '-') {
            return false;
        }
        if (index == 0) {
            return true;
        }
        char before = expression.charAt(index - 1);
        return before == '('
                || before == '+'
                || before == '-'
                || before == '×'
                || before == '÷'
                || before == '*'
                || before == '/'
                || before == '^'
                || before == '%';
    }

    private static int findMatchingCloseParen(@NonNull String expression, int openParenIndex) {
        int depth = 1;
        int index = openParenIndex + 1;
        while (index < expression.length() && depth > 0) {
            if (expression.startsWith("sqrt(", index)) {
                index = findMatchingCloseParen(expression, index + 4) + 1;
                continue;
            }
            char current = expression.charAt(index);
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
            }
            index++;
        }
        return index - 1;
    }

    private static char firstChar(@NonNull String value, char fallback) {
        return value.isEmpty() ? fallback : value.charAt(0);
    }

    private static final class NumberFormatter {
        private final DecimalFormat decimalFormat;

        NumberFormatter(
                char decimalSeparator,
                char groupSeparator,
                int maxDecimals,
                boolean useGrouping
        ) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            symbols.setDecimalSeparator(decimalSeparator);
            symbols.setGroupingSeparator(groupSeparator);
            decimalFormat = new DecimalFormat("#,##0.########", symbols);
            decimalFormat.setMaximumFractionDigits(maxDecimals);
            decimalFormat.setGroupingUsed(useGrouping);
        }

        @NonNull
        String format(@NonNull String rawNumber) {
            try {
                double value = Double.parseDouble(rawNumber.replace(',', '.'));
                return decimalFormat.format(value);
            } catch (NumberFormatException ignored) {
                return rawNumber;
            }
        }
    }

    public static final class GraphySemanticTheme {
        @ColorInt public final int numberColor;
        @ColorInt public final int operatorColor;
        @ColorInt public final int functionColor;
        @ColorInt public final int utilityColor;
        @ColorInt public final int equalColor;
        @ColorInt public final int primaryTextColor;
        @ColorInt public final int secondaryTextColor;
        @ColorInt public final int backgroundColor;

        public GraphySemanticTheme(
                @ColorInt int numberColor,
                @ColorInt int operatorColor,
                @ColorInt int functionColor,
                @ColorInt int utilityColor,
                @ColorInt int equalColor,
                @ColorInt int primaryTextColor,
                @ColorInt int secondaryTextColor,
                @ColorInt int backgroundColor
        ) {
            this.numberColor = numberColor;
            this.operatorColor = operatorColor;
            this.functionColor = functionColor;
            this.utilityColor = utilityColor;
            this.equalColor = equalColor;
            this.primaryTextColor = primaryTextColor;
            this.secondaryTextColor = secondaryTextColor;
            this.backgroundColor = backgroundColor;
        }

        @NonNull
        public static GraphySemanticTheme from(@NonNull Context context) {
            CalculatorTheme theme = ThemeManager.get();
            return new GraphySemanticTheme(
                    theme.mainText,
                    theme.operatorContentColor(),
                    theme.functions,
                    theme.functions,
                    theme.equal,
                    theme.mainText,
                    theme.functions,
                    theme.background
            );
        }

        @ColorInt
        public int colorFor(@NonNull SemanticRole role) {
            switch (role) {
                case OPERATOR:
                    return operatorColor;
                case FUNCTION:
                    return functionColor;
                case EQUAL:
                    return equalColor;
                case NUMBER:
                default:
                    return numberColor;
            }
        }
    }
}
