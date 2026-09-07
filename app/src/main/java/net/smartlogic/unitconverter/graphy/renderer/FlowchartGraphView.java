package net.smartlogic.unitconverter.graphy.renderer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
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
import java.util.List;
import java.util.Map;

/**
 * Canvas renderer for connected Graphy calculation graphs.
 */
public class FlowchartGraphView extends View {

    private GraphyOutput output;
    private GraphyViewTheme theme;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint connectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Map<String, RectF> nodeBounds = new HashMap<>();
    private final GraphLayoutEngine layoutEngine = new GraphLayoutEngine();
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
        connectorPaint.setStrokeCap(Paint.Cap.ROUND);
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

        canvas.drawColor(theme.getBackgroundColor());
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

        GraphLayoutEngine.LayoutResult result = layoutEngine.layout(output, theme);
        for (Map.Entry<String, GraphLayoutEngine.LayoutBox> entry : result.nodeBounds.entrySet()) {
            GraphLayoutEngine.LayoutBox box = entry.getValue();
            nodeBounds.put(entry.getKey(), new RectF(box.x, box.y, box.x + box.width, box.y + box.height));
        }
        contentWidth = result.contentWidth;
        contentHeight = result.contentHeight;
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

            GraphyNode fromNode = findNode(connection.getFromNodeId());
            GraphyNode toNode = findNode(connection.getToNodeId());
            if (fromNode == null || toNode == null) {
                continue;
            }

