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
