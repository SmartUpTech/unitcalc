package net.smartlogic.unitconverter.fragment;

import android.app.Application;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.fragment.app.FragmentActivity;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.helper.CurrencyRateStore;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.model.Currency;
import java.util.ArrayList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
public class CurrencyConverterFragmentTest {
    public static class OfflineCurrencyFragment extends CurrencyConverterFragment {
        @Override public void refreshCurrencyRates(boolean forceRefresh) { }
    }

    @Test public void firstVisitDefaultsToUsdEurInrAndLaterVisitsKeepAllUserSelections() {
        Preferences prefs = Preferences.getInstance(RuntimeEnvironment.getApplication());
        prefs.getPreferences().edit().clear().commit();
        for (int visit = 0; visit < 2; visit++) {
            var controller = Robolectric.buildActivity(FragmentActivity.class);
            controller.get().setTheme(R.style.AppTheme);
            controller.setup();
            try {
                FragmentActivity activity = controller.get();
                FrameLayout container = new FrameLayout(activity);
                container.setId(12345);
                activity.setContentView(container);
                CurrencyConverterFragment fragment = new OfflineCurrencyFragment();
                activity.getSupportFragmentManager().beginTransaction().add(12345, fragment).commitNow();
                String[] expected = visit == 0 ? new String[]{"USD", "EUR", "INR"}
                        : new String[]{"INR", "USD", "EUR"};
                int[] labels = {R.id.fromCurrencyISO, R.id.toCurrencyISO, R.id.additionalToCurrencyISO};
                for (int i = 0; i < labels.length; i++) {
                    assertEquals(expected[i], ((TextView) fragment.requireView().findViewById(labels[i])).getText().toString());
                }
                if (visit == 0) {
                    ArrayList<Currency> currencies = ReflectionHelpers.getField(fragment, "currencies");
                    fragment.onChooseCurrency(currencies.get(11), "FROM");
                    fragment.onChooseCurrency(currencies.get(31), "TO");
                    fragment.onChooseCurrency(currencies.get(32), "ADDITIONAL");
                    assertEquals(11, prefs.getFromCurrencyIndex());
                    assertEquals(31, prefs.getToCurrencyIndex());
                    assertEquals(32, prefs.getAdditionalCurrencyIndex());
                }
            } finally {
                controller.pause().stop().destroy();
            }
        }
    }

    @Test public void expandedCountryListHasResolvableFlagsAndPreservesLegacySelections() {
        CurrencyConverterFragment fragment = new CurrencyConverterFragment();
        var controller = Robolectric.buildActivity(FragmentActivity.class);
        controller.get().setTheme(R.style.AppTheme);
        controller.setup();
        // Localized currency labels require an attached Fragment context.
        controller.get().getSupportFragmentManager().beginTransaction().add(fragment, "currency-catalog").commitNow();
        fragment.initCurrencies();
        ArrayList<Currency> currencies = ReflectionHelpers.getField(fragment, "currencies");
        assertEquals(100, currencies.size());
        java.util.Set<String> countries = new java.util.HashSet<>();
        java.util.Set<String> codes = new java.util.HashSet<>();
        for (Currency currency : currencies) {
            assertFalse("Missing symbol: " + currency.getCurrencyISOCode(), currency.getCurrencySymbol().isEmpty());
            assertTrue("Duplicate country: " + currency.getCountry(), countries.add(currency.getCountry()));
            codes.add(currency.getCurrencyISOCode());
            assertNotNull(RuntimeEnvironment.getApplication().getDrawable(currency.getFlagImageResource()));
            assertFalse(currency.getCurrencyName().isEmpty());
        }
        assertEquals(98, codes.size()); // Three countries use EUR.
        assertFalse(codes.contains("BGN"));
        assertFalse(codes.contains("HRK"));
        assertEquals("USD", currencies.get(31).getCurrencyISOCode());
        assertEquals("INR", currencies.get(11).getCurrencyISOCode());
        assertEquals("EUR", currencies.get(32).getCurrencyISOCode());
        assertEquals("EUR", currencies.get(2).getCurrencyISOCode());
        assertEquals("€", currencies.get(2).getCurrencySymbol());
        assertEquals("EUR", currencies.get(5).getCurrencyISOCode());
        assertEquals("€", currencies.get(5).getCurrencySymbol());
        assertEquals(R.drawable.hr, currencies.get(5).getFlagImageResource());
        assertEquals(R.drawable.de, currencies.get(32).getFlagImageResource());
        assertEquals("AED", currencies.get(33).getCurrencyISOCode());
        controller.pause().stop().destroy();
    }

