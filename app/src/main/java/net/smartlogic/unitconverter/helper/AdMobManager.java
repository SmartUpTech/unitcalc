package net.smartlogic.unitconverter.helper;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import net.smartlogic.unitconverter.BuildConfig;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.utils.Utils;

import androidx.annotation.NonNull;

public class AdMobManager {

    private static final int AD_FREE_HOURS = 24;
    private static final String TAG = "SHRIKI";
    private static final boolean DEBUG_FLAG = false;
    private static long DELAY_BEFORE_LOAD = 1200;
    private static AdMobManager mInstance;
    private final Preferences prefs;
    private final Context context;
    private final Utils utils;
    private InterstitialAd mInterstitialAd;
    private RewardedAd mRewardedAd;

    private AdMobManager(Context context) {
        if (BuildConfig.DEBUG && DEBUG_FLAG) Log.d(TAG, "Inside AdMobManager");

        MobileAds.initialize(context, AdMobManager::onInitializationComplete);

        this.context = context;
        prefs = Preferences.getInstance(context);
        utils = new Utils(context);
        //evaluateAdFreeStatus();
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

    private static void onInitializationComplete(InitializationStatus initializationStatus) {
        if (BuildConfig.DEBUG && DEBUG_FLAG)
            Log.d(TAG, "AdMob onInitializationComplete with status " + initializationStatus);
    }

//    public void evaluateAdFreeStatus() {
//        long time_watched_ad = prefs.getAdFreeStartTime();
//        Calendar cal_now = Calendar.getInstance(Locale.ENGLISH);
//        long hours_since_last_watch = Math.round((float) (cal_now.getTimeInMillis() - time_watched_ad) / (1000 * 60 * 60));
//
//        if (BuildConfig.DEBUG && DEBUG_FLAG)
//            Log.d(TAG, "Hours since last ad was watched: " + hours_since_last_watch);
//        if (BuildConfig.DEBUG && DEBUG_FLAG)
//            Log.d(TAG, "Evaluating Ad Free Users Status: " + (AD_FREE_HOURS > hours_since_last_watch));
//
//        prefs.setAdFreeUser(AD_FREE_HOURS > hours_since_last_watch);
//    }

    public void loadBannerAd(final AdView mAdView) {
        AdRequest adRequest = new AdRequest.Builder().build();
        if (mAdView.getAdListener() == null) {
            mAdView.setAdListener(new AdListener() {

                @Override
                public void onAdLoaded() {
                    if (BuildConfig.DEBUG && DEBUG_FLAG) Log.d(TAG, "AdMob Banner onAdLoaded");
                    super.onAdLoaded();
                    mAdView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    if (BuildConfig.DEBUG && DEBUG_FLAG)
                        Log.d(TAG, "AdMob Banner onAdFailedToLoad. Error: " + loadAdError.toString());
                    super.onAdFailedToLoad(loadAdError);
                }

                @Override
                public void onAdClosed() {
                    if (BuildConfig.DEBUG && DEBUG_FLAG) Log.d(TAG, "AdMob Banner onAdClosed");
                    super.onAdClosed();
                }

                @Override
                public void onAdOpened() {
                    if (BuildConfig.DEBUG && DEBUG_FLAG) Log.d(TAG, "AdMob Banner onAdOpened");
                    super.onAdOpened();
                }

                @Override
                public void onAdClicked() {
                    if (BuildConfig.DEBUG && DEBUG_FLAG) Log.d(TAG, "AdMob Banner onAdClicked");
                    super.onAdClicked();
                }

                @Override
                public void onAdImpression() {
                    if (BuildConfig.DEBUG && DEBUG_FLAG) Log.d(TAG, "AdMob Banner onAdImpression");
                    super.onAdImpression();
                }
            });
        }

        Runnable loadAd = () -> {
//            if (prefs.isProUser()) {
//                if (BuildConfig.DEBUG && DEBUG_FLAG)
//                    Log.d(TAG, context.getString(R.string.info_user_pro));
//            } else if (prefs.isAdFreeUser()) {
//                if (BuildConfig.DEBUG && DEBUG_FLAG)
//                    Log.d(TAG, context.getString(R.string.info_user_ad_free));
//            } else
            mAdView.loadAd(adRequest);
        };
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(loadAd, DELAY_BEFORE_LOAD);
    }

    public void loadInterstitialAd() {

        Runnable loadAd = () -> {
            if (BuildConfig.DEBUG && DEBUG_FLAG) Log.d(TAG, "Inside loadInterstitialAd");

//            if (prefs.isProUser()) {
//                if (BuildConfig.DEBUG && DEBUG_FLAG)
//                    Log.d(TAG, context.getString(R.string.info_user_pro));
//            } else if (prefs.isAdFreeUser()) {
//                if (BuildConfig.DEBUG && DEBUG_FLAG)
//                    Log.d(TAG, context.getString(R.string.info_user_ad_free));
//            } else {
            AdRequest adRequest = new AdRequest.Builder().build();
            InterstitialAd.load(context, context.getString(R.string.am_interstitial_ad_unit), adRequest,
                    new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                            mInterstitialAd = interstitialAd;
                            if (BuildConfig.DEBUG && DEBUG_FLAG)
                                Log.d(TAG, "AdMob Interstitial onAdLoaded");
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            if (BuildConfig.DEBUG && DEBUG_FLAG)
                                Log.d(TAG, "AdMob Interstitial onAdFailedToLoad. Error: " + loadAdError.getMessage());
                            mInterstitialAd = null;
                        }
                    });

            //}
        };
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(loadAd, DELAY_BEFORE_LOAD);
    }

    public void showInterstitialAd(Activity activity) {
//        if (prefs.isProUser()) {
//            if (BuildConfig.DEBUG && DEBUG_FLAG)
//                Log.d(TAG, context.getString(R.string.info_user_pro));
//        } else if (prefs.isAdFreeUser()) {
//            if (BuildConfig.DEBUG && DEBUG_FLAG)
//                Log.d(TAG, context.getString(R.string.info_user_ad_free));
//        } else
        if (mInterstitialAd != null)
            mInterstitialAd.show(activity);
    }

    public void loadRewardedAd() {

        Runnable loadAd = () -> {
            if (BuildConfig.DEBUG && DEBUG_FLAG) Log.d(TAG, "Inside loadRewardedAd");

//            if (prefs.isProUser()) {
//                if (BuildConfig.DEBUG && DEBUG_FLAG)
//                    Log.d(TAG, context.getString(R.string.info_user_pro));
//            } else if (prefs.isAdFreeUser()) {
//                if (BuildConfig.DEBUG && DEBUG_FLAG)
//                    Log.d(TAG, context.getString(R.string.info_user_ad_free));
//            } else {
            AdRequest adRequest = new AdRequest.Builder().build();
            RewardedAd.load(context, context.getString(R.string.am_rewarded_ad_unit),
                    adRequest, new RewardedAdLoadCallback() {
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            if (BuildConfig.DEBUG && DEBUG_FLAG)
                                Log.d(TAG, "AdMob Rewarded Ad onAdFailedToLoad. Error: " + loadAdError.getMessage());
                            mRewardedAd = null;
                        }

                        @Override
                        public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                            mRewardedAd = rewardedAd;
                            if (BuildConfig.DEBUG && DEBUG_FLAG)
                                Log.d(TAG, "AdMob Rewarded Ad onAdLoaded");
                        }
                    });
            //}
        };
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(loadAd, DELAY_BEFORE_LOAD);
    }

    public boolean hasRewardedAd() {
        return mRewardedAd != null;
    }

    public void showRewardedAd(Activity activity) {
//        if (prefs.isProUser()) {
//            if (BuildConfig.DEBUG && DEBUG_FLAG)
//                Log.d(TAG, context.getString(R.string.info_user_pro));
//        } else if (prefs.isAdFreeUser()) {
//            if (BuildConfig.DEBUG && DEBUG_FLAG)
//                Log.d(TAG, context.getString(R.string.info_user_ad_free));
//        } else
        if (mRewardedAd != null)
            mRewardedAd.show(activity, rewardItem -> {
                if (BuildConfig.DEBUG && DEBUG_FLAG)
                    Log.d(TAG, "AdMob Rewarded Ad onUserEarnedReward");
                //prefs.setAdFreeStartTime(Calendar.getInstance(Locale.ENGLISH).getTimeInMillis());
                //prefs.setAdFreeUser(true);
            });
    }


}
