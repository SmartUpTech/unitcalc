package net.smartlogic.unitconverter.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A node in the evaluation trace tree produced alongside numeric evaluation.
 */
public final class EvalNode {

    public enum Kind {
        NUMBER,
        UNARY_PLUS,
        UNARY_MINUS,
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIVIDE,
        POWER,
        PERCENT,
        SQRT,
        GROUP
    }

    private final Kind kind;
    private final double value;
    private final BigDecimal decimalValue;
    private final String label;
    private final List<EvalNode> children;

    private EvalNode(Kind kind, double value, String label, List<EvalNode> children) {
        this(kind, Double.isFinite(value) ? BigDecimal.valueOf(value) : null, label, children);
    }

    private EvalNode(Kind kind, BigDecimal value, String label, List<EvalNode> children) {
        this.kind = kind;
        this.decimalValue = value;
        this.value = value == null ? Double.NaN : value.doubleValue();
        this.label = label;
        this.children = children == null ? Collections.emptyList() : new ArrayList<>(children);
    }

    public static EvalNode decimalNumber(BigDecimal value, String label) {
        return new EvalNode(Kind.NUMBER, DecimalMath.checked(value), label, Collections.emptyList());
    }

    public static EvalNode decimalOperation(Kind kind, BigDecimal value, String label, EvalNode... children) {
        return new EvalNode(kind, DecimalMath.checked(value), label, java.util.Arrays.asList(children));
    }

    public BigDecimal getDecimalValue() {
        if (decimalValue == null) throw new ArithmeticException("Invalid operand");
        return decimalValue;
    }

    public static EvalNode number(double value, String label) {
        return new EvalNode(Kind.NUMBER, value, label, Collections.emptyList());
    }

    public static EvalNode operation(Kind kind, double value, String label, EvalNode... children) {
        List<EvalNode> childList = new ArrayList<>();
        if (children != null) {
            for (EvalNode child : children) {
                if (child != null) {
                    childList.add(child);
                }
            }
        }
        return new EvalNode(kind, value, label, childList);
    }

    @NonNull
    public Kind getKind() {
        return kind;
    }

    public double getValue() {
        return value;
    }

    @NonNull
    public String getLabel() {
        return label;
    }

    @NonNull
    public List<EvalNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    @Nullable
    public EvalNode getChild(int index) {
        if (index < 0 || index >= children.size()) {
            return null;
        }
        return children.get(index);
    }

    public boolean isValid() {
        return decimalValue != null && Double.isFinite(value);
    }
}
