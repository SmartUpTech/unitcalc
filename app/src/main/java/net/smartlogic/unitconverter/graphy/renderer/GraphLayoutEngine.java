package net.smartlogic.unitconverter.graphy.renderer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.smartlogic.unitconverter.graphy.model.GraphyConnection;
import net.smartlogic.unitconverter.graphy.model.GraphyNode;
import net.smartlogic.unitconverter.graphy.model.GraphyNodeType;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Computes node positions for Graphy output graphs.
 * Expression graphs use recursive operation-tree layout; converter graphs use branch layout.
 */
final class GraphLayoutEngine {

    private final Map<String, GraphyNode> nodeMap = new HashMap<>();
    private final Map<String, List<String>> incoming = new HashMap<>();
    private final Map<String, List<String>> outgoing = new HashMap<>();
    private final Map<String, Float> nodeWidths = new HashMap<>();
    private final Map<String, Float> nodeHeights = new HashMap<>();
    private final Map<String, Float> textWidths = new HashMap<>();
    private final Map<String, LayoutBox> layoutCache = new HashMap<>();

    private float padding;
    private float horizontalGap;
    private float verticalGap;
    private float branchGap;
    private float operationSize;
    private float minNodeWidth;
    private float minNodeHeight;
    private float valueTextSize;
    private float resultTextSize;

    @NonNull
    LayoutResult layout(@NonNull GraphyOutput output, @NonNull GraphyViewTheme theme) {
        reset();
        configureMetrics(theme);

        for (GraphyNode node : output.getNodes()) {
            nodeMap.put(node.getId(), node);
            incoming.put(node.getId(), new ArrayList<>());
            outgoing.put(node.getId(), new ArrayList<>());
        }
        for (GraphyConnection connection : output.getConnections()) {
            if (!nodeMap.containsKey(connection.getFromNodeId())
                    || !nodeMap.containsKey(connection.getToNodeId())) {
                continue;
            }
            outgoing.get(connection.getFromNodeId()).add(connection.getToNodeId());
            incoming.get(connection.getToNodeId()).add(connection.getFromNodeId());
        }

        measureNodes(output, theme);

        Map<String, LayoutBox> nodeBounds = new HashMap<>();
        float contentWidth;
        float contentHeight;

        if ("branch".equals(output.getMetadata().get("layout"))) {
            LayoutBox root = layoutBranchGraph(output);
            nodeBounds.putAll(root.nodeBounds);
            contentWidth = root.width + padding * 2f;
            contentHeight = root.height + padding * 2f;
            offsetBounds(nodeBounds, padding, padding);
        } else {
            String rootValueId = findRootValueNode(output);
            if (rootValueId == null) {
                return LayoutResult.empty();
            }
            LayoutBox root = layoutValueSubgraph(rootValueId);
            nodeBounds.putAll(root.nodeBounds);
            contentWidth = root.width + padding * 2f;
            contentHeight = root.height + padding * 2f;
            offsetBounds(nodeBounds, padding, padding);
        }

        return new LayoutResult(nodeBounds, (int) Math.ceil(contentWidth), (int) Math.ceil(contentHeight));
    }

    private void reset() {
        nodeMap.clear();
        incoming.clear();
        outgoing.clear();
        nodeWidths.clear();
        nodeHeights.clear();
        textWidths.clear();
        layoutCache.clear();
    }

    private void configureMetrics(@NonNull GraphyViewTheme theme) {
        padding = theme.getNodeMinHeight() * 0.4f;
        horizontalGap = theme.getHorizontalGap();
        verticalGap = theme.getVerticalGap();
        branchGap = theme.getBranchGap();
        operationSize = theme.getOperationMarkerSize();
        minNodeWidth = theme.getNodeMinWidth();
        minNodeHeight = theme.getNodeMinHeight();
        valueTextSize = theme.getValueTextSize();
        resultTextSize = theme.getResultTextSize();
    }

    private void measureNodes(@NonNull GraphyOutput output, @NonNull GraphyViewTheme theme) {
        android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        for (GraphyNode node : output.getNodes()) {
            float textSize = node.getType() == GraphyNodeType.RESULT
                    ? resultTextSize : valueTextSize;
            paint.setTextSize(textSize);
            String text = displayText(node);
            float measured = paint.measureText(text);
            textWidths.put(node.getId(), measured);

            if (node.getType() == GraphyNodeType.OPERATION) {
                nodeWidths.put(node.getId(), operationSize);
                nodeHeights.put(node.getId(), operationSize);
            } else if (node.getType() == GraphyNodeType.RESULT) {
                float width = Math.max(minNodeWidth, measured + padding * 4f);
                float height = Math.max(minNodeHeight * 1.4f, textSize + padding * 2.5f);
                nodeWidths.put(node.getId(), width);
                nodeHeights.put(node.getId(), height);
            } else {
                float width = Math.max(minNodeWidth, measured + padding);
                float height = Math.max(minNodeHeight, textSize + padding * 0.5f);
                nodeWidths.put(node.getId(), width);
                nodeHeights.put(node.getId(), height);
            }
        }
    }

