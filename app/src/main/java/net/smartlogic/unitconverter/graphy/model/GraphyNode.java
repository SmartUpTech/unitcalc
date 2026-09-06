package net.smartlogic.unitconverter.graphy.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A single node in a Graphy output graph.
 */
public final class GraphyNode {

    private final String id;
    private final GraphyNodeType type;
    private final String label;
    private final String displayValue;
    private final String semanticRole;

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

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public GraphyNodeType getType() {
        return type;
    }

    @NonNull
    public String getLabel() {
        return label;
    }

    @NonNull
    public String getDisplayValue() {
        return displayValue;
    }

    @Nullable
    public String getSemanticRole() {
        return semanticRole;
    }
}
