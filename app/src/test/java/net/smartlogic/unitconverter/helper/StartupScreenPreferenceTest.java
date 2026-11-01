package net.smartlogic.unitconverter.helper;

import android.app.Application;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 35)
public class StartupScreenPreferenceTest {
    @Test public void defaultsAndInvalidStoredValuesOpenCalculator() {
        Preferences preferences = Preferences.getInstance(RuntimeEnvironment.getApplication());
        preferences.getPreferences().edit().remove(Preferences.PREFS_STARTUP_SCREEN).commit();
        assertEquals(CalculatorCatalog.ID_BASIC, preferences.getStartupScreen());
        preferences.getPreferences().edit().putString(Preferences.PREFS_STARTUP_SCREEN, "removed_tool").commit();
        assertEquals(CalculatorCatalog.ID_BASIC, preferences.getStartupScreen());
        preferences.getPreferences().edit().putInt(Preferences.PREFS_STARTUP_SCREEN, 7).commit();
        assertEquals(CalculatorCatalog.ID_BASIC, preferences.getStartupScreen());
    }

    @Test public void converterChoicesPersistAndInvalidUpdatesPreserveSelection() {
        Preferences preferences = Preferences.getInstance(RuntimeEnvironment.getApplication());
        for (String id : new String[]{CalculatorCatalog.ID_UNIT, CalculatorCatalog.ID_CURRENCY,
                CalculatorCatalog.ID_BASIC}) {
            preferences.setStartupScreen(id);
            assertEquals(id, preferences.getPreferences().getString(Preferences.PREFS_STARTUP_SCREEN, null));
            preferences.setStartupScreen("removed_tool");
            assertEquals(id, preferences.getStartupScreen());
        }
    }
}
