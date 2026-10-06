package net.smartlogic.unitconverter.games;

import android.app.Application;
import net.smartlogic.unitconverter.theme.CalculatorTheme;
import net.smartlogic.unitconverter.theme.CalculatorThemes;
import net.smartlogic.unitconverter.theme.ThemeManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.Locale;
import java.util.Map;
import java.lang.reflect.Field;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class GamesHostConfigurationTest {
    @Test public void everyExistingPaletteUsesItsSemanticTokensAndGenericCompletions() throws Exception {
        Field current = ThemeManager.class.getDeclaredField("current");
        current.setAccessible(true);
        Object original = current.get(null);
        try {
            for (CalculatorTheme palette : CalculatorThemes.all()) {
                current.set(null, palette);
                var config = GamesHostConfiguration.create(RuntimeEnvironment.getApplication(), "2026-10-06",
                        "Asia/Kolkata", Map.of("future_game", "2026-10-06"));
                var theme = config.getJSONObject("theme");
                assertEquals(hex(palette.background()), theme.getString("background"));
                assertEquals(hex(palette.background()), theme.getString("surface"));
                assertEquals(hex(palette.mainText()), theme.getString("primaryText"));
                assertEquals(hex(palette.functions()), theme.getString("secondaryText"));
                assertEquals(hex(palette.equal()), theme.getString("accent"));
                assertEquals(hex(palette.functions()), theme.getString("divider"));
                assertEquals(palette.isLightBackground() ? "light" : "dark", theme.getString("mode"));
                assertEquals("2026-10-06", config.getJSONObject("completions").getString("future_game"));
                assertEquals(config.getString("language"), config.getString("locale"));
                assertEquals(1, config.getInt("bridgeVersion"));
            }
        } finally { current.set(null, original); }
    }

    private static String hex(int color) { return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF); }
}
