package com.app.myapp.p770845;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.List;

/** Ensures MobileAds SDK is initialized before any ad load in exported APK. */
public final class MobileAdsGate {

    private static final String TAG = "MobileAdsGate";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static volatile boolean ready;
    private static volatile boolean initStarted;
    private static final List<Runnable> pending = new ArrayList<>();

    private MobileAdsGate() {}

    public static void ensureInit(Context context) {
        if (context == null) {
            return;
        }
        if (ready) {
            return;
        }
        Context app = context.getApplicationContext();
        synchronized (MobileAdsGate.class) {
            if (ready) {
                return;
            }
            if (!initStarted) {
                initStarted = true;
                try {
                    MobileAds.initialize(app, status -> MAIN.post(() -> {
                        Log.i(TAG, "MobileAds ready");
                        markReady();
                    }));
                } catch (Throwable t) {
                    Log.w(TAG, "MobileAds init error", t);
                    markReady();
                }
                MAIN.postDelayed(() -> {
                    if (!ready) {
                        Log.w(TAG, "MobileAds timeout — continue");
                        markReady();
                    }
                }, 5000);
            }
        }
    }

    /** Called from MobileAds.initialize callback when SDK is ready. */
    public static void markReady() {
        onReady();
    }

    private static void onReady() {
        if (ready) {
            return;
        }
        List<Runnable> copy;
        synchronized (pending) {
            ready = true;
            copy = new ArrayList<>(pending);
            pending.clear();
        }
        for (Runnable r : copy) {
            try {
                MAIN.post(r);
            } catch (Throwable t) {
                Log.w(TAG, "Ad task failed", t);
            }
        }
    }

    public static void runWhenReady(Context context, Runnable task) {
        if (task == null) {
            return;
        }
        ensureInit(context);
        if (ready) {
            MAIN.post(task);
            return;
        }
        synchronized (pending) {
            if (ready) {
                MAIN.post(task);
            } else {
                pending.add(task);
            }
        }
    }
}
