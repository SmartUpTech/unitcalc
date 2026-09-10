package net.smartlogic.unitconverter.graphy.renderer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
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
    private OrthogonalConnectorRouter connectorRouter;
    private int contentWidth;
    private int contentHeight;
    private int maxLayoutWidth = Integer.MAX_VALUE;
    private float drawScale = 1f;

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
        connectorPaint.setStrokeCap(Paint.Cap.SQUARE);
        connectorPaint.setStrokeJoin(Paint.Join.MITER);
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
        int specWidth = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        drawScale = 1f;
        int width = Math.max(contentWidth, 0);
        int height = Math.max(contentHeight, 0);

        if (contentWidth > 0 && widthMode != MeasureSpec.UNSPECIFIED && specWidth > 0
                && specWidth < contentWidth) {
            drawScale = specWidth / (float) contentWidth;
            width = specWidth;
            height = (int) Math.ceil(contentHeight * drawScale);
        } else if (widthMode == MeasureSpec.EXACTLY) {
            width = specWidth;
        }

        height = resolveSize(height, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (output == null || theme == null || output.isEmpty()) {
            return;
        }

        canvas.drawColor(theme.getBackgroundColor());
        canvas.save();
        if (drawScale != 1f) {
            canvas.scale(drawScale, drawScale);
        } else if (contentWidth > 0 && getWidth() > contentWidth) {
            canvas.translate((getWidth() - contentWidth) / 2f, 0f);
        }
        drawConnectors(canvas);
        drawNodes(canvas);
        canvas.restore();
    }

    private void layoutNodes() {
        nodeBounds.clear();
        if (output == null || theme == null || output.isEmpty()) {
            contentWidth = 0;
            contentHeight = 0;
            connectorRouter = null;
            return;
        }

        GraphLayoutEngine.LayoutResult result = layoutEngine.layout(output, theme, maxLayoutWidth);
        for (Map.Entry<String, GraphLayoutEngine.LayoutBox> entry : result.nodeBounds().entrySet()) {
            GraphLayoutEngine.LayoutBox box = entry.getValue();
            nodeBounds.put(entry.getKey(), new RectF(box.x(), box.y(), box.x() + box.width(), box.y() + box.height()));
        }
        contentWidth = result.contentWidth();
        contentHeight = result.contentHeight();
        connectorRouter = new OrthogonalConnectorRouter(
                output.getNodes(),
                output.getConnections(),
                nodeBounds,
                theme.getHorizontalGap(),
                theme.getVerticalGap(),
                contentWidth
        );
    }

    private void drawConnectors(@NonNull Canvas canvas) {
        connectorPaint.setColor(theme.getConnectorColor());
        connectorPaint.setStrokeWidth(theme.getConnectorStrokeWidth());

        for (GraphyConnection connection : output.getConnections()) {
            if (!nodeBounds.containsKey(connection.fromNodeId())
                    || !nodeBounds.containsKey(connection.toNodeId())) {
                continue;
            }
            drawOrthogonalConnector(canvas, connection.fromNodeId(), connection.toNodeId());
        }
    }

    private void drawOrthogonalConnector(@NonNull Canvas canvas,
                                         @NonNull String fromId,
                                         @NonNull String toId) {
        if (connectorRouter == null) {
            return;
        }
        List<PointF> points = connectorRouter.route(fromId, toId);
        if (points.size() < 2) {
            return;
        }

        float clearance = 8f;
        float arrowSize = theme.getConnectorStrokeWidth() * 2.1f;
        float arrowLength = arrowSize * 1.35f;

        List<PointF> drawn = new ArrayList<>(points.size());
        for (PointF point : points) {
            drawn.add(new PointF(point.x, point.y));
        }

        PointF tip = new PointF(
                drawn.get(drawn.size() - 1).x,
                drawn.get(drawn.size() - 1).y
        );
        PointF beforeTip = drawn.get(drawn.size() - 2);
        float dx = tip.x - beforeTip.x;
        float dy = tip.y - beforeTip.y;
        float length = (float) Math.hypot(dx, dy);
        if (length < 1f) {
            return;
        }
        float ux = dx / length;
        float uy = dy / length;
        float pullBack = Math.min(clearance + arrowLength, Math.max(0f, length - clearance));
        PointF shaftEnd = drawn.get(drawn.size() - 1);
        shaftEnd.x = tip.x - ux * pullBack;
        shaftEnd.y = tip.y - uy * pullBack;

        Path path = new Path();
        path.moveTo(drawn.get(0).x, drawn.get(0).y);
        for (int i = 1; i < drawn.size(); i++) {
            path.lineTo(drawn.get(i).x, drawn.get(i).y);
        }
        canvas.drawPath(path, connectorPaint);

        drawArrowHead(canvas, tip.x, tip.y, ux, uy, arrowSize);
    }

    private void drawArrowHead(@NonNull Canvas canvas,
                               float tipX,
                               float tipY,
                               float dirX,
                               float dirY,
                               float size) {
        float baseX = tipX - dirX * size * 1.35f;
        float baseY = tipY - dirY * size * 1.35f;
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

            if (node.type() == GraphyNodeType.OPERATION) {
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
            if (node.type() != GraphyNodeType.RESULT) {
                order.add(node.id());
            }
        }
        for (GraphyNode node : output.getNodes()) {
            if (node.type() == GraphyNodeType.RESULT) {
                order.add(node.id());
            }
        }
        return order;
    }

    private void drawValueNode(@NonNull Canvas canvas,
                               @NonNull GraphyNode node,
                               @NonNull RectF bounds) {
        int fillColor;
        int textColor;

        switch (node.type()) {
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
        textPaint.setTextSize(node.type() == GraphyNodeType.RESULT ? theme.getResultTextSize() : theme.getValueTextSize());
        textPaint.setFakeBoldText(node.type() == GraphyNodeType.RESULT || "primary".equals(node.semanticRole()));
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        float textY = bounds.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(node.displayValue(), bounds.centerX(), textY, textPaint);
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
        String symbol = GraphLayoutEngine.formatOperator(node.label());
        float textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(symbol, cx, textY, textPaint);
    }


    @Nullable
    private GraphyNode findNode(@NonNull String id) {
        for (GraphyNode node : output.getNodes()) {
            if (node.id().equals(id)) {
                return node;
            }
        }
        return null;
    }
}
