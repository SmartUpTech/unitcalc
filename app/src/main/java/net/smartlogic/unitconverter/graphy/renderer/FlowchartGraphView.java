package net.smartlogic.unitconverter.graphy.renderer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
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
    private GraphyOutput presentation;
    private Map<String, GraphyNode> originalNodes = java.util.Collections.emptyMap();
    private GraphyViewTheme theme;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private GraphyCanvasRenderer diagramRenderer;

    private final Map<String, RectF> nodeBounds = new HashMap<>();
    private final GraphLayoutEngine layoutEngine = new GraphLayoutEngine();
    private int contentWidth;
    private int contentHeight;
    private int maxLayoutWidth = Integer.MAX_VALUE;
    private List<GraphyNode> orderedNodes = java.util.Collections.emptyList();
    private NodeAccessibility accessibility;
    public interface PanListener { void onPan(float deltaX, float deltaY); }
    private PanListener panListener;
    private android.widget.OverScroller panScroller;
    private android.view.VelocityTracker velocityTracker;
    private float downRawX, downRawY, lastRawX, lastRawY;
    private int lastFlingX, lastFlingY;
    private boolean dragging;
    public void setOnPanListener(@Nullable PanListener listener) {
        panListener = listener;
        if (listener == null) panScroller.abortAnimation();
    }
    private void trackRawMovement(MotionEvent event) {
        if (velocityTracker == null) return;
        MotionEvent raw = MotionEvent.obtain(event);
        raw.offsetLocation(event.getRawX() - event.getX(), event.getRawY() - event.getY());
        velocityTracker.addMovement(raw);
        raw.recycle();
    }
    private void finishPan() {
        if (velocityTracker != null) { velocityTracker.recycle(); velocityTracker = null; }
        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
    }
    @Override public void computeScroll() {
        if (panListener != null && panScroller.computeScrollOffset()) {
            int x = panScroller.getCurrX(), y = panScroller.getCurrY();
            panListener.onPan(x - lastFlingX, y - lastFlingY);
            lastFlingX = x; lastFlingY = y;
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }
    @Override protected void onDetachedFromWindow() {
        panScroller.abortAnimation();
        finishPan();
        super.onDetachedFromWindow();
    }
    private String pressedNodeId;
    private int pressedStage = -1;
    private final java.util.Set<Integer> collapsedStages = new java.util.LinkedHashSet<>();
    private List<GraphLayoutEngine.StageMarker> stageMarkers = java.util.Collections.emptyList();
    private float gutterWidth;
    private int horizontalViewportOffset;
    public void setHorizontalViewportOffset(int offset) {
        if (horizontalViewportOffset == offset) return;
        horizontalViewportOffset = Math.max(0, offset);
        invalidate();
        accessibility.invalidateRoot();
    }
    public interface StageToggleListener { void onStagesChanged(java.util.Set<Integer> collapsed); }
    private StageToggleListener stageToggleListener;
    public void setOnStageToggleListener(@Nullable StageToggleListener listener) { stageToggleListener = listener; }
    public void setCollapsedStages(@NonNull java.util.Set<Integer> collapsed) {
        if (collapsedStages.equals(collapsed)) return;
        collapsedStages.clear();
        collapsedStages.addAll(collapsed);
        layoutNodes();
        requestLayout();
        invalidate();
    }
    public java.util.Set<Integer> getCollapsedStages() { return java.util.Collections.unmodifiableSet(new java.util.LinkedHashSet<>(collapsedStages)); }
    private void toggleStage(int index) {
        if (index < 0 || index >= stageMarkers.size()) return;
        int stage = stageMarkers.get(index).number();
        if (!collapsedStages.remove(stage)) collapsedStages.add(stage);
        // Node count changes during folding; restore focus to the same semantic control.
        int focused = accessibility.getKeyboardFocusedVirtualViewId();
        int spokenFocus = accessibility.getAccessibilityFocusedVirtualViewId();
        if (focused != ExploreByTouchHelper.INVALID_ID) accessibility.clearKeyboardFocusForVirtualView(focused);
        if (spokenFocus != ExploreByTouchHelper.INVALID_ID) getAccessibilityNodeProvider().performAction(
                spokenFocus, AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS, null);
        layoutNodes();
        int newId = orderedNodes.size() + index;
        if (focused != ExploreByTouchHelper.INVALID_ID) accessibility.requestKeyboardFocusForVirtualView(newId);
        if (spokenFocus != ExploreByTouchHelper.INVALID_ID) getAccessibilityNodeProvider().performAction(
                newId, AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS, null);
        requestLayout();
        invalidate();
        if (stageToggleListener != null) stageToggleListener.onStagesChanged(getCollapsedStages());
    }
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
        panScroller = new android.widget.OverScroller(getContext());
        strokePaint.setStyle(Paint.Style.STROKE);
        textPaint.setTextAlign(Paint.Align.CENTER);
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
        if (this.output != output) { collapsedStages.clear(); panScroller.abortAnimation(); }
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
                && maxLayoutWidth == Integer.MAX_VALUE) {
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

        canvas.save();
        // AndroidView does not automatically clip native drawing. A drawColor() can
        // erase Compose siblings (the expression and guide) outside this View.
        canvas.clipRect(0, 0, getWidth(), getHeight());
        fillPaint.setColor(theme.getBackgroundColor());
        canvas.drawRect(0, 0, getWidth(), getHeight(), fillPaint);
        diagramRenderer.draw(canvas);
        drawNodeFocus(canvas);
        drawStages(canvas);
        canvas.restore();
    }

    private void layoutNodes() {
        nodeBounds.clear();
        stageMarkers = java.util.Collections.emptyList();
        if (output == null || theme == null || output.isEmpty()) {
            contentWidth = 0;
            contentHeight = 0;
            diagramRenderer = null;
            presentation = null;
            originalNodes = java.util.Collections.emptyMap();
            orderedNodes = java.util.Collections.emptyList();
            return;
        }

        GraphLayoutEngine.LayoutResult result = layoutEngine.layout(output, theme, maxLayoutWidth == Integer.MAX_VALUE
                ? getResources().getDisplayMetrics().widthPixels : maxLayoutWidth, collapsedStages);
        for (Map.Entry<String, GraphLayoutEngine.LayoutBox> entry : result.nodeBounds().entrySet()) {
            GraphLayoutEngine.LayoutBox box = entry.getValue();
            nodeBounds.put(entry.getKey(), new RectF(box.x(), box.y(), box.x() + box.width(), box.y() + box.height()));
        }
        stageMarkers = result.stages();
        gutterWidth = result.gutterWidth();
        presentation = result.presentation();
        originalNodes = result.originals();
        // The layout supplies reading order; sorting by top would reverse operands
        // when a long right-hand value wraps higher than a short left-hand value.
        orderedNodes = new ArrayList<>(presentation.getNodes());
        contentWidth = result.contentWidth();
        contentHeight = result.contentHeight();
        diagramRenderer = new GraphyCanvasRenderer(result, theme);
        accessibility.invalidateRoot();
    }

    private void drawNodeFocus(Canvas canvas) {
        for (int index = 0; index < orderedNodes.size(); index++) {
            GraphyNode node = orderedNodes.get(index);
            if (node.id().equals(pressedNodeId) || accessibility.getKeyboardFocusedVirtualViewId() == index
                    || accessibility.getAccessibilityFocusedVirtualViewId() == index) {
                RectF bounds = nodeBounds.get(node.id());
                float radius = node.type() == GraphyNodeType.OPERATION ? bounds.height() / 2 : theme.getNodeCornerRadius();
                strokePaint.setColor(theme.nodeContent(node.type()));
                strokePaint.setStrokeWidth(theme.getNodeFocusStroke());
                RectF focus = new RectF(bounds);
                focus.inset(theme.getNodeFocusStroke() / 2, theme.getNodeFocusStroke() / 2);
                canvas.drawRoundRect(focus, radius, radius, strokePaint);
            }
        }
    }

    private void drawStages(Canvas canvas) {
        if (stageMarkers.isEmpty()) return;
        canvas.save();
        canvas.translate(horizontalViewportOffset, 0);
        fillPaint.setColor(theme.getBackgroundColor());
        canvas.drawRect(0, 0, gutterWidth, getHeight(), fillPaint);
        float icon = theme.getFoldIconSize();
        strokePaint.setColor(theme.getConnectorColor());
        strokePaint.setStrokeWidth(theme.getNodeBorderStroke());
        strokePaint.setPathEffect(null);
        float guideX = icon;
        canvas.drawLine(guideX, theme.getNodePadding(), guideX, getHeight() - theme.getNodePadding(), strokePaint);
        textPaint.setTypeface(theme.getTypeface());
        textPaint.setTextSize(theme.getLabelTextSize());
        textPaint.setColor(theme.getPrimaryTextColor());
        for (int i = 0; i < stageMarkers.size(); i++) {
            GraphLayoutEngine.StageMarker stage = stageMarkers.get(i);
            var box = stage.bounds();
            float cy = box.y() + box.height() / 2;
            fillPaint.setColor(theme.getBackgroundColor());
            canvas.drawRect(0, cy - icon, gutterWidth, cy + icon, fillPaint);
            Path chevron = new Path();
            if (stage.collapsed()) {
                chevron.moveTo(guideX - icon / 4, cy - icon / 2);
                chevron.lineTo(guideX + icon / 4, cy);
                chevron.lineTo(guideX - icon / 4, cy + icon / 2);
            } else {
                chevron.moveTo(guideX - icon / 2, cy - icon / 4);
                chevron.lineTo(guideX, cy + icon / 4);
                chevron.lineTo(guideX + icon / 2, cy - icon / 4);
            }
            canvas.drawPath(chevron, strokePaint);
            canvas.drawText(String.valueOf(stage.number()), (gutterWidth + icon * 2) / 2,
                    cy - (textPaint.ascent() + textPaint.descent()) / 2, textPaint);
            int virtualId = orderedNodes.size() + i;
            if (pressedStage == i || accessibility.getKeyboardFocusedVirtualViewId() == virtualId
                    || accessibility.getAccessibilityFocusedVirtualViewId() == virtualId) {
                canvas.drawRect(box.x(), box.y(), box.x() + box.width(), box.y() + box.height(), strokePaint);
            }
        }
        canvas.restore();
    }

    private int hitStage(float x, float y) {
        x -= horizontalViewportOffset;
        for (int i = 0; i < stageMarkers.size(); i++) {
            var box = stageMarkers.get(i).bounds();
            if (x >= box.x() && x <= box.x() + box.width() && y >= box.y() && y <= box.y() + box.height()) return i;
        }
        return -1;
    }

    @Nullable
    private GraphyNode findNode(String id) {
        if (output != null) for (GraphyNode node : output.getNodes()) if (node.id().equals(id)) return node;
        return null;
    }

    private RectF touchBounds(String id) {
        RectF visual = nodeBounds.get(id);
        if (visual == null) return null;
        RectF target = new RectF(visual);
        target.inset(-Math.max(0, (theme.getTouchTarget() - visual.width()) / 2),
                -Math.max(0, (theme.getTouchTarget() - visual.height()) / 2));
        return target;
    }

    @Nullable
    private GraphyNode hitNode(float x, float y) {
        if (!stageMarkers.isEmpty() && x >= horizontalViewportOffset && x < horizontalViewportOffset + gutterWidth) return null;
        for (GraphyNode node : orderedNodes) {
            RectF bounds = touchBounds(node.id());
            if (bounds != null && bounds.contains(x, y)) return node;
        }
        return null;
    }

    private void showDetails(GraphyNode node) {
        if (nodeClickListener != null) nodeClickListener.onNodeClicked(originalNodes.getOrDefault(node.id(), node));
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            panScroller.abortAnimation();
            dragging = false;
            downRawX = lastRawX = event.getRawX();
            downRawY = lastRawY = event.getRawY();
            if (panListener != null) {
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                velocityTracker = android.view.VelocityTracker.obtain();
                trackRawMovement(event);
            }
            pressedStage = hitStage(event.getX(), event.getY());
            GraphyNode hit = pressedStage < 0 ? hitNode(event.getX(), event.getY()) : null;
            pressedNodeId = hit == null ? null : hit.id();
            invalidate();
            return panListener != null || pressedStage >= 0 || pressedNodeId != null;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE && panListener != null) {
            trackRawMovement(event);
            int slop = android.view.ViewConfiguration.get(getContext()).getScaledTouchSlop();
            if (!dragging && Math.hypot(event.getRawX() - downRawX, event.getRawY() - downRawY) > slop) {
                dragging = true;
                pressedStage = -1;
                pressedNodeId = null;
            }
            if (dragging) {
                panListener.onPan(lastRawX - event.getRawX(), lastRawY - event.getRawY());
                lastRawX = event.getRawX(); lastRawY = event.getRawY();
                invalidate();
            }
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            trackRawMovement(event);
            if (dragging && panListener != null) {
                if (velocityTracker != null) {
                    var config = android.view.ViewConfiguration.get(getContext());
                    velocityTracker.computeCurrentVelocity(1000, config.getScaledMaximumFlingVelocity());
                    int vx = (int) -velocityTracker.getXVelocity(), vy = (int) -velocityTracker.getYVelocity();
                    if (Math.hypot(vx, vy) >= config.getScaledMinimumFlingVelocity()) {
                        lastFlingX = lastFlingY = 0;
                        panScroller.fling(0, 0, vx, vy, -contentWidth, contentWidth, -contentHeight, contentHeight);
                        ViewCompat.postInvalidateOnAnimation(this);
                    }
                }
                dragging = false;
                finishPan();
                return true;
            }
            finishPan();
            int stage = hitStage(event.getX(), event.getY());
            if (pressedStage >= 0 && stage == pressedStage) {
                performClick();
                pressedStage = -1;
                toggleStage(stage);
                return true;
            }
            pressedStage = -1;
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
            dragging = false;
            finishPan();
            pressedStage = -1;
            pressedNodeId = null;
            invalidate();
        }
        return pressedStage >= 0 || pressedNodeId != null;
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
            int stage = hitStage(x, y);
            if (stage >= 0) return orderedNodes.size() + stage;
            GraphyNode hit = hitNode(x, y);
            return hit == null ? INVALID_ID : orderedNodes.indexOf(hit);
        }
        @Override protected void getVisibleVirtualViews(List<Integer> ids) {
            for (int i = 0; i < orderedNodes.size() + stageMarkers.size(); i++) ids.add(i);
            ids.sort(java.util.Comparator.comparingDouble(id -> id < orderedNodes.size()
                    ? touchBounds(orderedNodes.get(id).id()).top
                    : stageMarkers.get(id - orderedNodes.size()).bounds().y()));
        }
        @Override protected void onPopulateNodeForVirtualView(int id, AccessibilityNodeInfoCompat info) {
            if (id >= orderedNodes.size()) {
                int index = id - orderedNodes.size();
                if (index < 0 || index >= stageMarkers.size()) { info.setContentDescription(""); info.setBoundsInParent(new Rect()); return; }
                var stage = stageMarkers.get(index);
                var box = stage.bounds();
                Rect rect = new Rect((int) box.x() + horizontalViewportOffset, (int) box.y(), (int) Math.ceil(box.x() + box.width()) + horizontalViewportOffset, (int) Math.ceil(box.y() + box.height()));
                info.setBoundsInParent(rect);
                info.setContentDescription(getContext().getString(stage.collapsed() ? R.string.graphy_expand_stage : R.string.graphy_collapse_stage,
                        stage.number(), getResources().getQuantityString(R.plurals.graphy_stage_calculations,
                                stage.calculationCount(), stage.calculationCount())));
                info.setStateDescription(getContext().getString(stage.collapsed() ? R.string.graphy_stage_collapsed : R.string.graphy_stage_expanded));
                info.setClassName(android.widget.Button.class.getName());
                info.setClickable(true);
                info.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                info.addAction(stage.collapsed() ? AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_EXPAND
                        : AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_COLLAPSE);
                return;
            }
            GraphyNode node = orderedNodes.get(id);
            GraphyNode original = originalNodes.getOrDefault(node.id(), node);
            RectF bounds = touchBounds(node.id());
            Rect rect = new Rect();
            if (bounds != null) bounds.roundOut(rect);
            info.setBoundsInParent(rect);
            String description = getContext().getString(R.string.graphy_node_accessibility,
                    theme.nodeRole(node.type()), node.displayValue());
            if (!node.id().equals(original.id())) {
                description += ". " + getContext().getString(R.string.graphy_reused_value);
            }
            for (var edge : output.getConnections()) {
                if (edge.toNodeId().equals(original.id())) {
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
            if (id >= orderedNodes.size() && id < orderedNodes.size() + stageMarkers.size()
                    && (action == AccessibilityNodeInfoCompat.ACTION_CLICK || action == AccessibilityNodeInfoCompat.ACTION_EXPAND
                    || action == AccessibilityNodeInfoCompat.ACTION_COLLAPSE)) {
                toggleStage(id - orderedNodes.size());
                return true;
            }
            if (action == AccessibilityNodeInfoCompat.ACTION_CLICK && nodeClickListener != null && id >= 0 && id < orderedNodes.size()) {
                showDetails(orderedNodes.get(id));
                return true;
            }
            return false;
        }
    }
}
