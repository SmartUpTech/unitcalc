package net.smartlogic.unitconverter.graphy.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renderer-neutral graph produced from a calculation snapshot.
 */
public final class GraphyOutput {

    public static final String METADATA_NOTICE = "notice";

    private final String calculatorId;
    private final String expression;
    private final String result;
    private final List<GraphyNode> nodes;
    private final List<GraphyConnection> connections;
    private final String explanationTemplate;
    private final Map<String, Object> metadata;

    private GraphyOutput(@NonNull String calculatorId,
                         @NonNull String expression,
                         @NonNull String result,
                         @NonNull List<GraphyNode> nodes,
                         @NonNull List<GraphyConnection> connections,
                         @Nullable String explanationTemplate,
                         @Nullable Map<String, Object> metadata) {
        this.calculatorId = calculatorId;
        this.expression = expression;
        this.result = result;
        this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
        this.connections = Collections.unmodifiableList(new ArrayList<>(connections));
        this.explanationTemplate = explanationTemplate;
        this.metadata = Collections.unmodifiableMap(metadata == null ? new HashMap<>() : new HashMap<>(metadata));
    }

    public static GraphyOutput empty(@NonNull String calculatorId,
                                     @NonNull String expression,
                                     @NonNull String result) {
        return new GraphyOutput(calculatorId, expression, result,
                Collections.emptyList(), Collections.emptyList(), null, Collections.emptyMap());
    }

    public static GraphyOutput create(@NonNull String calculatorId,
                                      @NonNull String expression,
                                      @NonNull String result,
                                      @NonNull List<GraphyNode> nodes,
                                      @NonNull List<GraphyConnection> connections,
                                      @Nullable String explanationTemplate,
                                      @Nullable Map<String, Object> metadata) {
        return new GraphyOutput(calculatorId, expression, result, nodes, connections,
                explanationTemplate, metadata);
    }

    @NonNull
    public String getCalculatorId() {
        return calculatorId;
    }

    @NonNull
    public String getExpression() {
        return expression;
    }

    @NonNull
    public String getResult() {
        return result;
    }

    @NonNull
    public List<GraphyNode> getNodes() {
        return nodes;
    }

    @NonNull
    public List<GraphyConnection> getConnections() {
        return connections;
    }

    @Nullable
    public String getExplanationTemplate() {
        return explanationTemplate;
    }

    @NonNull
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }
}
