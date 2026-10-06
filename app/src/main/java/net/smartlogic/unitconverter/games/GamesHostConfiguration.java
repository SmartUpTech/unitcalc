package net.smartlogic.unitconverter.games;

import android.content.Context;

import net.smartlogic.unitconverter.BuildConfig;
import net.smartlogic.unitconverter.theme.CalculatorTheme;
import net.smartlogic.unitconverter.theme.ThemeManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Map;

public final class GamesHostConfiguration {
    private GamesHostConfiguration() {}

    public static JSONObject create(Context context, String date, String timezone, Map<String, String> completions) {
        CalculatorTheme current = ThemeManager.get();
        String language = context.getResources().getConfiguration().getLocales().get(0).toLanguageTag();
        try {
            JSONObject theme = new JSONObject()
                    .put("mode", current.isLightBackground() ? "light" : "dark")
                    .put("background", hex(current.background()))
                    .put("surface", hex(current.background()))
                    .put("primaryText", hex(current.mainText()))
                    .put("secondaryText", hex(current.functions()))
                    .put("accent", hex(current.equal()))
                    .put("divider", hex(current.functions()));
            return new JSONObject().put("bridgeVersion", GamesConfig.BRIDGE_VERSION)
                    .put("appId", BuildConfig.APPLICATION_ID).put("appVersion", BuildConfig.VERSION_NAME)
                    .put("language", language).put("locale", language).put("date", date)
                    .put("timezone", timezone).put("theme", theme)
                    .put("completions", new JSONObject(completions));
        } catch (JSONException impossible) {
            throw new IllegalStateException("Invalid Games host configuration", impossible);
        }
    }

    private static String hex(int color) { return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF); }
}
