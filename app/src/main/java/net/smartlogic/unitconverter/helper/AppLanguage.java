package net.smartlogic.unitconverter.helper;

import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/** AppCompat owns the language choice; the stored observation is only for prompt transitions. */
public final class AppLanguage {
    private AppLanguage() {}
    public static Context resourceContext(Context context) {
        Context localized = androidx.core.content.ContextCompat.getContextForLanguage(context);
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty()) return localized;
        var configuration = new android.content.res.Configuration(localized.getResources().getConfiguration());
        configuration.setLocales(android.os.LocaleList.forLanguageTags(locales.toLanguageTags()));
        return localized.createConfigurationContext(configuration);
    }
    public static SupportedLanguage current(Context context) {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        return SupportedLanguage.resolve(locales.isEmpty()
                ? context.getResources().getConfiguration().getLocales().get(0).toLanguageTag()
                : locales.get(0).toLanguageTag());
    }

    public static void initializeAfterActivityCreated(Context context) {
        var store = Preferences.getInstance(context).getPreferences();
        if (!Boolean.TRUE.equals(store.getAll().get("regional_language_initialized"))) {
            store.edit().putBoolean("regional_language_initialized", true).apply();
            if (AppCompatDelegate.getApplicationLocales().isEmpty())
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en-US"));
        }
    }

    public static void select(Context context, SupportedLanguage selected) {
        if (current(context).tag.equals(selected.tag)) return;
        Preferences preferences = Preferences.getInstance(context);
        new RegionSuggestion(preferences.getPreferences()).changed(selected, preferences.getRegionalProfile().region);
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selected.tag));
        net.smartlogic.unitconverter.calculator.tools.timer.TimerEngine.refreshLanguageIfActive();
    }
}
