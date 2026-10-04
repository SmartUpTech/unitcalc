package net.smartlogic.unitconverter.graphy.builder;

import net.smartlogic.unitconverter.graphy.integration.ConversionSnapshot;
import net.smartlogic.unitconverter.graphy.model.ConversionTransformation;
import net.smartlogic.unitconverter.graphy.model.GraphyConnection;
import net.smartlogic.unitconverter.graphy.model.GraphyNode;
import net.smartlogic.unitconverter.graphy.model.GraphyNodeType;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Semantic converter adapter. Never guesses mathematics from display strings. */
public final class ConversionGraphBuilder {
    public static final String UNIT_CONVERTER_ID = "unit-converter";
    public static final String CURRENCY_CONVERTER_ID = "currency-converter";
    private ConversionGraphBuilder() { }

    /** Localized explanatory content; contains no layout, color or component instructions. */
    public record Text(String relationshipLabel, String relationship, String operation,
                       String explanation, String notice) {
        public Text(String relationshipLabel, String relationship, String operation, String explanation) {
            this(relationshipLabel, relationship, operation, explanation, null);
        }
    }

    public static GraphyOutput build(String calculatorId, ConversionSnapshot snapshot, Text text) {
        if (!snapshot.isValid()) {
            return GraphyOutput.empty(calculatorId, snapshot.sourceDisplay(), snapshot.targetDisplay());
        }
        var kind = snapshot.transformation().kind();
        boolean rate = kind == ConversionTransformation.Kind.RATE_BASED;
        var relationshipType = rate ? GraphyNodeType.DERIVED
                : kind == ConversionTransformation.Kind.DIRECT ? GraphyNodeType.EXPLANATION : GraphyNodeType.CONSTANT;
        List<GraphyNode> nodes = java.util.Arrays.asList(
                new GraphyNode("source", GraphyNodeType.INPUT, snapshot.sourceDisplay(),
                        snapshot.sourceDisplay(), "input", null, null),
                new GraphyNode("relationship", relationshipType, text.relationshipLabel(),
                        text.relationship(), rate ? "exchange-rate" : "relationship",
                        text.operation(), text.explanation()),
                new GraphyNode("operation", GraphyNodeType.OPERATION, text.operation(),
                        text.operation(), "conversion", text.operation(), text.explanation()),
                new GraphyNode("target", GraphyNodeType.RESULT, snapshot.targetDisplay(),
                        snapshot.targetDisplay(), "result", text.operation(), text.explanation())
        );
        List<GraphyConnection> edges = java.util.Arrays.asList(new GraphyConnection("source", "relationship", null),
                new GraphyConnection("relationship", "operation", null),
                // Carry the input as a real dependency, as well as the relationship.
                new GraphyConnection("source", "operation", null),
                new GraphyConnection("operation", "target", null));
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (text.notice() != null && !text.notice().trim().isEmpty()) {
            metadata.put(GraphyOutput.METADATA_NOTICE, text.notice());
        }
        metadata.put("transformation", snapshot.transformation());
        metadata.put("sourceUnit", snapshot.sourceUnit());
        metadata.put("targetUnit", snapshot.targetUnit());
        if (rate) {
            metadata.put("rate", snapshot.transformation().steps().get(0).operand());
            metadata.put("rateTimestamp", snapshot.rateTimestamp());
            metadata.put("sourceCurrency", snapshot.sourceUnit());
            metadata.put("targetCurrency", snapshot.targetUnit());
        }
        return GraphyOutput.create(calculatorId, snapshot.sourceDisplay() + " → " + snapshot.targetDisplay(),
                snapshot.targetDisplay(), nodes, edges, text.explanation(), metadata);
    }
}
