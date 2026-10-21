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
        String response = "[{\"date\":\"2026-10-20\",\"base\":\"USD\",\"quote\":\"INR\",\"rate\":80}]";
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
        fragment.onChooseCurrency(currencies.get(5), "TO"); // HRK has no current rate.
        assertEquals("", output.getText().toString());
        assertEquals(activity.getString(R.string.currency_unavailable),
                ((TextView) fragment.requireView().findViewById(R.id.txtRate)).getText().toString());
        assertEquals(5, prefs.getToCurrencyIndex());
        activity.getSupportFragmentManager().beginTransaction().detach(fragment).commitNow();
        activity.getSupportFragmentManager().beginTransaction().attach(fragment).commitNow();
        assertEquals("HRK", ((TextView) fragment.requireView().findViewById(R.id.toCurrencyISO)).getText().toString());
        assertEquals("", ((EditText) fragment.requireView().findViewById(R.id.output)).getText().toString());
        controller.pause().stop().destroy();
    }
}
