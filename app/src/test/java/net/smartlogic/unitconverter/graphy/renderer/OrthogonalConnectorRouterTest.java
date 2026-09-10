package net.smartlogic.unitconverter.graphy.renderer;

import android.graphics.PointF;
import android.graphics.RectF;

import net.smartlogic.unitconverter.graphy.model.GraphyConnection;
import net.smartlogic.unitconverter.graphy.model.GraphyNode;
import net.smartlogic.unitconverter.graphy.model.GraphyNodeType;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OrthogonalConnectorRouterTest {

    private static final float GAP = 24f;
    private static final float VERTICAL = 40f;

    @Test
    public void green_flatParallelism_usesTopEntryWithoutOuterBypass() {
        Map<String, RectF> bounds = new HashMap<>();
        bounds.put("in6", rect(80, 0, 40, 30));
        bounds.put("in9", rect(140, 0, 40, 30));
        bounds.put("mulL", rect(120, 50, 30, 30));
        bounds.put("d54", rect(110, 100, 50, 30));

        bounds.put("in3", rect(280, 0, 40, 30));
        bounds.put("in5", rect(340, 0, 40, 30));
        bounds.put("mulR", rect(320, 50, 30, 30));
        bounds.put("d15", rect(310, 100, 50, 30));

        bounds.put("add", rect(220, 180, 30, 30));
        bounds.put("res", rect(205, 230, 60, 30));

        List<GraphyNode> nodes = nodes(
                input("in6"), input("in9"), op("mulL"), derived("d54"),
                input("in3"), input("in5"), op("mulR"), derived("d15"),
                op("add"), result("res")
        );
        List<GraphyConnection> connections = connections(
                "in6", "mulL",
                "in9", "mulL",
                "mulL", "d54",
                "in3", "mulR",
                "in5", "mulR",
                "mulR", "d15",
                "d54", "add",
                "d15", "add",
                "add", "res"
        );

        OrthogonalConnectorRouter router = router(nodes, connections, bounds, 400f);

        List<PointF> leftIn = router.route("in6", "mulL");
        List<PointF> from54 = router.route("d54", "add");
        List<PointF> from15 = router.route("d15", "add");

        assertTrue(OrthogonalConnectorRouter.isAxisAligned(leftIn));
        assertTrue(OrthogonalConnectorRouter.isAxisAligned(from54));
        assertTrue(OrthogonalConnectorRouter.isAxisAligned(from15));

        float innerLeft = 80f;
        float innerRight = 380f;
        assertTrue("green bypass should stay in inner columns", minX(from54) >= innerLeft - 1f);
        assertTrue(maxX(from54) <= innerRight + 1f);
        assertTrue(minX(from15) >= innerLeft - 1f);
        assertTrue(maxX(from15) <= innerRight + 1f);

        assertEquals(bounds.get("in6").centerX(), leftIn.get(0).x, 1f);
        assertTrue(leftIn.get(0).y > bounds.get("in6").bottom);
        assertEquals(bounds.get("mulL").centerX(), last(leftIn).x, 1f);
        assertTrue(last(leftIn).y < bounds.get("mulL").top);

        PointF addTop = last(from54);
        assertEquals(bounds.get("add").centerX(), addTop.x, 1f);
        assertTrue(addTop.y < bounds.get("add").top);
        assertEquals(bounds.get("add").centerX(), last(from15).x, 1f);
        assertTrue(last(from15).y < bounds.get("add").top);
    }

    @Test
    public void derivedResult_overlappingOperator_isStraightVertical() {
        Map<String, RectF> bounds = new HashMap<>();
        bounds.put("eight", rect(106, 0, 28, 24));
        bounds.put("add", rect(106, 40, 28, 28));
        bounds.put("left", rect(20, 0, 40, 24));

        List<GraphyNode> nodes = nodes(derived("eight"), derived("left"), op("add"));
        List<GraphyConnection> connections = connections(
                "eight", "add",
                "left", "add"
        );
        OrthogonalConnectorRouter router = router(nodes, connections, bounds, 200f);

        List<PointF> path = router.route("eight", "add");
        assertTrue(OrthogonalConnectorRouter.isAxisAligned(path));
        assertEquals("straight drop should not jog", 2, path.size());
        assertEquals(path.get(0).x, path.get(1).x, 0.1f);
        assertEquals(bounds.get("eight").centerX(), path.get(0).x, 0.1f);
        assertTrue(path.get(0).y > bounds.get("eight").bottom);
        assertTrue(last(path).y < bounds.get("add").top);
    }

    @Test
    public void yellow_obstacleBypass_routesAroundLowerInputsToOperatorSide() {
        Map<String, RectF> bounds = new HashMap<>();
        bounds.put("d54", rect(100, 0, 50, 30));
        bounds.put("in3", rect(80, 80, 40, 30));
        bounds.put("in5", rect(140, 80, 40, 30));
        bounds.put("mul", rect(120, 130, 30, 30));
        bounds.put("d15", rect(110, 180, 50, 30));
        bounds.put("add", rect(120, 240, 30, 30));

        bounds.put("d15r", rect(400, 0, 50, 30));
        bounds.put("in6", rect(380, 80, 40, 30));
        bounds.put("in5r", rect(440, 80, 40, 30));
        bounds.put("mulR", rect(420, 130, 30, 30));
        bounds.put("d12", rect(410, 180, 50, 30));
        bounds.put("mulFinal", rect(420, 240, 30, 30));

        List<GraphyNode> nodes = nodes(
                derived("d54"), input("in3"), input("in5"), op("mul"), derived("d15"), op("add"),
                derived("d15r"), input("in6"), input("in5r"), op("mulR"), derived("d12"), op("mulFinal")
        );
        List<GraphyConnection> connections = connections(
                "d54", "add",
                "in3", "mul",
                "in5", "mul",
                "mul", "d15",
                "d15", "add",
                "d15r", "mulFinal",
                "in6", "mulR",
                "in5r", "mulR",
                "mulR", "d12",
                "d12", "mulFinal"
        );

        OrthogonalConnectorRouter router = router(nodes, connections, bounds, 520f);

        List<PointF> bypassLeft = router.route("d54", "add");
        List<PointF> lowerLeft = router.route("d15", "add");
        List<PointF> bypassRight = router.route("d15r", "mulFinal");

        assertTrue(OrthogonalConnectorRouter.isAxisAligned(bypassLeft));
        assertTrue(OrthogonalConnectorRouter.isAxisAligned(lowerLeft));
        assertTrue(OrthogonalConnectorRouter.isAxisAligned(bypassRight));

        RectF obstacleLeft = new RectF(bounds.get("in3"));
        obstacleLeft.union(bounds.get("in5"));
        assertTrue(minX(bypassLeft) < obstacleLeft.left);
        PointF leftTip = last(bypassLeft);
        assertEquals(bounds.get("add").left - 8f, leftTip.x, 1f);
        assertEquals(bounds.get("add").centerY(), leftTip.y, 1f);

        PointF fifteenTip = last(lowerLeft);
        assertEquals(bounds.get("add").centerX(), fifteenTip.x, 1f);
        assertEquals(bounds.get("add").top - 8f, fifteenTip.y, 1f);

        RectF obstacleRight = new RectF(bounds.get("in6"));
        obstacleRight.union(bounds.get("in5r"));
        assertTrue(maxX(bypassRight) > obstacleRight.right);
        PointF rightTip = last(bypassRight);
        assertEquals(bounds.get("mulFinal").right + 8f, rightTip.x, 1f);
        assertEquals(bounds.get("mulFinal").centerY(), rightTip.y, 1f);
    }

    @Test
    public void red_companionBypass_staysOutsideNestedStack_andNestedLaneIsFarther() {
        Map<String, RectF> bounds = new HashMap<>();
        bounds.put("n7", rect(40, 40, 40, 30));
        bounds.put("n8", rect(90, 40, 40, 30));
        bounds.put("add", rect(70, 90, 30, 30));
        bounds.put("n15", rect(60, 140, 50, 30));
        bounds.put("n6", rect(50, 190, 40, 30));
        bounds.put("div", rect(70, 240, 30, 30));
        bounds.put("n04", rect(60, 290, 50, 30));
        bounds.put("n5", rect(200, 0, 50, 30));
        bounds.put("sub", rect(100, 360, 30, 30));
        bounds.put("outer", rect(210, -80, 50, 30));
        bounds.put("rootMul", rect(140, 430, 30, 30));
        bounds.put("leftRes", rect(0, 400, 50, 30));

        List<GraphyNode> nodes = nodes(
                input("n7"), input("n8"), op("add"), derived("n15"),
                input("n6"), op("div"), derived("n04"),
                input("n5"), op("sub"),
                derived("outer"), derived("leftRes"), op("rootMul")
        );
        List<GraphyConnection> connections = connections(
                "n7", "add",
                "n8", "add",
                "add", "n15",
                "n6", "div",
                "n15", "div",
                "div", "n04",
                "n5", "sub",
                "n04", "sub",
                "outer", "rootMul",
                "leftRes", "rootMul"
        );

        OrthogonalConnectorRouter router = router(nodes, connections, bounds, 360f);

        List<PointF> companion = router.route("n5", "sub");
        assertTrue(OrthogonalConnectorRouter.isAxisAligned(companion));

        RectF stack = new RectF(bounds.get("n7"));
        stack.union(bounds.get("n8"));
        stack.union(bounds.get("add"));
        stack.union(bounds.get("n15"));
        stack.union(bounds.get("n6"));
        stack.union(bounds.get("div"));
        stack.union(bounds.get("n04"));

        assertTrue(maxX(companion) > stack.right);
        PointF tip = last(companion);
        assertEquals(bounds.get("sub").right + 8f, tip.x, 1f);
        assertEquals(bounds.get("sub").centerY(), tip.y, 1f);

        List<PointF> nested = router.route("outer", "rootMul");
        assertTrue(OrthogonalConnectorRouter.isAxisAligned(nested));
        assertTrue("nested bypass should sit farther out than the inner companion lane",
                maxX(nested) > maxX(companion));
        assertTrue(maxX(nested) > stack.right);
        assertEquals(bounds.get("rootMul").right + 8f, last(nested).x, 1f);
    }

    private static OrthogonalConnectorRouter router(List<GraphyNode> nodes,
                                                    List<GraphyConnection> connections,
                                                    Map<String, RectF> bounds,
                                                    float contentWidth) {
        return new OrthogonalConnectorRouter(nodes, connections, bounds, GAP, VERTICAL, contentWidth);
    }

    private static RectF rect(float x, float y, float w, float h) {
        return new RectF(x, y, x + w, y + h);
    }

    private static GraphyNode input(String id) {
        return new GraphyNode(id, GraphyNodeType.INPUT, id, id, "operand");
    }

    private static GraphyNode derived(String id) {
        return new GraphyNode(id, GraphyNodeType.DERIVED, id, id, "derived");
    }

    private static GraphyNode result(String id) {
        return new GraphyNode(id, GraphyNodeType.RESULT, id, id, "result");
    }

    private static GraphyNode op(String id) {
        return new GraphyNode(id, GraphyNodeType.OPERATION, "+", "+", "addition");
    }

    private static List<GraphyNode> nodes(GraphyNode... values) {
        List<GraphyNode> list = new ArrayList<>();
        Collections.addAll(list, values);
        return list;
    }

    private static List<GraphyConnection> connections(String... fromTo) {
        List<GraphyConnection> list = new ArrayList<>();
        for (int i = 0; i + 1 < fromTo.length; i += 2) {
            list.add(new GraphyConnection(fromTo[i], fromTo[i + 1], null));
        }
        return list;
    }

    private static PointF last(List<PointF> points) {
        return points.get(points.size() - 1);
    }

    private static float minX(List<PointF> points) {
        float min = Float.MAX_VALUE;
        for (PointF point : points) {
            min = Math.min(min, point.x);
        }
        return min;
    }

    private static float maxX(List<PointF> points) {
        float max = Float.MIN_VALUE;
        for (PointF point : points) {
            max = Math.max(max, point.x);
        }
        return max;
    }
}
