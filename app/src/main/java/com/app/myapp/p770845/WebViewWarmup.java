package com.app.myapp.p770845;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;

/**
 * Initializes Chromium/WebView on a background frame so the first real page loads faster.
 */
public final class WebViewWarmup {

    private static volatile boolean warmed;

    private WebViewWarmup() {}

    public static void warm(Context context) {
        if (warmed || context == null) {
            return;
        }
        Context app = context.getApplicationContext();
        new Handler(Looper.getMainLooper()).post(() -> {
            if (warmed) {
                return;
            }
            try {
                WebView probe = new WebView(app);
                probe.getSettings().setJavaScriptEnabled(true);
                probe.loadData("<html><body></body></html>", "text/html", "UTF-8");
                probe.destroy();
                warmed = true;
            } catch (Throwable ignored) {
            }
        });
    }
}
