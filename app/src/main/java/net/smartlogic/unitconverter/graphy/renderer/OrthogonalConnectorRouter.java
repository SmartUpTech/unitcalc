package net.smartlogic.unitconverter.graphy.renderer;

import android.graphics.PointF;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.smartlogic.unitconverter.graphy.model.GraphyConnection;
import net.smartlogic.unitconverter.graphy.model.GraphyNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds axis-aligned polylines for Graphy connectors (Cases A, B, and C).
 */
final class OrthogonalConnectorRouter {

    private final Map<String, RectF> nodeBounds;
    private final Map<String, List<String>> incoming;
    private final float horizontalGap;
    private final float contentWidth;
    private final float clearance;
    private final Map<String, Integer> leftLaneIndex = new HashMap<>();
    private final Map<String, Integer> rightLaneIndex = new HashMap<>();
    private final Map<String, Float> routeSpans = new HashMap<>();

    OrthogonalConnectorRouter(@NonNull List<GraphyNode> nodes,
                              @NonNull List<GraphyConnection> connections,
                              @NonNull Map<String, RectF> nodeBounds,
                              float horizontalGap,
                              float verticalGap,
                              float contentWidth) {
        this.nodeBounds = nodeBounds;
        this.horizontalGap = Math.max(horizontalGap, 1f);
        this.contentWidth = Math.max(contentWidth, this.horizontalGap * 2);
        this.clearance = this.horizontalGap / 3f;
        this.incoming = new HashMap<>();
        for (GraphyNode node : nodes) {
            incoming.put(node.id(), new ArrayList<>());
        }
        for (GraphyConnection connection : connections) {
            List<String> targets = incoming.get(connection.toNodeId());
            if (targets != null) {
                targets.add(connection.fromNodeId());
            }
        }
        assignBypassLanes(connections);
    }

    @NonNull
    List<PointF> route(@NonNull String fromId, @NonNull String toId) {
        RectF from = nodeBounds.get(fromId);
        RectF to = nodeBounds.get(toId);
        if (from == null || to == null) {
            return Collections.emptyList();
        }

        if (needsBypass(fromId, toId, from, to)) {
            return routeBypass(fromId, toId, from, to);
        }

        return routePorts(from, Port.BOTTOM, to, Port.TOP);
    }

    private boolean needsBypass(@NonNull String fromId,
                                @NonNull String toId,
                                @NonNull RectF from,
                                @NonNull RectF to) {
        return obstacleUnion(fromId, toId, from, to) != null;
    }

    @Nullable
    private RectF obstacleUnion(@NonNull String fromId,
                                @NonNull String toId,
                                @NonNull RectF from,
                                @NonNull RectF to) {
        float gapTop = from.bottom;
        float gapBottom = to.top;
        if (gapBottom <= gapTop + clearance / 2) {
            return null;
        }

        // Include the horizontal part of a merge, not just the source's vertical stem.
        RectF slab = new RectF(Math.min(from.centerX(), to.centerX()) - clearance,
                gapTop, Math.max(from.centerX(), to.centerX()) + clearance, gapBottom);

        RectF union = null;
        for (Map.Entry<String, RectF> entry : nodeBounds.entrySet()) {
            String id = entry.getKey();
            if (fromId.equals(id) || toId.equals(id)) {
                continue;
            }
            RectF box = entry.getValue();
            if (box.bottom <= gapTop + clearance / 4 || box.top >= gapBottom - clearance / 4) {
                continue;
            }
            if (RectF.intersects(box, slab) || occupiesColumn(box, from, gapTop, gapBottom)) {
                union = union == null ? new RectF(box) : unionWith(union, box);
            }
        }

        for (String other : incoming.getOrDefault(toId, Collections.emptyList())) {
            if (fromId.equals(other)) {
                continue;
            }
            RectF subgraph = descendantUnion(other, toId);
            if (subgraph == null) {
                continue;
            }
            if (subgraph.bottom <= gapTop + clearance / 4 || subgraph.top >= gapBottom - clearance / 4) {
                continue;
            }
            if (RectF.intersects(subgraph, slab)
                    || occupiesColumn(subgraph, from, gapTop, gapBottom)) {
                union = union == null ? new RectF(subgraph) : unionWith(union, subgraph);
            }
        }
        return union;
    }

    private boolean occupiesColumn(@NonNull RectF box,
                                   @NonNull RectF from,
                                   float gapTop,
                                   float gapBottom) {
        boolean inY = box.top < gapBottom && box.bottom > gapTop;
        boolean inX = box.centerX() > from.centerX() - from.width()
                && box.centerX() < from.centerX() + from.width();
        return inY && inX;
    }

    @Nullable
    private RectF descendantUnion(@NonNull String rootId, @NonNull String stopId) {
        RectF root = nodeBounds.get(rootId);
        if (root == null) {
            return null;
        }
        RectF union = new RectF(root);
        Set<String> visited = new HashSet<>();
        List<String> stack = new ArrayList<>();
        stack.add(rootId);
        while (!stack.isEmpty()) {
            String current = stack.remove(stack.size() - 1);
            if (!visited.add(current) || stopId.equals(current)) {
                continue;
            }
            RectF box = nodeBounds.get(current);
            if (box != null) {
                union.union(box);
            }
            stack.addAll(incoming.getOrDefault(current, Collections.emptyList()));
        }
        return union;
    }

    private enum Port {
        TOP, BOTTOM, LEFT, RIGHT
    }

    @NonNull
    private PointF portPoint(@NonNull RectF box, @NonNull Port port) {
        return portPoint(box, port, 0f);
    }

