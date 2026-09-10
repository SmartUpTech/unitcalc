package net.smartlogic.unitconverter.graphy.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A single node in a Graphy output graph.
 */
public record GraphyNode(String id, GraphyNodeType type, String label, String displayValue,
                         String semanticRole) {

    public GraphyNode(@NonNull String id,
                      @NonNull GraphyNodeType type,
                      @NonNull String label,
                      @NonNull String displayValue,
                      @Nullable String semanticRole) {
        this.id = id;
        this.type = type;
        this.label = label;
        this.displayValue = displayValue;
        this.semanticRole = semanticRole;
    }

    @Override
    @NonNull
    public String id() {
        return id;
    }

    @Override
    @NonNull
    public GraphyNodeType type() {
        return type;
    }

    @Override
    @NonNull
    public String label() {
        return label;
    }

    @Override
    @NonNull
    public String displayValue() {
        return displayValue;
    }

    @Override
    @Nullable
    public String semanticRole() {
        return semanticRole;
    }
}