    @Nullable
    private String findRootValueNode(@NonNull GraphyOutput output) {
        for (GraphyNode node : output.getNodes()) {
            if (node.getType() == GraphyNodeType.RESULT) {
                return node.getId();
            }
        }
        GraphyNode lastDerived = null;
        for (GraphyNode node : output.getNodes()) {
            if (node.getType() == GraphyNodeType.DERIVED) {
                lastDerived = node;
            }
        }
        return lastDerived != null ? lastDerived.getId() : null;
    }

    @NonNull
    private LayoutBox layoutValueSubgraph(@NonNull String valueNodeId) {
        if (layoutCache.containsKey(valueNodeId)) {
            return layoutCache.get(valueNodeId);
        }

        GraphyNode valueNode = nodeMap.get(valueNodeId);
        if (valueNode == null) {
            return LayoutBox.empty();
        }

        if (valueNode.getType() == GraphyNodeType.INPUT
                || valueNode.getType() == GraphyNodeType.CONSTANT) {
            LayoutBox leaf = singleNodeBox(valueNodeId);
            layoutCache.put(valueNodeId, leaf);
            return leaf;
        }

        String operationId = findProducer(valueNodeId);
        if (operationId == null) {
            LayoutBox leaf = singleNodeBox(valueNodeId);
            layoutCache.put(valueNodeId, leaf);
            return leaf;
        }

        List<String> operandIds = incoming.get(operationId);
        List<LayoutBox> operandBoxes = new ArrayList<>();
        for (String operandId : operandIds) {
            operandBoxes.add(layoutValueSubgraph(operandId));
        }

        LayoutBox cluster = layoutOperationCluster(operationId, valueNodeId, operandBoxes);
        layoutCache.put(valueNodeId, cluster);
        return cluster;
    }

    @NonNull
    private LayoutBox layoutOperationCluster(@NonNull String operationId,
                                           @NonNull String resultId,
                                           @NonNull List<LayoutBox> operandBoxes) {
        float operandsWidth = 0f;
        float maxOperandHeight = 0f;
        for (int i = 0; i < operandBoxes.size(); i++) {
            LayoutBox box = operandBoxes.get(i);
            operandsWidth += box.width;
            maxOperandHeight = Math.max(maxOperandHeight, box.height);
            if (i < operandBoxes.size() - 1) {
                operandsWidth += horizontalGap;
            }
        }

        float clusterWidth = Math.max(operandsWidth, operationSize);
        clusterWidth = Math.max(clusterWidth, nodeWidths.get(resultId));
        float resultHeight = nodeHeights.get(resultId);
        float clusterHeight = maxOperandHeight + verticalGap * 0.6f + operationSize
                + verticalGap * 0.5f + resultHeight;

        Map<String, LayoutBox> bounds = new HashMap<>();
        float operandY = 0f;
        float operandStartX = (clusterWidth - operandsWidth) / 2f;
        float x = operandStartX;
        for (LayoutBox operand : operandBoxes) {
            mergeBounds(bounds, operand.nodeBounds, x, operandY);
            x += operand.width + horizontalGap;
        }

        float opX = (clusterWidth - operationSize) / 2f;
        float opY = maxOperandHeight + verticalGap * 0.6f;
        putNode(bounds, operationId, opX, opY, operationSize, operationSize);

        float resultWidth = nodeWidths.get(resultId);
        float resultX = (clusterWidth - resultWidth) / 2f;
        float resultY = opY + operationSize + verticalGap * 0.5f;
        putNode(bounds, resultId, resultX, resultY, resultWidth, resultHeight);

        return new LayoutBox(bounds, clusterWidth, clusterHeight);
    }

    @NonNull
    private LayoutBox singleNodeBox(@NonNull String nodeId) {
        Map<String, LayoutBox> bounds = new HashMap<>();
        float width = nodeWidths.get(nodeId);
        float height = nodeHeights.get(nodeId);
        putNode(bounds, nodeId, 0f, 0f, width, height);
        return new LayoutBox(bounds, width, height);
    }

