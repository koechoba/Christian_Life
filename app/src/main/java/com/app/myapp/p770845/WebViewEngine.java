package com.app.myapp.p770845;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.RequiresApi;

/**
 * Production WebView setup: settings, in-app links, loading bar, errors + retry.
 */
public final class WebViewEngine {

    private static final String BRIDGE_NAME = "AndroidApp";
    private static final String VIEWPORT_JS =
            "(function(){try{var m=document.querySelector('meta[name=viewport]');"
                    + "if(!m){m=document.createElement('meta');m.name='viewport';"
                    + "m.content='width=device-width,initial-scale=1,maximum-scale=5,user-scalable=yes';"
                    + "document.head.appendChild(m);}}catch(e){}})();";

    public interface Callbacks {
        void onRetry();

        void onPageFinished(String url);

        /** Fired when enough content is painted to show the WebView (hides splash early). */
        default void onPageVisible() {
        }
    }

    private WebViewEngine() {}

    @SuppressLint("SetJavaScriptEnabled")
    public static void applySettings(WebView webView, AppConfig config) {
        if (webView == null || config == null) {
            return;
        }
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(config.jsEnabled);
        s.setDomStorageEnabled(config.domStorage);
        s.setDatabaseEnabled(config.domStorage);
        s.setBuiltInZoomControls(config.zoomControl);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(config.zoomControl);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        if (config.fileUpload) {
            s.setAllowFileAccessFromFileURLs(true);
            s.setAllowUniversalAccessFromFileURLs(true);
        }
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setLoadsImagesAutomatically(true);
        s.setBlockNetworkImage(false);
        s.setBlockNetworkLoads(false);
        s.setGeolocationEnabled(false);
        s.setSaveFormData(false);
        applyCacheMode(s, config.cacheMode);
        WebViewLinkHelper.applyWebViewDefaults(s);

        if (config.userAgent != null && !config.userAgent.trim().isEmpty()) {
            s.setUserAgentString(config.userAgent.trim());
        }

        try {
            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(config.cookiesEnabled);
            cm.setAcceptThirdPartyCookies(webView, config.cookiesEnabled);
        } catch (Exception ignored) {
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {){
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    public static void attachClients(WebView webView, AppConfig config, ProgressBar progressBar,
                                     Callbacks callbacks) {
        if (webView == null || config == null) {
            return;
        }
        final boolean linksInApp = config.externalLinksOpen;
        final Handler handler = new Handler(Looper.getMainLooper());
        final boolean[] mainFrameFailed = {false};

        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void retry() {
                handler.post(() -> {
                    if (callbacks != null) {
                        callbacks.onRetry();
                    }
                });
            }
        }, BRIDGE_NAME);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mainFrameFailed[0] = false;
                if (progressBar != null && config.loadingProgress) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(0);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return WebViewLinkHelper.handleNavigation(
                        view.getContext(), view, request, linksInApp);
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return WebViewLinkHelper.handleNavigation(
                        view.getContext(), view, url, linksInApp);
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                super.doUpdateVisitedHistory(view, url, isReload);
                if (!mainFrameFailed[0] && callbacks != null) {
                    callbacks.onPageFinished(url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!mainFrameFailed[0]) {
                    injectViewport(view);
                    if (callbacks != null) {
                        callbacks.onPageFinished(url);
                    }
                }
                if (progressBar != null && config.loadingProgress) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
             @RequiresApi(api = Build.VERSION_CODES.M)
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                if (request == null || !request.isForMainFrame()) {
                    return;
                }
                showError(view, config, mainFrameFailed);
            }

            @Override
            @SuppressWarnings("deprecation")
            public void onReceivedError(WebView view, int errorCode, String description,
                                        String failingUrl) {
                showError(view, config, mainFrameFailed);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture,
                                          Message resultMsg) {
                return WebViewLinkHelper.handleCreateWindow(view, resultMsg, linksInApp);
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress >= 15 && callbacks != null) {
                    callbacks.onPageVisible();
                }
                if (progressBar == null || !config.loadingProgress) {
                    return;
                }
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(newProgress);
                if (newProgress >= 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    public static void injectViewport(WebView webView) {
        if (webView == null) {
            return;
        }
        try {
            webView.evaluateJavascript(VIEWPORT_JS, null);
        } catch (Exception ignored) {
        }
    }

    /** Wraps HTML without viewport for mobile scaling. */
    public static String ensureMobileHtml(String html) {
        if (html == null || html.trim().isEmpty()) {
            return html;
        }
        String lower = html.toLowerCase();
        if (lower.contains("viewport") || lower.contains("<html")) {
            if (!lower.contains("viewport")) {
                return html.replaceFirst("(?i)<head>", "<head><meta name=\"viewport\" "
                        + "content=\"width=device-width,initial-scale=1,maximum-scale=5,user-scalable=yes\">");
            }
            return html;
        }
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" "
                + "content=\"width=device-width,initial-scale=1,maximum-scale=5,user-scalable=yes\">"
                + "</head><body>" + html + "</body></html>";
    }

    private static void showError(WebView view, AppConfig config, boolean[] failedFlag) {
        if (!config.customErrorPage) {
            return;
        }
        failedFlag[0] = true;
        try {
            view.loadUrl("file:///android_asset/error.html");
        } catch (Exception e) {
            view.loadData(
                    "<html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                            + "</head><body style='font-family:sans-serif;text-align:center;padding:32px'>"
                            + "<h2>Unable to load page</h2>"
                            + "<p>Check your connection.</p>"
                            + "<button onclick=\"AndroidApp.retry()\">Retry</button>"
                            + "</body></html>",
                    "text/html", "UTF-8");
        }
    }

    private static void applyCacheMode(WebSettings settings, String mode) {
        if (settings == null || mode == null) {
            return;
        }
        switch (mode) {
            case "no_cache":
                settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
                break;
            case "cache_only":
                settings.setCacheMode(WebSettings.LOAD_CACHE_ONLY);
                break;
            case "cache_else_network":
                settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                break;
            case "default":
            default:
                settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                break;
        }
    }
}
