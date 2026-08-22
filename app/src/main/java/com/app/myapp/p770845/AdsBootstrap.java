package com.app.myapp.p770845;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdView;

/**
 * Starts ads in exported APK — loads config from export assets, then same path as Run/Preview.
 */
public final class AdsBootstrap {

    private static final String TAG = "AdsBootstrap";
    private static int bannerAttempts;

    private AdsBootstrap() {}

    public static void start(Activity activity, AppConfig ignoredConfig,
                             FrameLayout bannerContainer, Button rewardedButton,
                             Runnable interstitialRunnable) {
        if (activity == null) {
            return;
        }
        AppConfig cfg = AppConfig.load(activity);
        LaunchConfig.applyTo(cfg, activity);

        if (cfg == null || !cfg.adsEnabled) {
            hide(bannerContainer, rewardedButton);
            Log.i(TAG, "ads disabled in export config");
            return;
        }

        Log.i(TAG, "start adsEnabled=true testMode=" + cfg.useTestAds
                + " banner=" + cfg.showBanner()
                + " rewarded=" + cfg.showRewarded()
                + " bannerUnit=" + cfg.admobBannerId);

        Runnable showAds = () -> {
            if (activity.isFinishing()) {
                return;
            }
            ExportAds.attach(activity, cfg, bannerContainer, rewardedButton, interstitialRunnable);
        };

        MobileAdsGate.ensureInit(activity);
        MobileAdsGate.runWhenReady(activity, showAds);
    }

    public static void retryBannerIfEmpty(Activity activity, AppConfig config, FrameLayout bannerContainer) {
        if (activity == null || config == null || bannerContainer == null
                || !config.showBanner() || activity.isFinishing()) {
            return;
        }
        if (bannerContainer.getChildCount() > 0) {
            for (int i = 0; i < bannerContainer.getChildCount(); i++) {
                View child = bannerContainer.getChildAt(i);
                if (child instanceof AdView && child.getVisibility() == View.VISIBLE) {
                    return;
                }
            }
        }
        if (bannerAttempts >= 8) {
            return;
        }
        bannerAttempts++;
        Log.i(TAG, "Retry banner #" + bannerAttempts);
        MobileAdsGate.runWhenReady(activity, () ->
                ExportAds.loadBanner(activity, bannerContainer, config.admobBannerId));
    }

    private static void hide(FrameLayout banner, Button rewarded) {
        if (banner != null) {
            banner.setVisibility(View.GONE);
            banner.removeAllViews();
        }
        if (rewarded != null) {
            rewarded.setVisibility(View.GONE);
        }
    }
}
