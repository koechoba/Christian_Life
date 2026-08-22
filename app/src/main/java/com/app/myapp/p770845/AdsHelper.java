package com.app.myapp.p770845;

import android.app.Activity;
import android.graphics.Color;
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
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

/**
 * AdMob for exported WebView apps — banner hidden until loaded (no empty white bar).
 */
public final class AdsHelper {

    private static final String TAG = "AdsHelper";
    private static final int BANNER_MAX_RETRY = 4;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AdsHelper() {}

    public static void warm(Activity activity, AppConfig config) {
        if (activity == null || config == null) {
            return;
        }
        AdPreloader.warm(activity, config);
    }

    public static void loadAppOpen(Activity activity, AppConfig config, Runnable onDone) {
        if (!config.showAppOpen() || activity.isFinishing()) {
            if (onDone != null) onDone.run();
            return;
        }
        String id = AdIds.validAppOpen(config.admobAppOpenId);
        AppOpenAd.load(activity, id, new AdRequest.Builder().build(),
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(AppOpenAd ad) {
                        if (activity.isFinishing()) {
                            if (onDone != null) onDone.run();
                            return;
                        }
                        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                if (onDone != null) onDone.run();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(
                                    com.google.android.gms.ads.AdError adError) {
                                if (onDone != null) onDone.run();
                            }
                        });
                        ad.show(activity);
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        Log.w(TAG, "App open failed: " + error.getMessage());
                        if (onDone != null) onDone.run();
                    }
                });
    }

    public static AdView attachBanner(Activity activity, FrameLayout container, AppConfig config) {
        if (container == null || !config.showBanner()) {
            if (container != null) {
                container.setVisibility(View.GONE);
                container.removeAllViews();
            }
            return null;
        }
        container.removeAllViews();
        showBannerLoadingSlot(container, activity);
        loadBannerWithRetry(activity, container, config, 0);
        return null;
    }

    private static void showBannerLoadingSlot(FrameLayout container, Activity activity) {
        float density = activity.getResources().getDisplayMetrics().density;
        int minPx = (int) (50f * density);
        container.setMinimumHeight(minPx);
        container.setBackgroundColor(Color.parseColor("#F5F5F5"));
        container.setVisibility(View.VISIBLE);
    }

    private static void loadBannerWithRetry(Activity activity, FrameLayout container,
                                          AppConfig config, int attempt) {
        if (activity == null || activity.isFinishing() || container == null) {
            return;
        }
        MobileAdsGate.runWhenReady(activity, () -> {
            if (activity.isFinishing()) {
                return;
            }
            try {
                container.removeAllViews();
                AdView adView = new AdView(activity);
                adView.setAdUnitId(AdIds.validBanner(config.admobBannerId));
                int widthDp = (int) (activity.getResources().getDisplayMetrics().widthPixels
                        / activity.getResources().getDisplayMetrics().density);
                adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                        activity, widthDp));
                adView.setAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        if (activity.isFinishing()) {
                            return;
                        }
                        container.setBackgroundColor(Color.WHITE);
                        container.setVisibility(View.VISIBLE);
                        Log.i(TAG, "Banner loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        Log.w(TAG, "Banner failed (" + attempt + "): " + error.getMessage());
                        container.setVisibility(View.GONE);
                        container.removeAllViews();
                        if (attempt + 1 < BANNER_MAX_RETRY) {
                            MAIN.postDelayed(
                                    () -> loadBannerWithRetry(activity, container, config, attempt + 1),
                                    1500L * (attempt + 1));
                        }
                    }
                });
                container.addView(adView, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                adView.loadAd(new AdRequest.Builder().build());
            } catch (Throwable t) {
                Log.w(TAG, "Banner setup error", t);
                container.setVisibility(View.GONE);
            }
        });
    }

    public static void setupRewardedButton(Activity activity, Button button, AppConfig config) {
        if (button == null) return;
        if (!config.showRewarded()) {
            button.setVisibility(View.GONE);
            return;
        }
        button.setVisibility(View.VISIBLE);
        button.setEnabled(true);
        button.setOnClickListener(v -> MobileAdsGate.runWhenReady(activity, () -> {
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
            RewardedAd.load(activity, AdIds.validRewarded(config.admobRewardedId),
                    new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
                        @Override
                        public void onAdLoaded(RewardedAd ad) {
                            ad.show(activity, rewardItem ->
                                    Toast.makeText(activity, R.string.reward_received, Toast.LENGTH_SHORT).show());
                            AdPreloader.loadRewarded(activity.getApplicationContext(), config);
                        }

                        @Override
                        public void onAdFailedToLoad(LoadAdError error) {
                            Toast.makeText(activity, R.string.rewarded_not_ready, Toast.LENGTH_SHORT).show();
                            AdPreloader.loadRewarded(activity.getApplicationContext(), config);
                        }
                    });
        }));
    }

    public static void showInterstitial(Activity activity, AppConfig config, Runnable after) {
        MobileAdsGate.runWhenReady(activity, () -> AdPreloader.showInterstitial(activity, config, after));
    }
}