    @NonNull
    private LayoutBox layoutBranchGraph(@NonNull GraphyOutput output) {
        String inputId = null;
        for (GraphyNode node : output.getNodes()) {
            if (node.getType() == GraphyNodeType.INPUT) {
                inputId = node.getId();
            }
        }
        if (inputId == null) {
            return LayoutBox.empty();
        }

        String primaryResultId = null;
        Object primaryMeta = output.getMetadata().get("primaryBranchId");
        if (primaryMeta instanceof String) {
            primaryResultId = (String) primaryMeta;
        }

        List<String> branchConstantIds = new ArrayList<>();
        for (GraphyConnection connection : output.getConnections()) {
            if (connection.getFromNodeId().equals(inputId)) {
                branchConstantIds.add(connection.getToNodeId());
            }
        }

        String primaryConstantId = findBranchConstantForResult(primaryResultId);
        List<String> relatedConstantIds = new ArrayList<>();
        for (String constantId : branchConstantIds) {
            if (!constantId.equals(primaryConstantId)) {
                relatedConstantIds.add(constantId);
            }
        }
        if (primaryConstantId == null && !branchConstantIds.isEmpty()) {
            primaryConstantId = branchConstantIds.get(0);
            relatedConstantIds.remove(primaryConstantId);
        }

        Map<String, LayoutBox> bounds = new HashMap<>();
        float inputWidth = nodeWidths.get(inputId);
        float inputHeight = nodeHeights.get(inputId);

        List<LayoutBox> relatedBoxes = new ArrayList<>();
        for (String constantId : relatedConstantIds) {
            relatedBoxes.add(layoutBranchChain(constantId));
        }
        LayoutBox primaryBox = primaryConstantId != null
                ? layoutBranchChain(primaryConstantId) : LayoutBox.empty();

        float relatedRowWidth = 0f;
        float maxRelatedHeight = 0f;
        for (int i = 0; i < relatedBoxes.size(); i++) {
            LayoutBox box = relatedBoxes.get(i);
            relatedRowWidth += box.width;
            maxRelatedHeight = Math.max(maxRelatedHeight, box.height);
            if (i < relatedBoxes.size() - 1) {
                relatedRowWidth += branchGap;
            }
        }

        float primaryWidth = primaryBox.width;
        float contentWidth = Math.max(inputWidth, Math.max(primaryWidth, relatedRowWidth));

        float y = 0f;
        putNode(bounds, inputId, (contentWidth - inputWidth) / 2f, y, inputWidth, inputHeight);
        y += inputHeight + verticalGap;

        if (primaryConstantId != null) {
            mergeBounds(bounds, primaryBox.nodeBounds,
                    (contentWidth - primaryWidth) / 2f, y);
            y += primaryBox.height + verticalGap;
        }

        if (!relatedBoxes.isEmpty()) {
            float relatedY = y;
            float relatedX = (contentWidth - relatedRowWidth) / 2f;
            for (LayoutBox related : relatedBoxes) {
                mergeBounds(bounds, related.nodeBounds, relatedX, relatedY);
                relatedX += related.width + branchGap;
            }
            y = relatedY + maxRelatedHeight;
        }

        return new LayoutBox(bounds, contentWidth, y);
    }

