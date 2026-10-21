package net.smartlogic.unitconverter.fragment;

import android.os.Bundle;
import android.app.Application;
import android.os.Looper;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.model.Conversion;
import net.smartlogic.unitconverter.model.Unit;
import net.smartlogic.unitconverter.theme.CalculatorTheme;
import net.smartlogic.unitconverter.theme.CalculatorThemes;
import net.smartlogic.unitconverter.theme.ThemeManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.util.ReflectionHelpers;
import java.util.ArrayList;
import java.util.Arrays;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34, qualifiers = "w360dp-h720dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ConverterPickerDialogTest {
    private ArrayList<String> values(String... values) { return new ArrayList<>(Arrays.asList(values)); }

    private ActivityController<AppCompatActivity> activity() {
        ActivityController<AppCompatActivity> controller = Robolectric.buildActivity(AppCompatActivity.class);
        controller.get().setTheme(R.style.AppTheme);
        return controller.create().start().resume().visible();
    }

    private Fragment host(AppCompatActivity activity) {
        Fragment host = new Fragment();
        activity.getSupportFragmentManager().beginTransaction().add(host, "host").commitNow();
        return host;
    }

    private ConverterPickerDialogFragment unitDialog() {
        return ConverterPickerDialogFragment.newInstance("Length - Select From Value", true, 4, 0,
                values("Kilometre (km)", "Miles (mi)", "Metre (m)"), values("", "", ""),
                values("km", "mi", "m"), null);
    }

    @Test public void searchAndSelectionUseOriginalIndexAndNoResultsCanRecover() {
        try (ActivityController<AppCompatActivity> controller = activity()) {
            Fragment host = host(controller.get());
            Bundle[] result = new Bundle[1];
            host.getChildFragmentManager().setFragmentResultListener(ConverterPickerDialogFragment.RESULT_KEY,
                    host, (key, value) -> result[0] = value);
            ConverterPickerDialogFragment dialog = unitDialog();
            dialog.showFor(host);
            assertEquals("Length - Select From Value", ((TextView) dialog.requireDialog()
                    .findViewById(R.id.converter_picker_title)).getText().toString());
            EditText search = dialog.requireDialog().findViewById(R.id.converter_picker_search);
            ListView list = dialog.requireDialog().findViewById(R.id.converter_picker_list);
            search.setText("nothing matches");
            assertEquals(0, list.getCount());
            assertEquals(android.view.View.VISIBLE, dialog.requireDialog()
                    .findViewById(R.id.converter_picker_empty).getVisibility());
            search.setText("");
            assertEquals(3, list.getCount());
            search.setText("MI");
            assertEquals(1, list.getCount());
            list.performItemClick(list.getAdapter().getView(0, null, list), 0, list.getAdapter().getItemId(0));
            assertNotNull(result[0]);
            assertEquals(1, result[0].getInt(ConverterPickerDialogFragment.INDEX));
            assertTrue(result[0].getBoolean(ConverterPickerDialogFragment.SOURCE));
            assertEquals(4, result[0].getInt(ConverterPickerDialogFragment.GROUP));
        }
    }

    @Test public void searchRestoresAfterRecreationAndDuplicateOpenIsIgnored() {
        try (ActivityController<AppCompatActivity> controller = activity()) {
            Fragment host = host(controller.get());
            unitDialog().showFor(host);
            unitDialog().showFor(host);
            assertEquals(1, host.getChildFragmentManager().getFragments().size());
            ConverterPickerDialogFragment original = (ConverterPickerDialogFragment)
                    host.getChildFragmentManager().findFragmentByTag(ConverterPickerDialogFragment.TAG);
            ((EditText) original.requireDialog().findViewById(R.id.converter_picker_search)).setText("kilo");
            controller.recreate();
            shadowOf(Looper.getMainLooper()).idle();
            Fragment restoredHost = controller.get().getSupportFragmentManager().findFragmentByTag("host");
            ConverterPickerDialogFragment restored = (ConverterPickerDialogFragment) restoredHost
                    .getChildFragmentManager().findFragmentByTag(ConverterPickerDialogFragment.TAG);
            assertEquals("kilo", ((EditText) restored.requireDialog()
                    .findViewById(R.id.converter_picker_search)).getText().toString());
            assertEquals(1, ((ListView) restored.requireDialog().findViewById(R.id.converter_picker_list)).getCount());
        }
    }

    @Test public void currencySearchesCodesCountriesAndSymbolsAndFlagsAreSquare() {
        try (ActivityController<AppCompatActivity> controller = activity()) {
            Fragment host = host(controller.get());
            ConverterPickerDialogFragment dialog = ConverterPickerDialogFragment.newInstance(
                    "Currency - Select To Value", false, 0, 1, values("US dollar", "Indian rupee"),
                    values("USD", "INR"), values("United States $", "India ₹"), new int[]{R.drawable.us, R.drawable.in});
            dialog.showFor(host);
            EditText search = dialog.requireDialog().findViewById(R.id.converter_picker_search);
            ListView list = dialog.requireDialog().findViewById(R.id.converter_picker_list);
            for (String query : new String[]{"INR", "india", "₹"}) {
                search.setText(query);
                assertEquals(1, list.getCount());
                android.view.View row = list.getAdapter().getView(0, null, list);
                ImageView flag = row.findViewById(R.id.converter_picker_flag);
                assertEquals(flag.getLayoutParams().width, flag.getLayoutParams().height);
                assertEquals(android.view.View.VISIBLE, row.findViewById(R.id.converter_picker_check).getVisibility());
            }
            assertEquals("metre", ConverterPickerDialogFragment.normalize("MÈTRE"));
        }
    }

    public static class OfflineCurrencyFragment extends CurrencyConverterFragment {
        @Override public void refreshCurrencyRates(boolean forceRefresh) { }
    }

    @Test public void allFourRealDropdownsUseTheDialogAndKeepInputAndPreferences() {
        try (ActivityController<AppCompatActivity> controller = activity()) {
            Preferences prefs = Preferences.getInstance(controller.get());
            prefs.setLastConversion(Conversion.LENGTH);
            prefs.setLastFromConversion(0);
            prefs.setLastToConversion(1);
            UnitConverterFragment unit = new UnitConverterFragment();
            controller.get().getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, unit, "unit").commitNow();
            ((EditText) unit.requireView().findViewById(R.id.input)).setText("12");
            for (boolean source : new boolean[]{true, false}) {
                android.widget.Spinner spinner = unit.requireView().findViewById(source ? R.id.fromUnit : R.id.toUnit);
                spinner.performClick();
                choose(unit, "Metre (m)", source ? "Length - Select From Value" : "Length - Select To Value");
                shadowOf(Looper.getMainLooper()).idle();
                assertEquals(Unit.METRE, ((Unit) spinner.getSelectedItem()).getId());
                assertEquals(spinner.getSelectedItemPosition(), source ? prefs.getLastFromConversion() : prefs.getLastToConversion());
                assertEquals("12", ((EditText) unit.requireView().findViewById(R.id.input)).getText().toString());
            }
            unit.requireView().findViewById(R.id.fromUnit).performClick();
            choose(unit, "Feet (ft)", "Length - Select From Value");
            unit.requireView().findViewById(R.id.toUnit).performClick();
            choose(unit, "Picometre (pm)", "Length - Select To Value");
            shadowOf(Looper.getMainLooper()).idle();
            EditText unitInput = unit.requireView().findViewById(R.id.input);
            EditText unitOutput = unit.requireView().findViewById(R.id.output);
            unitInput.setText("99999");
            android.view.View unitRoot = unit.requireView();
            unitRoot.measure(android.view.View.MeasureSpec.makeMeasureSpec(360, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(460, android.view.View.MeasureSpec.EXACTLY));
            unitRoot.layout(0, 0, 360, 460);
            assertEquals(30479695200000000d, Double.parseDouble(unitOutput.getText().toString()), 4d);
            assertEquals(unit.getResources().getDimension(R.dimen.converter_value_text_size), unitInput.getTextSize(), 0.1f);
            assertTrue(unitOutput.getTextSize() < unitInput.getTextSize());
            OfflineCurrencyFragment currency = new OfflineCurrencyFragment();
            controller.get().getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, currency, "currency").commitNow();
            ((EditText) currency.requireView().findViewById(R.id.input)).setText("12");
            for (boolean source : new boolean[]{true, false}) {
                currency.requireView().findViewById(source ? R.id.fromUnit : R.id.toUnit).performClick();
                String code = source ? "INR" : "USD";
                choose(currency, code, source ? "Currency - Select From Value" : "Currency - Select To Value");
                assertEquals(code, ((TextView) currency.requireView().findViewById(source
                        ? R.id.fromCurrencyISO : R.id.toCurrencyISO)).getText().toString());
                assertEquals("12", ((EditText) currency.requireView().findViewById(R.id.input)).getText().toString());
            }
        }
    }

    private void choose(Fragment host, String query, String title) {
        ConverterPickerDialogFragment dialog = (ConverterPickerDialogFragment) host.getChildFragmentManager()
                .findFragmentByTag(ConverterPickerDialogFragment.TAG);
        assertNotNull(dialog);
        assertEquals(title, ((TextView) dialog.requireDialog().findViewById(R.id.converter_picker_title)).getText().toString());
        ((EditText) dialog.requireDialog().findViewById(R.id.converter_picker_search)).setText(query);
        ListView list = dialog.requireDialog().findViewById(R.id.converter_picker_list);
        assertEquals(1, list.getCount());
        list.performItemClick(list.getAdapter().getView(0, null, list), 0, list.getAdapter().getItemId(0));
        host.getChildFragmentManager().executePendingTransactions();
    }

    @Test public void lightAndDarkDialogsRenderAndRetainAListWhenHeightIsConstrained() throws Exception {
        CalculatorTheme original = ThemeManager.get();
        try {
            for (CalculatorTheme theme : new CalculatorTheme[]{CalculatorThemes.TITANIUM_GRAY, CalculatorThemes.GRAPHITE_BLACK}) {
                ReflectionHelpers.setStaticField(ThemeManager.class, "current", theme);
                for (boolean currency : new boolean[]{false, true}) {
                    try (ActivityController<AppCompatActivity> controller = activity()) {
                        Fragment host = host(controller.get());
                        ConverterPickerDialogFragment dialog = currency
                                ? ConverterPickerDialogFragment.newInstance("Currency - Select To Value", false, 0, 1,
                                values("US dollar", "Euro", "Indian rupee", "British pound", "Japanese yen", "Canadian dollar"),
                                values("USD", "EUR", "INR", "GBP", "JPY", "CAD"),
                                values("United States $", "European Union €", "India ₹", "United Kingdom £", "Japan ¥", "Canada $"),
                                new int[]{R.drawable.us, R.drawable.eu, R.drawable.in, R.drawable.gb, R.drawable.jp, R.drawable.ca})
                                : unitDialog();
                        dialog.showFor(host);
                        android.view.View decor = dialog.requireDialog().getWindow().getDecorView();
                        int width = dialog.requireDialog().getWindow().getAttributes().width;
                        int height = dialog.requireDialog().getWindow().getAttributes().height;
                        decor.measure(android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                                android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY));
                        decor.layout(0, 0, width, height);
                        // TextInputLayout reserves its start icon after the first layout.
                        shadowOf(Looper.getMainLooper()).idle();
                        decor.forceLayout();
                        decor.measure(android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                                android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY));
                        decor.layout(0, 0, width, height);
                        TextView title = dialog.requireDialog().findViewById(R.id.converter_picker_title);
                        assertEquals(theme.mainText(), title.getCurrentTextColor());
                        ListView list = dialog.requireDialog().findViewById(R.id.converter_picker_list);
                        assertTrue(list.getHeight() >= 48);
                        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888);
                        decor.draw(new android.graphics.Canvas(bitmap));
                        java.io.File directory = new java.io.File("build/reports/converter-picker");
                        assertTrue(directory.isDirectory() || directory.mkdirs());
                        try (java.io.FileOutputStream out = new java.io.FileOutputStream(new java.io.File(directory,
                                (currency ? "currency-" : "unit-") + theme.id() + ".png"))) {
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
                        }
                        bitmap.recycle();
                        decor.measure(android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                                android.view.View.MeasureSpec.makeMeasureSpec(300, android.view.View.MeasureSpec.EXACTLY));
                        decor.layout(0, 0, width, 300);
                        assertTrue(list.getHeight() >= 48);
                    }
                }
            }
        } finally {
            ReflectionHelpers.setStaticField(ThemeManager.class, "current", original);
        }
    }

    @Test @Config(qualifiers = "w320dp-h480dp-mdpi")
    public void largerFontsKeepTitleSearchAndListUsableInAConstrainedWindow() {
        try (ActivityController<AppCompatActivity> controller = activity()) {
            android.content.res.Resources resources = controller.get().getResources();
            android.content.res.Configuration config = new android.content.res.Configuration(resources.getConfiguration());
            config.fontScale = 1.3f;
            resources.updateConfiguration(config, resources.getDisplayMetrics());
            Fragment host = host(controller.get());
            ConverterPickerDialogFragment dialog = unitDialog();
            dialog.showFor(host);
            android.view.View decor = dialog.requireDialog().getWindow().getDecorView();
            decor.measure(android.view.View.MeasureSpec.makeMeasureSpec(280, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(300, android.view.View.MeasureSpec.EXACTLY));
            decor.layout(0, 0, 280, 300);
            TextView title = dialog.requireDialog().findViewById(R.id.converter_picker_title);
            EditText search = dialog.requireDialog().findViewById(R.id.converter_picker_search);
            ListView list = dialog.requireDialog().findViewById(R.id.converter_picker_list);
            assertTrue(title.getTextSize() > 20f);
            assertTrue(title.getHeight() >= title.getLineHeight());
            assertTrue(search.getHeight() >= 48);
            assertTrue(list.getHeight() >= 48);
        }
    }
}
