package net.smartlogic.unitconverter.graphy.renderer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

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
 * Canvas-based flowchart renderer for Graphy output graphs.
 */
public class FlowchartGraphView extends View {

    private static final float HORIZONTAL_GAP_DP = 32f;
    private static final float VERTICAL_GAP_DP = 16f;
    private static final float PADDING_DP = 12f;
    private static final float OPERATION_PADDING_DP = 6f;

    private GraphyOutput output;
    private GraphyViewTheme theme;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint connectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Map<String, RectF> nodeBounds = new HashMap<>();
    private int contentWidth;
    private int contentHeight;

    public FlowchartGraphView(@NonNull Context context) {
        super(context);
        init();
    }

    public FlowchartGraphView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        strokePaint.setStyle(Paint.Style.STROKE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        connectorPaint.setStyle(Paint.Style.STROKE);
    }

    public void setGraph(@NonNull GraphyOutput output, @NonNull GraphyViewTheme theme) {
        this.output = output;
        this.theme = theme;
        layoutNodes();
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = resolveSize(contentWidth, widthMeasureSpec);
        int height = resolveSize(contentHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (output == null || theme == null || output.isEmpty()) {
            return;
        }

        drawConnectors(canvas);
        drawNodes(canvas);
    }

    private void layoutNodes() {
        nodeBounds.clear();
        if (output == null || theme == null || output.isEmpty()) {
            contentWidth = 0;
            contentHeight = 0;
            return;
        }

        float density = getResources().getDisplayMetrics().density;
        float padding = PADDING_DP * density;
        float horizontalGap = HORIZONTAL_GAP_DP * density;
        float verticalGap = VERTICAL_GAP_DP * density;
        float cornerRadius = theme.getNodeCornerRadius();
        float minWidth = theme.getNodeMinWidth();
        float minHeight = theme.getNodeMinHeight();
        float operationSize = theme.getOperationMarkerSize();

        Map<String, GraphyNode> nodeMap = new HashMap<>();
        for (GraphyNode node : output.getNodes()) {
            nodeMap.put(node.getId(), node);
        }

        Map<String, Integer> layers = assignLayers(nodeMap, output.getConnections());
        Map<Integer, List<String>> layerNodes = new HashMap<>();
        for (Map.Entry<String, Integer> entry : layers.entrySet()) {
            int layer = entry.getValue();
            layerNodes.computeIfAbsent(layer, key -> new ArrayList<>()).add(entry.getKey());
        }

        Map<String, Float> nodeWidths = new HashMap<>();
        Map<String, Float> nodeHeights = new HashMap<>();
        configureTextPaint();

        for (GraphyNode node : output.getNodes()) {
            if (node.getType() == GraphyNodeType.OPERATION) {
                nodeWidths.put(node.getId(), operationSize);
                nodeHeights.put(node.getId(), operationSize);
            } else {
                float textWidth = textPaint.measureText(node.getDisplayValue());
                float width = Math.max(minWidth, textWidth + padding * 2);
                float height = Math.max(minHeight, textPaint.getTextSize() + padding * 1.5f);
                nodeWidths.put(node.getId(), width);
                nodeHeights.put(node.getId(), height);
            }
        }

        int maxLayer = 0;
        float maxLayerHeight = 0f;
        for (Map.Entry<Integer, List<String>> entry : layerNodes.entrySet()) {
            maxLayer = Math.max(maxLayer, entry.getKey());
            float layerHeight = 0f;
            List<String> ids = entry.getValue();
            for (int i = 0; i < ids.size(); i++) {
                layerHeight += nodeHeights.get(ids.get(i));
                if (i < ids.size() - 1) {
                    layerHeight += verticalGap;
                }
            }
            maxLayerHeight = Math.max(maxLayerHeight, layerHeight);
        }

        float x = padding;
        for (int layer = 0; layer <= maxLayer; layer++) {
            List<String> ids = layerNodes.get(layer);
            if (ids == null) {
                continue;
            }

            float layerHeight = 0f;
            for (int i = 0; i < ids.size(); i++) {
                layerHeight += nodeHeights.get(ids.get(i));
                if (i < ids.size() - 1) {
                    layerHeight += verticalGap;
                }
            }

            float y = padding + (maxLayerHeight - layerHeight) / 2f;
            float maxWidthInLayer = 0f;

            for (String nodeId : ids) {
                float width = nodeWidths.get(nodeId);
                float height = nodeHeights.get(nodeId);
                nodeBounds.put(nodeId, new RectF(x, y, x + width, y + height));
                maxWidthInLayer = Math.max(maxWidthInLayer, width);
                y += height + verticalGap;
            }

            x += maxWidthInLayer + horizontalGap;
        }

        float maxRight = padding;
        float maxBottom = padding;
        for (RectF bounds : nodeBounds.values()) {
            maxRight = Math.max(maxRight, bounds.right);
            maxBottom = Math.max(maxBottom, bounds.bottom);
        }

        contentWidth = (int) Math.ceil(maxRight + padding);
        contentHeight = (int) Math.ceil(maxBottom + padding);
    }

    @NonNull
    private Map<String, Integer> assignLayers(@NonNull Map<String, GraphyNode> nodeMap,
                                                @NonNull List<GraphyConnection> connections) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> outgoing = new HashMap<>();

        for (String id : nodeMap.keySet()) {
            inDegree.put(id, 0);
            outgoing.put(id, new ArrayList<>());
        }

        for (GraphyConnection connection : connections) {
            if (!nodeMap.containsKey(connection.getFromNodeId())
                    || !nodeMap.containsKey(connection.getToNodeId())) {
                continue;
            }
            outgoing.get(connection.getFromNodeId()).add(connection.getToNodeId());
            inDegree.put(connection.getToNodeId(), inDegree.get(connection.getToNodeId()) + 1);
        }

        Map<String, Integer> layers = new HashMap<>();
        List<String> queue = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
                layers.put(entry.getKey(), 0);
            }
        }

        Set<String> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            String current = queue.remove(0);
            visited.add(current);
            int currentLayer = layers.getOrDefault(current, 0);

            for (String target : outgoing.get(current)) {
                int nextLayer = currentLayer + 1;
                layers.put(target, Math.max(layers.getOrDefault(target, 0), nextLayer));
                inDegree.put(target, inDegree.get(target) - 1);
                if (inDegree.get(target) == 0 && !visited.contains(target)) {
                    queue.add(target);
                }
            }
        }

        for (String id : nodeMap.keySet()) {
            if (!layers.containsKey(id)) {
                layers.put(id, 0);
            }
        }

        return layers;
    }

    private void configureTextPaint() {
        if (theme == null) {
            return;
        }
        textPaint.setTextSize(spToPx(14f));
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    private void drawConnectors(@NonNull Canvas canvas) {
        connectorPaint.setColor(theme.getConnectorColor());
        connectorPaint.setStrokeWidth(theme.getConnectorStrokeWidth());

        for (GraphyConnection connection : output.getConnections()) {
            RectF from = nodeBounds.get(connection.getFromNodeId());
            RectF to = nodeBounds.get(connection.getToNodeId());
            if (from == null || to == null) {
                continue;
            }

            float startX = from.right;
            float startY = from.centerY();
            float endX = to.left;
            float endY = to.centerY();
            float midX = (startX + endX) / 2f;

            canvas.drawLine(startX, startY, midX, startY, connectorPaint);
            canvas.drawLine(midX, startY, midX, endY, connectorPaint);
            canvas.drawLine(midX, endY, endX, endY, connectorPaint);
        }
    }

    private void drawNodes(@NonNull Canvas canvas) {
        strokePaint.setStrokeWidth(theme.getConnectorStrokeWidth());

        for (GraphyNode node : output.getNodes()) {
            RectF bounds = nodeBounds.get(node.getId());
            if (bounds == null) {
                continue;
            }

            switch (node.getType()) {
                case OPERATION:
                    drawOperationNode(canvas, node, bounds);
                    break;
                case RESULT:
                    drawRoundedNode(canvas, node, bounds, theme.getResultNodeColor(), theme.getOnResultColor());
                    break;
                case INPUT:
                case CONSTANT:
                    drawRoundedNode(canvas, node, bounds, theme.getInputNodeColor(), theme.getOnInputColor());
                    break;
                default:
                    drawRoundedNode(canvas, node, bounds, theme.getDerivedNodeColor(), theme.getOnInputColor());
                    break;
            }
        }
    }

    private void drawRoundedNode(@NonNull Canvas canvas,
                                 @NonNull GraphyNode node,
                                 @NonNull RectF bounds,
                                 int fillColor,
                                 int textColor) {
        float radius = theme.getNodeCornerRadius();
        fillPaint.setColor(fillColor);
        strokePaint.setColor(theme.getConnectorColor());
        canvas.drawRoundRect(bounds, radius, radius, fillPaint);
        canvas.drawRoundRect(bounds, radius, radius, strokePaint);

        textPaint.setColor(textColor);
        textPaint.setTextAlign(Paint.Align.CENTER);
        float textY = bounds.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(node.getDisplayValue(), bounds.centerX(), textY, textPaint);
    }

    private void drawOperationNode(@NonNull Canvas canvas,
                                   @NonNull GraphyNode node,
                                   @NonNull RectF bounds) {
        fillPaint.setColor(theme.getOperationNodeColor());
        strokePaint.setColor(theme.getConnectorColor());
        canvas.drawOval(bounds, fillPaint);
        canvas.drawOval(bounds, strokePaint);

        textPaint.setColor(theme.getOnOperationColor());
        textPaint.setTextAlign(Paint.Align.CENTER);
        float textY = bounds.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(node.getLabel(), bounds.centerX(), textY, textPaint);
    }
}
