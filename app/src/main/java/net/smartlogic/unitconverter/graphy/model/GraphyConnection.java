package net.smartlogic.unitconverter.graphy.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A directed connection between two Graphy nodes.
 */
public record GraphyConnection(String fromNodeId, String toNodeId, String label) {

    public GraphyConnection(@NonNull String fromNodeId,
                            @NonNull String toNodeId,
                            @Nullable String label) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.label = label;
    }

    @Override
    @NonNull
    public String fromNodeId() {
        return fromNodeId;
    }

    @Override
    @NonNull
    public String toNodeId() {
        return toNodeId;
    }

    @Override
    @Nullable
    public String label() {
        return label;
    }
}
