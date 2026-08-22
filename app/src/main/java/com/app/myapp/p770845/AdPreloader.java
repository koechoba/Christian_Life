package com.app.myapp.p770845;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

/** Preloads interstitial + rewarded for exported APK. */
public final class AdPreloader {

    private static final String TAG = "AdPreloader";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static volatile InterstitialAd interstitial;
    private static volatile RewardedAd rewarded;
    private static long lastInterstitialShownMs;
    private static boolean firstInterstitial = true;

    private AdPreloader() {}

    public static void warm(Context context, AppConfig config) {
        if (context == null || config == null || !config.adsEnabled) {
            return;
        }
        Context app = context.getApplicationContext();
        MobileAdsGate.runWhenReady(context, () -> {
            if (config.showInterstitial() && interstitial == null) {
                loadInterstitial(app, config);
            }
            if (config.rewardedAdEnabled && rewarded == null) {
                loadRewarded(app, config);
            }
        });
    }

    public static void loadInterstitial(Context context, AppConfig config) {
        if (context == null || config == null || !config.showInterstitial()) {
            return;
        }
        String id = resolveInterstitialId(config);
        InterstitialAd.load(context, id, new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        interstitial = ad;
                        Log.i(TAG, "Interstitial preloaded");
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        Log.w(TAG, "Interstitial preload: " + error.getCode() + " " + error.getMessage());
                        interstitial = null;
                        MAIN.postDelayed(() -> loadInterstitial(context, config), 3000L);
                    }
                });
    }

    public static void loadRewarded(Context context, AppConfig config) {
        if (context == null || config == null || !config.rewardedAdEnabled) {
            return;
        }
        String id = resolveRewardedId(config);
        RewardedAd.load(context, id, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(RewardedAd ad) {
                rewarded = ad;
                Log.i(TAG, "Rewarded preloaded");
            }

            @Override
            public void onAdFailedToLoad(LoadAdError error) {
                Log.w(TAG, "Rewarded preload: " + error.getCode() + " " + error.getMessage());
                rewarded = null;
                MAIN.postDelayed(() -> loadRewarded(context, config), 3000L);
            }
        });
    }

    public static RewardedAd takeRewarded() {
        RewardedAd ad = rewarded;
        rewarded = null;
        return ad;
    }

    public static void showInterstitial(Activity activity, AppConfig config, Runnable after) {
        if (activity == null || activity.isFinishing() || config == null || !config.showInterstitial()) {
            if (after != null) {
                after.run();
            }
            return;
        }
        long intervalMs = Math.max(8_000L, config.interstitialIntervalSec * 1000L);
        long now = System.currentTimeMillis();
        if (!firstInterstitial && now - lastInterstitialShownMs < intervalMs) {
            if (after != null) {
                after.run();
            }
            return;
        }
        firstInterstitial = false;

        String id = resolveInterstitialId(config);
        InterstitialAd ready = interstitial;
        interstitial = null;

        if (ready != null) {
            ready.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    lastInterstitialShownMs = System.currentTimeMillis();
                    loadInterstitial(activity.getApplicationContext(), config);
                    if (after != null) {
                        after.run();
                    }
                }

                @Override
                public void onAdFailedToShowFullScreenContent(
                        com.google.android.gms.ads.AdError e) {
                    loadInterstitial(activity.getApplicationContext(), config);
                    if (after != null) {
                        after.run();
                    }
                }
            });
            try {
                ready.show(activity);
                return;
            } catch (Throwable t) {
                Log.w(TAG, "Interstitial show", t);
            }
        }

        MobileAdsGate.runWhenReady(activity, () -> InterstitialAd.load(activity, id,
                new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        if (activity.isFinishing()) {
                            if (after != null) {
                                after.run();
                            }
                            return;
                        }
                        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                lastInterstitialShownMs = System.currentTimeMillis();
                                loadInterstitial(activity.getApplicationContext(), config);
                                if (after != null) {
                                    after.run();
                                }
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(
                                    com.google.android.gms.ads.AdError e) {
                                loadInterstitial(activity.getApplicationContext(), config);
                                if (after != null) {
                                    after.run();
                                }
                            }
                        });
                        ad.show(activity);
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        Log.w(TAG, "Interstitial load: " + error.getCode() + " " + error.getMessage());
                        loadInterstitial(activity.getApplicationContext(), config);
                        if (after != null) {
                            after.run();
                        }
                    }
                }));
    }

    private static String resolveInterstitialId(AppConfig config) {
        if (config.useTestAds || AdIds.isGoogleTestId(config.admobInterstitialId)) {
            return AdIds.INTERSTITIAL;
        }
        return AdIds.validInterstitial(config.admobInterstitialId);
    }

    private static String resolveRewardedId(AppConfig config) {
        if (config.useTestAds || AdIds.isGoogleTestId(config.admobRewardedId)) {
            return AdIds.REWARDED;
        }
        return AdIds.validRewarded(config.admobRewardedId);
    }
}