    @Test public void converterStartupExposesOnlyTheSelectedChildAndSwitchesCleanly() throws Exception {
        Preferences prefs = Preferences.getInstance(RuntimeEnvironment.getApplication());
        prefs.getPreferences().edit().clear().commit();
        String response = "[{\"date\":\"2026-10-20\",\"base\":\"USD\",\"quote\":\"INR\",\"rate\":80}]";
        long now = System.currentTimeMillis();
        prefs.setCurrencySnapshot(CurrencyRateStore.parseResponse(response, now).encode(), now);
        net.smartlogic.unitconverter.app.AppConst.CONVERTER_TAB = ConverterFragment.TAB_UNIT;
        var controller = Robolectric.buildActivity(FragmentActivity.class);
        controller.get().setTheme(R.style.AppTheme);
        controller.setup();
        FragmentActivity activity = controller.get();
        FrameLayout container = new FrameLayout(activity);
        container.setId(12345);
        activity.setContentView(container);
        ConverterFragment fragment = new ConverterFragment();
        activity.getSupportFragmentManager().beginTransaction().add(12345, fragment).commitNow();
        assertEquals(1, fragment.getChildFragmentManager().getFragments().stream().filter(f -> !f.isHidden()).count());
        assertTrue(fragment.getChildFragmentManager().findFragmentByTag("converter_currency").isHidden());
        var unit = fragment.getChildFragmentManager().findFragmentByTag("converter_unit");
        ((EditText) unit.requireView().findViewById(R.id.input)).setText("12");
        unit.requireView().findViewById(R.id.ac).performClick();
        assertEquals("", ((EditText) unit.requireView().findViewById(R.id.input)).getText().toString());
        assertEquals("", ((EditText) unit.requireView().findViewById(R.id.output)).getText().toString());
        ReflectionHelpers.callInstanceMethod(fragment, "showConverterTab",
                ReflectionHelpers.ClassParameter.from(int.class, ConverterFragment.TAB_CURRENCY));
        fragment.getChildFragmentManager().executePendingTransactions();
        assertTrue(fragment.getChildFragmentManager().findFragmentByTag("converter_unit").isHidden());
        assertFalse(fragment.getChildFragmentManager().findFragmentByTag("converter_currency").isHidden());
        controller.pause().stop().destroy();
    }

