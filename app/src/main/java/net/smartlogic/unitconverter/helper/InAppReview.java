package net.smartlogic.unitconverter.helper;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import java.util.concurrent.TimeUnit;

/** A quiet review opportunity after sustained use, with no rating pre-screen. */
public final class InAppReview {
    private static boolean requesting;
    private InAppReview() { }
    public static boolean isActive() { return requesting; }

    public static void recordUse(Context context) {
        SharedPreferences prefs = Preferences.getInstance(context).getPreferences();
        prefs.edit().putLong("review_first_use", prefs.getLong("review_first_use", System.currentTimeMillis()))
                .putInt("review_uses", Math.min(1000, prefs.getInt("review_uses", 0) + 1)).apply();
    }

    public static boolean eligible(long now, long firstUse, int uses, long lastAttempt) {
        return firstUse > 0 && now >= firstUse && uses >= 10
                && now - firstUse >= TimeUnit.DAYS.toMillis(7)
                && (lastAttempt == 0 || now >= lastAttempt && now - lastAttempt >= TimeUnit.DAYS.toMillis(90));
    }

    public static void maybeRequest(Activity activity) {
        SharedPreferences prefs = Preferences.getInstance(activity).getPreferences();
        long now = System.currentTimeMillis();
        if (requesting || activity.isFinishing() || activity.isDestroyed()
                || AppOpenManager.isShowingAd() || AdMobManager.isGamesAdShowing()
                || AdMobManager.isThemeAdShowing() || AdMobManager.getInstance(activity).isConsentUpdating()
                || !eligible(now, prefs.getLong("review_first_use", 0), prefs.getInt("review_uses", 0),
                prefs.getLong("review_last_attempt", 0))) return;
        requesting = true;
        prefs.edit().putLong("review_last_attempt", now).apply();
        ReviewManager manager = ReviewManagerFactory.create(activity);
        manager.requestReviewFlow().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || activity.isFinishing() || activity.isDestroyed()
                    || !activity.hasWindowFocus() || AppOpenManager.isShowingAd()
                    || AdMobManager.isGamesAdShowing() || AdMobManager.isThemeAdShowing()) {
                requesting = false;
                return;
            }
            manager.launchReviewFlow(activity, task.getResult()).addOnCompleteListener(flow -> requesting = false);
        });
    }
}
