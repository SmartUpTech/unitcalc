package net.smartlogic.unitconverter.graphy.renderer;

import android.app.Application;
import android.graphics.*;
import net.smartlogic.unitconverter.graphy.integration.*;
import net.smartlogic.unitconverter.graphy.model.*;
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme;
import net.smartlogic.unitconverter.theme.*;
import net.smartlogic.unitconverter.utils.ExpressionEvaluator;
import net.smartlogic.unitconverter.helper.Preferences;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class, qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class GraphyExportTest {
    @Before public void unlockThemes() {
        Preferences.getInstance(RuntimeEnvironment.getApplication()).getPreferences().edit()
                .putInt(Preferences.PREFS_THEME_USAGE_DAYS, Integer.MAX_VALUE).apply();
    }
    @After public void restore() {
        RuntimeEnvironment.setFontScale(1f);
        ThemeManager.select(RuntimeEnvironment.getApplication(), CalculatorThemes.DEFAULT_ID);
    }
    private GraphyOutput expression(String text, String result) {
        return new GraphyBridge().build(CalculationSnapshot.create("basic", text, text, result, ExpressionEvaluator.evaluate(text)));
    }
    private void assertSeparated(GraphyDocumentRenderer.Document document) {
        var regions = List.of(document.headerBounds, document.graphBounds, document.legendBounds);
        for (int i = 0; i < regions.size(); i++) {
            RectF a = regions.get(i);
            assertTrue(a.left >= 0 && a.top >= 0 && a.right <= document.width && a.bottom <= document.height);
            for (int j = i + 1; j < regions.size(); j++) assertFalse("Export regions must not overlap", RectF.intersects(a, regions.get(j)));
        }
        var nodes = document.graph.nodeBounds().values().toArray(new GraphLayoutEngine.LayoutBox[0]);
        for (int i = 0; i < nodes.length; i++) for (int j = i + 1; j < nodes.length; j++) {
            var a = nodes[i]; var b = nodes[j];
            assertFalse(RectF.intersects(new RectF(a.x(), a.y(), a.x()+a.width(), a.y()+a.height()),
                    new RectF(b.x(), b.y(), b.x()+b.width(), b.y()+b.height())));
        }
        assertEquals(0f, document.graph.gutterWidth(), 0f);
        assertTrue(document.legendRowCount <= 2);
        for (int i = 0; i < document.legendItems.size(); i++) {
            RectF item = document.legendItems.get(i).bounds();
            assertTrue(document.legendBounds.contains(item));
            for (int j = i + 1; j < document.legendItems.size(); j++) assertFalse(RectF.intersects(item, document.legendItems.get(j).bounds()));
        }
        float watermarkArea = 0;
        for (var mark : document.watermarks) {
            assertTrue(mark.degrees() < 0);
            assertTrue(mark.bounds().left >= 0 && mark.bounds().top >= 0 && mark.bounds().right <= document.width && mark.bounds().bottom <= document.height);
            watermarkArea += mark.bounds().width() * mark.bounds().height();
        }
        assertTrue("Diagonal watermark coverage must be at least 40%", watermarkArea >= document.width * document.height * .4f);
        assertEquals(document.headerBounds.right, document.legendBounds.right, 0f);
    }
    @Test public void completeGraphAndFooterStaySeparateForSimpleComplexLongAndCurrencyGraphs() throws Exception {
        var context = RuntimeEnvironment.getApplication();
        Map<String, GraphyOutput> examples = new LinkedHashMap<>();
        examples.put("simple", expression("20-5-3", "12"));
        examples.put("complex", expression("((12+8)*(9-4)+(18/3-2))/(7-3)", "26"));
        examples.put("long-heading", expression("1+".repeat(60) + "1", "61"));
        examples.put("currency", ConversionGraphAdapter.currency(context, 100, 8350, 83.5,
                "100 USD", "8,350 INR", "USD", "INR", 0));
        File directory = new File("build/reports/graphy-export"); assertTrue(directory.isDirectory() || directory.mkdirs());
        for (var example : examples.entrySet()) {
            var output = example.getValue();
            var doc = GraphyDocumentRenderer.prepare(context, output, 360);
            assertSeparated(doc);
            assertEquals(output.getNodes().size(), doc.graph.presentation().getNodes().size());
            assertEquals(output.getConnections(), doc.graph.presentation().getConnections());
            assertEquals(output.getResult(), doc.graph.presentation().getResult());
            // JVM Android stubs have no PDF native backend; PDF bytes/rendering are checked on-device.
            var file = new File(directory, example.getKey() + ".png");
            try (var stream = new FileOutputStream(file)) { doc.write(stream, GraphyDocumentRenderer.Format.PNG); }
            assertTrue(file.length() > 100);
            if (example.getKey().equals("long-heading")) assertTrue(doc.headerBounds.height() > 100);
        }
    }
    @Test public void exportIsWhiteAndThemeIndependentWithoutChangingAppThemeOrScaling() throws Exception {
        var context = RuntimeEnvironment.getApplication();
        var output = expression("(12+8)*3", "60");
        byte[] reference = null;
        for (var palette : CalculatorThemes.all()) {
            ThemeManager.select(context, palette.id());
            RuntimeEnvironment.setFontScale(2f);
            var selected = ThemeManager.get();
            var doc = GraphyDocumentRenderer.prepare(context, output, 360);
            assertSeparated(doc);
            assertEquals(16f, doc.graph.content().values().stream().filter(c -> c.value().getText().toString().equals("12"))
                    .findFirst().orElseThrow().value().getPaint().getTextSize(), .01f);
            var stream = new ByteArrayOutputStream(); doc.write(stream, GraphyDocumentRenderer.Format.PNG);
            byte[] data = stream.toByteArray();
            if (reference == null) reference = data; else assertArrayEquals(reference, data);
            assertSame(selected, ThemeManager.get());
            assertEquals(2f, context.getResources().getConfiguration().fontScale, 0f);
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            assertNotNull(bitmap);
            assertEquals(Color.WHITE, bitmap.getPixel(0, 0));
            assertEquals(Color.WHITE, bitmap.getPixel(bitmap.getWidth()-1, bitmap.getHeight()-1));
            bitmap.recycle();
        }
    }
    @Test public void narrowDocumentUsesAtMostTwoFooterRowsAndKeepsAllItems() throws Exception {
        var doc = GraphyDocumentRenderer.prepare(RuntimeEnvironment.getApplication(), expression("(12+8)*3", "60"), 200);
        assertEquals(4, doc.legendItems.size());
        assertEquals(2, doc.legendRowCount);
        assertSeparated(doc);
    }

    @Test public void watermarkIsFaintAndFooterForegroundIsSeparate() throws Exception {
        var doc = GraphyDocumentRenderer.prepare(RuntimeEnvironment.getApplication(), expression("8+2", "10"), 360);
        Bitmap bitmap = Bitmap.createBitmap(doc.width, doc.height, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.WHITE);
        doc.drawWatermarks(new Canvas(bitmap));
        int ink = 0;
        for (int y = 0; y < doc.height; y++) {
            for (int x = 0; x < doc.width; x++) {
                int color = bitmap.getPixel(x,y);
                if (color != Color.WHITE) { ink++; assertTrue("Watermark must have low opacity", Color.red(color) >= 220); }
            }
        }
        assertTrue(ink > 0);
        assertSeparated(doc);
        bitmap.recycle();
    }
}
