package net.smartlogic.unitconverter;
import net.smartlogic.unitconverter.calculator.finance.emi.EmiFormatter;
import net.smartlogic.unitconverter.calculator.tools.fuel.FuelEfficiencyCalculatorFragment;
import net.smartlogic.unitconverter.calculator.tools.fuel.FuelEfficiencyLayout;
import net.smartlogic.unitconverter.calculator.tools.fuel.FuelHistoryState;
import net.smartlogic.unitconverter.calculator.tools.percentage.PercentageCalculatorFragment;
import net.smartlogic.unitconverter.calculator.tools.percentage.PercentageLayout;

import android.app.Application;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.smartlogic.unitconverter.helper.Preferences;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ToolReviewRegressionTest {
    public static class Host extends FragmentActivity {
        @Override public void onCreate(android.os.Bundle state) {
            setTheme(R.style.AppTheme);
            super.onCreate(state);
            FrameLayout content = new FrameLayout(this);
            content.setId(android.R.id.content);
            setContentView(content);
        }
    }

    private Host open(Fragment fragment) {
        Host host = Robolectric.buildActivity(Host.class).setup().get();
        Preferences.getInstance(host).setFuelEfficiencyState("");
        Preferences.getInstance(host).setPercentageState("");
        host.getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commitNow();
        layout(host);
        return host;
    }

    private void layout(Host host) {
        View root = host.findViewById(android.R.id.content);
        root.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(700, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 400, 700);
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    }

    private void preview(Host host, String name) {
        layout(host);
        View root = host.findViewById(android.R.id.content);
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(400, 700,
                android.graphics.Bitmap.Config.ARGB_8888);
        root.draw(new android.graphics.Canvas(bitmap));
        java.io.File file = new java.io.File("build/reports/tool-review/" + name + ".png");
        file.getParentFile().mkdirs();
        try (var out = new java.io.FileOutputStream(file)) {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
        } catch (java.io.IOException error) {
            throw new AssertionError(error);
        }
    }

    @Test public void fuelRoutesAllKeysRejectsPasteAndBackClosesKeypad() {
        FuelEfficiencyCalculatorFragment fragment = new FuelEfficiencyCalculatorFragment();
        Host host = open(fragment);
        assertTrue(((FuelEfficiencyLayout) host.findViewById(R.id.fuel_root)).contentFits());
        int[] fields = {R.id.fe_distance_input, R.id.fe_efficiency_input, R.id.fe_price_input, R.id.fe_passengers_input};
        for (int id : fields) {
            EditText field = host.findViewById(id);
            assertNotNull(field);
            field.setText("2");
            field.performClick();
            field.setSelection(1);
            host.findViewById(R.id.three).performClick();
            assertEquals("23", field.getText().toString());
            host.findViewById(R.id.backspace).performClick();
            assertEquals("2", field.getText().toString());
            host.findViewById(R.id.ac).performClick();
            assertEquals("", field.getText().toString());
        }
        EditText pax = host.findViewById(R.id.fe_passengers_input);
        pax.setText("4");
        pax.getText().replace(0, 1, "100");
        assertEquals("4", pax.getText().toString());
        layout(host);
        assertTrue(((FuelEfficiencyLayout) host.findViewById(R.id.fuel_root)).contentFits());
        host.getOnBackPressedDispatcher().onBackPressed();
        assertEquals(View.GONE, host.findViewById(R.id.fe_keypad_host).getVisibility());
        assertEquals(View.VISIBLE, host.findViewById(R.id.fe_results).getVisibility());
        assertFalse(host.isFinishing());
    }

    @Test public void percentageRoutesKeysAndRejectsWholeOversizedReplacement() {
        PercentageCalculatorFragment fragment = new PercentageCalculatorFragment();
        Host host = open(fragment);
        EditText first = host.findViewById(R.id.percentage_first_input);
        EditText second = host.findViewById(R.id.percentage_second_input);
        first.setText("250");
        second.setText("12");
        second.performClick();
        layout(host);
        assertTrue(((PercentageLayout) host.findViewById(R.id.percentage_root)).contentFits());
        second.setSelection(2);
        host.findViewById(R.id.three).performClick();
        assertEquals("123", second.getText().toString());
        assertEquals("250", first.getText().toString());
        second.getText().replace(0, 3, "1234567890123");
        assertEquals("123", second.getText().toString());
        second.getText().replace(0, 3, "1.2345");
        assertEquals("123", second.getText().toString());
        host.getOnBackPressedDispatcher().onBackPressed();
        assertEquals(View.GONE, host.findViewById(R.id.percentage_keypad_host).getVisibility());
        assertEquals(View.VISIBLE, host.findViewById(R.id.percentage_results).getVisibility());
        assertFalse(host.isFinishing());
    }

    @Test public void selectedModesRemainDistinctAcrossEveryPalette() {
        PercentageCalculatorFragment fragment = new PercentageCalculatorFragment();
        Host host = open(fragment);
        var group = (com.google.android.material.button.MaterialButtonToggleGroup)
                host.findViewById(R.id.percentage_mode_group);
        ((EditText) host.findViewById(R.id.percentage_first_input)).setText("250");
        ((EditText) host.findViewById(R.id.percentage_second_input)).setText("12");
        for (var theme : net.smartlogic.unitconverter.theme.CalculatorThemes.all()) {
            net.smartlogic.unitconverter.theme.ThemeManager.select(host, theme.id());
            for (int id : new int[]{R.id.percentage_mode_of, R.id.percentage_mode_increase,
                    R.id.percentage_mode_decrease, R.id.percentage_mode_what}) {
                group.check(id);
                net.smartlogic.unitconverter.theme.ThemeApplier.apply(group);
                var button = (com.google.android.material.button.MaterialButton) host.findViewById(id);
                assertTrue(button.isChecked());
                int checked = button.getBackgroundTintList().getColorForState(
                        new int[]{android.R.attr.state_enabled, android.R.attr.state_checked}, 0);
                int unchecked = button.getBackgroundTintList().getColorForState(
                        new int[]{android.R.attr.state_enabled}, 0);
                assertNotEquals(theme.id(), checked, unchecked);
            }
            assertEquals("250", ((EditText) host.findViewById(R.id.percentage_first_input)).getText().toString());
            preview(host, "percentage-" + theme.id());
        }
    }

    @Test public void historyCurrencySurvivesPreferenceChangeWithoutChangingPreference() {
        FuelEfficiencyCalculatorFragment fragment = new FuelEfficiencyCalculatorFragment();
        Host host = open(fragment);
        Preferences.getInstance(host).setPreferredCurrency("INR");
        FuelHistoryState saved = new FuelHistoryState("100", "10", "2", "4",
                net.smartlogic.unitconverter.model.Unit.KILOMETRE,
                net.smartlogic.unitconverter.model.Unit.L_100K, false, "USD");
        fragment.restoreFromHistory(null, saved.toPayload());
        String usd = new net.smartlogic.unitconverter.calculator.finance.emi.EmiFormatter(host).money(new java.math.BigDecimal("20"), "USD");
        assertEquals(usd, ((android.widget.TextView) host.findViewById(R.id.fe_trip_cost_value)).getText().toString());
        assertEquals("INR", Preferences.getInstance(host).getPreferredCurrency());
        assertEquals("USD", FuelHistoryState.fromDraft(Preferences.getInstance(host).getFuelEfficiencyState()).currencyCode);
        for (var theme : net.smartlogic.unitconverter.theme.CalculatorThemes.all()) {
            net.smartlogic.unitconverter.theme.ThemeManager.select(host, theme.id());
            preview(host, "fuel-" + theme.id());
            for (int id : new int[]{R.id.fe_distance_unit_label, R.id.fe_efficiency_unit_label, R.id.fe_price_symbol}) {
                var label = (android.widget.TextView) host.findViewById(id);
                assertTrue(label.length() > 0);
                assertEquals(theme.primaryContentColor(), label.getCurrentTextColor());
            }
            var toggle = (com.google.android.material.materialswitch.MaterialSwitch) host.findViewById(R.id.fe_round_trip);
            assertNotNull(toggle.getThumbDrawable());
            assertNotNull(toggle.getTrackDrawable());
        }
    }
}
