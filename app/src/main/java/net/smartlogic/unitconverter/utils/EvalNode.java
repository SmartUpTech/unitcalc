package net.smartlogic.unitconverter.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    private final String label;
    private final List<EvalNode> children;

    private EvalNode(Kind kind, double value, String label, List<EvalNode> children) {
        this.kind = kind;
        this.value = value;
        this.label = label;
        this.children = children == null ? Collections.emptyList() : new ArrayList<>(children);
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
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
