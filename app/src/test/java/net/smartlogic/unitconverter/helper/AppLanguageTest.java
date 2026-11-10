package net.smartlogic.unitconverter.helper;

import android.app.Application;
import android.app.LocaleManager;
import android.os.Bundle;
import android.os.LocaleList;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import net.smartlogic.unitconverter.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** System per-app locale is authoritative, including before our first Settings visit. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = Application.class)
public class AppLanguageTest {
    public static class LanguageActivity extends AppCompatActivity {
        @Override protected void onCreate(Bundle state) {
            setTheme(R.style.AppTheme);
            super.onCreate(state);
            AppLanguage.initializeAfterActivityCreated(this);
        }
    }

    @Test public void firstInitializationKeepsLanguageChosenInAndroidSettings() {
        Application app = RuntimeEnvironment.getApplication();
        Preferences prefs = Preferences.getInstance(app);
        prefs.getPreferences().edit().clear().commit();
        LocaleManager manager = app.getSystemService(LocaleManager.class);
        try (ActivityController<LanguageActivity> controller = Robolectric.buildActivity(LanguageActivity.class)) {
            // Robolectric attachment resets its framework LocaleManager; inject the OS choice
            // after attachment, before app onCreate/init executes.
            manager.setApplicationLocales(LocaleList.forLanguageTags("fr-CA"));
            assertEquals("fr-CA", manager.getApplicationLocales().toLanguageTags());
            controller.setup();
            assertEquals("fr-CA", AppCompatDelegate.getApplicationLocales().toLanguageTags());
            assertEquals("fr-CA", AppLanguage.current(controller.get()).tag);
            assertEquals("fr-CA", prefs.getPreferences().getString("regional_observed_translation", ""));
            assertEquals("US", prefs.getRegionalProfile().region.code);
        } finally {
            manager.setApplicationLocales(LocaleList.getEmptyLocaleList());
        }
    }

    @Test @Config(sdk = 28) public void freshLanguageDefaultDoesNotFollowDeviceLanguage() {
        RuntimeEnvironment.setQualifiers("hi-rIN");
        Application app = RuntimeEnvironment.getApplication();
        Preferences prefs = Preferences.getInstance(app);
        prefs.getPreferences().edit().clear().commit();
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        try (ActivityController<LanguageActivity> controller = Robolectric.buildActivity(LanguageActivity.class).setup()) {
            assertEquals("en-US", AppCompatDelegate.getApplicationLocales().toLanguageTags());
            assertEquals("en-US", AppLanguage.current(controller.get()).tag);
            assertEquals("US", prefs.getRegionalProfile().region.code);
            assertEquals("USD", prefs.getRegionalProfile().currency);
        } finally {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        }
    }
}
