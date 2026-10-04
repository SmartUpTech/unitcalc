package net.smartlogic.unitconverter.graphy.renderer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.os.Bundle;
import android.graphics.Rect;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.graphy.model.GraphyTopology;

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
    private Map<String, GraphyNodeContent> nodeContent = java.util.Collections.emptyMap();
    private List<GraphyNode> orderedNodes = java.util.Collections.emptyList();
    private NodeAccessibility accessibility;
    private String pressedNodeId;
    public interface NodeClickListener { void onNodeClicked(GraphyNode node); }
    private NodeClickListener nodeClickListener;
    public void setOnNodeClickListener(@Nullable NodeClickListener listener) {
        nodeClickListener = listener;
        accessibility.invalidateRoot();
    }
    private net.smartlogic.unitconverter.theme.CalculatorTheme resolvedTheme;
    private android.content.res.Configuration resolvedConfiguration;

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
        setFocusable(true);
        accessibility = new NodeAccessibility();
        ViewCompat.setAccessibilityDelegate(this, accessibility);
    }

    public void setMaxWidth(int maxWidth) {
        if (this.maxLayoutWidth != maxWidth && maxWidth > 0) {
            this.maxLayoutWidth = maxWidth;
            layoutNodes();
            requestLayout();
        }
    }

    public void setGraph(@NonNull GraphyOutput output, @NonNull GraphyViewTheme theme) {
        setGraph(output, theme, getResources().getConfiguration());
    }

    public void setGraph(@NonNull GraphyOutput output, @NonNull GraphyViewTheme theme,
                         @NonNull android.content.res.Configuration configuration) {
        var selectedTheme = net.smartlogic.unitconverter.theme.ThemeManager.get();
        if (this.output == output && this.theme == theme && selectedTheme == resolvedTheme
                && configuration.equals(resolvedConfiguration)) return;
        resolvedTheme = selectedTheme;
        resolvedConfiguration = new android.content.res.Configuration(configuration);
        this.output = output;
        orderedNodes = GraphyTopology.orderedNodes(output);
        this.theme = theme;
        layoutNodes();
        requestLayout();
        invalidate();
        accessibility.invalidateRoot();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED && width > 0
                && width != maxLayoutWidth) {
            maxLayoutWidth = width;
            layoutNodes();
        }
        setMeasuredDimension(resolveSize(contentWidth, widthMeasureSpec),
                resolveSize(contentHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (output == null || theme == null || output.isEmpty()) {
            return;
        }

        canvas.drawColor(theme.getBackgroundColor());
        canvas.save();
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

        GraphLayoutEngine.LayoutResult result = layoutEngine.layout(output, theme, maxLayoutWidth == Integer.MAX_VALUE
                ? getResources().getDisplayMetrics().widthPixels : maxLayoutWidth);
        for (Map.Entry<String, GraphLayoutEngine.LayoutBox> entry : result.nodeBounds().entrySet()) {
            GraphLayoutEngine.LayoutBox box = entry.getValue();
            nodeBounds.put(entry.getKey(), new RectF(box.x(), box.y(), box.x() + box.width(), box.y() + box.height()));
        }
        orderedNodes = new ArrayList<>(orderedNodes);
        orderedNodes.sort(java.util.Comparator
                .comparingDouble((GraphyNode node) -> nodeBounds.containsKey(node.id()) ? nodeBounds.get(node.id()).top : 0)
                .thenComparingDouble(node -> nodeBounds.containsKey(node.id()) ? nodeBounds.get(node.id()).left : 0));
        nodeContent = result.content();
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

        float clearance = theme.getConnectorClearance();
        float arrowSize = theme.getArrowSize();
        float arrowLength = theme.getArrowLength();

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
        float baseX = tipX - dirX * theme.getArrowLength();
        float baseY = tipY - dirY * theme.getArrowLength();
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
        for (int index = 0; index < orderedNodes.size(); index++) {
            GraphyNode node = orderedNodes.get(index);
            RectF bounds = nodeBounds.get(node.id());
            GraphyNodeContent content = nodeContent.get(node.id());
            if (bounds == null || content == null) continue;
            fillPaint.setColor(theme.nodeFill(node.type()));
            float radius = node.type() == GraphyNodeType.OPERATION
                    ? bounds.height() / 2 : theme.getNodeCornerRadius();
            canvas.drawRoundRect(bounds, radius, radius, fillPaint);
            content.draw(canvas, bounds);
            if (node.id().equals(pressedNodeId)
                    || accessibility.getKeyboardFocusedVirtualViewId() == index
                    || accessibility.getAccessibilityFocusedVirtualViewId() == index) {
                strokePaint.setColor(theme.getConnectorColor());
                strokePaint.setStrokeWidth(theme.getConnectorStrokeWidth());
                canvas.drawRoundRect(bounds, radius, radius, strokePaint);
            }
        }
    }

    @Nullable
    private GraphyNode findNode(String id) {
        for (GraphyNode node : orderedNodes) if (node.id().equals(id)) return node;
        return null;
    }

    @Nullable
    private GraphyNode hitNode(float x, float y) {
        for (GraphyNode node : orderedNodes) {
            RectF bounds = nodeBounds.get(node.id());
            if (bounds != null && bounds.contains(x, y)) return node;
        }
        return null;
    }

    private void showDetails(GraphyNode node) {
        if (nodeClickListener != null) nodeClickListener.onNodeClicked(node);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            GraphyNode hit = hitNode(event.getX(), event.getY());
            pressedNodeId = hit == null ? null : hit.id();
            invalidate();
            return pressedNodeId != null;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            GraphyNode hit = hitNode(event.getX(), event.getY());
            if (hit != null && hit.id().equals(pressedNodeId)) {
                performClick();
                showDetails(hit);
            }
            pressedNodeId = null;
            invalidate();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            pressedNodeId = null;
            invalidate();
        }
        return pressedNodeId != null;
    }

    @Override public boolean performClick() { super.performClick(); return true; }
    @Override public boolean dispatchHoverEvent(MotionEvent event) {
        return accessibility.dispatchHoverEvent(event) || super.dispatchHoverEvent(event);
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        return accessibility.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }
    @Override protected void onFocusChanged(boolean gainFocus, int direction, Rect previous) {
        super.onFocusChanged(gainFocus, direction, previous);
        accessibility.onFocusChanged(gainFocus, direction, previous);
    }

    private final class NodeAccessibility extends ExploreByTouchHelper {
        NodeAccessibility() { super(FlowchartGraphView.this); }
        @Override protected int getVirtualViewAt(float x, float y) {
            GraphyNode hit = hitNode(x, y);
            return hit == null ? INVALID_ID : orderedNodes.indexOf(hit);
        }
        @Override protected void getVisibleVirtualViews(List<Integer> ids) {
            for (int i = 0; i < orderedNodes.size(); i++) ids.add(i);
        }
        @Override protected void onPopulateNodeForVirtualView(int id, AccessibilityNodeInfoCompat info) {
            GraphyNode node = orderedNodes.get(id);
            RectF bounds = nodeBounds.get(node.id());
            Rect rect = new Rect();
            if (bounds != null) bounds.roundOut(rect);
            info.setBoundsInParent(rect);
            String description = getContext().getString(R.string.graphy_node_accessibility,
                    theme.nodeRole(node.type()), node.displayValue());
            for (var edge : output.getConnections()) {
                if (edge.toNodeId().equals(node.id())) {
                    GraphyNode source = findNode(edge.fromNodeId());
                    if (source != null) description += ". " + getContext().getString(
                            R.string.graphy_dependency, source.displayValue());
                }
            }
            info.setContentDescription(description);
            if (nodeClickListener != null) {
                info.setClassName(android.widget.Button.class.getName());
                info.setClickable(true);
                info.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
            }
        }
        @Override protected void onVirtualViewKeyboardFocusChanged(int id, boolean focused) {
            invalidate();
        }
        @Override protected boolean onPerformActionForVirtualView(int id, int action, Bundle args) {
            if (action == AccessibilityNodeInfoCompat.ACTION_CLICK && nodeClickListener != null && id >= 0 && id < orderedNodes.size()) {
                showDetails(orderedNodes.get(id));
                return true;
            }
            return false;
        }
    }
}
