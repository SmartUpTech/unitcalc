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
        FirebaseAnalytics target = analytics;
        if (!enabled || target == null) return;
        Bundle parameters = new Bundle();
        if (value != null) parameters.putString(dimension, value.substring(0, Math.min(100, value.length())));
        target.logEvent(name, parameters);
    }
}