    @Nullable
    private String findBranchConstantForResult(@Nullable String resultId) {
        if (resultId == null) {
            return null;
        }
        List<String> queue = new ArrayList<>();
        queue.add(resultId);
        Set<String> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            String current = queue.remove(0);
            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);
            GraphyNode node = nodeMap.get(current);
            if (node != null && node.getType() == GraphyNodeType.CONSTANT) {
                return current;
            }
            for (String source : incoming.getOrDefault(current, new ArrayList<>())) {
                queue.add(source);
            }
        }
        return null;
    }

    @NonNull
    private LayoutBox layoutBranchChain(@NonNull String constantId) {
        String operationId = firstOutgoing(constantId);
        String resultId = operationId != null ? firstResultOutgoing(operationId) : null;

        Map<String, LayoutBox> bounds = new HashMap<>();
        float width = nodeWidths.get(constantId);
        if (operationId != null) {
            width = Math.max(width, nodeWidths.get(operationId));
        }
        if (resultId != null) {
            width = Math.max(width, nodeWidths.get(resultId));
        }

        float y = 0f;
        float cw = nodeWidths.get(constantId);
        float ch = nodeHeights.get(constantId);
        putNode(bounds, constantId, (width - cw) / 2f, y, cw, ch);
        y += ch + verticalGap * 0.35f;

        if (operationId != null) {
            float opW = nodeWidths.get(operationId);
            float opH = nodeHeights.get(operationId);
            putNode(bounds, operationId, (width - opW) / 2f, y, opW, opH);
            y += opH + verticalGap * 0.35f;
        }

        if (resultId != null) {
            float rw = nodeWidths.get(resultId);
            float rh = nodeHeights.get(resultId);
            putNode(bounds, resultId, (width - rw) / 2f, y, rw, rh);
            y += rh;
        }

        return new LayoutBox(bounds, width, y);
    }

    @Nullable
    private String firstOutgoing(@NonNull String nodeId) {
        List<String> targets = outgoing.get(nodeId);
        if (targets == null || targets.isEmpty()) {
            return null;
        }
        return targets.get(0);
    }

    @Nullable
    private String firstResultOutgoing(@NonNull String operationId) {
        for (String target : outgoing.getOrDefault(operationId, new ArrayList<>())) {
            GraphyNode node = nodeMap.get(target);
            if (node != null && (node.getType() == GraphyNodeType.RESULT
                    || node.getType() == GraphyNodeType.DERIVED)) {
                return target;
            }
        }
        return null;
    }

    @Nullable
    private String findProducer(@NonNull String valueNodeId) {
        for (String source : incoming.get(valueNodeId)) {
            GraphyNode node = nodeMap.get(source);
            if (node != null && node.getType() == GraphyNodeType.OPERATION) {
                return source;
            }
        }
        return null;
    }

    @NonNull
    private static String displayText(@NonNull GraphyNode node) {
        if (node.getType() == GraphyNodeType.OPERATION) {
            return formatOperator(node.getLabel());
        }
        return node.getDisplayValue();
    }

    @NonNull
    static String formatOperator(@NonNull String label) {
        switch (label) {
            case "*":
                return "×";
            case "/":
                return "÷";
            case "-":
                return "−";
            default:
                return label;
        }
    }

    private static void putNode(@NonNull Map<String, LayoutBox> bounds,
                                @NonNull String nodeId,
                                float x,
                                float y,
                                float width,
                                float height) {
        bounds.put(nodeId, new LayoutBox(x, y, width, height));
    }

    private static void mergeBounds(@NonNull Map<String, LayoutBox> target,
                                    @NonNull Map<String, LayoutBox> source,
                                    float offsetX,
                                    float offsetY) {
        for (Map.Entry<String, LayoutBox> entry : source.entrySet()) {
            LayoutBox box = entry.getValue();
            target.put(entry.getKey(), box.offset(offsetX, offsetY));
        }
    }

    private static void offsetBounds(@NonNull Map<String, LayoutBox> bounds, float dx, float dy) {
        for (Map.Entry<String, LayoutBox> entry : bounds.entrySet()) {
            bounds.put(entry.getKey(), entry.getValue().offset(dx, dy));
        }
    }

    static final class LayoutResult {
        final Map<String, LayoutBox> nodeBounds;
        final int contentWidth;
        final int contentHeight;

        LayoutResult(@NonNull Map<String, LayoutBox> nodeBounds, int contentWidth, int contentHeight) {
            this.nodeBounds = nodeBounds;
            this.contentWidth = contentWidth;
            this.contentHeight = contentHeight;
        }

        @NonNull
        static LayoutResult empty() {
            return new LayoutResult(new HashMap<>(), 0, 0);
        }
    }

    static final class LayoutBox {
        final Map<String, LayoutBox> nodeBounds;
        final float width;
        final float height;
        final float x;
        final float y;

        LayoutBox(@NonNull Map<String, LayoutBox> nodeBounds, float width, float height) {
            this(nodeBounds, 0f, 0f, width, height);
        }

        LayoutBox(float x, float y, float width, float height) {
            this.nodeBounds = new HashMap<>();
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        LayoutBox(@NonNull Map<String, LayoutBox> nodeBounds, float x, float y, float width, float height) {
            this.nodeBounds = nodeBounds;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @NonNull
        LayoutBox offset(float dx, float dy) {
            if (nodeBounds.isEmpty()) {
                return new LayoutBox(x + dx, y + dy, width, height);
            }
            Map<String, LayoutBox> shifted = new HashMap<>();
            for (Map.Entry<String, LayoutBox> entry : nodeBounds.entrySet()) {
                shifted.put(entry.getKey(), entry.getValue().offset(dx, dy));
            }
            return new LayoutBox(shifted, x + dx, y + dy, width, height);
        }

        @NonNull
        static LayoutBox empty() {
            return new LayoutBox(new HashMap<>(), 0f, 0f);
        }
    }
}
