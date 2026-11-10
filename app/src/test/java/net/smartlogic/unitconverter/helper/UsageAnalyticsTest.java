package net.smartlogic.unitconverter.helper;

import android.app.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 35)
public class UsageAnalyticsTest {
    @Test public void allToolDimensionsAreCatalogBoundedAndConvertersCanBeRankedTogether() {
        for (var entry : net.smartlogic.unitconverter.model.CalculatorCatalog.getStartupEntries()) {
            var parameters = UsageAnalytics.toolParameters(entry.id);
            assertEquals(entry.id, parameters.getString("tool_id"));
            assertEquals(entry.id, parameters.getString("tool_family"));
            assertEquals(2, parameters.size());
        }
        for (var entry : net.smartlogic.unitconverter.model.CalculatorCatalog.getFavoriteEligibleEntries()) {
            if (entry.destination != net.smartlogic.unitconverter.model.CalculatorCatalog.Destination.UNIT_CATEGORY) continue;
            var parameters = UsageAnalytics.toolParameters(entry.id);
            assertEquals(entry.id, parameters.getString("tool_id"));
            assertEquals("unit_converter", parameters.getString("tool_family"));
        }
        assertNull(UsageAnalytics.toolParameters("settings"));
        assertNull(UsageAnalytics.toolParameters("history"));
        assertNull(UsageAnalytics.toolParameters("1234.56"));
    }
    @Test public void unconfiguredFirebaseIsOptionalAndConsentPersists() {
        var app = RuntimeEnvironment.getApplication();
        var prefs = Preferences.getInstance(app).getPreferences();
        prefs.edit().remove(UsageAnalytics.ENABLED).commit();
        UsageAnalytics.initialize(app);
        assertFalse(prefs.getBoolean(UsageAnalytics.ENABLED, true));
        UsageAnalytics.event("tool_opened", "tool_id", "basic");
        UsageAnalytics.setEnabled(app, true);
        assertTrue(prefs.getBoolean(UsageAnalytics.ENABLED, false));
        UsageAnalytics.initialize(app);
        assertTrue(prefs.getBoolean(UsageAnalytics.ENABLED, false));
        UsageAnalytics.setEnabled(app, false);
        UsageAnalytics.initialize(app);
        assertFalse(prefs.getBoolean(UsageAnalytics.ENABLED, true));
        UsageAnalytics.event("tool_opened", "tool_id", "basic");
    }
}
