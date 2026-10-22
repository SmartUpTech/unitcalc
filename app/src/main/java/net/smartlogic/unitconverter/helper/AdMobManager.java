package net.smartlogic.unitconverter.helper;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.google.android.libraries.ads.mobile.sdk.MobileAds;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration;
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import net.smartlogic.unitconverter.BuildConfig;
import net.smartlogic.unitconverter.R;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Consent-gated rewarded inventory. No banner or unsolicited interstitial requests. */
public final class AdMobManager {
    private static AdMobManager instance;
    private static boolean gamesAdShowing;
    private static final String GAMES_PRELOAD_ID = "games_rewarded";
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Runnable> readyCallbacks = new ArrayList<>();
    private boolean initialized;
    private boolean consentRefreshFinished;
    private boolean gamesConsentUpdating;
    private boolean gamesConsentChecked;
    private boolean gamesPreloadStarted;

    private boolean initializationStarted;

    private AdMobManager(Context context) {
        this.context = context.getApplicationContext();
    }

    private void initializeSdkIfEligible() {
        if (initializationStarted || !canRequestAds() || gamesConsentUpdating) return;
        initializationStarted = true;
        // Next-Gen initialization can block: run once, after consent, off the UI thread.
        new Thread(() -> {
            try {
                MobileAds.initialize(context,
                        new InitializationConfig.Builder(context.getString(R.string.am_app_id)).build(),
                        status -> main.post(() -> {
                            initialized = true;
                            List<Runnable> callbacks = new ArrayList<>(readyCallbacks);
                            readyCallbacks.clear();
                            for (Runnable callback : callbacks) callback.run();
                        }));
            } catch (RuntimeException error) {
                android.util.Log.w("AdMobManager", "Unable to initialize ads", error);
                main.post(() -> {
                    initializationStarted = false;
                    gamesPreloadStarted = false;
                    readyCallbacks.clear();
                });
            }
        }, "mobile-ads-init").start();
    }

    public boolean isInitialized() { return initialized; }

    public static synchronized AdMobManager getInstance(Context context) {
        if (instance == null) instance = new AdMobManager(context);
        return instance;
    }

    public void onSdkReady(Runnable callback) {
        if (initialized) callback.run();
        else {
            readyCallbacks.add(callback);
            initializeSdkIfEligible();
        }
    }

    static boolean isValidAdUnitId(String id) {
        return id != null && id.matches("ca-app-pub-[0-9]{16}/[0-9]{10}");
    }

    private String gamesAdUnit() {
        return BuildConfig.DEBUG ? "ca-app-pub-3940256099942544/5224354917"
                : context.getString(R.string.am_rewarded_ad_unit);
    }

    /** Refresh consent before any ad requests, including app-open. */
    public void prepareGamesAds(Activity activity, BooleanSupplier stillVisible, Runnable onPrivacyUpdated) {
        if (gamesConsentUpdating) return;
        if (gamesConsentChecked) {
            startGamesPreloading();
            AppOpenManager.onConsentChanged();
            return;
        }
        gamesConsentUpdating = true;
        ConsentInformation consent = UserMessagingPlatform.getConsentInformation(context);
        consent.requestConsentInfoUpdate(activity, new ConsentRequestParameters.Builder().build(), () -> {
            onPrivacyUpdated.run();
            if (!activity.isFinishing() && !activity.isDestroyed() && stillVisible.getAsBoolean()) {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, error -> {
                    gamesConsentUpdating = false;
                    gamesConsentChecked = consent.canRequestAds();
                    consentRefreshFinished = true;
                    onPrivacyUpdated.run();
                    startGamesPreloading();
                    AppOpenManager.onConsentChanged();
                });
            } else {
                gamesConsentUpdating = false;
                gamesConsentChecked = consent.canRequestAds();
                consentRefreshFinished = true;
                startGamesPreloading();
                AppOpenManager.onConsentChanged();
            }
        }, error -> {
            gamesConsentUpdating = false;
            gamesConsentChecked = consent.canRequestAds();
            consentRefreshFinished = true;
            onPrivacyUpdated.run();
            startGamesPreloading();
            AppOpenManager.onConsentChanged();
        });
    }

    private void startGamesPreloading() {
        if (gamesPreloadStarted || gamesConsentUpdating || !canRequestAds() || !isValidAdUnitId(gamesAdUnit())) return;
        gamesPreloadStarted = true;
        onSdkReady(() -> {
            if (!canRequestAds() || gamesConsentUpdating) { gamesPreloadStarted = false; return; }
            RewardedAdPreloader.start(GAMES_PRELOAD_ID,
                    new PreloadConfiguration(new AdRequest.Builder(gamesAdUnit()).build()));
        });
    }

    public boolean isConsentUpdating() { return gamesConsentUpdating; }

    public boolean canRequestAds() {
        return consentRefreshFinished && UserMessagingPlatform.getConsentInformation(context).canRequestAds();
    }

    public boolean isGamesPrivacyOptionsRequired() {
        return UserMessagingPlatform.getConsentInformation(context).getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    public void showGamesPrivacyOptions(Activity activity) {
        if (!isGamesPrivacyOptionsRequired()) return;
        AppOpenManager.invalidateInventory();
        if (initialized) RewardedAdPreloader.destroy(GAMES_PRELOAD_ID);
        gamesPreloadStarted = false;
        gamesConsentUpdating = true;
        UserMessagingPlatform.showPrivacyOptionsForm(activity, error -> {
            gamesConsentUpdating = false;
            startGamesPreloading();
            AppOpenManager.onConsentChanged();
        });
    }

    public static boolean isGamesAdShowing() { return gamesAdShowing; }

    /** Only the earned callback unlocks content; dismissal and no-fill never grant a set. */
    public boolean showNewSetAd(Activity activity, Runnable onEarned, Runnable onClosed) {
        if (!initialized || !canRequestAds() || gamesConsentUpdating || gamesAdShowing || AppOpenManager.isShowingAd()
                || activity.isFinishing() || activity.isDestroyed()) return false;
        RewardedAd ad = RewardedAdPreloader.pollAd(GAMES_PRELOAD_ID);
        if (ad == null) { startGamesPreloading(); return false; }
        gamesAdShowing = true;
        ad.setAdEventCallback(new RewardedAdEventCallback() {
            private boolean closed;
            private void close() {
                if (closed) return;
                closed = true;
                gamesAdShowing = false;
                main.post(onClosed);
            }
            @Override public void onAdDismissedFullScreenContent() { close(); }
            @Override public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError error) { close(); }
        });
        try {
            java.util.concurrent.atomic.AtomicBoolean earned = new java.util.concurrent.atomic.AtomicBoolean();
            ad.show(activity, reward -> {
                if (earned.compareAndSet(false, true)) main.post(onEarned);
            });
            return true;
        } catch (RuntimeException error) {
            gamesAdShowing = false;
            return false;
        }
    }
}
