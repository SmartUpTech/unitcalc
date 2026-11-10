package net.smartlogic.unitconverter.helper;

import android.content.Context;
import android.os.Bundle;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import java.util.EnumMap;

/** Opt-in product telemetry. Never pass expressions, amounts, history or web payloads. */
public final class UsageAnalytics {
    public static final String ENABLED = "usage_analytics_enabled";
    private static volatile FirebaseAnalytics analytics;
    private static volatile boolean enabled;
    private UsageAnalytics() { }

    public static void initialize(Context context) {
        FirebaseApp app = FirebaseApp.initializeApp(context);
        if (app != null) analytics = FirebaseAnalytics.getInstance(context);
        setEnabled(context, Preferences.getInstance(context).getPreferences().getBoolean(ENABLED, false));
    }

    public static void setEnabled(Context context, boolean value) {
        enabled = value;
        Preferences.getInstance(context).getPreferences().edit().putBoolean(ENABLED, value).apply();
        FirebaseAnalytics target = analytics;
        if (target == null) return;
        EnumMap<FirebaseAnalytics.ConsentType, FirebaseAnalytics.ConsentStatus> consent =
                new EnumMap<>(FirebaseAnalytics.ConsentType.class);
        consent.put(FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE, value
                ? FirebaseAnalytics.ConsentStatus.GRANTED : FirebaseAnalytics.ConsentStatus.DENIED);
        consent.put(FirebaseAnalytics.ConsentType.AD_STORAGE, FirebaseAnalytics.ConsentStatus.DENIED);
        consent.put(FirebaseAnalytics.ConsentType.AD_USER_DATA, FirebaseAnalytics.ConsentStatus.DENIED);
        consent.put(FirebaseAnalytics.ConsentType.AD_PERSONALIZATION, FirebaseAnalytics.ConsentStatus.DENIED);
        target.setConsent(consent);
        target.setAnalyticsCollectionEnabled(value);
        if (!value) target.resetAnalyticsData();
    }

    public static void event(String name, String dimension, String value) {
        Bundle parameters = new Bundle();
        if (value != null) parameters.putString(dimension, value.substring(0, Math.min(100, value.length())));
        log(name, parameters);
    }

    public static void toolOpened(String id) {
        Bundle parameters = toolParameters(id);
        if (parameters != null) log("tool_opened", parameters);
    }

    static Bundle toolParameters(String id) {
        var entry = net.smartlogic.unitconverter.model.CalculatorCatalog.getById(id);
        if (entry == null || !entry.isTool()) return null;
        Bundle parameters = new Bundle();
        parameters.putString("tool_id", entry.id);
        parameters.putString("tool_family", entry.destination
                == net.smartlogic.unitconverter.model.CalculatorCatalog.Destination.UNIT_CATEGORY
                ? net.smartlogic.unitconverter.model.CalculatorCatalog.ID_UNIT : entry.id);
        return parameters;
    }

    private static void log(String name, Bundle parameters) {
        FirebaseAnalytics target = analytics;
        if (enabled && target != null) target.logEvent(name, parameters);
    }
}
