package net.smartlogic.unitconverter.utils;

import androidx.annotation.Nullable;

/**
 * Root trace for a single expression evaluation.
 */
public final class EvalTrace {

    private final EvalNode root;

    private EvalTrace(EvalNode root) {
        this.root = root;
    }

    public static EvalTrace of(@Nullable EvalNode root) {
        return new EvalTrace(root);
    }

    public static EvalTrace empty() {
        return new EvalTrace(null);
    }

    @Nullable
    public EvalNode getRoot() {
        return root;
    }

    public boolean isValid() {
        return root != null && root.isValid();
    }
}
