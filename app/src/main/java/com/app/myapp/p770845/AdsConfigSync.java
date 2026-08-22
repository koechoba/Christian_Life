package com.app.myapp.p770845;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Aligns AdMob unit IDs with manifest App ID so exported APK always shows ads
 * (test ads by default; real ads only when App ID + units all match).
 */
public final class AdsConfigSync {

    private static final String TAG = "AdsConfigSync";
    private static final String META_APP_ID = "com.google.android.gms.ads.APPLICATION_ID";

    private AdsConfigSync() {}

    public static void apply(Context context, AppConfig config) {
        if (context == null || config == null || !config.adsEnabled) {
            return;
        }
        String manifestAppId = readManifestAppId(context);
        if (manifestAppId.isEmpty()) {
            manifestAppId = AdIds.APP_ID;
        }

        boolean manifestTest = AdIds.isGoogleTestId(manifestAppId);
        boolean jsonTest = config.useTestAds;
        boolean unitsLookReal = hasMatchingRealUnits(config, manifestAppId);

        boolean testMode = jsonTest || manifestTest || !unitsLookReal;

        if (testMode) {
            applyTestMode(config, manifestTest ? manifestAppId : AdIds.APP_ID);
            return;
        }

        config.useTestAds = false;
        config.admobAppId = manifestAppId;
        Log.i(TAG, "Using real AdMob units from export config");
    }

    private static void applyTestMode(AppConfig config, String appId) {
        config.useTestAds = true;
        config.admobAppId = appId;
        config.admobBannerId = AdIds.BANNER;
        config.admobInterstitialId = AdIds.INTERSTITIAL;
        config.admobRewardedId = AdIds.REWARDED;
        config.admobAppOpenId = AdIds.APP_OPEN;
        Log.i(TAG, "Using Google test ad units");
    }

    private static boolean hasMatchingRealUnits(AppConfig config, String manifestAppId) {
        if (AdIds.isGoogleTestId(manifestAppId)) {
            return false;
        }
        if (config.showBanner()
                && (!AdIds.isRealUnitId(config.admobBannerId)
                || !AdIds.samePublisher(manifestAppId, config.admobBannerId))) {
            return false;
        }
        if (config.showInterstitial()
                && (!AdIds.isRealUnitId(config.admobInterstitialId)
                || !AdIds.samePublisher(manifestAppId, config.admobInterstitialId))) {
            return false;
        }
        if (config.rewardedAdEnabled
                && (!AdIds.isRealUnitId(config.admobRewardedId)
                || !AdIds.samePublisher(manifestAppId, config.admobRewardedId))) {
            return false;
        }
        if (config.showAppOpen()
                && (!AdIds.isRealUnitId(config.admobAppOpenId)
                || !AdIds.samePublisher(manifestAppId, config.admobAppOpenId))) {
            return false;
        }
        return true;
    }

    public static String readManifestAppId(Context context) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            if (ai.metaData == null) {
                return "";
            }
            Object raw = ai.metaData.get(META_APP_ID);
            if (raw instanceof String) {
                return ((String) raw).trim();
            }
            if (raw instanceof Integer) {
                return context.getResources().getString((Integer) raw).trim();
            }
        } catch (Exception e) {
            Log.w(TAG, "readManifestAppId", e);
        }
        return "";
    }
}
