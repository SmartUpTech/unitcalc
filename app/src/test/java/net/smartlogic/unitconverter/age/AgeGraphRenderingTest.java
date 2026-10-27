package net.smartlogic.unitconverter.age;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import net.smartlogic.unitconverter.graphy.model.GraphyNode;
import net.smartlogic.unitconverter.graphy.renderer.FlowchartGraphView;
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme;
import net.smartlogic.unitconverter.theme.CalculatorThemes;
import net.smartlogic.unitconverter.theme.ThemeManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AgeGraphRenderingTest {
    @Test public void sharedEngineRendersEveryAgeNodeInEveryTheme() throws Exception {
        var context = RuntimeEnvironment.getApplication();
        ThemeManager.setProAccessProvider(() -> true);
        try {
            for (var theme : CalculatorThemes.all()) {
                ThemeManager.select(context, theme.id());
                var graph = AgeGraphBuilder.build(context, AgeCalculator.calculate(LocalDate.of(2010, 8, 16), LocalDate.of(2020, 1, 21)));
                var view = new FlowchartGraphView(context);
                view.setGraph(graph, new GraphyViewTheme(context));
                view.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                view.layout(0, 0, 360, view.getMeasuredHeight());
                assertTrue(view.getMeasuredHeight() > 0);
                var provider = view.getAccessibilityNodeProvider();
                for (int i = 0; i < graph.getNodes().size(); i++) {
                    assertNotNull(provider.createAccessibilityNodeInfo(i));
                }
                Bitmap image = Bitmap.createBitmap(360, view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
                image.eraseColor(theme.background());
                view.draw(new Canvas(image));
                File file = new File("build/reports/age/graph-" + theme.id() + ".png");
                file.getParentFile().mkdirs();
                try (var stream = new FileOutputStream(file)) { image.compress(Bitmap.CompressFormat.PNG, 100, stream); }
                image.recycle();
                assertEquals("9 years, 5 months, 5 days", graph.getResult());
                assertTrue(graph.getNodes().stream().map(GraphyNode::id).anyMatch("age"::equals));
            }
        } finally { ThemeManager.setProAccessProvider(() -> false); }
    }
}
