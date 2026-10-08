package net.smartlogic.unitconverter.widget;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.adapter.UnitAdapter;
import net.smartlogic.unitconverter.model.Conversion;
import net.smartlogic.unitconverter.theme.ThemeApplier;
import net.smartlogic.unitconverter.utils.Conversions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

/** Real Android measurements of inflated production layouts, after persistent chrome. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CalculationLayoutTest {
    private static final int[] SCREENS = {R.layout.pane_calculator_calculate,
            R.layout.fragment_unit_converter, R.layout.fragment_currency_converter};

    @Test public void normalWorkspacesKeepEveryPrimaryControlVisible() {
        for (int layout : SCREENS) {
            for (int[] size : new int[][]{{320, 400}, {360, 460}, {412, 600}, {600, 340}, {840, 700}}) {
                View root = inflate(layout, 1f);
                measure(root, size[0], size[1]);
                CalculationViewport viewport = viewport(root);
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

    @Test public void extremeFontScalingFallsBackWithoutCrushingKeysAndRecoversOnResize() {
        for (int layout : SCREENS) {
            View root = inflate(layout, 2f);
            measure(root, 320, 240);
            assertTrue(viewport(root).canScrollVertically(1));
            assertKeys(root, false);
            CalculationViewport viewport = viewport(root);
            viewport.scrollTo(0, viewport.getChildAt(0).getHeight());
            assertTrue(viewport.getScrollY() > 0);
            measure(root, 840, 1100);
            assertFalse(viewport.canScrollVertically(1));
            assertEquals(0, viewport.getScrollY());
            assertKeys(root, true);
        }
    }

    @Test public void compactLayoutRestoresAfterRepeatedWindowChanges() {
        for (int layout : SCREENS) {
            View root = inflate(layout, 1f);
            for (int[] size : new int[][]{{360, 460}, {840, 700}, {600, 340}, {360, 460}}) {
                measure(root, size[0], size[1]);
                assertFalse(label(layout, size), viewport(root).canScrollVertically(1));
                assertKeys(root, true);
            }
        }
    }

    private View inflate(int layout, float scale) {
        Context app = RuntimeEnvironment.getApplication();
        Configuration config = new Configuration(app.getResources().getConfiguration());
        config.fontScale = scale;
        Context context = new ContextThemeWrapper(app.createConfigurationContext(config), R.style.AppTheme);
        View root = LayoutInflater.from(context).inflate(layout, null);
        if (root.findViewById(R.id.input) != null) {
            ((EditText) root.findViewById(R.id.input)).setShowSoftInputOnFocus(false);
            ((TextView) root.findViewById(R.id.input)).setText("100");
            ((TextView) root.findViewById(R.id.output)).setText("328.084");
        }
        if (root.findViewById(R.id.fromUnit) instanceof Spinner) bindUnits(root, Conversion.LENGTH);
        if (root.findViewById(R.id.fromCurrencyISO) != null) {
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
        ((TextView) root.findViewById(R.id.category_title)).setText(conversion.getLabelResource());
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
        ViewGroup keys = root.findViewById(R.id.keypad);
        assertKeyChildren((ViewGroup) root, keys, inside);
    }

    private void assertKeyChildren(ViewGroup root, View view, boolean inside) {
        if (view.getVisibility() == View.GONE) return;
        if (view instanceof android.widget.Button || view instanceof android.widget.ImageButton) {
            assertTrue("key height " + view.getId() + ": " + view.getHeight(), view.getHeight() >= 48);
            assertTrue("key width " + view.getId(), view.getWidth() >= 48);
            if (inside) assertInside(root, view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) assertKeyChildren(root, group.getChildAt(i), inside);
        }
    }

    private void assertInside(ViewGroup root, View view) {
        Rect bounds = new Rect(0, 0, view.getWidth(), view.getHeight());
        root.offsetDescendantRectToMyCoords(view, bounds);
        assertTrue("empty control " + view.getId(), bounds.width() > 0 && bounds.height() > 0);
        assertTrue("outside viewport " + bounds, bounds.left >= 0 && bounds.top >= 0
                && bounds.right <= root.getWidth() && bounds.bottom <= root.getHeight());
    }

    private String label(int layout, int[] size) { return layout + " at " + size[0] + "x" + size[1]; }
}
