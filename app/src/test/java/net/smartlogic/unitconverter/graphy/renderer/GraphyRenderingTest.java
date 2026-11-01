package net.smartlogic.unitconverter.graphy.renderer;

import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.View;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.graphy.integration.*;
import net.smartlogic.unitconverter.graphy.model.*;
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme;
import net.smartlogic.unitconverter.model.Conversion;
import net.smartlogic.unitconverter.model.Unit;
import net.smartlogic.unitconverter.theme.CalculatorThemes;
import net.smartlogic.unitconverter.theme.ThemeManager;
import net.smartlogic.unitconverter.utils.Conversions;
import net.smartlogic.unitconverter.utils.ExpressionEvaluator;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.io.File;
import java.io.FileOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class, qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class GraphyRenderingTest {
    @org.junit.Before public void unlockThemesForRenderingCoverage() {
        ThemeManager.setProAccessProvider(() -> true);
    }
    @After public void reset() {
        ThemeManager.setProAccessProvider(() -> false);
        RuntimeEnvironment.setFontScale(1f);
        ThemeManager.select(RuntimeEnvironment.getApplication(), CalculatorThemes.DEFAULT_ID);
    }
    private GraphyOutput expression(String expression, String result) {
        return new GraphyBridge().build(CalculationSnapshot.create("basic", expression, expression,
                result, ExpressionEvaluator.evaluate(expression)));
    }
    private GraphyOutput linear(String input, String result) {
        return net.smartlogic.unitconverter.graphy.builder.ConversionGraphBuilder.build("unit",
                new ConversionSnapshot(5, 3.106855, input, result, "km", "mi",
                        ConversionTransformation.linear(0.621371), 0),
                new net.smartlogic.unitconverter.graphy.builder.ConversionGraphBuilder.Text(
                        "Conversion relationship", "1 km = 0.621371 mi", "5 × 0.621371", "5 × 0.621371 = 3.106855 mi"));
    }
    @Test
    @Config(sdk = 26, application = Application.class)
    @GraphicsMode(GraphicsMode.Mode.LEGACY)
    public void sharedNodesCanBeMeasuredAtMinimumSupportedApi() {
        var layout = new GraphLayoutEngine().layout(linear("5 km", "3.106855 mi"),
                new GraphyViewTheme(RuntimeEnvironment.getApplication()), 320);
        assertTrue(layout.nodeBounds().keySet().containsAll(List.of("source", "relationship", "operation", "target")));
        assertTrue(layout.contentHeight() > 0);
    }
    @Test public void graphFitsNarrowWidthWithoutShrinkingOrClippingText() {
        RuntimeEnvironment.setFontScale(2f);
        GraphyOutput graph = linear("₹1,25,67,890.45 kilometres with a long translated unit name",
                "0.000000123 international nautical miles");
        GraphyViewTheme theme = new GraphyViewTheme(RuntimeEnvironment.getApplication());
        var layout = new GraphLayoutEngine().layout(graph, theme, 240);
        assertTrue(layout.nodeBounds().keySet().containsAll(graph.getNodes().stream().map(GraphyNode::id).toList()));
        for (var entry : layout.nodeBounds().entrySet()) {
            var box = entry.getValue();
            assertTrue(box.x() >= 0);
            assertTrue(box.x() + box.width() <= layout.contentWidth());
            assertTrue(box.height() >= theme.getNodeMinHeight());
            var text = layout.content().get(entry.getKey());
            assertTrue(text.value().getPaint().getTextSize() >= theme.getValueTextSize());
        }
        assertTrue(layout.content().get("source").value().getLineCount() > 1);
        assertNoOverlaps(layout);
    }
    @Test public void compactShapesRetainSeparateAccessibleTouchTargets() {
        var context = RuntimeEnvironment.getApplication();
        var theme = new GraphyViewTheme(context);
        var graph = expression("1+2", "3");
        var layout = new GraphLayoutEngine().layout(graph, theme, 320);
        var view = new FlowchartGraphView(context);
        view.setGraph(graph, theme);
        view.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, 320, view.getMeasuredHeight());
        var provider = view.getAccessibilityNodeProvider();
        java.util.List<android.graphics.Rect> targets = new java.util.ArrayList<>();
        for (int i = 0; i < layout.presentation().getNodes().size(); i++) {
            var node = layout.presentation().getNodes().get(i);
            var visual = layout.nodeBounds().get(node.id());
            var target = new android.graphics.Rect();
            provider.createAccessibilityNodeInfo(i).getBoundsInParent(target);
            assertTrue(target.width() >= theme.getTouchTarget());
            assertTrue(target.height() >= theme.getTouchTarget());
            assertTrue(target.top >= 0 && target.bottom <= view.getMeasuredHeight());
            for (var other : targets) assertFalse(android.graphics.Rect.intersects(other, target));
            targets.add(target);
            assertTrue("Default cards should be visibly compact", visual.height() < theme.getTouchTarget());
            if (node.type() == GraphyNodeType.OPERATION) {
                assertTrue(visual.width() >= theme.getOperationMarkerSize());
                assertTrue(visual.width() < theme.getTouchTarget());
                assertEquals(visual.width(), visual.height(), 0.01f);
                assertTrue(theme.getOperationTextSize() > theme.getValueTextSize());
                GraphyNode[] selected = new GraphyNode[1];
                view.setOnNodeClickListener(clicked -> selected[0] = clicked);
                float x = target.centerX(), y = target.top + 1;
                assertTrue(y < visual.y());
                for (int action : new int[]{android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP}) {
                    var event = android.view.MotionEvent.obtain(0, 0, action, x, y, 0);
                    view.onTouchEvent(event);
                    event.recycle();
                }
                assertNotNull(selected[0]);
                assertEquals(node.id(), selected[0].id());
            }
        }
    }

    @Test public void multiStepInputsAppearNearTheirConsumers() {
        var graph = expression("(500+600*6%)/2+1250", "1,518");
        var layout = new GraphLayoutEngine().layout(graph, new GraphyViewTheme(RuntimeEnvironment.getApplication()), 360);
        String early = graph.getNodes().stream().filter(n -> n.displayValue().equals("600")).findFirst().orElseThrow().id();
        String late = graph.getNodes().stream().filter(n -> n.displayValue().equals("1250")).findFirst().orElseThrow().id();
        assertTrue(layout.nodeBounds().get(late).y() > layout.nodeBounds().get(early).y());
        assertNoOverlaps(layout);
    }

    @Test public void onlyFinalResultIsBoldAndGenericRolesDoNotConsumeCardSpace() {
        var graph = expression("(12+8)*3-15", "45");
        var theme = new GraphyViewTheme(RuntimeEnvironment.getApplication());
        var layout = new GraphLayoutEngine().layout(graph, theme, 320);
        for (var node : graph.getNodes()) {
            var content = layout.content().get(node.id());
            assertEquals(node.type() == GraphyNodeType.RESULT, content.value().getPaint().getTypeface().isBold());
            assertEquals("Role belongs in details/accessibility, not a repeated canvas caption",
                    "", content.caption().getText().toString());
            assertTrue(content.width() <= theme.getNodeMaxWidth());
            assertTrue(content.height() >= theme.getNodeMinHeight());
        }
        assertEquals("45", graph.getResult());
        assertTrue(graph.getNodes().stream().anyMatch(n -> n.type() == GraphyNodeType.DERIVED && n.displayValue().equals("20")));
        assertTrue(graph.getNodes().stream().anyMatch(n -> n.type() == GraphyNodeType.DERIVED && n.displayValue().equals("60")));
        assertEquals("(12+8)*3-15", graph.getMetadata().get(GraphyOutput.METADATA_CONTEXT));
        var genericDerived = new GraphyNode("derived", GraphyNodeType.DERIVED,
                theme.nodeRole(GraphyNodeType.DERIVED), "20", "derived");
        assertEquals("", GraphyNodeContent.measure(genericDerived, theme, 320).caption().getText().toString());
    }

    @Test public void longerGraphsGrowDownwardAndKeepMeaningfulCaptionsAndWrappedValues() {
        var theme = new GraphyViewTheme(RuntimeEnvironment.getApplication());
        var shortGraph = new GraphLayoutEngine().layout(expression("12+8", "20"), theme, 240);
        var longGraph = new GraphLayoutEngine().layout(expression("((12+8)*3-15)/5+7", "16"), theme, 240);
        assertEquals(shortGraph.contentWidth(), longGraph.contentWidth());
        assertTrue(longGraph.contentHeight() > shortGraph.contentHeight());
        assertNoOverlaps(longGraph);
        var conversion = new GraphLayoutEngine().layout(linear("5 km", "3.106855 mi"), theme, 240);
        assertEquals("Conversion relationship", conversion.content().get("relationship").caption().getText().toString());
        assertEquals("", conversion.content().get("source").caption().getText().toString());
        assertEquals("", conversion.content().get("target").caption().getText().toString());
    }

    @Test public void nonCommutativeOperandsKeepTheirDeclaredLeftToRightOrder() {
        var graph = expression("(12+8)*3-15", "45");
        var layout = new GraphLayoutEngine().layout(graph, new GraphyViewTheme(RuntimeEnvironment.getApplication()), 320);
        var left = graph.getNodes().stream().filter(n -> n.displayValue().equals("60")).findFirst().orElseThrow();
        var right = graph.getNodes().stream().filter(n -> n.displayValue().equals("15")).findFirst().orElseThrow();
        assertEquals(layout.nodeBounds().get(left.id()).y(), layout.nodeBounds().get(right.id()).y(), 0f);
        assertTrue(layout.nodeBounds().get(left.id()).x() < layout.nodeBounds().get(right.id()).x());
    }

    @Test public void twoShortIndependentCalculationsFitWithoutHorizontalOverflow() {
        var theme = new GraphyViewTheme(RuntimeEnvironment.getApplication());
        var layout = new GraphLayoutEngine().layout(expression("(12+8)*(9-4)", "100"), theme, 320);
        assertEquals(320, layout.contentWidth());
        assertEquals(List.of(2, 1), layout.stages().stream().map(GraphLayoutEngine.StageMarker::calculationCount).toList());
        assertNoOverlaps(layout);
    }

    @Test public void declaredOperandEdgesDetermineOrderEvenWhenNodeListIsReversed() {
        var original = expression("20-5", "15");
        var nodes = new java.util.ArrayList<>(original.getNodes());
        java.util.Collections.reverse(nodes);
        var graph = GraphyOutput.create("test", "20-5", "15", nodes, original.getConnections(), null, null);
        var theme = new GraphyViewTheme(RuntimeEnvironment.getApplication());
        var layout = new GraphLayoutEngine().layout(graph, theme, 320);
        var left = graph.getNodes().stream().filter(n -> n.type() == GraphyNodeType.INPUT && n.displayValue().equals("20")).findFirst().orElseThrow();
        var right = graph.getNodes().stream().filter(n -> n.type() == GraphyNodeType.INPUT && n.displayValue().equals("5")).findFirst().orElseThrow();
        assertTrue(layout.nodeBounds().get(left.id()).x() < layout.nodeBounds().get(right.id()).x());
    }

    @Test public void reportedExpressionUsesThreeParallelStagesWithoutValueCopies() {
        var graph = expression("633-9-9+6*9/2", "642");
        var theme = new GraphyViewTheme(RuntimeEnvironment.getApplication());
        var layout = new GraphLayoutEngine().layout(graph, theme, 360);
        assertEquals(List.of(2, 2, 1), layout.stages().stream().map(GraphLayoutEngine.StageMarker::calculationCount).toList());
        assertEquals(1, layout.presentation().getNodes().stream().filter(n -> n.displayValue().equals("615")).count());
        assertEquals(graph.getNodes().size(), layout.presentation().getNodes().size());
        assertEquals(graph.getConnections(), layout.presentation().getConnections());
        for (var edge : graph.getConnections()) {
            var from = layout.nodeBounds().get(edge.fromNodeId());
            var to = layout.nodeBounds().get(edge.toNodeId());
            assertTrue(to.y() > from.y() + from.height());
        }
        assertEquals("642", graph.getResult());
        assertNoOverlaps(layout);
    }

    @Test public void complexStagesFoldWithoutLosingBoundaryOutputsOrConfusingEqualValues() {
        var graph = expression("((12+8)*(9-4)+(18/3-2))/(7-3)", "26");
        var theme = new GraphyViewTheme(RuntimeEnvironment.getApplication());
        var expanded = new GraphLayoutEngine().layout(graph, theme, 320);
        assertEquals(List.of(4, 2, 1, 1), expanded.stages().stream().map(GraphLayoutEngine.StageMarker::calculationCount).toList());
        assertTrue("Four independent calculations need horizontal room", expanded.contentWidth() > 320);
        var fours = graph.getNodes().stream().filter(n -> n.type() == GraphyNodeType.DERIVED && n.displayValue().equals("4")).toList();
        assertEquals(2, fours.size());
        assertNotEquals(fours.get(0).id(), fours.get(1).id());
        for (int mask = 0; mask < 16; mask++) {
            java.util.Set<Integer> collapsed = new java.util.HashSet<>();
            for (int stage = 1; stage <= 4; stage++) if ((mask & (1 << (stage - 1))) != 0) collapsed.add(stage);
            var folded = new GraphLayoutEngine().layout(graph, theme, 320, collapsed);
            assertEquals(4, folded.stages().size());
            assertEquals(folded.presentation().getNodes().size(), GraphyTopology.orderedNodes(folded.presentation()).size());
            assertEquals("26", folded.presentation().getResult());
            for (var four : fours) assertTrue(folded.nodeBounds().containsKey(four.id()));
            for (var node : folded.presentation().getNodes()) assertSame(node, folded.originals().get(node.id()));
            assertNoOverlaps(folded);
        }
        var firstFolded = new GraphLayoutEngine().layout(graph, theme, 320, java.util.Set.of(1));
        assertTrue(firstFolded.contentHeight() < expanded.contentHeight());
        assertTrue(firstFolded.contentWidth() < expanded.contentWidth());
        assertTrue(firstFolded.stages().get(0).collapsed());
    }

    @Test public void foldingLaterStageContractsEdgesFromSharedPreviousResults() {
        var graph = expression("20-5-3", "12");
        var theme = new GraphyViewTheme(RuntimeEnvironment.getApplication());
        var expanded = new GraphLayoutEngine().layout(graph, theme, 320);
        var folded = new GraphLayoutEngine().layout(graph, theme, 320, java.util.Set.of(2));
        var fifteen = graph.getNodes().stream().filter(n -> n.type() == GraphyNodeType.DERIVED && n.displayValue().equals("15")).findFirst().orElseThrow();
        var result = graph.getNodes().stream().filter(n -> n.type() == GraphyNodeType.RESULT).findFirst().orElseThrow();
        assertEquals(1, expanded.presentation().getNodes().stream().filter(n -> n.id().equals(fifteen.id())).count());
        assertTrue(folded.presentation().getConnections().stream().anyMatch(e -> e.fromNodeId().equals(fifteen.id()) && e.toNodeId().equals(result.id())));
        assertEquals(expanded.stages().get(1).number(), folded.stages().get(1).number());
        assertTrue(folded.contentHeight() < expanded.contentHeight());
    }

    @Config(sdk = 26, application = Application.class)
    @Test public void foldControlsHaveAccessibleActionsAndKeepOriginalResultDetails() {
        var context = RuntimeEnvironment.getApplication();
        var graph = expression("20-5-3", "12");
        var theme = new GraphyViewTheme(context);
        var view = new FlowchartGraphView(context);
        view.setGraph(graph, theme);
        view.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, 320, view.getMeasuredHeight());
        var provider = view.getAccessibilityNodeProvider();
        int foldId = graph.getNodes().size();
        var control = provider.createAccessibilityNodeInfo(foldId);
        assertTrue(control.getContentDescription().toString().startsWith("Collapse stage 1"));
        var rect = new android.graphics.Rect();
        control.getBoundsInParent(rect);
        assertTrue(rect.width() >= theme.getTouchTarget() && rect.height() >= theme.getTouchTarget());
        assertTrue(provider.performAction(foldId, androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_COLLAPSE, null));
        assertEquals(java.util.Set.of(1), view.getCollapsedStages());
        var folded = new GraphLayoutEngine().layout(graph, theme, 320, java.util.Set.of(1));
        int newId = folded.presentation().getNodes().size();
        assertTrue(provider.createAccessibilityNodeInfo(newId).getContentDescription().toString().startsWith("Expand stage 1"));
        assertTrue(provider.performAction(newId, androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_EXPAND, null));
        assertTrue(view.getCollapsedStages().isEmpty());
    }

    @Test public void wrappedRightOperandDoesNotReverseAccessibilityReadingOrder() {
        RuntimeEnvironment.setFontScale(2f);
        var graph = GraphyOutput.create("test", "", "answer", List.of(
                new GraphyNode("left", GraphyNodeType.INPUT, "1", "1", "operand"),
                new GraphyNode("right", GraphyNodeType.INPUT, "123456789012345678901234567890",
                        "123456789012345678901234567890", "operand"),
                new GraphyNode("subtract", GraphyNodeType.OPERATION, "-", "-", "subtraction"),
                new GraphyNode("result", GraphyNodeType.RESULT, "answer", "answer", "result")),
                List.of(new GraphyConnection("left", "subtract", null),
                        new GraphyConnection("right", "subtract", null),
                        new GraphyConnection("subtract", "result", null)), null, null);
        var context = RuntimeEnvironment.getApplication();
        var view = new FlowchartGraphView(context);
        view.setMaxWidth(240);
        view.setGraph(graph, new GraphyViewTheme(context));
        view.measure(View.MeasureSpec.makeMeasureSpec(240, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, 240, view.getMeasuredHeight());
        assertEquals("Input: 1", view.getAccessibilityNodeProvider().createAccessibilityNodeInfo(0)
                .getContentDescription().toString());
    }

    @Test public void nativeCanvasCannotEraseSiblingContentOutsideItsBounds() {
        var context = RuntimeEnvironment.getApplication();
        var view = new FlowchartGraphView(context);
        view.setMaxWidth(240);
        view.setGraph(expression("2", "2"), new GraphyViewTheme(context));
        view.measure(View.MeasureSpec.makeMeasureSpec(240, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, 240, view.getMeasuredHeight());
        var bitmap = Bitmap.createBitmap(320, view.getMeasuredHeight() + 100, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(android.graphics.Color.MAGENTA);
        var canvas = new Canvas(bitmap);
        canvas.translate(20, 60);
        view.draw(canvas);
        assertEquals("Compose header must survive native graph drawing", android.graphics.Color.MAGENTA, bitmap.getPixel(30, 20));
        assertEquals("Right sibling must survive", android.graphics.Color.MAGENTA, bitmap.getPixel(300, 80));
        bitmap.recycle();
    }

    @Test public void connectorHasNoBlankGapAtSourceArrowBaseOrDestination() {
        var context = RuntimeEnvironment.getApplication();
        var graph = expression("1+2", "3");
        var theme = new GraphyViewTheme(context);
        var layout = new GraphLayoutEngine().layout(graph, theme, 320);
        var operation = graph.getNodes().stream().filter(n -> n.type() == GraphyNodeType.OPERATION).findFirst().orElseThrow();
        var result = graph.getNodes().stream().filter(n -> n.type() == GraphyNodeType.RESULT).findFirst().orElseThrow();
        var from = layout.nodeBounds().get(operation.id());
        var to = layout.nodeBounds().get(result.id());
        var view = new FlowchartGraphView(context);
        view.setMaxWidth(320);
        view.setGraph(graph, theme);
        view.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, 320, view.getMeasuredHeight());
        var bitmap = Bitmap.createBitmap(320, view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        int center = Math.round(from.x() + from.width() / 2);
        for (int y = (int) Math.ceil(from.y() + from.height()); y < to.y(); y++) {
            boolean ink = false;
            for (int x = center - 1; x <= center + 1; x++) {
                ink |= bitmap.getPixel(x, y) != theme.getBackgroundColor();
            }
            assertTrue("Disconnected connector at row " + y, ink);
        }
        bitmap.recycle();
    }

    @Test public void intermediateBorderIsActuallyDashedAndNextInputBorderRemainsSolid() {
        var context = RuntimeEnvironment.getApplication();
        ThemeManager.select(context, CalculatorThemes.DEFAULT_ID);
        var graph = expression("(12+8)*3-15", "45");
        var theme = new GraphyViewTheme(context);
        var layout = new GraphLayoutEngine().layout(graph, theme, 320);
        var view = new FlowchartGraphView(context);
        view.setMaxWidth(320);
        view.setGraph(graph, theme);
        view.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, 320, view.getMeasuredHeight());
        var bitmap = Bitmap.createBitmap(320, view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        for (var node : graph.getNodes()) {
            if (node.type() != GraphyNodeType.INPUT && node.type() != GraphyNodeType.DERIVED) continue;
            var box = layout.nodeBounds().get(node.id());
            int ink = 0, gaps = 0;
            int y = (int) box.y();
            int margin = (int) Math.ceil(theme.getNodeCornerRadius() + theme.getNodeBorderStroke());
            for (int x = (int) box.x() + margin; x < box.x() + box.width() - margin; x++) {
                if (bitmap.getPixel(x, y) == theme.nodeBorder(node.type())) ink++; else gaps++;
            }
            assertTrue("Border is visible for " + node.id(), ink > 0);
            if (node.type() == GraphyNodeType.DERIVED) assertTrue("Intermediate border has gaps", gaps > 0);
            else assertEquals("Dash effect must not leak to input borders", 0, gaps);
        }
        bitmap.recycle();
    }
    @Test public void branchingLayoutIncludesEveryNodeWithoutOverlaps() {
        var graph = GraphyOutput.create("test", "", "result", List.of(
                new GraphyNode("input", GraphyNodeType.INPUT, "input", "5", "input"),
                new GraphyNode("a", GraphyNodeType.OPERATION, "×", "×", "operation"),
                new GraphyNode("b", GraphyNodeType.OPERATION, "+", "+", "operation"),
                new GraphyNode("result", GraphyNodeType.RESULT, "result", "10", "result")), List.of(
                new GraphyConnection("input", "a", null), new GraphyConnection("input", "b", null),
                new GraphyConnection("a", "result", null), new GraphyConnection("b", "result", null)), null, null);
        var layout = new GraphLayoutEngine().layout(graph, new GraphyViewTheme(RuntimeEnvironment.getApplication()), 240);
        assertTrue(layout.nodeBounds().keySet().containsAll(List.of("input", "a", "b", "result")));
        assertNoOverlaps(layout);
        assertTrue(layout.nodeBounds().get("result").y() > layout.nodeBounds().get("b").y());
    }
    @Test public void talkBackExposesRoleValueAndContributorsAndSelectsDetails() {
        var controller = Robolectric.buildActivity(Activity.class);
        Activity activity = controller.get();
        activity.setTheme(R.style.AppTheme);
        controller.setup();
        FlowchartGraphView view = new FlowchartGraphView(activity);
        activity.setContentView(view);
        GraphyNode[] selected = new GraphyNode[1];
        view.setOnNodeClickListener(node -> selected[0] = node);
        view.setMaxWidth(320);
        view.setGraph(linear("5 km", "3.106855 mi"), new GraphyViewTheme(activity));
        view.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, 320, view.getMeasuredHeight());
        var provider = view.getAccessibilityNodeProvider();
        assertNotNull(provider);
        var source = provider.createAccessibilityNodeInfo(0);
        assertTrue(source.getContentDescription().toString().contains("Input"));
        assertTrue(source.getContentDescription().toString().contains("5 km"));
        var layout = new GraphLayoutEngine().layout(linear("5 km", "3.106855 mi"), new GraphyViewTheme(activity), 320);
        var visualOrder = layout.presentation().getNodes();
        int resultId = java.util.stream.IntStream.range(0, visualOrder.size())
                .filter(i -> visualOrder.get(i).id().equals("target")).findFirst().orElseThrow();
        var result = provider.createAccessibilityNodeInfo(resultId);
        assertTrue(result.getContentDescription().toString().contains("Uses"));
        assertTrue(provider.performAction(resultId, androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_CLICK, null));
        assertNotNull(selected[0]);
        assertEquals("target", selected[0].id());
        controller.pause().stop().destroy();
    }
    @Test public void themeAndFontChangesRefreshAnExistingViewWithTheSameOutput() {
        var context = RuntimeEnvironment.getApplication();
        var theme = new GraphyViewTheme(context);
        var output = linear("5 km", "3.106855 mi");
        var view = new FlowchartGraphView(context);
        view.setMaxWidth(320);
        ThemeManager.select(context, CalculatorThemes.DEFAULT_ID);
        view.setGraph(output, theme);
        ThemeManager.select(context, CalculatorThemes.GRAPHITE_BLACK.id());
        view.setGraph(output, theme);
        view.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int normalHeight = view.getMeasuredHeight();
        view.layout(0, 0, 320, normalHeight);
        Bitmap bitmap = Bitmap.createBitmap(320, normalHeight, Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        var resultBounds = new GraphLayoutEngine().layout(output, theme, 320).nodeBounds().get("target");
        int inkPixels = 0;
        int inset = (int) theme.getNodePadding();
        for (int y = (int) resultBounds.y() + inset; y < resultBounds.y() + resultBounds.height() - inset; y++) {
            for (int x = (int) resultBounds.x() + inset; x < resultBounds.x() + resultBounds.width() - inset; x++) {
                if (bitmap.getPixel(x, y) == theme.nodeContent(GraphyNodeType.RESULT)) inkPixels++;
            }
        }
        assertTrue("Cached text paint must refresh for a changed theme", inkPixels > 0);
        bitmap.recycle();
        RuntimeEnvironment.setFontScale(2f);
        view.setGraph(output, theme);
        view.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        assertTrue(view.getMeasuredHeight() > normalHeight);
    }

    @Test public void renderRepresentativeMatrixInEveryTheme() throws Exception {
        var context = RuntimeEnvironment.getApplication();
        var conversions = Conversions.getInstance();
        Map<String, GraphyOutput> examples = new LinkedHashMap<>();
        examples.put("very-complex", expression("((12+8)*(9-4)+(18/3-2))/(7-3)", "26"));
        examples.put("simple-chain", expression("20-5-3", "12"));
        examples.put("percentage", expression("600*6%", "36"));
        examples.put("multi-step", expression("(500+600*6%)/2+1250", "1,518"));
        examples.put("reported-expression", expression("633-9-9+6*9/2", "642"));
        examples.put("length", linear("5 km", "3.106855 mi"));
        var temperatures = conversions.getById(Conversion.TEMPERATURE).getUnits();
        Unit c = temperatures.stream().filter(u -> u.getId() == Unit.CELSIUS).findFirst().orElseThrow();
        Unit f = temperatures.stream().filter(u -> u.getId() == Unit.FAHRENHEIT).findFirst().orElseThrow();
        examples.put("temperature", ConversionGraphAdapter.unit(context, conversions, Conversion.TEMPERATURE,
                c, f, 100, 212, "100 °C", "212 °F", "°C", "°F"));
        examples.put("currency", ConversionGraphAdapter.currency(context, 100, 8350, 83.5,
                "100 USD", "8,350 INR", "USD", "INR", 0));
        var data = conversions.getById(Conversion.STORAGE).getUnits();
        Unit from = data.get(0), to = data.get(1);
        examples.put("data", ConversionGraphAdapter.unit(context, conversions, Conversion.STORAGE,
                from, to, 1024, conversions.convert(1024, from, to), "1024 " + from.getSymbol(),
                String.valueOf(conversions.convert(1024, from, to)) + " " + to.getSymbol(), from.getSymbol(), to.getSymbol()));
        File directory = new File("build/reports/graphy");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        for (var theme : CalculatorThemes.all()) {
            ThemeManager.select(context, theme.id());
            for (var example : examples.entrySet()) {
                FlowchartGraphView view = new FlowchartGraphView(context);
                view.setMaxWidth(360);
                view.setGraph(example.getValue(), new GraphyViewTheme(context));
                view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                assertTrue(view.getMeasuredHeight() > 0);
                view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
                Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
                view.draw(new Canvas(bitmap));
                try (var stream = new FileOutputStream(new File(directory, theme.id() + "-" + example.getKey() + ".png"))) {
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
                }
                bitmap.recycle();
            }
        }
    }
    private void assertNoOverlaps(GraphLayoutEngine.LayoutResult layout) {
        var bounds = layout.nodeBounds().values().toArray(new GraphLayoutEngine.LayoutBox[0]);
        for (int i = 0; i < bounds.length; i++) for (int j = i + 1; j < bounds.length; j++) {
            var a = bounds[i]; var b = bounds[j];
            assertFalse(RectF.intersects(new RectF(a.x(), a.y(), a.x() + a.width(), a.y() + a.height()),
                    new RectF(b.x(), b.y(), b.x() + b.width(), b.y() + b.height())));
        }
    }
}
