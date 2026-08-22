package com.app.myapp.p770845;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

/**
 * Exported APK ads — same flow as builder Run/Preview (init SDK → preload → show).
 */
public final class ExportAds {

    private static final String TAG = "ExportAds";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ExportAds() {}

    public static void attach(Activity activity, AppConfig config,
                              FrameLayout bannerContainer, Button rewardedButton,
                              Runnable interstitialRunnable) {
        if (activity == null || config == null || activity.isFinishing()) {
            return;
        }
        if (!config.adsEnabled) {
            hide(bannerContainer, rewardedButton);
            return;
        }

        final String bannerId = resolveBannerId(config);
        final String rewardedId = resolveRewardedId(config);

        Log.i(TAG, "attach testMode=" + config.useTestAds
                + " banner=" + config.showBanner()
                + " interstitial=" + config.showInterstitial()
                + " rewarded=" + config.showRewarded()
                + " bannerId=" + bannerId);

        Runnable start = () -> {
            if (activity.isFinishing()) {
                return;
            }
            AdPreloader.warm(activity, config);

            if (config.showBanner() && bannerContainer != null) {
                loadBanner(activity, bannerContainer, bannerId);
            } else if (bannerContainer != null) {
                bannerContainer.setVisibility(View.GONE);
            }

            if (rewardedButton != null) {
                if (config.showRewarded()) {
                    rewardedButton.setVisibility(View.VISIBLE);
                    setupRewarded(activity, rewardedButton, config, rewardedId);
                } else {
                    rewardedButton.setVisibility(View.GONE);
                }
            }

            if (interstitialRunnable != null && config.showInterstitial()) {
                MAIN.postDelayed(interstitialRunnable, 1200);
            }
        };

        runAfterSdkInit(activity, start);
    }

    private static void runAfterSdkInit(Activity activity, Runnable task) {
        MobileAdsGate.runWhenReady(activity, task);
    }

    public static void loadBanner(Activity activity, FrameLayout container, String unitId) {
        if (activity == null || activity.isFinishing() || container == null) {
            return;
        }
        final String id = AdIds.validBanner(unitId);
        container.removeAllViews();
        container.setVisibility(View.VISIBLE);
        try {
            AdView adView = new AdView(activity);
            adView.setAdUnitId(id);
            int widthDp = (int) (activity.getResources().getDisplayMetrics().widthPixels
                    / activity.getResources().getDisplayMetrics().density);
            adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    activity, Math.max(320, widthDp)));
            adView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    if (!activity.isFinishing()) {
                        container.setVisibility(View.VISIBLE);
                        Log.i(TAG, "Banner loaded");
                    }
                }

                @Override
                public void onAdFailedToLoad(LoadAdError error) {
                    Log.w(TAG, "Banner failed: " + error.getCode() + " " + error.getMessage());
                    MAIN.postDelayed(() -> {
                        if (!activity.isFinishing()) {
                            loadBanner(activity, container, id);
                        }
                    }, 2500L);
                }
            });
            container.addView(adView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            adView.loadAd(new AdRequest.Builder().build());
        } catch (Throwable t) {
            Log.w(TAG, "Banner error", t);
        }
    }

    private static void setupRewarded(Activity activity, Button button, AppConfig config,
                                      String unitId) {
        final String id = AdIds.validRewarded(unitId);
        button.setOnClickListener(v -> showRewarded(activity, button, config, id, 0));
    }

    private static void showRewarded(Activity activity, Button button, AppConfig config,
                                     String unitId, int attempt) {
        if (activity.isFinishing()) {
            return;
        }
        RewardedAd ready = AdPreloader.takeRewarded();
        if (ready != null) {
            ready.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    AdPreloader.loadRewarded(activity.getApplicationContext(), config);
                }
            });
            ready.show(activity, rewardItem ->
                    Toast.makeText(activity, R.string.reward_received, Toast.LENGTH_SHORT).show());
            AdPreloader.loadRewarded(activity.getApplicationContext(), config);
            return;
        }

        button.setEnabled(false);
        Toast.makeText(activity, R.string.rewarded_loading, Toast.LENGTH_SHORT).show();

        final String adUnit = AdIds.validRewarded(unitId);
        MobileAdsGate.runWhenReady(activity, () -> RewardedAd.load(activity, adUnit,
                new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedAd ad) {
                        button.setEnabled(true);
                        if (activity.isFinishing()) {
                            return;
                        }
                        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                AdPreloader.loadRewarded(activity.getApplicationContext(), config);
                            }
                        });
                        ad.show(activity, rewardItem ->
                                Toast.makeText(activity, R.string.reward_received, Toast.LENGTH_SHORT).show());
                        AdPreloader.loadRewarded(activity.getApplicationContext(), config);
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        Log.w(TAG, "Rewarded fail: " + error.getCode() + " " + error.getMessage());
                        button.setEnabled(true);
                        if (attempt < 2) {
                            MAIN.postDelayed(
                                    () -> showRewarded(activity, button, config, unitId, attempt + 1),
                                    2000L);
                        } else {
                            Toast.makeText(activity, R.string.rewarded_not_ready, Toast.LENGTH_SHORT).show();
                            AdPreloader.loadRewarded(activity.getApplicationContext(), config);
                        }
                    }
                }));
    }

    private static String resolveBannerId(AppConfig config) {
        if (config.useTestAds || AdIds.isGoogleTestId(config.admobBannerId)) {
            return AdIds.BANNER;
        }
        return AdIds.validBanner(config.admobBannerId);
    }

    private static String resolveRewardedId(AppConfig config) {
        if (config.useTestAds || AdIds.isGoogleTestId(config.admobRewardedId)) {
            return AdIds.REWARDED;
        }
        return AdIds.validRewarded(config.admobRewardedId);
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
