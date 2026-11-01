package net.smartlogic.unitconverter.graphy.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Immutable semantic content. Shapes, colors and dimensions belong to the renderer. */
public record GraphyNode(@NonNull String id, @NonNull GraphyNodeType type,
                         @NonNull String label, @NonNull String displayValue,
                         @Nullable String semanticRole, @Nullable String formula,
                         @Nullable String description) {
    public GraphyNode(String id, GraphyNodeType type, String label, String displayValue,
                      String semanticRole) {
        this(id, type, label, displayValue, semanticRole, null, null);
    }
}
