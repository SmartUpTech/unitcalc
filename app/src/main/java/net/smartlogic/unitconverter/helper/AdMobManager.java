package net.smartlogic.unitconverter.helper;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.libraries.ads.mobile.sdk.MobileAds;
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize;
import com.google.android.libraries.ads.mobile.sdk.banner.AdView;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration;
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader;

import net.smartlogic.unitconverter.BuildConfig;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.games.GamesSession;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class AdMobManager {
    public static final int FIRST_GAMES_AD_THRESHOLD = 3;
    public static final int SECOND_GAMES_AD_THRESHOLD = 8;
    public static final int MAX_GAMES_VIDEO_ADS_PER_SESSION = 2;
    private static final String GAMES_PRELOAD_ID = "games_interstitial";
    private static final String GAMES_TEST_AD_UNIT = "ca-app-pub-3940256099942544/1033173712";
    private static boolean gamesAdShowing;
    private final GamesSession gamesSession = new GamesSession(FIRST_GAMES_AD_THRESHOLD,
            SECOND_GAMES_AD_THRESHOLD, MAX_GAMES_VIDEO_ADS_PER_SESSION);
    private boolean gamesConsentUpdating;
    private boolean gamesConsentChecked;
    private boolean gamesPreloadStarted;

    private static final String TAG = "SHRIKI";
    private static final boolean DEBUG_FLAG = false;
    private static long DELAY_BEFORE_LOAD = 1200;
    private static AdMobManager mInstance;
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<Runnable> readyCallbacks = new ArrayList<>();
    private volatile boolean initialized = false;

    private AdMobManager(Context context) {
        if (BuildConfig.DEBUG && DEBUG_FLAG) Log.d(TAG, "Inside AdMobManager");

        this.context = context.getApplicationContext();
        String appId = this.context.getString(R.string.am_app_id);

        new Thread(() -> MobileAds.initialize(
                this.context,
                new InitializationConfig.Builder(appId).build(),
                initializationStatus -> {
                    initialized = true;
                    startPreloading();
                    mainHandler.post(this::notifyReadyCallbacks);
                    if (BuildConfig.DEBUG && DEBUG_FLAG) {
                        Log.d(TAG, "AdMob onInitializationComplete with status " + initializationStatus);
                    }
                }
        )).start();
    }

    public static synchronized AdMobManager getInstance(Context context) {
        if (mInstance == null) {
            if (BuildConfig.DEBUG && DEBUG_FLAG) Log.d(TAG, "AdMob instance is null. Initializing");
            mInstance = new AdMobManager(context);
            return mInstance;
        }
        DELAY_BEFORE_LOAD = 1;
        if (BuildConfig.DEBUG && DEBUG_FLAG) Log.d(TAG, "AdMob is already initialized.");
        return mInstance;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void onSdkReady(Runnable runnable) {
        if (initialized) {
            runnable.run();
        } else {
            readyCallbacks.add(runnable);
        }
    }

    private void notifyReadyCallbacks() {
        for (Runnable callback : readyCallbacks) {
            callback.run();
        }
        readyCallbacks.clear();
    }

    private void startPreloading() {
        String interstitialUnitId = context.getString(R.string.am_interstitial_ad_unit);
        if (isValidAdUnitId(interstitialUnitId)) {
            AdRequest interstitialRequest = new AdRequest.Builder(interstitialUnitId).build();
            InterstitialAdPreloader.start(
                    interstitialUnitId,
                    new PreloadConfiguration(interstitialRequest));
        }

        String rewardedUnitId = context.getString(R.string.am_rewarded_ad_unit);
        if (isValidAdUnitId(rewardedUnitId)) {
            AdRequest rewardedRequest = new AdRequest.Builder(rewardedUnitId).build();
            RewardedAdPreloader.start(
                    rewardedUnitId,
                    new PreloadConfiguration(rewardedRequest));
        }
    }

    static boolean isValidAdUnitId(String unitId) {
        return unitId != null && unitId.startsWith("ca-app-pub-");
    }

    public void loadBannerAd(final AdView adView, Activity activity) {
        if (!initialized) {
            onSdkReady(() -> loadBannerAd(adView, activity));
            return;
        }

        String bannerUnitId = context.getString(R.string.am_banner_ad_unit);
        if (!isValidAdUnitId(bannerUnitId)) {
            return;
        }

        Runnable loadAd = () -> {
            AdSize adSize = AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, 360);
            BannerAdRequest adRequest = new BannerAdRequest.Builder(bannerUnitId, adSize).build();
            adView.loadAd(
                    adRequest,
                    new AdLoadCallback<BannerAd>() {
                        @Override
                        public void onAdLoaded(@NonNull BannerAd bannerAd) {
                            if (BuildConfig.DEBUG && DEBUG_FLAG) Log.d(TAG, "AdMob Banner onAdLoaded");
                            bannerAd.setAdEventCallback(new BannerAdEventCallback() {
                                @Override
                                public void onAdImpression() {
                                    if (BuildConfig.DEBUG && DEBUG_FLAG) {
                                        Log.d(TAG, "AdMob Banner onAdImpression");
                                    }
                                }

                                @Override
                                public void onAdClicked() {
                                    if (BuildConfig.DEBUG && DEBUG_FLAG) Log.d(TAG, "AdMob Banner onAdClicked");
                                }
                            });
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            if (BuildConfig.DEBUG && DEBUG_FLAG) {
                                Log.d(TAG, "AdMob Banner onAdFailedToLoad. Error: " + loadAdError);
                            }
                        }
                    });
        };
        mainHandler.postDelayed(loadAd, DELAY_BEFORE_LOAD);
    }

    public void loadInterstitialAd() {
        if (!initialized) {
            onSdkReady(this::loadInterstitialAd);
            return;
        }

        String interstitialUnitId = context.getString(R.string.am_interstitial_ad_unit);
        if (!isValidAdUnitId(interstitialUnitId)) {
            return;
        }

        Runnable loadAd = () -> {
            if (BuildConfig.DEBUG && DEBUG_FLAG) Log.d(TAG, "Inside loadInterstitialAd");
            if (!InterstitialAdPreloader.isAdAvailable(interstitialUnitId)) {
                AdRequest adRequest = new AdRequest.Builder(interstitialUnitId).build();
                InterstitialAdPreloader.start(
                        interstitialUnitId,
                        new PreloadConfiguration(adRequest));
            }
        };
        mainHandler.postDelayed(loadAd, DELAY_BEFORE_LOAD);
    }

    public void showInterstitialAd(Activity activity) {
        if (!initialized || gamesAdShowing) {
            return;
        }

        String interstitialUnitId = context.getString(R.string.am_interstitial_ad_unit);
        if (!isValidAdUnitId(interstitialUnitId)) {
            return;
        }

        InterstitialAd interstitialAd = InterstitialAdPreloader.pollAd(interstitialUnitId);
        if (interstitialAd == null) {
            return;
        }

        interstitialAd.setAdEventCallback(new InterstitialAdEventCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                if (BuildConfig.DEBUG && DEBUG_FLAG) Log.d(TAG, "AdMob Interstitial onAdDismissedFullScreenContent");
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                if (BuildConfig.DEBUG && DEBUG_FLAG) {
                    Log.d(TAG, "AdMob Interstitial onAdFailedToShow. Error: "
                            + fullScreenContentError.getMessage());
                }
            }

            @Override
            public void onAdShowedFullScreenContent() {
                if (BuildConfig.DEBUG && DEBUG_FLAG) Log.d(TAG, "AdMob Interstitial onAdShowedFullScreenContent");
            }
        });
        interstitialAd.show(activity);
    }

    public void loadRewardedAd() {
        if (!initialized) {
            onSdkReady(this::loadRewardedAd);
            return;
        }

        String rewardedUnitId = context.getString(R.string.am_rewarded_ad_unit);
        if (!isValidAdUnitId(rewardedUnitId)) {
            return;
        }

        Runnable loadAd = () -> {
            if (BuildConfig.DEBUG && DEBUG_FLAG) Log.d(TAG, "Inside loadRewardedAd");
            if (!RewardedAdPreloader.isAdAvailable(rewardedUnitId)) {
                AdRequest adRequest = new AdRequest.Builder(rewardedUnitId).build();
                RewardedAdPreloader.start(
                        rewardedUnitId,
                        new PreloadConfiguration(adRequest));
            }
        };
        mainHandler.postDelayed(loadAd, DELAY_BEFORE_LOAD);
    }

    public boolean hasRewardedAd() {
        if (!initialized) {
            return false;
        }

        String rewardedUnitId = context.getString(R.string.am_rewarded_ad_unit);
        return isValidAdUnitId(rewardedUnitId)
                && RewardedAdPreloader.isAdAvailable(rewardedUnitId);
    }

    public void showRewardedAd(Activity activity) {
        if (!initialized || gamesAdShowing) {
            return;
        }

        String rewardedUnitId = context.getString(R.string.am_rewarded_ad_unit);
        if (!isValidAdUnitId(rewardedUnitId)) {
            return;
        }

        RewardedAd rewardedAd = RewardedAdPreloader.pollAd(rewardedUnitId);
        if (rewardedAd == null) {
            return;
        }

        rewardedAd.setAdEventCallback(new RewardedAdEventCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                if (BuildConfig.DEBUG && DEBUG_FLAG) Log.d(TAG, "AdMob Rewarded onAdDismissedFullScreenContent");
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                if (BuildConfig.DEBUG && DEBUG_FLAG) {
                    Log.d(TAG, "AdMob Rewarded onAdFailedToShow. Error: "
                            + fullScreenContentError.getMessage());
                }
            }
        });
        rewardedAd.show(
                activity,
                rewardItem -> {
                    if (BuildConfig.DEBUG && DEBUG_FLAG) {
                        Log.d(TAG, "AdMob Rewarded Ad onUserEarnedReward");
                    }
                });
    }

    private String gamesAdUnit() {
        return BuildConfig.DEBUG ? GAMES_TEST_AD_UNIT : context.getString(R.string.am_interstitial_ad_unit);
    }

    /** Games-only consent flow. Existing SDK/startup/ad paths are unchanged. */
    public void prepareGamesAds(Activity activity, BooleanSupplier stillVisible, Runnable onPrivacyUpdated) {
        if (!isValidAdUnitId(gamesAdUnit()) || gamesConsentUpdating || gamesConsentChecked) return;
        gamesConsentUpdating = true;
        ConsentInformation consent = UserMessagingPlatform.getConsentInformation(context);
        consent.requestConsentInfoUpdate(activity, new ConsentRequestParameters.Builder().build(), () -> {
            onPrivacyUpdated.run();
            if (!activity.isFinishing() && !activity.isDestroyed() && stillVisible.getAsBoolean()) {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, error -> {
                    gamesConsentUpdating = false;
                    gamesConsentChecked = consent.canRequestAds();
                    onPrivacyUpdated.run();
                    startGamesPreloading();
                });
            } else {
                gamesConsentUpdating = false;
                gamesConsentChecked = consent.canRequestAds();
                startGamesPreloading();
            }
        }, error -> {
            gamesConsentUpdating = false;
            gamesConsentChecked = consent.canRequestAds();
            onPrivacyUpdated.run();
            startGamesPreloading();
        });
    }

    private void startGamesPreloading() {
        if (gamesPreloadStarted || !UserMessagingPlatform.getConsentInformation(context).canRequestAds()) return;
        gamesPreloadStarted = true;
        onSdkReady(() -> {
            if (!UserMessagingPlatform.getConsentInformation(context).canRequestAds()) {
                gamesPreloadStarted = false;
                return;
            }
            // Separate preload slot preserves existing ad inventory and timing.
            InterstitialAdPreloader.start(GAMES_PRELOAD_ID,
                    new PreloadConfiguration(new AdRequest.Builder(gamesAdUnit()).build()));
        });
    }

    public boolean isGamesPrivacyOptionsRequired() {
        return UserMessagingPlatform.getConsentInformation(context).getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    public void showGamesPrivacyOptions(Activity activity) {
        if (!isGamesPrivacyOptionsRequired()) return;
        InterstitialAdPreloader.destroy(GAMES_PRELOAD_ID);
        gamesPreloadStarted = false;
        UserMessagingPlatform.showPrivacyOptionsForm(activity, error -> startGamesPreloading());
    }

    public static boolean isGamesAdShowing() { return gamesAdShowing; }

    /** Completion must already be durable. Thresholds are immediate opportunities, not queued ads. */
    public void onGamesCompletion(@Nullable Activity activity, String gameId, String date, BooleanSupplier stillAtBreak) {
        boolean threshold = gamesSession.recordCompletion(gameId, date);
        if (!threshold || activity == null || activity.isFinishing() || activity.isDestroyed()
                || !stillAtBreak.getAsBoolean() || !initialized || gamesAdShowing
                || AppOpenManager.isShowingAd()
                || !UserMessagingPlatform.getConsentInformation(context).canRequestAds()
                || !isValidAdUnitId(gamesAdUnit())) return;
        InterstitialAd ad = InterstitialAdPreloader.pollAd(GAMES_PRELOAD_ID);
        if (ad == null || !gamesSession.reserveAd()) return;
        gamesAdShowing = true;
        ad.setAdEventCallback(new InterstitialAdEventCallback() {
            @Override public void onAdDismissedFullScreenContent() { gamesAdShowing = false; }
            @Override public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError error) {
                gamesAdShowing = false;
            }
        });
        try {
            ad.show(activity);
        } catch (RuntimeException failedToShow) {
            gamesAdShowing = false;
        }
    }
}
