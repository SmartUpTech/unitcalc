package net.smartlogic.unitconverter.helper;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;
import net.smartlogic.unitconverter.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Exercises Android's actual resource fallback, including the additional regional variants. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class SettingsLanguageResourcesTest {
    private Context language(String tag) {
        Context app = RuntimeEnvironment.getApplication();
        Configuration configuration = new Configuration(app.getResources().getConfiguration());
        configuration.setLocales(LocaleList.forLanguageTags(tag));
        return app.createConfigurationContext(configuration);
    }

    @Test public void everyOfferedLanguageResolvesSettingsAndDialogs() {
        Context english = language("en-US");
        int[] text = {R.string.title_activity_settings, R.string.appearance_reset,
                R.string.settings_privacy_choices, R.string.button_style_flat};
        for (SupportedLanguage choice : SupportedLanguage.ALL) {
            Context selected = language(choice.tag);
            for (int resource : text) {
                assertFalse(choice.tag, selected.getString(resource).isEmpty());
                if (!choice.language.equals("en"))
                    assertNotEquals(choice.tag + ": English fallback for " + resource,
                            english.getString(resource), selected.getString(resource));
            }
            for (RegionDefinition region : RegionRegistry.ALL)
                assertFalse(choice.tag + "/" + region.code, selected.getString(region.nameResource).isEmpty());
        }
    }

    @Test public void hindiIncludesTheWholeSettingsSurface() {
        Context hindi = language("hi-IN");
        assertEquals("सेटिंग", hindi.getString(R.string.title_activity_settings));
        assertEquals("सामान्य", hindi.getString(R.string.settings_section_general));
        assertEquals("दिखावट", hindi.getString(R.string.settings_section_appearance));
        assertEquals("प्राइवेसी", hindi.getString(R.string.settings_section_privacy));
        assertEquals("इस्तेमाल का डेटा", hindi.getString(R.string.settings_privacy_choices));
        assertEquals("फ़्लैट (बिना बॉर्डर)", hindi.getString(R.string.button_style_flat));
        assertEquals("ऐप का वर्शन", hindi.getString(R.string.pref_version_title));
        assertFalse(hindi.getString(R.string.settings_usage_analytics_summary).contains("Sharing is optional"));
    }

    @Test public void portugueseVariantsShareResourcesAndOverrideLocalWording() {
        assertEquals("Idioma do app", language("pt-BR").getString(R.string.regional_language));
        assertEquals("Idioma da aplicação", language("pt-PT").getString(R.string.regional_language));
        assertEquals("Definições", language("pt-PT").getString(R.string.title_activity_settings));
    }
}