    @Test public void cachedRatesConvertImmediatelyAndUnsupportedSelectionClearsPreviousResult() throws Exception {
        Preferences prefs = Preferences.getInstance(RuntimeEnvironment.getApplication());
        prefs.getPreferences().edit().clear().commit();
        String response = "[{\"date\":\"2026-10-20\",\"base\":\"USD\",\"quote\":\"INR\",\"rate\":80},"
                + "{\"date\":\"2026-10-20\",\"base\":\"USD\",\"quote\":\"AED\",\"rate\":4}]";
        long now = System.currentTimeMillis();
        prefs.setCurrencySnapshot(CurrencyRateStore.parseResponse(response, now).encode(), now);
        prefs.setFromCurrencyIndex(31); // Existing persisted USD / INR positions.
        prefs.setToCurrencyIndex(11);
        var controller = Robolectric.buildActivity(FragmentActivity.class);
        controller.get().setTheme(R.style.AppTheme);
        controller.setup();
        FragmentActivity activity = controller.get();
        FrameLayout container = new FrameLayout(activity);
        container.setId(12345);
        activity.setContentView(container);
        CurrencyConverterFragment fragment = new CurrencyConverterFragment();
        activity.getSupportFragmentManager().beginTransaction().add(12345, fragment).commitNow();
        EditText input = fragment.requireView().findViewById(R.id.input);
        EditText output = fragment.requireView().findViewById(R.id.output);
        input.setText("2");
        assertEquals("160", output.getText().toString());
        ArrayList<Currency> extraCurrencies = ReflectionHelpers.getField(fragment, "currencies");
        EditText additional = fragment.requireView().findViewById(R.id.additionalOutput);
        fragment.onChooseCurrency(extraCurrencies.get(33), "ADDITIONAL");
        assertEquals("8", additional.getText().toString());
        assertEquals("160", output.getText().toString());
        fragment.requireView().findViewById(R.id.reverse_destinations).performClick();
        assertEquals("8", output.getText().toString());
        assertEquals("160", additional.getText().toString());
        assertEquals(33, prefs.getToCurrencyIndex());
        assertEquals(11, prefs.getAdditionalCurrencyIndex());
        assertEquals(31, prefs.getFromCurrencyIndex());
        assertEquals("2", input.getText().toString());
        fragment.requireView().findViewById(R.id.reverse_destinations).performClick();
        assertEquals("160", output.getText().toString());
        assertEquals("8", additional.getText().toString());
        input.setText("-2");
        assertEquals("-8", additional.getText().toString());
        input.setText("-");
        assertEquals("", additional.getText().toString());
        input.setText("2");
        fragment.onChooseCurrency(extraCurrencies.get(5), "ADDITIONAL");
        assertEquals("", additional.getText().toString());
        fragment.onChooseCurrency(extraCurrencies.get(31), "ADDITIONAL");
        assertEquals("2", additional.getText().toString());
        assertEquals("$", ((TextView) fragment.requireView().findViewById(R.id.inputSymbol)).getText().toString());
        assertEquals("₹", ((TextView) fragment.requireView().findViewById(R.id.outputSymbol)).getText().toString());
        assertEquals(android.view.View.VISIBLE, fragment.requireView().findViewById(R.id.inputSymbol).getVisibility());
        assertTrue(fragment.requireView().findViewById(R.id.inputSymbol).getParent()
                == input.getParent());
        fragment.requireView().findViewById(R.id.minus).performClick();
        assertEquals("-2", input.getText().toString());
        assertEquals("-160", output.getText().toString());
        fragment.requireView().findViewById(R.id.minus).performClick();
        assertEquals("160", output.getText().toString());
        fragment.requireView().findViewById(R.id.ac).performClick();
        assertEquals("", input.getText().toString());
        assertEquals("", output.getText().toString());
        input.setText("2");
        ArrayList<Currency> currencies = ReflectionHelpers.getField(fragment, "currencies");
        fragment.onChooseCurrency(currencies.get(33), "TO"); // USD -> newly added AED.
        assertEquals("8", output.getText().toString());
        fragment.onChooseCurrency(currencies.get(33), "FROM"); // Same currency remains identity.
        assertEquals("2", output.getText().toString());
        fragment.onChooseCurrency(currencies.get(11), "TO"); // AED -> INR via USD base.
        assertEquals("40", output.getText().toString());
        fragment.onChooseCurrency(currencies.get(31), "FROM");
        fragment.onChooseCurrency(currencies.get(5), "TO"); // Croatia uses EUR; this fixture has no EUR rate.
        assertEquals("", output.getText().toString());
        assertEquals(activity.getString(R.string.currency_unavailable),
                ((TextView) fragment.requireView().findViewById(R.id.txtRate)).getText().toString());
        assertEquals(5, prefs.getToCurrencyIndex());
        activity.getSupportFragmentManager().beginTransaction().detach(fragment).commitNow();
        activity.getSupportFragmentManager().beginTransaction().attach(fragment).commitNow();
        assertEquals("EUR", ((TextView) fragment.requireView().findViewById(R.id.toCurrencyISO)).getText().toString());
        assertEquals("€", ((TextView) fragment.requireView().findViewById(R.id.outputSymbol)).getText().toString());
        assertEquals("", ((EditText) fragment.requireView().findViewById(R.id.output)).getText().toString());
        controller.pause().stop().destroy();
    }
}
