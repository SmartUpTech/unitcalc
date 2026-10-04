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
    @After public void reset() {
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
        assertEquals(4, layout.nodeBounds().size());
        assertTrue(layout.contentHeight() > 0);
    }
    @Test public void graphFitsNarrowWidthWithoutShrinkingOrClippingText() {
        RuntimeEnvironment.setFontScale(2f);
        GraphyOutput graph = linear("₹1,25,67,890.45 kilometres with a long translated unit name",
                "0.000000123 international nautical miles");
        GraphyViewTheme theme = new GraphyViewTheme(RuntimeEnvironment.getApplication());
        var layout = new GraphLayoutEngine().layout(graph, theme, 240);
        assertEquals(graph.getNodes().size(), layout.nodeBounds().size());
        for (var entry : layout.nodeBounds().entrySet()) {
            var box = entry.getValue();
            assertTrue(box.x() >= 0);
            assertTrue(box.x() + box.width() <= 240);
            assertTrue(box.height() >= theme.getNodeMinHeight());
            var text = layout.content().get(entry.getKey());
            assertTrue(text.value().getPaint().getTextSize() >= theme.getValueTextSize());
        }
        assertTrue(layout.content().get("source").value().getLineCount() > 1);
        assertNoOverlaps(layout);
    }
    @Test public void multiStepInputsAppearNearTheirConsumers() {
        var graph = expression("(500+600*6%)/2+1250", "1,518");
        var layout = new GraphLayoutEngine().layout(graph, new GraphyViewTheme(RuntimeEnvironment.getApplication()), 360);
        String early = graph.getNodes().stream().filter(n -> n.displayValue().equals("600")).findFirst().orElseThrow().id();
        String late = graph.getNodes().stream().filter(n -> n.displayValue().equals("1250")).findFirst().orElseThrow().id();
        assertTrue(layout.nodeBounds().get(late).y() > layout.nodeBounds().get(early).y());
        assertNoOverlaps(layout);
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
        assertEquals(4, layout.nodeBounds().size());
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
        var result = provider.createAccessibilityNodeInfo(3);
        assertTrue(result.getContentDescription().toString().contains("Uses"));
        assertTrue(provider.performAction(3, androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_CLICK, null));
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
        examples.put("percentage", expression("600*6%", "36"));
        examples.put("multi-step", expression("(500+600*6%)/2+1250", "1,518"));
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
                view.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                assertTrue(view.getMeasuredHeight() > 0);
                view.layout(0, 0, 360, view.getMeasuredHeight());
                Bitmap bitmap = Bitmap.createBitmap(360, view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
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
