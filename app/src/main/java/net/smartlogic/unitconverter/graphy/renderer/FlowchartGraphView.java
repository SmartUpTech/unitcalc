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
    private int maxLayoutWidth = Integer.MAX_VALUE;

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

    public void setMaxWidth(int maxWidth) {
        this.maxLayoutWidth = maxWidth;
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

        GraphLayoutEngine.LayoutResult result = layoutEngine.layout(output, theme, maxLayoutWidth);
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

            if (node.getType() == GraphyNodeType.OPERATION) {
                drawOperationNode(canvas, node, bounds);
            } else {
                drawValueNode(canvas, node, bounds);
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
                               @NonNull RectF bounds) {
        int fillColor;
        int textColor;

        switch (node.getType()) {
            case INPUT:
                fillColor = theme.getInputColor();
                textColor = theme.getOnInputColor();
                break;
            case CONSTANT:
                fillColor = theme.getConstantColor();
                textColor = theme.getOnConstantColor();
                break;
            case DERIVED:
                fillColor = theme.getDerivedColor();
                textColor = theme.getOnDerivedColor();
                break;
            case RESULT:
                fillColor = theme.getResultFillColor();
                textColor = theme.getOnResultColor();
                break;
            default:
                fillColor = theme.getSurfaceElevatedColor();
                textColor = theme.getPrimaryTextColor();
                break;
        }

        float radius = theme.getNodeCornerRadius();

        // Draw slight shadow
        fillPaint.setColor(0x20000000);
        RectF shadowBounds = new RectF(bounds.left + 2, bounds.top + 2, bounds.right + 2, bounds.bottom + 2);
        canvas.drawRoundRect(shadowBounds, radius, radius, fillPaint);

        // Draw block
        fillPaint.setColor(fillColor);
        canvas.drawRoundRect(bounds, radius, radius, fillPaint);

        // Draw text
        textPaint.setColor(textColor);
        textPaint.setTextSize(node.getType() == GraphyNodeType.RESULT ? theme.getResultTextSize() : theme.getValueTextSize());
        textPaint.setFakeBoldText(node.getType() == GraphyNodeType.RESULT || "primary".equals(node.getSemanticRole()));
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