    @NonNull
    private PointF portPoint(@NonNull RectF box, @NonNull Port port, float outset) {
        switch (port) {
            case TOP:
                return new PointF(box.centerX(), box.top - outset);
            case BOTTOM:
                return new PointF(box.centerX(), box.bottom + outset);
            case LEFT:
                return new PointF(box.left - outset, box.centerY());
            case RIGHT:
            default:
                return new PointF(box.right + outset, box.centerY());
        }
    }

    /**
     * Orthogonal path between two cardinal ports. The first segment leaves along
     * the source side normal; the last segment arrives along the dest side normal.
     */
    @NonNull
    private List<PointF> routePorts(@NonNull RectF from,
                                    @NonNull Port startPort,
                                    @NonNull RectF to,
                                    @NonNull Port endPort) {
        PointF start = portPoint(from, startPort, clearance);
        PointF end = portPoint(to, endPort, clearance);
        List<PointF> points = new ArrayList<>();
        points.add(start);

        if (Math.abs(start.x - end.x) < 1.5f) {
            points.add(new PointF(start.x, end.y));
            return points;
        }
        if (Math.abs(start.y - end.y) < 1.5f) {
            points.add(new PointF(end.x, start.y));
            return points;
        }

        if (startPort == Port.BOTTOM && endPort == Port.TOP) {
            // Keep the T-bar just below the sources so the stem into the operator is long.
            float minStem = clearance * 3f;
            float mergeY = start.y + clearance;
            mergeY = Math.min(mergeY, end.y - minStem);
            mergeY = Math.max(start.y, Math.min(end.y - clearance, mergeY));
            points.add(new PointF(start.x, mergeY));
            points.add(new PointF(end.x, mergeY));
            points.add(end);
            return points;
        }

        points.add(new PointF(start.x, end.y));
        points.add(end);
        return points;
    }

    @NonNull
    private List<PointF> routeBypass(@NonNull String fromId,
                                     @NonNull String toId,
                                     @NonNull RectF from,
                                     @NonNull RectF to) {
        RectF obstacle = obstacleUnion(fromId, toId, from, to);
        if (obstacle == null) {
            obstacle = new RectF(from);
            obstacle.union(to);
        }

        boolean goLeft = chooseLeftLane(from, to);
        int lane = goLeft
                ? leftLaneIndex.getOrDefault(fromId, 1)
                : rightLaneIndex.getOrDefault(fromId, 1);
        lane = Math.max(lane, 1);

        float laneX = goLeft
                ? Math.min(from.left, obstacle.left) - lane * horizontalGap
                : Math.max(from.right, obstacle.right) + lane * horizontalGap;
        float minX = horizontalGap * 0.4f;
        float maxX = contentWidth - horizontalGap * 0.4f;
        if (maxX <= minX) {
            maxX = Math.max(laneX, minX);
        }
        laneX = Math.max(minX, Math.min(maxX, laneX));

        PointF start = portPoint(from, goLeft ? Port.LEFT : Port.RIGHT, clearance);
        PointF end = portPoint(to, goLeft ? Port.LEFT : Port.RIGHT, clearance);

        List<PointF> points = new ArrayList<>();
        points.add(start);
        points.add(new PointF(laneX, start.y));
        points.add(new PointF(laneX, end.y));
        points.add(end);
        return points;
    }

    private boolean chooseLeftLane(@NonNull RectF from, @NonNull RectF to) {
        return from.centerX() <= contentWidth / 2;
    }

    private void assignBypassLanes(@NonNull List<GraphyConnection> connections) {
        List<String> left = new ArrayList<>();
        List<String> right = new ArrayList<>();
        for (GraphyConnection connection : connections) {
            String fromId = connection.fromNodeId();
            String toId = connection.toNodeId();
            RectF from = nodeBounds.get(fromId);
            RectF to = nodeBounds.get(toId);
            if (from == null || to == null) {
                continue;
            }
            RectF obstacle = obstacleUnion(fromId, toId, from, to);
            if (obstacle == null) {
                continue;
            }
            routeSpans.put(fromId, Math.max(routeSpans.getOrDefault(fromId, 0f), to.top - from.bottom));
            if (chooseLeftLane(from, to)) {
                if (!left.contains(fromId)) {
                    left.add(fromId);
                }
            } else if (!right.contains(fromId)) {
                right.add(fromId);
            }
        }
        sortByRouteSpan(left);
        sortByRouteSpan(right);
        for (int i = 0; i < left.size(); i++) {
            leftLaneIndex.put(left.get(i), i + 1);
        }
        for (int i = 0; i < right.size(); i++) {
            rightLaneIndex.put(right.get(i), i + 1);
        }
    }

    private void sortByRouteSpan(@NonNull List<String> ids) {
        Collections.sort(ids, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                float ha = routeSpans.getOrDefault(a, 0f);
                float hb = routeSpans.getOrDefault(b, 0f);
                return Float.compare(ha, hb);
            }
        });
    }

    @NonNull
    private static RectF unionWith(@NonNull RectF a, @NonNull RectF b) {
        RectF out = new RectF(a);
        out.union(b);
        return out;
    }

    static boolean isAxisAligned(@NonNull List<PointF> points) {
        for (int i = 1; i < points.size(); i++) {
            PointF a = points.get(i - 1);
            PointF b = points.get(i);
            if (Math.abs(a.x - b.x) > 0.5f && Math.abs(a.y - b.y) > 0.5f) {
                return false;
            }
        }
        return true;
    }
}
