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

    enum PipelineSide {
        ROOT,
        LEFT,
        RIGHT
    }

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
    private float currentMaxWidth;

    @NonNull
    LayoutResult layout(@NonNull GraphyOutput output, @NonNull GraphyViewTheme theme, int maxWidthPx) {
        reset();
        configureMetrics(theme);
        this.currentMaxWidth = maxWidthPx > 0 ? maxWidthPx : 1000f; // Default if not provided

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
            LayoutBox root = layoutValueSubgraph(rootValueId, PipelineSide.ROOT);
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
        padding = theme.getNodeMinHeight() * 0.28f;
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
                float width = Math.max(minNodeWidth, measured + padding * 1.6f);
                float height = Math.max(minNodeHeight, textSize + padding * 1.2f);
                nodeWidths.put(node.getId(), width);
                nodeHeights.put(node.getId(), height);
            } else {
                float width = Math.max(minNodeWidth, measured + padding * 1.4f);
                float height = Math.max(minNodeHeight, textSize + padding);
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
    private LayoutBox layoutValueSubgraph(@NonNull String valueNodeId, @NonNull PipelineSide side) {
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

        List<String> operandIds = incoming.getOrDefault(operationId, new ArrayList<>());
        LayoutBox cluster = layoutOperationCluster(operationId, valueNodeId, operandIds, side);
        layoutCache.put(valueNodeId, cluster);
        return cluster;
    }

    @NonNull
    private LayoutBox layoutOperationCluster(@NonNull String operationId,
                                           @NonNull String resultId,
                                           @NonNull List<String> operandIds,
                                           @NonNull PipelineSide side) {
        if (operandIds.size() == 2) {
            String leftId = operandIds.get(0);
            String rightId = operandIds.get(1);
            boolean leftCluster = isClusterOperand(leftId);
            boolean rightCluster = isClusterOperand(rightId);

            if (leftCluster && rightCluster) {
                PipelineSide childSide = side == PipelineSide.ROOT ? PipelineSide.LEFT : side;
                LayoutBox top = layoutValueSubgraph(leftId, childSide);
                LayoutBox bottom = layoutValueSubgraph(rightId, childSide);
                return layoutVerticalPipeline(operationId, resultId, top, bottom, childSide);
            }

            if (leftCluster || rightCluster) {
                LayoutBox cluster = layoutValueSubgraph(leftCluster ? leftId : rightId, side);
                LayoutBox companion = layoutValueSubgraph(leftCluster ? rightId : leftId, side);
                boolean companionOnLeft = !leftCluster;
                return layoutAccumulator(operationId, resultId, cluster, companion, companionOnLeft);
            }
        }

        List<LayoutBox> operandBoxes = new ArrayList<>();
        for (String operandId : operandIds) {
            operandBoxes.add(layoutValueSubgraph(operandId, side));
        }
        return layoutHorizontalCluster(operationId, resultId, operandBoxes);
    }

    private boolean isClusterOperand(@NonNull String valueNodeId) {
        GraphyNode node = nodeMap.get(valueNodeId);
        if (node == null) {
            return false;
        }
        if (node.getType() == GraphyNodeType.INPUT
                || node.getType() == GraphyNodeType.CONSTANT) {
            return false;
        }
        String producer = findProducer(valueNodeId);
        if (producer == null) {
            return false;
        }
        List<String> operands = incoming.get(producer);
        return operands != null && !operands.isEmpty();
    }

    private float runway() {
        return horizontalGap;
    }

    /** Space from operand/result boxes down to the operator they feed. */
    private float stemGap() {
        return Math.max(verticalGap, operationSize);
    }

    /** Space from an operator down to its result box. */
    private float resultGap() {
        return Math.max(verticalGap * 0.7f, operationSize * 0.55f);
    }

    @NonNull
    private LayoutBox layoutHorizontalCluster(@NonNull String operationId,
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
        float clusterHeight = maxOperandHeight + stemGap() + operationSize
                + resultGap() + resultHeight;

        Map<String, LayoutBox> bounds = new HashMap<>();
        float operandStartX = (clusterWidth - operandsWidth) / 2f;
        float x = operandStartX;
        for (LayoutBox operand : operandBoxes) {
            float yOffset = (maxOperandHeight - operand.height) / 2f;
            mergeBounds(bounds, operand.nodeBounds, x, yOffset);
            x += operand.width + horizontalGap;
        }

        float opX = (clusterWidth - operationSize) / 2f;
        float opY = maxOperandHeight + stemGap();
        putNode(bounds, operationId, opX, opY, operationSize, operationSize);

        float resultWidth = nodeWidths.get(resultId);
        float resultX = (clusterWidth - resultWidth) / 2f;
        float resultY = opY + operationSize + resultGap();
        putNode(bounds, resultId, resultX, resultY, resultWidth, resultHeight);

        return packBounds(bounds, resultId, 0f, 0f);
    }

    @NonNull
    private LayoutBox layoutParallelColumns(@NonNull String operationId,
                                            @NonNull String resultId,
                                            @NonNull LayoutBox left,
                                            @NonNull LayoutBox right) {
        float gutter = runway();
        Map<String, LayoutBox> bounds = new HashMap<>();
        mergeBounds(bounds, left.nodeBounds, 0f, 0f);
        mergeBounds(bounds, right.nodeBounds, left.width + gutter, 0f);

        float columnsHeight = Math.max(left.height, right.height);
        float innerWidth = left.width + gutter + right.width;
        float opY = columnsHeight + stemGap();
        float opX = (innerWidth - operationSize) / 2f;
        putNode(bounds, operationId, opX, opY, operationSize, operationSize);

        float resultWidth = nodeWidths.get(resultId);
        float resultHeight = nodeHeights.get(resultId);
        float resultX = opX + operationSize / 2f - resultWidth / 2f;
        float resultY = opY + operationSize + resultGap();
        putNode(bounds, resultId, resultX, resultY, resultWidth, resultHeight);

        return packBounds(bounds, resultId, gutter, gutter);
    }

    @NonNull
    private LayoutBox layoutVerticalPipeline(@NonNull String operationId,
                                             @NonNull String resultId,
                                             @NonNull LayoutBox top,
                                             @NonNull LayoutBox bottom,
                                             @NonNull PipelineSide side) {
        float gutter = runway();
        float innerWidth = Math.max(top.width, bottom.width);
        innerWidth = Math.max(innerWidth, operationSize);
        innerWidth = Math.max(innerWidth, nodeWidths.get(resultId));

        Map<String, LayoutBox> bounds = new HashMap<>();
        mergeBounds(bounds, top.nodeBounds, (innerWidth - top.width) / 2f, 0f);
        float bottomY = top.height + verticalGap;
        mergeBounds(bounds, bottom.nodeBounds, (innerWidth - bottom.width) / 2f, bottomY);

        float opY = bottomY + bottom.height + stemGap();
        float opX = (innerWidth - operationSize) / 2f;
        putNode(bounds, operationId, opX, opY, operationSize, operationSize);

        float resultWidth = nodeWidths.get(resultId);
        float resultHeight = nodeHeights.get(resultId);
        float resultX = opX + operationSize / 2f - resultWidth / 2f;
        float resultY = opY + operationSize + resultGap();
        putNode(bounds, resultId, resultX, resultY, resultWidth, resultHeight);

        boolean runwayOnLeft = side != PipelineSide.RIGHT;
        return packBounds(bounds, resultId, runwayOnLeft ? gutter : 0f, runwayOnLeft ? 0f : gutter);
    }

    /**
     * Timeline / BODMAS accumulator: the already-evaluated cluster is drawn first
     * (above). The remaining operand joins at the cluster result row, then the
     * operator consumes both. Example: 9×6=54, then 54×6.
     */
    @NonNull
    private LayoutBox layoutAccumulator(@NonNull String operationId,
                                      @NonNull String resultId,
                                      @NonNull LayoutBox cluster,
                                      @NonNull LayoutBox sideBranch,
                                      boolean companionOnLeft) {
        if (cluster.nodeBounds.get(cluster.rootNodeId) == null) {
            return layoutHorizontalCluster(operationId, resultId, companionOnLeft
                    ? java.util.Arrays.asList(sideBranch, cluster)
                    : java.util.Arrays.asList(cluster, sideBranch));
        }

        float spacing = horizontalGap;
        float clusterX = companionOnLeft ? sideBranch.width + spacing : 0f;
        float sideX = companionOnLeft ? 0f : cluster.width + spacing;

        Map<String, LayoutBox> bounds = new HashMap<>();
        mergeBounds(bounds, cluster.nodeBounds, clusterX, 0f);

        LayoutBox clusterRoot = bounds.get(cluster.rootNodeId);
        LayoutBox sideRoot = sideBranch.nodeBounds.get(sideBranch.rootNodeId);
        float sideRootLocalY = sideRoot != null ? sideRoot.y : 0f;
        float sideY = clusterRoot.y - sideRootLocalY;
        if (sideY < 0f) {
            offsetBounds(bounds, 0f, -sideY);
            clusterRoot = bounds.get(cluster.rootNodeId);
            sideY = 0f;
        }
        mergeBounds(bounds, sideBranch.nodeBounds, sideX, sideY);

        float clusterBottom = clusterRoot.y + clusterRoot.height;
        float sideBottom = sideY + sideBranch.height;
        float opY = Math.max(clusterBottom, sideBottom) + stemGap();

        float clusterCenterX = clusterRoot.x + clusterRoot.width / 2f;
        float sideCenterX = sideX + (sideRoot != null ? sideRoot.x + sideRoot.width / 2f : sideBranch.width / 2f);
        float opX = (clusterCenterX + sideCenterX) / 2f - operationSize / 2f;
        putNode(bounds, operationId, opX, opY, operationSize, operationSize);

        float resultWidth = nodeWidths.get(resultId);
        float resultHeight = nodeHeights.get(resultId);
        float resultX = opX + operationSize / 2f - resultWidth / 2f;
        float resultY = opY + operationSize + resultGap();
        putNode(bounds, resultId, resultX, resultY, resultWidth, resultHeight);

        return packBounds(bounds, resultId, 0f, 0f);
    }

    @NonNull
    private LayoutBox layoutWrappedOperationCluster(@NonNull String operationId,
                                                  @NonNull String resultId,
                                                  @NonNull List<LayoutBox> operandBoxes) {
        List<List<LayoutBox>> rows = new ArrayList<>();
        List<LayoutBox> currentRow = new ArrayList<>();
        float currentRowWidth = 0f;
        float maxRowWidth = 0f;

        for (LayoutBox box : operandBoxes) {
            if (!currentRow.isEmpty() && currentRowWidth + horizontalGap + box.width > currentMaxWidth - padding * 2f) {
                rows.add(currentRow);
                maxRowWidth = Math.max(maxRowWidth, currentRowWidth);
                currentRow = new ArrayList<>();
                currentRowWidth = 0f;
            }
            if (!currentRow.isEmpty()) currentRowWidth += horizontalGap;
            currentRow.add(box);
            currentRowWidth += box.width;
        }
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
            maxRowWidth = Math.max(maxRowWidth, currentRowWidth);
        }

        float clusterWidth = Math.max(maxRowWidth, operationSize);
        clusterWidth = Math.max(clusterWidth, nodeWidths.get(resultId));

        Map<String, LayoutBox> bounds = new HashMap<>();
        float currentY = 0f;
        for (List<LayoutBox> row : rows) {
            float rowWidth = 0f;
            float rowMaxHeight = 0f;
            for (LayoutBox box : row) {
                rowWidth += box.width;
                rowMaxHeight = Math.max(rowMaxHeight, box.height);
            }
            rowWidth += (row.size() - 1) * horizontalGap;
            float x = (clusterWidth - rowWidth) / 2f;
            for (LayoutBox box : row) {
                mergeBounds(bounds, box.nodeBounds, x, currentY + (rowMaxHeight - box.height) / 2f);
                x += box.width + horizontalGap;
            }
            currentY += rowMaxHeight + stemGap();
        }

        float opX = (clusterWidth - operationSize) / 2f;
        float opY = currentY;
        putNode(bounds, operationId, opX, opY, operationSize, operationSize);

        float resultWidth = nodeWidths.get(resultId);
        float resultHeight = nodeHeights.get(resultId);
        float resultY = opY + operationSize + resultGap();
        putNode(bounds, resultId, (clusterWidth - resultWidth) / 2f, resultY, resultWidth, resultHeight);

        return new LayoutBox(bounds, clusterWidth, resultY + resultHeight, resultId);
    }

    @NonNull
    private LayoutBox singleNodeBox(@NonNull String nodeId) {
        Map<String, LayoutBox> bounds = new HashMap<>();
        float width = nodeWidths.get(nodeId);
        float height = nodeHeights.get(nodeId);
        putNode(bounds, nodeId, 0f, 0f, width, height);
        return new LayoutBox(bounds, width, height, nodeId);
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
        
        // Use primary result as root if available
        String rootId = primaryResultId != null ? primaryResultId : inputId;

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

        return new LayoutBox(bounds, contentWidth, y, rootId);
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
        y += ch + stemGap();

        if (operationId != null) {
            float opW = nodeWidths.get(operationId);
            float opH = nodeHeights.get(operationId);
            putNode(bounds, operationId, (width - opW) / 2f, y, opW, opH);
            y += opH + resultGap();
        }

        String rootId = resultId != null ? resultId : (operationId != null ? operationId : constantId);

        if (resultId != null) {
            float rw = nodeWidths.get(resultId);
            float rh = nodeHeights.get(resultId);
            putNode(bounds, resultId, (width - rw) / 2f, y, rw, rh);
            y += rh;
        }

        return new LayoutBox(bounds, width, y, rootId);
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
        bounds.put(nodeId, new LayoutBox(x, y, width, height, nodeId));
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

    @NonNull
    private static LayoutBox packBounds(@NonNull Map<String, LayoutBox> bounds,
                                        @NonNull String rootNodeId,
                                        float leftGutter,
                                        float rightGutter) {
        if (bounds.isEmpty()) {
            return LayoutBox.empty();
        }
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;
        for (LayoutBox box : bounds.values()) {
            minX = Math.min(minX, box.x);
            minY = Math.min(minY, box.y);
            maxX = Math.max(maxX, box.x + box.width);
            maxY = Math.max(maxY, box.y + box.height);
        }
        offsetBounds(bounds, leftGutter - minX, -minY);
        float width = leftGutter + (maxX - minX) + rightGutter;
        float height = maxY - minY;
        return new LayoutBox(bounds, width, height, rootNodeId);
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
        final String rootNodeId;

        LayoutBox(@NonNull Map<String, LayoutBox> nodeBounds, float width, float height, String rootNodeId) {
            this(nodeBounds, 0f, 0f, width, height, rootNodeId);
        }

        LayoutBox(float x, float y, float width, float height, String rootNodeId) {
            this.nodeBounds = new HashMap<>();
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.rootNodeId = rootNodeId;
        }

        LayoutBox(@NonNull Map<String, LayoutBox> nodeBounds, float x, float y, float width, float height, String rootNodeId) {
            this.nodeBounds = nodeBounds;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.rootNodeId = rootNodeId;
        }

        @NonNull
        LayoutBox offset(float dx, float dy) {
            if (nodeBounds.isEmpty()) {
                return new LayoutBox(x + dx, y + dy, width, height, rootNodeId);
            }
            Map<String, LayoutBox> shifted = new HashMap<>();
            for (Map.Entry<String, LayoutBox> entry : nodeBounds.entrySet()) {
                shifted.put(entry.getKey(), entry.getValue().offset(dx, dy));
            }
            return new LayoutBox(shifted, x + dx, y + dy, width, height, rootNodeId);
        }

        int complexity() {
            if (nodeBounds.isEmpty()) return 1;
            int count = 0;
            for (LayoutBox b : nodeBounds.values()) {
                if (b.nodeBounds.isEmpty()) count++;
            }
            return count;
        }

        @NonNull
        static LayoutBox empty() {
            return new LayoutBox(new HashMap<>(), 0f, 0f, null);
        }
    }
}
