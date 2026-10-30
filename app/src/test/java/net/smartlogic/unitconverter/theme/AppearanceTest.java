package net.smartlogic.unitconverter.theme;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.helper.Preferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.util.ReflectionHelpers;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AppearanceTest {
    private Context context;
    @Before public void setup() {
        context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.AppTheme);
        Preferences.getInstance(context).getPreferences().edit().clear().commit();
        ThemeManager.init(context);
    }
    private View inflate(Context ctx, int layout) {
        LayoutInflater inflater = LayoutInflater.from(ctx).cloneInContext(ctx);
        var widgets = new com.google.android.material.theme.MaterialComponentsViewInflater();
        inflater.setFactory2(new LayoutInflater.Factory2() {
            @Override public View onCreateView(View parent, String name, Context c, android.util.AttributeSet attrs) {
                return widgets.createView(parent, name, c, attrs, false, true, true, false);
            }
            @Override public View onCreateView(String name, Context c, android.util.AttributeSet attrs) {
                return onCreateView(null, name, c, attrs);
            }
        });
        return inflater.inflate(layout, null);
    }
    @Test public void defaultsPersistenceRememberedShapeAndResetPreserveOtherPreferences() {
        Preferences prefs = Preferences.getInstance(context);
        prefs.getPreferences().edit().putString(Preferences.PREFS_SELECTED_THEME, "titanium_gray")
                .putString(Preferences.PREFS_NUMBER_OF_DECIMALS, "6").commit();
        assertEquals(Appearance.Style.FLAT, Appearance.style(context));
        assertEquals(Appearance.Shape.ROUNDED, Appearance.shape(context));
        assertEquals(Appearance.Font.OSWALD, Appearance.font(context));
        for (Appearance.Shape shape : Appearance.Shape.values()) {
            for (Appearance.Style style : Appearance.Style.values()) {
                Appearance.applyButtons(context, style, shape);
                Appearance.applyButtons(context, Appearance.Style.FLAT, Appearance.shape(context));
                assertEquals(shape, Appearance.shape(context));
                Appearance.applyButtons(context, style, Appearance.shape(context));
                assertEquals(shape, Appearance.shape(context));
                assertEquals(style, Appearance.style(context));
            }
        }
        for (Appearance.Font font : Appearance.Font.values()) {
            Appearance.applyFont(context, font);
            assertEquals(font, Appearance.font(context));
            TextView text = new TextView(context);
            AppearanceTypography.apply(text);
            assertNotNull(text.getTypeface());
            assertFalse(text.getTypeface().isBold());
        }
        Appearance.reset(context);
        assertEquals(Appearance.Style.FLAT, Appearance.style(context));
        assertEquals(Appearance.Shape.ROUNDED, Appearance.shape(context));
        assertEquals(Appearance.Font.OSWALD, Appearance.font(context));
        assertEquals("titanium_gray", prefs.getPreferences().getString(Preferences.PREFS_SELECTED_THEME, ""));
        assertEquals("6", prefs.getPreferences().getString(Preferences.PREFS_NUMBER_OF_DECIMALS, ""));
        prefs.getPreferences().edit().putString(Appearance.STYLE, "future-style")
                .putInt(Appearance.SHAPE, 42).putString(Appearance.FONT, "unknown").commit();
        assertEquals(Appearance.Style.FLAT, Appearance.style(context));
        assertEquals(Appearance.Shape.ROUNDED, Appearance.shape(context));
        assertEquals(Appearance.Font.OSWALD, Appearance.font(context));
    }
    @Test public void everyStyleShapeAndThemeRendersReadableAccentFilledEqualAndCircularBounds() throws Exception {
        for (CalculatorTheme theme : CalculatorThemes.all()) {
            for (Appearance.Style style : Appearance.Style.values()) {
                for (Appearance.Shape shape : Appearance.Shape.values()) {
                    for (KeyAppearanceDrawable.Role role : KeyAppearanceDrawable.Role.values()) {
                        KeyAppearanceDrawable d = new KeyAppearanceDrawable(context.getResources(), theme, style, shape, role);
                        assertTrue(theme.id()+" "+style+" "+role,
                                ColorUtils.calculateContrast(d.foreground(), d.surface()) >= 4.5);
                        Bitmap bitmap = Bitmap.createBitmap(100, 60, Bitmap.Config.ARGB_8888);
                        d.setBounds(0, 0, 100, 60);
                        d.draw(new Canvas(bitmap));
                        if (role == KeyAppearanceDrawable.Role.EQUAL) {
                            assertTrue(Color.alpha(bitmap.getPixel(50, 30)) > 0);
                            if (style == Appearance.Style.FLAT || style == Appearance.Style.OUTLINE)
                                assertEquals(theme.accentContentColor(), bitmap.getPixel(50, 30));
                            if (shape == Appearance.Shape.CIRCLE) {
                                assertEquals(0, Color.alpha(bitmap.getPixel(10, 30)));
                                assertTrue(Color.alpha(bitmap.getPixel(50, 8)) > 0);
                            }
                        } else if (style == Appearance.Style.FLAT) {
                            assertEquals(0, Color.alpha(bitmap.getPixel(50, 30)));
                        }
                        bitmap.recycle();
                    }
                }
            }
        }
    }
    @Test public void allFontsKeepNumericKeysReadableInSmallToolViewports() {
        for (float scale : new float[]{1f, 1.3f}) {
            android.content.res.Configuration config = new android.content.res.Configuration(context.getResources().getConfiguration());
            config.fontScale = scale;
            Context scaled = new ContextThemeWrapper(context.createConfigurationContext(config), R.style.AppTheme);
            for (Appearance.Font font : Appearance.Font.values()) {
                Appearance.applyFont(scaled, font);
                for (int layout : new int[]{R.layout.pane_calculator_calculate, R.layout.fragment_unit_converter,
                        R.layout.fragment_currency_converter}) {
                    View root = inflate(scaled, layout);
                    ThemeApplier.apply(root);
                    root.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY));
                    root.layout(0, 0, 360, 600);
                    for (int id : new int[]{R.id.zero, R.id.double_zero, R.id.one, R.id.ac}) {
                        View key = root.findViewById(id);
                        if (key == null) continue;
                        String message = font+" scale="+scale+" layout="+layout+" key="+id;
                        assertTrue(message, key.getWidth() >= 48 && key.getHeight() >= 48);
                        android.graphics.Rect rect = new android.graphics.Rect();
                        key.getDrawingRect(rect);
                        ((android.view.ViewGroup)root).offsetDescendantRectToMyCoords(key, rect);
                        assertTrue(message, rect.left >= 0 && rect.right <= 360 && rect.top >= 0 && rect.bottom <= 600);
                        if (key instanceof TextView text) {
                            assertTrue(message, text.getPaint().measureText(text.getText().toString()) <= key.getWidth());
                            assertTrue(message, text.getLineHeight() <= key.getHeight());
                        }
                    }
                }
            }
        }
    }
    @Test public void reviewEveryStyleAndShapeWithTheSharedPreviewComponent() throws Exception {
        for (CalculatorTheme theme : new CalculatorTheme[]{CalculatorThemes.PORCELAIN_WHITE, CalculatorThemes.GRAPHITE_BLACK}) {
            android.widget.LinearLayout sheet = new android.widget.LinearLayout(context);
            sheet.setOrientation(android.widget.LinearLayout.VERTICAL);
            sheet.setBackgroundColor(theme.background());
            for (Appearance.Style style : Appearance.Style.values()) {
                TextView label = new TextView(context);
                label.setText(style.label); label.setTextColor(theme.primaryContentColor());
                label.setTypeface(AppearanceTypography.typeface(context)); label.setTextSize(18);
                sheet.addView(label);
                KeypadPreviewView preview = new KeypadPreviewView(context);
                preview.render(theme, style, Appearance.Shape.ROUNDED, Appearance.Font.OSWALD);
                sheet.addView(preview, new android.widget.LinearLayout.LayoutParams(-1, 64));
            }
            for (Appearance.Shape shape : Appearance.Shape.values()) {
                TextView label = new TextView(context);
                label.setText(shape.label); label.setTextColor(theme.primaryContentColor());
                label.setTypeface(AppearanceTypography.typeface(context)); label.setTextSize(18);
                sheet.addView(label);
                KeypadPreviewView preview = new KeypadPreviewView(context);
                preview.render(theme, Appearance.Style.TONAL, shape, Appearance.Font.OSWALD);
                sheet.addView(preview, new android.widget.LinearLayout.LayoutParams(-1, 64));
            }
            sheet.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1800, View.MeasureSpec.AT_MOST));
            sheet.layout(0, 0, sheet.getMeasuredWidth(), sheet.getMeasuredHeight());
            Bitmap bitmap = Bitmap.createBitmap(sheet.getWidth(), sheet.getHeight(), Bitmap.Config.ARGB_8888);
            sheet.draw(new Canvas(bitmap));
            File file = new File("../.artifacts/appearance/styles-shapes-"+theme.id()+".png");
            file.getParentFile().mkdirs();
            try (FileOutputStream out = new FileOutputStream(file)) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); }
            bitmap.recycle();
        }
    }
    @Test public void reviewActualEqualAndBmiDoneAcrossStyles() throws Exception {
        for (CalculatorTheme theme : new CalculatorTheme[]{CalculatorThemes.PORCELAIN_WHITE, CalculatorThemes.GRAPHITE_BLACK}) {
            ReflectionHelpers.setStaticField(ThemeManager.class, "current", theme);
            Bitmap sheet = Bitmap.createBitmap(360, 800, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(sheet); canvas.drawColor(theme.background());
            android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            paint.setTypeface(AppearanceTypography.typeface(context)); paint.setTextSize(18);
            int row = 0;
            for (Appearance.Style style : Appearance.Style.values()) {
                Appearance.applyButtons(context, style, Appearance.Shape.CIRCLE);
                paint.setColor(theme.primaryContentColor());
                canvas.drawText(context.getString(style.label), 12, row * 100 + 24, paint);
                View calculator = inflate(context, R.layout.pane_calculator_calculate);
                View done = inflate(context, R.layout.bmi_keypad_done);
                ThemeApplier.apply(calculator); ThemeApplier.apply(done);
                calculator.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY));
                calculator.layout(0, 0, 360, 600);
                View equal = calculator.findViewById(R.id.equal);
                assertEquals("=", ((TextView)equal).getText().toString());
                int col = 0;
                for (View key : new View[]{equal, done}) {
                    key.measure(View.MeasureSpec.makeMeasureSpec(64, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(64, View.MeasureSpec.EXACTLY));
                    key.layout(0, 0, 64, 64);
                    // These isolated keys are not attached to a ViewRoot; perform TextView's
                    // normal pre-draw alignment for horizontally scrolling single-line labels.
                    if (key instanceof TextView text) text.onPreDraw();
                    canvas.save(); canvas.translate(170 + col * 80, row * 100 + 28);
                    // Match ViewGroup.drawChild's scroll transform when drawing an isolated key.
                    canvas.translate(-key.getScrollX(), -key.getScrollY());
                    key.draw(canvas); canvas.restore();
                    assertNotEquals("Missing key fill", theme.background(),
                            sheet.getPixel(170 + col * 80 + 18, row * 100 + 28 + 18));
                    int ink = ((KeyAppearanceDrawable)key.getBackground()).foreground();
                    boolean glyph = false;
                    for (int y = 18; y < 46; y++) for (int x = 18; x < 46; x++) {
                        if (sheet.getPixel(170 + col * 80 + x, row * 100 + 28 + y) == ink) glyph = true;
                    }
                    assertTrue("Missing key symbol in native capture: " + style + " column=" + col, glyph);
                    col++;
                }
                row++;
            }
            File file = new File("../.artifacts/appearance/equal-bmi-"+theme.id()+".png");
            file.getParentFile().mkdirs();
            try (FileOutputStream out = new FileOutputStream(file)) { sheet.compress(Bitmap.CompressFormat.PNG, 100, out); }
            sheet.recycle();
        }
    }
    @Test public void actualCalculatorAndBmiDoneUseSameAppearanceAndFont() {
        for (Appearance.Style style : Appearance.Style.values()) {
            Appearance.applyButtons(context, style, Appearance.Shape.CIRCLE);
            Appearance.applyFont(context, Appearance.Font.ROBOTO_MONO);
            View calculator = inflate(context, R.layout.pane_calculator_calculate);
            View bmi = inflate(context, R.layout.bmi_keypad_done);
            ThemeApplier.apply(calculator); ThemeApplier.apply(bmi);
            View equal = calculator.findViewById(R.id.equal), done = bmi.findViewById(R.id.equal);
            assertTrue(equal.getBackground() instanceof KeyAppearanceDrawable);
            assertTrue(done.getBackground() instanceof KeyAppearanceDrawable);
            assertEquals(((KeyAppearanceDrawable)equal.getBackground()).surface(),
                    ((KeyAppearanceDrawable)done.getBackground()).surface());
            assertEquals(Appearance.Font.ROBOTO_MONO, equal.getTag(R.id.appearance_font_applied));
        }
    }
}
