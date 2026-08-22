package com.app.myapp.p770845;

import android.app.Application;
import android.util.Log;

import com.google.android.gms.ads.MobileAds;

public class ChristianLifeApplication extends Application {

    private static final String TAG = "ChristianLifeApp";

    @Override
    public void onCreate() {
        LocaleHelper.forceEnglish(this);
        super.onCreate();
        WebViewWarmup.warm(this);
        ExportAssets.preload(this);
        try {
            MobileAdsGate.ensureInit(this);
            AppConfig config = AppConfig.load(this);
            if (config != null && config.adsEnabled) {
                Log.i(TAG, "Preload ads testMode=" + config.useTestAds
                        + " banner=" + config.admobBannerId);
                MobileAdsGate.runWhenReady(this,
                        () -> AdPreloader.warm(ChristianLifeApplication.this, config));
            }
        } catch (Throwable t) {
            Log.w(TAG, "Ads init skipped", t);
        }
    }
}
