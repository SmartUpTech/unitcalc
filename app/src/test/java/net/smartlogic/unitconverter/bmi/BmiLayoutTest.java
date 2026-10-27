package net.smartlogic.unitconverter.bmi;

import android.app.Application;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import java.io.File;
import java.io.FileOutputStream;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.theme.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class BmiLayoutTest {
    private void render(int width, int height, float scale, CalculatorTheme theme, boolean imperial, boolean verify) throws Exception {
        Application app = RuntimeEnvironment.getApplication();
        Configuration original = new Configuration(app.getResources().getConfiguration());
        Configuration config = new Configuration(original); config.fontScale = scale;
        app.getResources().updateConfiguration(config, app.getResources().getDisplayMetrics());
        ThemeManager.init(app); ReflectionHelpers.setStaticField(ThemeManager.class, "current", theme);
        Preferences.getInstance(app).setBmiState("");
        var controller = Robolectric.buildActivity(AppCompatActivity.class);
        controller.get().setTheme(R.style.AppTheme); controller.setup().visible();
        try {
            BmiCalculatorFragment fragment = new BmiCalculatorFragment();
            controller.get().getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment).commitNow();
            BmiLayout pane = (BmiLayout) fragment.requireView();
            ((EditText) pane.findViewById(R.id.bmi_height_input)).setText("186");
            ((EditText) pane.findViewById(R.id.bmi_weight_input)).setText("73");
            if (imperial) {
                android.os.Bundle choice = new android.os.Bundle(); choice.putString("mode", BmiDialogFragment.HEIGHT); choice.putInt("selected", 1);
                fragment.getChildFragmentManager().setFragmentResult(BmiDialogFragment.RESULT, choice);
            }
            pane.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            pane.layout(0, 0, width, height);
            String name = theme.id() + "-" + width + "x" + height + "-font" + scale + (imperial ? "-imperial" : "");
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); pane.draw(new Canvas(bitmap));
            File file = new File("build/reports/bmi/" + name + ".png"); file.getParentFile().mkdirs();
            try (FileOutputStream out = new FileOutputStream(file)) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); }
            bitmap.recycle();
            if (verify) {
                assertTrue(name + " fits", pane.contentFits());
                bounds(pane, name);
                int touch = pane.getResources().getDimensionPixelSize(R.dimen.converter_touch_target);
                for (int id : new int[]{R.id.zero, R.id.nine, R.id.ac, R.id.backspace, R.id.copy, R.id.equal,
                        R.id.bmi_height_input, R.id.bmi_weight_input, R.id.bmi_height_unit, R.id.bmi_weight_unit, R.id.bmi_ranges}) {
                    View key = pane.findViewById(id);
                    assertTrue(name + " target " + id, key.getWidth() >= touch && key.getHeight() >= touch);
                }
                if (imperial) assertTrue(name + " inches target", pane.findViewById(R.id.bmi_inches_input).getWidth() >= touch);
            } else assertFalse("Extreme configuration is explicitly unresolved", pane.contentFits());
        } finally { controller.pause().stop().destroy(); app.getResources().updateConfiguration(original, app.getResources().getDisplayMetrics()); }
    }
    private void bounds(View view, String name) {
        if (view.getVisibility() != View.VISIBLE) return;
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i); if (child.getVisibility() != View.VISIBLE) continue;
            assertTrue(name + " child bounds " + child.getId(), child.getLeft() >= 0 && child.getTop() >= 0 && child.getRight() <= view.getWidth() && child.getBottom() <= view.getHeight());
            bounds(child, name);
        }
        if (view instanceof TextView text && text.getLayout() != null && text.length() > 0) {
            assertTrue(name + " text height " + view.getId(), text.getLayout().getHeight() <= text.getHeight() - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom());
        }
    }
    @Test public void nativeRenderedThemesAndConstrainedViewports() throws Exception {
        for (CalculatorTheme theme : CalculatorThemes.all()) for (float scale : new float[]{1f, 1.3f}) render(360, 552, scale, theme, false, true);
        render(320, 520, 1f, CalculatorThemes.PORCELAIN_WHITE, false, true);
        render(360, 552, 1.3f, CalculatorThemes.GRAPHITE_BLACK, true, true);
        render(640, 320, 1f, CalculatorThemes.PORCELAIN_WHITE, false, true);
        render(720, 360, 1.3f, CalculatorThemes.GRAPHITE_BLACK, false, true);
    }
    @Test public void statusColorsRemainDistinctAndReadableOnEveryTheme() {
        for (CalculatorTheme theme : CalculatorThemes.all()) {
            java.util.Set<Integer> colors = new java.util.HashSet<>();
            for (CalculatorTheme.StatusTone tone : CalculatorTheme.StatusTone.values()) {
                int color = theme.statusContentColor(tone); colors.add(color);
                assertTrue(theme.id(), ColorUtils.calculateContrast(color, theme.background()) >= 4.5);
            }
            assertEquals(theme.id(), 6, colors.size());
        }
    }
    @Test public void extremeAccessibilityLimitsAreNotClaimedAsFit() throws Exception {
        render(320, 320, 2f, CalculatorThemes.PORCELAIN_WHITE, false, false);
    }
}
