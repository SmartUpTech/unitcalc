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
                assertTrue(name + " fits; heights controls/results/actions/keypad="
                        + pane.getChildAt(0).getMeasuredHeight() + "/" + pane.getChildAt(1).getMeasuredHeight()
                        + "/" + pane.getChildAt(2).getMeasuredHeight() + "/" + pane.getChildAt(3).getMeasuredHeight(), pane.contentFits());
                bounds(pane, name);
                int touch = pane.getResources().getDimensionPixelSize(R.dimen.converter_touch_target);
                for (int id : new int[]{R.id.zero, R.id.nine, R.id.ac, R.id.backspace,
                        R.id.bmi_height_input, R.id.bmi_weight_input, R.id.bmi_height_unit, R.id.bmi_weight_unit, R.id.bmi_ranges}) {
                    View key = pane.findViewById(id);
                    assertTrue(name + " target " + id, key.getWidth() >= touch && key.getHeight() >= touch);
                }
                if (imperial) assertTrue(name + " inches target", pane.findViewById(R.id.bmi_inches_input).getWidth() >= touch);
                ((EditText) pane.findViewById(R.id.bmi_weight_input)).setText("0");
                pane.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                pane.layout(0, 0, width, height);
                assertTrue(name + " invalid state fits", pane.contentFits());
                bounds(pane, name + " invalid");
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
        render(360, 532, 1f, CalculatorThemes.PORCELAIN_WHITE, false, true);
        render(360, 532, 1.3f, CalculatorThemes.PORCELAIN_WHITE, false, true);
        render(360, 528, 1.3f, CalculatorThemes.PORCELAIN_WHITE, false, true);
        render(360, 528, 1.3f, CalculatorThemes.GRAPHITE_BLACK, true, true);
        // 1080×2160 device with enlarged 520dpi display sizing, after persistent chrome.
        render(332, 472, 1f, CalculatorThemes.PORCELAIN_WHITE, false, true);
        render(332, 472, 1.3f, CalculatorThemes.GRAPHITE_BLACK, true, true);
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
    @Test public void gaugeActuallyDrawsTheResultCategoryColor() {
        Application app = RuntimeEnvironment.getApplication();
        ThemeManager.init(app);
        for (CalculatorTheme theme : CalculatorThemes.all()) {
            ReflectionHelpers.setStaticField(ThemeManager.class, "current", theme);
            String[] weights = {"60", "73", "90"};
            CalculatorTheme.StatusTone[] expectedTones = {CalculatorTheme.StatusTone.LOW,
                    CalculatorTheme.StatusTone.POSITIVE, CalculatorTheme.StatusTone.CAUTION};
            for (int index = 0; index < weights.length; index++) {
                String weight = weights[index];
                BmiCalculator.Result result = BmiCalculator.calculate(new java.math.BigDecimal("186"), new java.math.BigDecimal(weight));
                BmiGaugeView gauge = new BmiGaugeView(app, null);
                gauge.setResult(result); gauge.layout(0, 0, 300, 180);
                Bitmap bitmap = Bitmap.createBitmap(300, 180, Bitmap.Config.ARGB_8888);
                gauge.draw(new Canvas(bitmap));
                int color = theme.statusContentColor(expectedTones[index]), pixels = 0;
                for (int y = 0; y < 180; y++) for (int x = 0; x < 300; x++) if (bitmap.getPixel(x, y) == color) pixels++;
                bitmap.recycle();
                assertTrue(theme.id() + " " + result.category() + " draws its status color", pixels > 100);
            }
        }
    }
    @Test public void rangesPopupRendersInLightAndDarkWithLargeText() throws Exception {
        Application app = RuntimeEnvironment.getApplication();
        Configuration original = new Configuration(app.getResources().getConfiguration());
        Configuration config = new Configuration(original); config.fontScale = 1.3f;
        app.getResources().updateConfiguration(config, app.getResources().getDisplayMetrics());
        try {
            for (CalculatorTheme theme : new CalculatorTheme[]{CalculatorThemes.PORCELAIN_WHITE, CalculatorThemes.GRAPHITE_BLACK}) {
                ThemeManager.init(app); ReflectionHelpers.setStaticField(ThemeManager.class, "current", theme);
                Preferences.getInstance(app).setBmiState("");
                var controller = Robolectric.buildActivity(AppCompatActivity.class);
                controller.get().setTheme(R.style.AppTheme); controller.setup().visible();
                try {
                    BmiCalculatorFragment fragment = new BmiCalculatorFragment();
                    controller.get().getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment).commitNow();
                    ((EditText) fragment.requireView().findViewById(R.id.bmi_height_input)).setText("186");
                    ((EditText) fragment.requireView().findViewById(R.id.bmi_weight_input)).setText("73");
                    fragment.requireView().findViewById(R.id.bmi_ranges).performClick();
                    BmiDialogFragment popup = (BmiDialogFragment) fragment.getChildFragmentManager().findFragmentByTag(BmiDialogFragment.TAG);
                    ViewGroup decor = (ViewGroup) popup.requireDialog().getWindow().getDecorView();
                    decor.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.AT_MOST));
                    decor.layout(0, 0, decor.getMeasuredWidth(), decor.getMeasuredHeight());
                    assertRangeRowsVisible(decor, decor);
                    Bitmap bitmap = Bitmap.createBitmap(decor.getWidth(), decor.getHeight(), Bitmap.Config.ARGB_8888);
                    decor.draw(new Canvas(bitmap));
                    File file = new File("build/reports/bmi/ranges-" + theme.id() + "-font1.3.png"); file.getParentFile().mkdirs();
                    try (FileOutputStream out = new FileOutputStream(file)) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); }
                    bitmap.recycle(); popup.dismissNow();
                } finally { controller.pause().stop().destroy(); }
            }
        } finally { app.getResources().updateConfiguration(original, app.getResources().getDisplayMetrics()); }
    }
    private void assertRangeRowsVisible(View view, ViewGroup decor) {
        if (view instanceof TextView text && text.getTag() != null
                && ("content_primary".equals(text.getTag()) || "bmi_selected".equals(text.getTag()))) {
            android.graphics.Rect bounds = new android.graphics.Rect(); text.getDrawingRect(bounds);
            decor.offsetDescendantRectToMyCoords(text, bounds);
            assertTrue(text.getText().toString(), bounds.top >= 0 && bounds.bottom <= decor.getHeight());
            assertTrue(text.getText().toString(), text.getLayout().getHeight() <= text.getHeight() - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom());
        }
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) assertRangeRowsVisible(group.getChildAt(i), decor);
    }
    @Test public void extremeAccessibilityLimitsAreNotClaimedAsFit() throws Exception {
        render(320, 320, 2f, CalculatorThemes.PORCELAIN_WHITE, false, false);
    }
}
