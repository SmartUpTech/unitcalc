package net.smartlogic.unitconverter.graphy.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A directed connection between two Graphy nodes.
 */
public final class GraphyConnection {

    private final String fromNodeId;
    private final String toNodeId;
    private final String label;

    public GraphyConnection(@NonNull String fromNodeId,
                            @NonNull String toNodeId,
                            @Nullable String label) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.label = label;
    }

    @NonNull
    public String getFromNodeId() {
        return fromNodeId;
    }

    @NonNull
    public String getToNodeId() {
        return toNodeId;
    }

    @Nullable
    public String getLabel() {
        return label;
    }
}
