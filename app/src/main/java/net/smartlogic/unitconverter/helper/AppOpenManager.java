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

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

import net.smartlogic.unitconverter.BuildConfig;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.app.UnitConverter;

import java.util.Date;

public class AppOpenManager implements ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private static final String TAG = "SHRIKI";
    private static final boolean DEBUG_FLAG = false;
    private static boolean isShowingAd = false;
    private final UnitConverter myApplication;
    private AppOpenAd appOpenAd = null;
    private long loadTime = 0;
    private Activity currentActivity;

    private static AppOpenManager mInstance;

    public static AppOpenManager getInstance(UnitConverter myApplication) {
        if (mInstance == null) {
            if (BuildConfig.DEBUG && DEBUG_FLAG)
                Log.d(TAG, "AppOpenManager instance is null. Initializing");
            mInstance = new AppOpenManager(myApplication);
        }
        return mInstance;
    }

    public AppOpenManager(UnitConverter myApplication) {
        this.myApplication = myApplication;
        this.myApplication.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        fetchAd();
    }

    public void fetchAd() {
        // Have unused ad, no need to fetch another.
        if (BuildConfig.DEBUG && DEBUG_FLAG) Log.d("SHRIKI","AppOpen Fetch Ad called");
        if (isAdAvailable()) {
            return;
        }
        AppOpenAd.AppOpenAdLoadCallback loadCallback = new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                if (BuildConfig.DEBUG && DEBUG_FLAG) Log.d(TAG, "AdMob AppOpen onAdLoaded");
                appOpenAd = ad;
                loadTime = (new Date()).getTime();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                if (BuildConfig.DEBUG && DEBUG_FLAG)
                    Log.d(TAG, "AdMob AppOpen onAdFailedToLoad. Error: " + loadAdError);
            }
        };
        AdRequest request = getAdRequest();

            AppOpenAd.load(
                    myApplication,
                    myApplication.getString(R.string.am_app_open_ad_unit),
                    request,
                    loadCallback);
    }

    public boolean isAdAvailable() {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(2);
    }

    public void showAdIfAvailable() {
        if (BuildConfig.DEBUG && DEBUG_FLAG)
            Log.d(TAG,"AdMob AppOpen showAdIfAvailable in AppOpenManager");

        if (!isShowingAd && isAdAvailable()) {
            if (BuildConfig.DEBUG && DEBUG_FLAG)
                Log.d(TAG, "Will show ad.");

            FullScreenContentCallback fullScreenContentCallback =
                    new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            if (BuildConfig.DEBUG && DEBUG_FLAG)
                                Log.d(TAG, "AdMob AppOpen onAdDismissedFullScreenContent");
                            appOpenAd = null;
                            isShowingAd = false;
                            fetchAd();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                            if (BuildConfig.DEBUG && DEBUG_FLAG)
                                Log.d(TAG, "AdMob AppOpen onAdFailedToShowFullScreenContent. Error: " + adError.getMessage());
                            appOpenAd = null;
                            isShowingAd = false;
                            fetchAd();
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            if (BuildConfig.DEBUG && DEBUG_FLAG)
                                Log.d(TAG, "AdMob AppOpen onAdShowedFullScreenContent");
                            isShowingAd = true;
                        }
                    };
            appOpenAd.setFullScreenContentCallback(fullScreenContentCallback);
            appOpenAd.show(currentActivity);

        } else {
            if (BuildConfig.DEBUG && DEBUG_FLAG)
                Log.d(TAG, "AdMob AppOpen cannot show ad. isAdAvailable(): " + isAdAvailable());
            fetchAd();
        }
    }

    private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
        long dateDifference = (new Date()).getTime() - this.loadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }

    /**
     * Creates and returns ad request.
     */
    private AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
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
    /** LifecycleObserver methods */
    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (BuildConfig.DEBUG && DEBUG_FLAG)
            Log.d(TAG, "App Open onStart");

        showAdIfAvailable();
    }

}