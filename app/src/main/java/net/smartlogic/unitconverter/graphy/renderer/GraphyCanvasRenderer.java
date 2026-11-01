package net.smartlogic.unitconverter.graphy.renderer;

import android.graphics.*;
import androidx.annotation.NonNull;
import net.smartlogic.unitconverter.graphy.model.*;
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme;
import java.util.*;

/** Thread-confined drawing primitives shared by the interactive View and exported documents. */
final class GraphyCanvasRenderer {
    private final GraphLayoutEngine.LayoutResult layout;
    private final GraphyViewTheme theme;
    private final Map<String, RectF> nodeBounds = new HashMap<>();
    private final OrthogonalConnectorRouter connectorRouter;
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint connectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final DashPathEffect intermediateBorder;

    GraphyCanvasRenderer(GraphLayoutEngine.LayoutResult layout, GraphyViewTheme theme) {
        this.layout = layout;
        this.theme = theme;
        strokePaint.setStyle(Paint.Style.STROKE);
        connectorPaint.setStyle(Paint.Style.STROKE);
        connectorPaint.setStrokeCap(Paint.Cap.SQUARE);
        connectorPaint.setStrokeJoin(Paint.Join.MITER);
        intermediateBorder = new DashPathEffect(new float[]{theme.getNodeDashLength(), theme.getNodeDashGap()}, 0);
        layout.nodeBounds().forEach((id, box) -> nodeBounds.put(id,
                new RectF(box.x(), box.y(), box.x() + box.width(), box.y() + box.height())));
        connectorRouter = new OrthogonalConnectorRouter(layout.presentation().getNodes(),
                layout.presentation().getConnections(), nodeBounds, theme.getHorizontalGap(),
                theme.getVerticalGap(), layout.contentWidth());
    }

    void draw(Canvas canvas) {
        drawConnectors(canvas);
        for (GraphyNode node : layout.presentation().getNodes()) {
            RectF bounds = nodeBounds.get(node.id());
            drawNodeFrame(canvas, bounds, node.type());
            layout.content().get(node.id()).draw(canvas, bounds);
        }
    }

    void drawNodeFrame(Canvas canvas, RectF bounds, GraphyNodeType type) {
            fillPaint.setColor(theme.nodeFill(type));
            float radius = type == GraphyNodeType.OPERATION
                    ? bounds.height() / 2 : theme.getNodeCornerRadius();
            canvas.drawRoundRect(bounds, radius, radius, fillPaint);
            // Role styling is shared by every builder, never inferred from numeric text.
            strokePaint.setColor(theme.nodeBorder(type));
            strokePaint.setStrokeWidth(theme.getNodeBorderStroke());
            strokePaint.setPathEffect(net.smartlogic.unitconverter.graphy.theme.GraphySemanticStyle
                    .hasDashedBorder(type) ? intermediateBorder : null);
            if (type != GraphyNodeType.RESULT) {
                RectF border = new RectF(bounds);
                float inset = theme.getNodeBorderStroke() / 2;
                border.inset(inset, inset);
                canvas.drawRoundRect(border, radius, radius, strokePaint);
            }
            strokePaint.setPathEffect(null);
    }

    private void drawConnectors(@NonNull Canvas canvas) {
        connectorPaint.setColor(theme.getConnectorColor());
        connectorPaint.setStrokeWidth(theme.getConnectorStrokeWidth());

        for (GraphyConnection connection : layout.presentation().getConnections()) {
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

        float arrowSize = theme.getArrowSize();

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
        float arrowLength = Math.min(theme.getArrowLength(), length);
        arrowSize *= arrowLength / theme.getArrowLength();
        PointF shaftEnd = drawn.get(drawn.size() - 1);
        // Meet the arrow base exactly; extra clearance would split shaft and arrow.
        shaftEnd.x = tip.x - ux * arrowLength;
        shaftEnd.y = tip.y - uy * arrowLength;

        Path path = new Path();
        path.moveTo(drawn.get(0).x, drawn.get(0).y);
        for (int i = 1; i < drawn.size(); i++) {
            path.lineTo(drawn.get(i).x, drawn.get(i).y);
        }
        canvas.drawPath(path, connectorPaint);

        drawArrowHead(canvas, tip.x, tip.y, ux, uy, arrowSize, arrowLength);
    }

    private void drawArrowHead(@NonNull Canvas canvas,
                               float tipX,
                               float tipY,
                               float dirX,
                               float dirY,
                               float size,
                               float length) {
        float baseX = tipX - dirX * length;
        float baseY = tipY - dirY * length;
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

 }