            drawOrthogonalConnector(canvas, from, to, fromNode, toNode);
        }
    }

    private void drawOrthogonalConnector(@NonNull Canvas canvas,
                                         @NonNull RectF from,
                                         @NonNull RectF to,
                                         @NonNull GraphyNode fromNode,
                                         @NonNull GraphyNode toNode) {
        float startX = from.centerX();
        float startY = connectionStartY(from, fromNode);
        float endX = to.centerX();
        float endY = connectionEndY(to, toNode);

        if (Math.abs(startX - endX) < 1f) {
            drawArrowLine(canvas, startX, startY, endX, endY);
            return;
        }

        float midY = (startY + endY) / 2f;
        canvas.drawLine(startX, startY, startX, midY, connectorPaint);
        canvas.drawLine(startX, midY, endX, midY, connectorPaint);
        drawArrowLine(canvas, endX, midY, endX, endY);
    }

    private float connectionStartY(@NonNull RectF bounds, @NonNull GraphyNode node) {
        if (node.getType() == GraphyNodeType.OPERATION) {
            return bounds.bottom;
        }
        return bounds.bottom;
    }

    private float connectionEndY(@NonNull RectF bounds, @NonNull GraphyNode node) {
        if (node.getType() == GraphyNodeType.OPERATION) {
            return bounds.top;
        }
        return bounds.top;
    }

    private void drawArrowLine(@NonNull Canvas canvas, float x1, float y1, float x2, float y2) {
        canvas.drawLine(x1, y1, x2, y2, connectorPaint);
        if (y2 > y1 + 2f) {
            drawArrowHead(canvas, x2, y2, 0f, 1f);
        } else if (y2 < y1 - 2f) {
            drawArrowHead(canvas, x2, y2, 0f, -1f);
        } else if (x2 > x1 + 2f) {
            drawArrowHead(canvas, x2, y2, 1f, 0f);
        } else if (x2 < x1 - 2f) {
            drawArrowHead(canvas, x2, y2, -1f, 0f);
        }
    }

    private void drawArrowHead(@NonNull Canvas canvas, float tipX, float tipY, float dirX, float dirY) {
        float size = theme.getConnectorStrokeWidth() * 3.5f;
        float baseX = tipX - dirX * size * 1.8f;
        float baseY = tipY - dirY * size * 1.8f;
        float perpX = -dirY;
        float perpY = dirX;
        Path arrow = new Path();
        arrow.moveTo(tipX, tipY);
        arrow.lineTo(baseX + perpX * size, baseY + perpY * size);
        arrow.lineTo(baseX - perpX * size, baseY - perpY * size);
        arrow.close();
        fillPaint.setColor(theme.getConnectorColor());
        fillPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(arrow, fillPaint);
        fillPaint.setStyle(Paint.Style.FILL);
    }

    private void drawNodes(@NonNull Canvas canvas) {
        List<String> drawOrder = buildDrawOrder();
        for (String nodeId : drawOrder) {
            GraphyNode node = findNode(nodeId);
            RectF bounds = nodeBounds.get(nodeId);
            if (node == null || bounds == null) {
                continue;
            }

            switch (node.getType()) {
                case OPERATION:
                    drawOperationNode(canvas, node, bounds);
                    break;
                case RESULT:
                    drawFinalResultNode(canvas, node, bounds);
                    break;
                case INPUT:
                    drawValueNode(canvas, node, bounds, theme.getInputColor(), false);
                    break;
                case CONSTANT:
                    drawValueNode(canvas, node, bounds, theme.getConstantColor(), false);
                    break;
                case DERIVED:
                    boolean primary = "primary".equals(node.getSemanticRole());
                    drawValueNode(canvas, node, bounds, theme.getDerivedColor(), primary);
                    break;
                default:
                    drawValueNode(canvas, node, bounds, theme.getPrimaryTextColor(), false);
                    break;
            }
        }
    }

    @NonNull
    private List<String> buildDrawOrder() {
        List<String> order = new ArrayList<>();
        for (GraphyNode node : output.getNodes()) {
            if (node.getType() != GraphyNodeType.RESULT) {
                order.add(node.getId());
            }
        }
        for (GraphyNode node : output.getNodes()) {
            if (node.getType() == GraphyNodeType.RESULT) {
                order.add(node.getId());
            }
        }
        return order;
    }

    private void drawValueNode(@NonNull Canvas canvas,
                               @NonNull GraphyNode node,
                               @NonNull RectF bounds,
                               int textColor,
                               boolean emphasized) {
        textPaint.setColor(textColor);
        textPaint.setTextSize(theme.getValueTextSize());
        if (emphasized) {
            textPaint.setFakeBoldText(true);
        } else {
            textPaint.setFakeBoldText(false);
        }
        textPaint.setTextAlign(Paint.Align.CENTER);
        float textY = bounds.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(node.getDisplayValue(), bounds.centerX(), textY, textPaint);
    }

    private void drawOperationNode(@NonNull Canvas canvas,
                                   @NonNull GraphyNode node,
                                   @NonNull RectF bounds) {
        float cx = bounds.centerX();
        float cy = bounds.centerY();
        float radius = bounds.width() / 2f;

        fillPaint.setColor(theme.getOperationNodeColor());
        strokePaint.setColor(theme.getOperationStrokeColor());
        strokePaint.setStrokeWidth(theme.getConnectorStrokeWidth());
        canvas.drawCircle(cx, cy, radius, fillPaint);
        canvas.drawCircle(cx, cy, radius, strokePaint);

        textPaint.setColor(theme.getOperationColor());
        textPaint.setTextSize(theme.getOperationTextSize());
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        String symbol = GraphLayoutEngine.formatOperator(node.getLabel());
        float textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(symbol, cx, textY, textPaint);
    }

    private void drawFinalResultNode(@NonNull Canvas canvas,
                                     @NonNull GraphyNode node,
                                     @NonNull RectF bounds) {
        float radius = theme.getNodeCornerRadius();
        fillPaint.setColor(theme.getResultFillColor());
        strokePaint.setColor(theme.getResultFillColor());
        strokePaint.setStrokeWidth(0f);
        canvas.drawRoundRect(bounds, radius, radius, fillPaint);

        textPaint.setColor(theme.getResultOnColor());
        textPaint.setTextSize(theme.getResultTextSize());
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        String text = "✓ " + node.getDisplayValue();
        float textY = bounds.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(text, bounds.centerX(), textY, textPaint);
    }

    @Nullable
    private GraphyNode findNode(@NonNull String id) {
        for (GraphyNode node : output.getNodes()) {
            if (node.getId().equals(id)) {
                return node;
            }
        }
        return null;
    }
}
