package net.smartlogic.unitconverter.widget;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import com.google.android.material.theme.MaterialComponentsViewInflater;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.adapter.UnitAdapter;
import net.smartlogic.unitconverter.model.Conversion;
import net.smartlogic.unitconverter.theme.ThemeApplier;
import net.smartlogic.unitconverter.theme.CalculatorTheme;
import net.smartlogic.unitconverter.theme.CalculatorThemes;
import net.smartlogic.unitconverter.theme.ThemeManager;
import net.smartlogic.unitconverter.utils.Conversions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

/** Real Android measurements of inflated production layouts, after persistent chrome. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CalculationLayoutTest {
    @Test public void reorderedConverterRowsAlignAcrossThemesAndRenderPreviews() throws Exception {
        CalculatorTheme original = ThemeManager.get();
        try {
            int themeIndex = 0;
            for (CalculatorTheme theme : CalculatorThemes.all()) {
                ReflectionHelpers.setStaticField(ThemeManager.class, "current", theme);
                for (float scale : new float[]{1f, 1.3f}) {
                    for (int[] size : new int[][]{{320, 400}, {360, 460}, {412, 600}}) {
                        ViewGroup unit = (ViewGroup) inflate(R.layout.fragment_unit_converter, scale);
                        ViewGroup currency = (ViewGroup) inflate(R.layout.fragment_currency_converter, scale);
                        measure(unit, size[0], size[1]);
                        measure(currency, size[0], size[1]);
                        for (ViewGroup root : new ViewGroup[]{unit, currency}) {
                            boolean fits = viewport(root).getChildAt(0).getVisibility() == View.VISIBLE;
                            if (fits) {
                                assertKeys(root, true);
                            } else {
                                // Compare against the previous order before classifying a
                                // physically constrained configuration as an existing limit.
                                LinearLayout controls = root.findViewById(R.id.calculation_controls);
                                View strip = root.findViewById(R.id.category_selector);
                                controls.removeView(strip);
                                controls.addView(strip, 0);
                                measure(root, size[0], size[1]);
                                assertEquals("previous order must have the same space limit",
                                        View.INVISIBLE, viewport(root).getChildAt(0).getVisibility());
                                controls.removeView(strip);
                                controls.addView(strip, 1);
                                measure(root, size[0], size[1]);
                                assertEquals(View.VISIBLE, root.findViewById(R.id.tool_space_message).getVisibility());
                            }
                        }
                        for (int id : new int[]{R.id.converter_source_section, R.id.converter_target_section, R.id.keypad}) {
                            assertEquals("theme=" + themeIndex + " scale=" + scale + " size=" + size[0],
                                    bounds(unit, unit.findViewById(id)), bounds(currency, currency.findViewById(id)));
                        }
                        for (ViewGroup root : new ViewGroup[]{unit, currency}) {
                            assertTrue(bounds(root, root.findViewById(R.id.category_selector)).top
                                    >= bounds(root, root.findViewById(R.id.converter_target_section)).bottom);
                        }
                        if (viewport(currency).getChildAt(0).getVisibility() == View.VISIBLE) {
                            assertInside(currency, currency.findViewById(R.id.refresh));
                        }
                        if (size[0] == 360 && (theme == CalculatorThemes.PORCELAIN_WHITE || theme == CalculatorThemes.GRAPHITE_BLACK)) {
                            android.graphics.Bitmap preview = android.graphics.Bitmap.createBitmap(
                                    720, 460, android.graphics.Bitmap.Config.ARGB_8888);
                            android.graphics.Canvas canvas = new android.graphics.Canvas(preview);
                            unit.draw(canvas);
                            canvas.translate(360, 0);
                            currency.draw(canvas);
                            java.io.File directory = new java.io.File("build/reports/converter-layout");
                            assertTrue(directory.isDirectory() || directory.mkdirs());
                            try (java.io.FileOutputStream stream = new java.io.FileOutputStream(new java.io.File(directory,
                                    "converter-" + themeIndex + "-" + scale + ".png"))) {
                                preview.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
                            }
                            preview.recycle();
                        }
                    }
                }
                themeIndex++;
            }
        } finally {
            ReflectionHelpers.setStaticField(ThemeManager.class, "current", original);
        }
    }

    @Test public void previousCurrencyOrderHasSameLimitAtLargeDisplayDensity() {
        ViewGroup root = (ViewGroup) inflate(R.layout.fragment_currency_converter, 1f, 520);
        measure(root, 1080, 1185);
        int reorderedVisibility = viewport(root).getChildAt(0).getVisibility();
        LinearLayout controls = root.findViewById(R.id.calculation_controls);
        View strip = root.findViewById(R.id.category_selector);
        controls.removeView(strip);
        controls.addView(strip, 0);
        measure(root, 1080, 1185);
        assertEquals(reorderedVisibility, viewport(root).getChildAt(0).getVisibility());
    }

    @Test public void converterBordersAlignAndCategoriesFollowValueRows() {
        CalculatorTheme original = ThemeManager.get();
        try {
            for (CalculatorTheme theme : new CalculatorTheme[]{CalculatorThemes.PORCELAIN_WHITE, CalculatorThemes.GRAPHITE_BLACK}) {
                ReflectionHelpers.setStaticField(ThemeManager.class, "current", theme);
                for (float scale : new float[]{1f, 1.3f}) {
                    for (int layout : new int[]{R.layout.fragment_unit_converter, R.layout.fragment_currency_converter}) {
                        ViewGroup root = (ViewGroup) inflate(layout, scale);
                        measure(root, 360, 460);
                        assertKeys(root, true);
                        for (int[] ids : new int[][]{{R.id.converter_source_section, R.id.fromUnit},
                                {R.id.converter_target_section, R.id.toUnit}}) {
                            ViewGroup row = root.findViewById(ids[0]);
                            View amount = row.getChildAt(0);
                            View selector = root.findViewById(ids[1] == R.id.fromUnit ? R.id.from_selector : R.id.to_selector);
                            if (selector == null) selector = root.findViewById(ids[1]);
                            assertNotNull("layout=" + layout + " amount=" + amount.getId() + " tag=" + amount.getTag(), amount.getBackground());
                            assertTrue(amount.getPaddingRight() >= root.getResources().getDimensionPixelSize(R.dimen.converter_spacing));
                            assertEquals(bounds(root, amount).top, bounds(root, selector).top);
                            assertEquals(bounds(root, amount).bottom, bounds(root, selector).bottom);
                        }
                        assertTrue(bounds(root, root.findViewById(R.id.category_selector)).top
                                - bounds(root, root.findViewById(R.id.converter_target_section)).bottom
                                >= root.getResources().getDimensionPixelSize(R.dimen.converter_small_spacing));
                        View heading = root.findViewById(R.id.category_selector);
                        ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) heading.getLayoutParams();
                        int stripGap = root.getResources().getDimensionPixelSize(R.dimen.converter_spacing);
                        assertTrue(margins.topMargin >= stripGap);
                        assertTrue(margins.bottomMargin >= stripGap);
                        if (heading instanceof com.google.android.material.tabs.TabLayout tabs && !tabs.isInlineLabel()) {
                            View item = tabs.getTabAt(0).getCustomView();
                            View icon = item.findViewById(R.id.converter_category_tab_icon);
                            View label = item.findViewById(R.id.converter_category_tab_label);
                            assertEquals(root.getResources().getDimensionPixelSize(R.dimen.converter_category_icon_label_gap),
                                    label.getTop() - icon.getBottom());
                            assertEquals(theme.equal(), ((TextView) label).getCurrentTextColor());
                        }
                    }
                }
            }
        } finally {
            ReflectionHelpers.setStaticField(ThemeManager.class, "current", original);
        }
    }

    @Test public void converterModeSwitchKeepsPillSelectionAcrossThemes() {
        CalculatorTheme original = ThemeManager.get();
        try {
            for (CalculatorTheme theme : CalculatorThemes.all()) {
                ReflectionHelpers.setStaticField(ThemeManager.class, "current", theme);
                View root = inflate(R.layout.pane_converter_convert, 1.3f);
                com.google.android.material.button.MaterialButtonToggleGroup toggle = root.findViewById(R.id.converter_mode_toggle);
                com.google.android.material.button.MaterialButton unit = root.findViewById(R.id.btn_unit_mode);
                com.google.android.material.button.MaterialButton currency = root.findViewById(R.id.btn_currency_mode);
                toggle.check(R.id.btn_unit_mode);
                measure(root, 320, 500);
                assertEquals(unit.getWidth(), currency.getWidth());
                assertTrue(unit.getHeight() >= root.getResources().getDimensionPixelSize(R.dimen.converter_touch_target));
                android.graphics.RectF segmentBounds = new android.graphics.RectF(0, 0, unit.getWidth(), unit.getHeight());
                assertEquals((unit.getHeight() - 2 * root.getResources().getDimensionPixelSize(
                        R.dimen.converter_mode_vertical_inset)) / 2f, unit.getShapeAppearanceModel()
                        .getTopRightCornerSize().getCornerSize(segmentBounds), 0.1f);
                assertTrue(androidx.core.graphics.ColorUtils.calculateContrast(
                        unit.getCurrentTextColor(), theme.equal()) >= 4.5);
                currency.performClick();
                assertEquals(R.id.btn_currency_mode, toggle.getCheckedButtonId());
                assertFalse(unit.isChecked());
                assertTrue(currency.isChecked());
                assertNotNull(toggle.getBackground());
            }
        } finally {
            ReflectionHelpers.setStaticField(ThemeManager.class, "current", original);
        }
    }

    private static final int[] SCREENS = {R.layout.pane_calculator_calculate,
            R.layout.fragment_unit_converter, R.layout.fragment_currency_converter};

    @Test public void resizingAmountsNeverMovesConverterControlsOrKeypad() {
        for (int layout : new int[]{R.layout.fragment_unit_converter, R.layout.fragment_currency_converter}) {
            for (float scale : new float[]{1f, 1.3f}) {
                ViewGroup root = (ViewGroup) inflate(layout, scale);
                TextView input = root.findViewById(R.id.input);
                TextView output = root.findViewById(R.id.output);
                input.setText("1");
                output.setText("1.12");
                measure(root, 360, 460);
                int[] ids = {R.id.input, R.id.output, R.id.fromUnit, R.id.toUnit,
                        R.id.reverse, R.id.keypad, R.id.seven, R.id.zero};
                Rect[] original = new Rect[ids.length];
                for (int i = 0; i < ids.length; i++) original[i] = bounds(root, root.findViewById(ids[i]));
                float maximum = input.getTextSize();
                for (String value : new String[]{"123456789012", "1", ""}) {
                    input.setText(value);
                    output.setText(value.isEmpty() ? "" : value + ".1234567890123456");
                    measure(root, 360, 460);
                    if (value.length() > 1) assertTrue(input.getTextSize() < maximum);
                    for (int i = 0; i < ids.length; i++) {
                        assertEquals("layout=" + layout + " scale=" + scale + " id=" + ids[i],
                                original[i], bounds(root, root.findViewById(ids[i])));
                    }
                }
            }
        }
    }

    @Test public void converterAmountsSizeIndependentlyAndSelectorsStayAtQuarterWidth() {
        for (int layout : new int[]{R.layout.fragment_unit_converter, R.layout.fragment_currency_converter}) {
            View root = inflate(layout, 1f);
            TextView input = root.findViewById(R.id.input);
            TextView output = root.findViewById(R.id.output);
            assertEquals("0", input.getHint().toString());
            View empty = LayoutInflater.from(root.getContext()).inflate(layout, null);
            assertEquals("", ((TextView) empty.findViewById(R.id.output)).getText().toString());
            assertFalse(input.getTypeface().isBold());
            assertFalse(output.getTypeface().isBold());
            float maximum = input.getTextSize();
            for (String value : new String[]{"1", "123456789012"}) {
                input.setText(value);
                output.setText(value + ".123456789");
                measure(root, 360, 460);
                ViewGroup row = root.findViewById(R.id.converter_source_section);
                assertEquals(90 + root.getResources().getDimensionPixelSize(R.dimen.converter_selector_extra_width),
                        row.getChildAt(1).getWidth());
            }
            input.setText("99999");
            output.setText("30479695200000000");
            measure(root, 360, 460);
            assertEquals(maximum, input.getTextSize(), 0.1f);
            assertTrue(output.getTextSize() < input.getTextSize());
            output.setText("1");
            measure(root, 360, 460);
            assertEquals(maximum, output.getTextSize(), 0.1f);
        }
    }

    @Test public void compactCategoriesHaveOneVisibleLabelPerTab() {
        View root = inflate(R.layout.fragment_unit_converter, 1.3f);
        measure(root, 320, 460);
        com.google.android.material.tabs.TabLayout tabs = root.findViewById(R.id.category_selector);
        ViewGroup strip = (ViewGroup) tabs.getChildAt(0);
        for (int i = 0; i < tabs.getTabCount(); i++) {
            String label = tabs.getTabAt(i).getText().toString();
            assertEquals(label, 1, visibleTextCount(strip.getChildAt(i), label));
        }
    }

    private int visibleTextCount(View view, String label) {
        if (view.getVisibility() != View.VISIBLE) return 0;
        int count = view instanceof TextView text && label.contentEquals(text.getText()) ? 1 : 0;
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) count += visibleTextCount(group.getChildAt(i), label);
        }
        return count;
    }

    @Test public void convertersKeepKeypadGeometryStableAcrossModes() {
        for (float scale : new float[]{1f, 1.3f}) {
            for (int[] size : new int[][]{{320, 400}, {360, 460}, {412, 600}}) {
                View unit = inflate(R.layout.fragment_unit_converter, scale);
                View currency = inflate(R.layout.fragment_currency_converter, scale);
                ((TextView) currency.findViewById(R.id.txtLastUpdateTime)).setText(
                        currency.getContext().getString(R.string.currency_cached_at, "21-Oct 09:45 AM"));
                measure(unit, size[0], size[1]);
                measure(currency, size[0], size[1]);
                assertKeys(unit, true);
                assertKeys(currency, true);
                for (View converter : new View[]{unit, currency}) {
                    ViewGroup root = (ViewGroup) converter;
                    assertEquals(View.GONE, converter.findViewById(R.id.converter_from_label).getVisibility());
                    assertEquals(View.GONE, converter.findViewById(R.id.converter_to_label).getVisibility());
                    assertTrue(bounds(root, converter.findViewById(R.id.input)).right
                            <= bounds(root, converter.findViewById(R.id.fromUnit)).left);
                    assertTrue(bounds(root, converter.findViewById(R.id.output)).right
                            <= bounds(root, converter.findViewById(R.id.toUnit)).left);
                    assertTrue(bounds(root, converter.findViewById(R.id.fromUnit)).right
                            <= bounds(root, converter.findViewById(R.id.reverse)).left);
                    ViewGroup source = converter.findViewById(R.id.converter_source_section);
                    View selector = converter.findViewById(R.id.from_selector);
                    if (selector == null) selector = converter.findViewById(R.id.fromUnit);
                    assertNotNull(source.getChildAt(0).getBackground());
                    assertEquals(bounds(root, source.getChildAt(0)).top,
                            bounds(root, selector).top);
                    assertEquals(bounds(root, source.getChildAt(0)).bottom,
                            bounds(root, selector).bottom);
                    assertTrue(bounds(root, converter.findViewById(R.id.category_selector)).top
                            - bounds(root, converter.findViewById(R.id.converter_target_section)).bottom
                            >= converter.getResources().getDimensionPixelSize(R.dimen.converter_small_spacing));
                }
                assertEquals("keypad parity " + size[0] + "x" + size[1] + " scale=" + scale,
                        bounds((ViewGroup) unit, unit.findViewById(R.id.keypad)),
                        bounds((ViewGroup) currency, currency.findViewById(R.id.keypad)));
                for (int id : new int[]{R.id.seven, R.id.nine, R.id.ac, R.id.zero, R.id.minus}) {
                    assertEquals(bounds((ViewGroup) unit, unit.findViewById(id)),
                            bounds((ViewGroup) currency, currency.findViewById(id)));
                }
                for (int id : new int[]{R.id.converter_source_section, R.id.converter_target_section}) {
                    assertEquals("value row alignment at scale=" + scale,
                            bounds((ViewGroup) unit, unit.findViewById(id)),
                            bounds((ViewGroup) currency, currency.findViewById(id)));
                }
                ViewGroup keypad = unit.findViewById(R.id.keypad);
                for (int row = 0; row < keypad.getChildCount(); row++) {
                    assertEquals(4, ((ViewGroup) keypad.getChildAt(row)).getChildCount());
                }
                assertEquals(unit.findViewById(R.id.nine).getParent(), unit.findViewById(R.id.ac).getParent());
            }
        }
    }

    @Test public void normalWorkspacesKeepEveryPrimaryControlVisible() {
        for (int layout : SCREENS) {
            for (int[] size : new int[][]{{320, 400}, {360, 460}, {412, 600}, {600, 340}, {840, 700}}) {
                View root = inflate(layout, 1f);
                measure(root, size[0], size[1]);
                CalculationViewport viewport = viewport(root);
                assertKeys(root, true);
                assertFalse(label(layout, size), viewport.canScrollVertically(1));
                assertFalse(viewport.canScrollVertically(-1));
                assertKeys(root, true);
                for (int id : new int[]{R.id.input, R.id.output, R.id.fromUnit, R.id.toUnit,
                        R.id.reverse, R.id.category_selector, R.id.expression, R.id.result}) {
                    View control = root.findViewById(id);
                    if (control != null) assertInside((ViewGroup) root, control);
                }
            }
        }
    }

    @Test public void enlargedUnitResultUsesAvailableWidthWithoutChangingTypography() {
        View root = inflate(R.layout.fragment_unit_converter, 1.3f);
        TextView input = root.findViewById(R.id.input);
        TextView output = root.findViewById(R.id.output);
        float originalSize = output.getTextSize();
        input.setText("12");
        output.setText("7.45645");
        measure(root, 360, 460);
        assertKeys(root, true);
        assertEquals(originalSize, output.getTextSize(), 0f);
        assertTrue("result should fit without horizontal clipping", output.getPaint().measureText(
                output.getText().toString()) <= output.getWidth() - output.getCompoundPaddingLeft()
                - output.getCompoundPaddingRight());
        assertEquals(input.getWidth(), output.getWidth());
    }

    @Test public void currencyCacheAndUnavailableLabelsFitConstrainedWorkspace() {
        CalculatorTheme original = ThemeManager.get();
        try {
            for (CalculatorTheme theme : CalculatorThemes.all()) {
                ReflectionHelpers.setStaticField(ThemeManager.class, "current", theme);
                for (float scale : new float[]{1f, 1.3f}) {
                    for (int status : new int[]{R.string.currency_cached_at, R.string.currency_stale_at}) {
                        View root = inflate(R.layout.fragment_currency_converter, scale);
                        ((TextView) root.findViewById(R.id.txtLastUpdateTime)).setText(
                                root.getContext().getString(status, "21-Oct 12:03 AM"));
                        ((TextView) root.findViewById(R.id.txtRate)).setText(R.string.currency_unavailable);
                        measure(root, 360, 600);
                        assertFalse(viewport(root).canScrollVertically(1));
                        assertKeys(root, true);
                        assertInside((ViewGroup) root, root.findViewById(R.id.txtLastUpdateTime));
                        assertInside((ViewGroup) root, root.findViewById(R.id.txtRate));
                        assertEquals(theme.functions(), ((TextView) root.findViewById(R.id.txtRate)).getCurrentTextColor());
                        assertEquals(theme.functions(), ((TextView) root.findViewById(R.id.inputSymbol)).getCurrentTextColor());
                        assertEquals(theme.functions(), ((TextView) root.findViewById(R.id.outputSymbol)).getCurrentTextColor());
                        assertEquals(theme.mainText(), ((TextView) root.findViewById(R.id.input)).getCurrentTextColor());
                    }
                }
            }
        } finally {
            ReflectionHelpers.setStaticField(ThemeManager.class, "current", original);
        }
    }

    @Test public void allUnitCategoriesAndLongValuesRetainTheSameViewport() {
        for (int category = Conversion.LENGTH; category <= Conversion.TORQUE; category++) {
            View root = inflate(R.layout.fragment_unit_converter, 1f);
            bindUnits(root, category);
            ((TextView) root.findViewById(R.id.input)).setText("999999999999");
            ((TextView) root.findViewById(R.id.output)).setText("12345678901234567890.123456789");
            measure(root, 360, 460);
            assertFalse("category " + category, viewport(root).canScrollVertically(1));
            assertKeys(root, true);
            assertEquals("12345678901234567890.123456789",
                    ((TextView) root.findViewById(R.id.output)).getText().toString());
        }
    }

    @Test public void impossibleViewportShowsExplicitLimitWithoutScrollingAndRecovers() {
        for (int layout : SCREENS) {
            View root = inflate(layout, 2f);
            measure(root, 320, 240);
            CalculationViewport viewport = viewport(root);
            assertFalse(viewport.canScrollVertically(1));
            assertFalse(viewport.canScrollVertically(-1));
            assertEquals(View.INVISIBLE, viewport.getChildAt(0).getVisibility());
            assertEquals(View.VISIBLE, root.findViewById(R.id.tool_space_message).getVisibility());
            TextView input = root.findViewById(R.id.input);
            String value = input == null ? null : input.getText().toString();
            measure(root, 840, 1100);
            assertFalse(viewport.canScrollVertically(1));
            assertEquals(0, viewport.getScrollY());
            assertEquals(View.VISIBLE, viewport.getChildAt(0).getVisibility());
            assertEquals(View.GONE, root.findViewById(R.id.tool_space_message).getVisibility());
            if (input != null) assertEquals(value, input.getText().toString());
            assertKeys(root, true);
        }
    }

    @Test public void compactLayoutRestoresAfterRepeatedWindowChanges() {
        for (int layout : SCREENS) {
            View root = inflate(layout, 1f);
            for (int[] size : new int[][]{{360, 700}, {360, 460}, {360, 700},
                    {840, 700}, {600, 340}, {360, 460}}) {
                measure(root, size[0], size[1]);
                assertFalse(label(layout, size), viewport(root).canScrollVertically(1));
                assertEquals(label(layout, size) + " controls=" + root.findViewById(R.id.calculation_controls).getMeasuredHeight()
                        + " keypad=" + root.findViewById(R.id.keypad).getMeasuredHeight(),
                        View.VISIBLE, viewport(root).getChildAt(0).getVisibility());
                assertKeys(root, true);
            }
        }
    }

    @Test public void calculatorKeepsStackedDisplayAndRestoresResultSizeWithoutLosingValues() {
        View root = inflate(R.layout.pane_calculator_calculate, 1f);
        TextView expression = root.findViewById(R.id.expression);
        TextView result = root.findViewById(R.id.result);
        expression.setText("125 × 8");
        result.setText("1000");
        LinearLayout display = root.findViewById(R.id.display_values);
        measure(root, 320, 400);
        assertKeys(root, true);
        assertEquals(LinearLayout.VERTICAL, display.getOrientation());
        assertInside((ViewGroup) root, expression);
        assertInside((ViewGroup) root, result);
        assertFalse(Rect.intersects(bounds((ViewGroup) root, expression), bounds((ViewGroup) root, result)));
        assertTrue(bounds((ViewGroup) root, expression).bottom <= bounds((ViewGroup) root, result).top);
        assertTrue(result.getTextSize() >= result.getAutoSizeMinTextSize());
        int compactHeight = result.getHeight();
        measure(root, 360, 700);
        assertKeys(root, true);
        assertEquals(LinearLayout.VERTICAL, display.getOrientation());
        assertTrue(result.getHeight() > compactHeight);
        assertEquals(result.getAutoSizeMaxTextSize(), result.getTextSize(), 1f);
        assertEquals("125 × 8", expression.getText().toString());
        assertEquals("1000", result.getText().toString());
    }

    @Test public void currencyFitsActualDeviceViewportAtLargerDisplayDensity() {
        View root = inflate(R.layout.fragment_currency_converter, 1f, 520);
        measure(root, 1080, 1185); // Mi A2 workspace after system and app chrome at 520dpi.
        assertKeys(root, true);
        assertInside((ViewGroup) root, root.findViewById(R.id.input));
        assertInside((ViewGroup) root, root.findViewById(R.id.output));
        assertInside((ViewGroup) root, root.findViewById(R.id.txtLastUpdateTime));
    }

    @Test public void longScaledLabelsDoNotCollapseUnitSelectorTouchTargets() {
        View root = inflate(R.layout.fragment_unit_converter, 2f);
        ((TextView) root.findViewById(R.id.converter_from_label)).setText("Convert from");
        ((TextView) root.findViewById(R.id.converter_to_label)).setText("Convert into");
        measure(root, 840, 1100);
        assertKeys(root, true);
        for (int id : new int[]{R.id.fromUnit, R.id.toUnit}) {
            View selector = root.findViewById(id);
            assertTrue("selector must stay tappable", selector.getWidth() >= 48);
            assertTrue("selector must stay tappable", selector.getHeight() >= 48);
        }
    }

    @Test public void inputAndOutputNeverOverlapSelectorsOrKeypad() {
        for (int layout : new int[]{R.layout.fragment_unit_converter, R.layout.fragment_currency_converter}) {
            View root = inflate(layout, 1f);
            measure(root, 360, 460);
            int[] ids = {R.id.fromUnit, R.id.toUnit, R.id.input, R.id.output, R.id.reverse, R.id.keypad};
            for (int i = 0; i < ids.length; i++) {
                for (int j = i + 1; j < ids.length; j++) {
                    assertFalse("controls overlap: " + ids[i] + ", " + ids[j],
                            Rect.intersects(bounds((ViewGroup) root, root.findViewById(ids[i])),
                                    bounds((ViewGroup) root, root.findViewById(ids[j]))));
                }
            }
        }
    }

    @Test public void equivalentKeysShareTypographyPaddingAndTouchFeedback() {
        View calculator = inflate(R.layout.pane_calculator_calculate, 1f);
        for (int layout : new int[]{R.layout.fragment_unit_converter, R.layout.fragment_currency_converter}) {
            View converter = inflate(layout, 1f);
            for (int id : new int[]{R.id.one, R.id.seven, R.id.zero, R.id.dot}) {
                TextView expected = calculator.findViewById(id);
                TextView actual = converter.findViewById(id);
                assertEquals(expected.getTextSize(), actual.getTextSize(), 0.01f);
                assertEquals(expected.getTypeface(), actual.getTypeface());
                assertEquals(expected.getCurrentTextColor(), actual.getCurrentTextColor());
                assertEquals(expected.getPaddingTop(), actual.getPaddingTop());
                assertEquals(expected.getPaddingBottom(), actual.getPaddingBottom());
                assertNotNull(actual.getForeground());
            }
            View expected = calculator.findViewById(R.id.backspace);
            View actual = converter.findViewById(R.id.backspace);
            assertEquals(expected.getPaddingLeft(), actual.getPaddingLeft());
            assertEquals(expected.getPaddingTop(), actual.getPaddingTop());
            assertNotNull(actual.getForeground());
        }
    }

    @Test public void narrowWindowDoesNotExposeUndersizedKeyTargets() {
        for (int layout : SCREENS) {
            View root = inflate(layout, 1f);
            measure(root, 180, 700);
            assertEquals(View.INVISIBLE, viewport(root).getChildAt(0).getVisibility());
            assertEquals(View.VISIBLE, root.findViewById(R.id.tool_space_message).getVisibility());
            assertFalse(viewport(root).canScrollVertically(1));
        }
    }

    private View inflate(int layout, float scale) {
        return inflate(layout, scale, 0);
    }

    private View inflate(int layout, float scale, int density) {
        Context app = RuntimeEnvironment.getApplication();
        Configuration config = new Configuration(app.getResources().getConfiguration());
        config.fontScale = scale;
        if (density > 0) config.densityDpi = density;
        Context context = new ContextThemeWrapper(app.createConfigurationContext(config), R.style.AppTheme);
        LayoutInflater inflater = LayoutInflater.from(context).cloneInContext(context);
        MaterialComponentsViewInflater widgets = new MaterialComponentsViewInflater();
        // Match AppCompat/Material activity inflation, rather than silently measuring
        // framework Buttons whose padding and minimum dimensions may differ.
        inflater.setFactory2(new LayoutInflater.Factory2() {
            @Override public View onCreateView(View parent, String name, Context ctx, AttributeSet attrs) {
                return widgets.createView(parent, name, ctx, attrs, false, true, true, false);
            }
            @Override public View onCreateView(String name, Context ctx, AttributeSet attrs) {
                return onCreateView(null, name, ctx, attrs);
            }
        });
        View root = inflater.inflate(layout, null);
        if (root.findViewById(R.id.input) != null) {
            ((EditText) root.findViewById(R.id.input)).setShowSoftInputOnFocus(false);
            ((TextView) root.findViewById(R.id.input)).setText("100");
            ((TextView) root.findViewById(R.id.output)).setText("328.084");
        }
        if (root.findViewById(R.id.fromUnit) instanceof Spinner) bindUnits(root, Conversion.LENGTH);
        if (root.findViewById(R.id.fromCurrencyISO) != null) {
            ((TextView) root.findViewById(R.id.inputSymbol)).setText("د.إ");
            ((TextView) root.findViewById(R.id.outputSymbol)).setText("₹");
            ((TextView) root.findViewById(R.id.fromCurrencyISO)).setText("USD");
            ((TextView) root.findViewById(R.id.toCurrencyISO)).setText("INR");
            ((TextView) root.findViewById(R.id.fromCurrency)).setText("United States Dollar");
            ((TextView) root.findViewById(R.id.toCurrency)).setText("An exceptionally long currency name");
            ((TextView) root.findViewById(R.id.txtRate)).setText("1 USD = 83.50 INR");
            ((TextView) root.findViewById(R.id.txtLastUpdateTime)).setText("Last updated 7 Oct, 11:03");
            root.findViewById(R.id.ac).setVisibility(View.VISIBLE);
        }
        ThemeApplier.apply(root);
        return root;
    }

    private void bindUnits(View root, int category) {
        Conversion conversion = Conversions.getInstance().getById(category);
        for (int id : new int[]{R.id.fromUnit, R.id.toUnit}) {
            ((Spinner) root.findViewById(id)).setAdapter(new UnitAdapter(root.getContext(), conversion.getUnits()));
        }
        com.google.android.material.tabs.TabLayout categories = root.findViewById(R.id.category_selector);
        if (categories.getTabCount() == 0) {
            for (int id : Conversions.getInstance().getCategoryIds()) {
                Conversion item = Conversions.getInstance().getById(id);
                View custom = LayoutInflater.from(root.getContext()).inflate(R.layout.converter_category_tab, categories, false);
                ((android.widget.ImageView) custom.findViewById(R.id.converter_category_tab_icon)).setImageResource(item.getImageResource());
                ((TextView) custom.findViewById(R.id.converter_category_tab_label)).setText(item.getLabelResource());
                categories.addTab(categories.newTab().setTag(id).setText(item.getLabelResource())
                        .setIcon(item.getImageResource()).setCustomView(custom), id == category);
            }
        }
        ((TextView) root.findViewById(R.id.unit_rate)).setText("1 m = 3.28084 ft");
        root.findViewById(R.id.minus).setVisibility(View.VISIBLE);
    }

    private void measure(View root, int width, int height) {
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, width, height);
    }

    private CalculationViewport viewport(View view) {
        if (view instanceof CalculationViewport) return (CalculationViewport) view;
        return (CalculationViewport) ((ViewGroup) view).getChildAt(0);
    }

    private void assertKeys(View root, boolean inside) {
        assertEquals("workspace must be operable at " + root.getWidth() + "x" + root.getHeight()
                        + " scale=" + root.getResources().getConfiguration().fontScale
                        + " controls=" + root.findViewById(R.id.calculation_controls).getMeasuredHeight()
                        + " keypad=" + root.findViewById(R.id.keypad).getMeasuredHeight()
                        + " amount=" + (root.findViewById(R.id.input) == null ? 0 : root.findViewById(R.id.input).getMeasuredHeight())
                        + " heading=" + (root.findViewById(R.id.category_selector) == null ? 0 : root.findViewById(R.id.category_selector).getMeasuredHeight())
                        + " symbol=" + (root.findViewById(R.id.inputSymbol) == null ? 0 : root.findViewById(R.id.inputSymbol).getMeasuredHeight())
                        + " source=" + (root.findViewById(R.id.converter_source_section) == null ? 0 : root.findViewById(R.id.converter_source_section).getMeasuredHeight()),
                View.VISIBLE, viewport(root).getChildAt(0).getVisibility());
        ViewGroup keys = root.findViewById(R.id.keypad);
        assertKeyChildren((ViewGroup) root, keys, inside);
    }

    private void assertKeyChildren(ViewGroup root, View view, boolean inside) {
        if (view.getVisibility() == View.GONE) return;
        if (view instanceof android.widget.Button || view instanceof android.widget.ImageButton) {
            int minimum = root.getResources().getDimensionPixelSize(R.dimen.converter_touch_target);
            assertTrue("key height " + view.getId() + ": " + view.getHeight(), view.getHeight() >= minimum);
            assertTrue("key width " + view.getId(), view.getWidth() >= minimum);
            if (inside) assertInside(root, view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) assertKeyChildren(root, group.getChildAt(i), inside);
        }
    }

    private void assertInside(ViewGroup root, View view) {
        Rect bounds = bounds(root, view);
        assertTrue("empty control " + view.getId(), bounds.width() > 0 && bounds.height() > 0);
        assertTrue("outside viewport " + bounds, bounds.left >= 0 && bounds.top >= 0
                && bounds.right <= root.getWidth() && bounds.bottom <= root.getHeight());
    }

    private Rect bounds(ViewGroup root, View view) {
        Rect bounds = new Rect(0, 0, view.getWidth(), view.getHeight());
        root.offsetDescendantRectToMyCoords(view, bounds);
        return bounds;
    }

    private String label(int layout, int[] size) {
        return RuntimeEnvironment.getApplication().getResources().getResourceEntryName(layout)
                + " at " + size[0] + "x" + size[1];
    }
}
