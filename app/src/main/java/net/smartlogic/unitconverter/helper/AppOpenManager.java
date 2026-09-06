package net.smartlogic.unitconverter.helper;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd;
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdPreloader;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration;

import net.smartlogic.unitconverter.BuildConfig;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.app.UnitConverter;

public class AppOpenManager implements ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private static final String TAG = "SHRIKI";
    private static final boolean DEBUG_FLAG = false;
    private static boolean isShowingAd = false;
    private final UnitConverter myApplication;
    private Activity currentActivity;
    private boolean isFirstLaunch = true;
    private boolean preloadStarted = false;

    private static AppOpenManager mInstance;

    public static AppOpenManager getInstance(UnitConverter myApplication) {
        if (mInstance == null) {
            if (BuildConfig.DEBUG && DEBUG_FLAG) {
                Log.d(TAG, "AppOpenManager instance is null. Initializing");
            }
            mInstance = new AppOpenManager(myApplication);
        }
        return mInstance;
    }

    public AppOpenManager(UnitConverter myApplication) {
        this.myApplication = myApplication;
        this.myApplication.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        AdMobManager.getInstance(myApplication).onSdkReady(this::startPreload);
    }

    private void startPreload() {
        if (preloadStarted) {
            return;
        }

        String adUnitId = myApplication.getString(R.string.am_app_open_ad_unit);
        if (!AdMobManager.isValidAdUnitId(adUnitId)) {
            return;
        }

        preloadStarted = true;
        AdRequest adRequest = new AdRequest.Builder(adUnitId).build();
        PreloadConfiguration preloadConfig = new PreloadConfiguration(adRequest);
        AppOpenAdPreloader.start(adUnitId, preloadConfig);

        if (BuildConfig.DEBUG && DEBUG_FLAG) {
            Log.d(TAG, "AppOpen preloading started");
        }
    }

    public boolean isAdAvailable() {
        String adUnitId = myApplication.getString(R.string.am_app_open_ad_unit);
        return AdMobManager.isValidAdUnitId(adUnitId)
                && AppOpenAdPreloader.isAdAvailable(adUnitId);
    }

    public void showAdIfAvailable() {
        if (BuildConfig.DEBUG && DEBUG_FLAG) {
            Log.d(TAG, "AdMob AppOpen showAdIfAvailable in AppOpenManager");
        }

        if (isShowingAd || currentActivity == null) {
            return;
        }

        String adUnitId = myApplication.getString(R.string.am_app_open_ad_unit);
        if (!AdMobManager.isValidAdUnitId(adUnitId)) {
            return;
        }

        if (!AppOpenAdPreloader.isAdAvailable(adUnitId)) {
            if (BuildConfig.DEBUG && DEBUG_FLAG) {
                Log.d(TAG, "AdMob AppOpen cannot show ad. isAdAvailable(): false");
            }
            return;
        }

        AppOpenAd appOpenAd = AppOpenAdPreloader.pollAd(adUnitId);
        if (appOpenAd == null) {
            if (BuildConfig.DEBUG && DEBUG_FLAG) {
                Log.d(TAG, "AdMob AppOpen pollAd returned null");
            }
            return;
        }

        if (BuildConfig.DEBUG && DEBUG_FLAG) {
            Log.d(TAG, "Will show ad.");
        }

        appOpenAd.setAdEventCallback(new AppOpenAdEventCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                if (BuildConfig.DEBUG && DEBUG_FLAG) {
                    Log.d(TAG, "AdMob AppOpen onAdDismissedFullScreenContent");
                }
                isShowingAd = false;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                if (BuildConfig.DEBUG && DEBUG_FLAG) {
                    Log.d(TAG, "AdMob AppOpen onAdFailedToShowFullScreenContent. Error: "
                            + fullScreenContentError.getMessage());
                }
                isShowingAd = false;
            }

            @Override
            public void onAdShowedFullScreenContent() {
                if (BuildConfig.DEBUG && DEBUG_FLAG) {
                    Log.d(TAG, "AdMob AppOpen onAdShowedFullScreenContent");
                }
                isShowingAd = true;
            }
        });
        appOpenAd.show(currentActivity);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        currentActivity = null;
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (BuildConfig.DEBUG && DEBUG_FLAG) {
            Log.d(TAG, "App Open onStart");
        }

        if (isFirstLaunch) {
            isFirstLaunch = false;
            return;
        }

        showAdIfAvailable();
    }
}
