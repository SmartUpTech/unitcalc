package net.smartlogic.unitconverter.graphy.theme;

import android.app.Application;
import androidx.core.graphics.ColorUtils;
import net.smartlogic.unitconverter.graphy.model.GraphyNodeType;
import net.smartlogic.unitconverter.theme.CalculatorThemes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
public class GraphySemanticStyleTest {
    @Test public void everySemanticForegroundHasContrastInEveryAppTheme() {
        for (var theme : CalculatorThemes.all()) {
            for (var type : GraphyNodeType.values()) {
                assertTrue(theme.id() + ": " + type,
                        ColorUtils.calculateContrast(GraphySemanticStyle.content(theme, type),
                                GraphySemanticStyle.fill(theme, type)) >= 4.5);
            }
            assertTrue(ColorUtils.calculateContrast(GraphySemanticStyle.connector(theme), theme.background()) >= 3);
        }
    }
    @Test public void resultUsesAppAccentAndOtherSemanticNodesHaveQuietSurfaces() {
        for (var theme : CalculatorThemes.all()) {
            assertEquals(theme.equal(), GraphySemanticStyle.fill(theme, GraphyNodeType.RESULT));
            assertNotEquals(GraphySemanticStyle.fill(theme, GraphyNodeType.INPUT),
                    GraphySemanticStyle.fill(theme, GraphyNodeType.RESULT));
        }
    }
}
