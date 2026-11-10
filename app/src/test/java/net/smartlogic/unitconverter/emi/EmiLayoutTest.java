package net.smartlogic.unitconverter.calculator.finance.emi;

import android.app.Application;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.theme.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.util.ReflectionHelpers;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class EmiLayoutTest {
    @Test public void allThemesAndFontsKeepSummaryAndKeypadInSupportedViewport() throws Exception {
        for (var theme : CalculatorThemes.all()) render(theme, Appearance.Font.OSWALD, 1f, 360, 600, true, true);
        for (var font : Appearance.Font.values()) render(CalculatorThemes.GRAPHITE_BLACK, font, 1.3f, 412, 660, true, true);
    }
    @Test public void summaryAndLandscapeReflowAndImpossibleViewport() throws Exception {
        render(CalculatorThemes.PORCELAIN_WHITE, Appearance.Font.OSWALD, 1f, 360, 600, false, true);
        render(CalculatorThemes.GRAPHITE_BLACK, Appearance.Font.OSWALD, 1f, 800, 350, true, true);
        render(CalculatorThemes.GRAPHITE_BLACK, Appearance.Font.OSWALD, 1f, 800, 350, false, true);
        render(CalculatorThemes.PORCELAIN_WHITE, Appearance.Font.OSWALD, 1f, 360, 480, true, true);
        render(CalculatorThemes.PORCELAIN_WHITE, Appearance.Font.OSWALD, 1f, 360, 480, true, true, true);
        render(CalculatorThemes.PORCELAIN_WHITE, Appearance.Font.ROBOTO_MONO, 2f, 320, 300, true, false);
    }
    private void render(CalculatorTheme theme, Appearance.Font font, float scale, int width, int height, boolean editing, boolean expected) throws Exception {
        render(theme, font, scale, width, height, editing, expected, false);
    }
    private void render(CalculatorTheme theme, Appearance.Font font, float scale, int width, int height, boolean editing, boolean expected, boolean empty) throws Exception {
        Application app = RuntimeEnvironment.getApplication(); Configuration original = new Configuration(app.getResources().getConfiguration());
        Configuration configuration = new Configuration(original); configuration.fontScale = scale; app.getResources().updateConfiguration(configuration, app.getResources().getDisplayMetrics());
        ThemeManager.init(app); ReflectionHelpers.setStaticField(ThemeManager.class, "current", theme);
        Preferences.getInstance(app).getPreferences().edit().putString(Appearance.FONT, font.name()).apply();
        var controller = Robolectric.buildActivity(AppCompatActivity.class); controller.get().setTheme(R.style.AppTheme); controller.setup().visible();
        try {
            View viewport = controller.get().getLayoutInflater().inflate(R.layout.pane_emi_calculate, null);
            viewport.setBackgroundColor(theme.background()); controller.get().setContentView(viewport);
            EmiLayout content = viewport.findViewById(R.id.emi_layout);
            ((TextView) viewport.findViewById(R.id.emi_principal)).setText("1000000"); ((TextView) viewport.findViewById(R.id.emi_rate)).setText("8.5");
            ((TextView) viewport.findViewById(R.id.emi_tenure)).setText("20"); ((TextView) viewport.findViewById(R.id.emi_date)).setText("02 Dec 2026");
            ((TextView) viewport.findViewById(R.id.emi_value)).setText("₹8,678.23");
            ((TextView) viewport.findViewById(R.id.emi_total_interest)).setText("Total interest: ₹10,82,776.63");
            ((TextView) viewport.findViewById(R.id.emi_total_payment)).setText("Total payment: ₹20,82,776.63");
            ((TextView) viewport.findViewById(R.id.emi_message)).setText(R.string.emi_fixed);
            viewport.findViewById(R.id.emi_message).setVisibility(editing ? View.GONE : View.VISIBLE);
            if (empty) {
                ((TextView) viewport.findViewById(R.id.emi_principal)).setText("");
                ((TextView) viewport.findViewById(R.id.emi_rate)).setText("");
                ((TextView) viewport.findViewById(R.id.emi_value)).setText(R.string.emi_no_result);
                ((TextView) viewport.findViewById(R.id.emi_total_interest)).setText("Total interest: —");
                ((TextView) viewport.findViewById(R.id.emi_total_payment)).setText("Total payment: —");
                ((TextView) viewport.findViewById(R.id.emi_message)).setText(R.string.emi_empty);
                viewport.findViewById(R.id.emi_message).setVisibility(View.VISIBLE);
            }
            viewport.findViewById(R.id.emi_keys).setVisibility(editing ? View.VISIBLE : View.GONE); viewport.findViewById(R.id.emi_actions).setVisibility(editing ? View.GONE : View.VISIBLE);
            content.setResultState(!empty, editing);
            ThemeApplier.apply(viewport);
            for (int i = 0; i < 3; i++) { viewport.forceLayout(); viewport.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)); viewport.layout(0, 0, width, height); viewport.getViewTreeObserver().dispatchOnPreDraw(); }
            String name = theme.id() + "-" + font.name() + "-" + scale + "-" + width + "x" + height + "-" + editing + (empty ? "-empty" : "");
            assertEquals(name, expected, content.fitsViewport());
            if (expected && editing) {
                View summary = viewport.findViewById(R.id.emi_result_section), keys = viewport.findViewById(R.id.emi_keys);
                if (width < height && summary.getVisibility() == View.VISIBLE) {
                    int margin = app.getResources().getDimensionPixelSize(R.dimen.emi_summary_dense_margin);
                    assertTrue(name + " result top space", summary.getTop() - viewport.findViewById(R.id.emi_fields).getBottom() >= margin);
                    assertTrue(name + " result bottom space", keys.getTop() - summary.getBottom() >= margin);
                }
                assertTrue(name + " readable result caption", ((TextView) viewport.findViewById(R.id.emi_value_label)).getTextSize()
                        >= app.getResources().getDimension(R.dimen.graphy_value_text_size));
                assertTrue("numeric key width", viewport.findViewById(R.id.seven).getMeasuredWidth() >= app.getResources().getDimensionPixelSize(R.dimen.converter_touch_target));
                assertTrue("numeric key height", viewport.findViewById(R.id.seven).getMeasuredHeight() >= app.getResources().getDimensionPixelSize(R.dimen.converter_touch_target));
                assertTrue(keys.getMeasuredHeight() >= app.getResources().getDimensionPixelSize(R.dimen.converter_touch_target) * 4);
            }
            View note = viewport.findViewById(R.id.emi_message);
            if (expected && note.getVisibility() == View.VISIBLE && note.getRootView().findViewById(R.id.emi_summary).getVisibility() == View.VISIBLE) {
                assertEquals(R.id.emi_result_section, ((View) note.getParent()).getId());
                assertTrue("Notes stay below the result card", note.getTop() >= viewport.findViewById(R.id.emi_summary).getBottom());
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); viewport.draw(new Canvas(bitmap));
            File file = new File("build/reports/emi/" + name + ".png"); file.getParentFile().mkdirs();
            try (var out = new FileOutputStream(file)) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); } bitmap.recycle();
        } finally { controller.pause().stop().destroy(); app.getResources().updateConfiguration(original, app.getResources().getDisplayMetrics()); }
    }
}
