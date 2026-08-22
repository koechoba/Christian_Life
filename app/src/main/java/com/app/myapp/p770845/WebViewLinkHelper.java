package com.app.myapp.p770845;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Message;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Locale;

/**
 * Keeps http/https inside the WebView when keepLinksInApp is true.
 * tel:, mailto:, intent:, whatsapp: etc. open in other apps.
 */
public final class WebViewLinkHelper {

    private WebViewLinkHelper() {}

    /**
     * @param keepLinksInApp true = website links stay inside WebView; false = http(s) opens in browser
     */
    public static boolean handleNavigation(Context context, WebView webView,
                                         WebResourceRequest request, boolean keepLinksInApp) {
        if (request == null || !request.isForMainFrame()) {
            return false;
        }
        return handleNavigation(context, webView, request.getUrl(), keepLinksInApp);
    }

    public static boolean handleNavigation(Context context, WebView webView, String url,
                                         boolean keepLinksInApp) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        try {
            return handleNavigation(context, webView, Uri.parse(url), keepLinksInApp);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean handleNavigation(Context context, WebView webView, Uri uri,
                                         boolean keepLinksInApp) {
        if (uri == null) {
            return false;
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            return false;
        }
        scheme = scheme.toLowerCase(Locale.US);

        if ("http".equals(scheme) || "https".equals(scheme)) {
            if (!keepLinksInApp) {
                return openExternalApp(context, uri);
            }
            return false;
        }

        if ("file".equals(scheme) || "about".equals(scheme) || "data".equals(scheme)
                || "blob".equals(scheme)) {
            return false;
        }

        if ("intent".equals(scheme)) {
            return openIntentUrl(context, webView, uri.toString(), keepLinksInApp);
        }

        return openExternalApp(context, uri);
    }

    public static boolean handleCreateWindow(WebView parent, Message resultMsg,
                                             boolean keepLinksInApp) {
        if (parent == null || resultMsg == null
                || !(resultMsg.obj instanceof WebView.WebViewTransport)) {
            return false;
        }

        WebView child = new WebView(parent.getContext());
        copyEssentialSettings(parent.getSettings(), child.getSettings());

        child.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    return routeToParent(parent, request.getUrl().toString(), keepLinksInApp);
                }
                return false;
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return routeToParent(parent, url, keepLinksInApp);
            }
        });

        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(child);
        resultMsg.sendToTarget();
        return true;
    }

    private static boolean routeToParent(WebView parent, String url, boolean keepLinksInApp) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            if (keepLinksInApp) {
                parent.post(() -> parent.loadUrl(url));
                return true;
            }
            return openExternalApp(parent.getContext(), Uri.parse(url));
        }
        return handleNavigation(parent.getContext(), parent, url, keepLinksInApp);
    }

    public static void applyWebViewDefaults(WebSettings settings) {
        if (settings == null) {
            return;
        }
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        String ua = settings.getUserAgentString();
        if (ua != null && ua.contains("; wv")) {
            settings.setUserAgentString(ua.replace("; wv", ""));
        }
    }

    private static void copyEssentialSettings(WebSettings from, WebSettings to) {
        if (from == null || to == null) {
            return;
        }
        to.setJavaScriptEnabled(from.getJavaScriptEnabled());
        to.setDomStorageEnabled(from.getDomStorageEnabled());
        to.setAllowFileAccess(from.getAllowFileAccess());
        to.setAllowContentAccess(from.getAllowContentAccess());
        to.setMixedContentMode(from.getMixedContentMode());
        applyWebViewDefaults(to);
    }

    public static boolean openExternalApp(Context context, Uri uri) {
        if (context == null || uri == null) {
            return false;
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            return false;
        }
        scheme = scheme.toLowerCase(Locale.US);
        if ("http".equals(scheme) || "https".equals(scheme) || "file".equals(scheme)
                || "about".equals(scheme)) {
            return false;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if (!(context instanceof android.app.Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean openIntentUrl(Context context, WebView webView, String url,
                                         boolean keepLinksInApp) {
        if (context == null || url == null) {
            return false;
        }
        try {
            Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            if (!(context instanceof android.app.Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            String fallback = extractIntentFallback(url);
            if (fallback == null) {
                return false;
            }
            if (keepLinksInApp && webView != null
                    && (fallback.startsWith("http://") || fallback.startsWith("https://"))) {
                webView.post(() -> webView.loadUrl(fallback));
                return true;
            }
            return openExternalApp(context, Uri.parse(fallback));
        } catch (Exception e) {
            return false;
        }
    }

    private static String extractIntentFallback(String intentUrl) {
        int idx = intentUrl.indexOf("S.browser_fallback_url=");
        if (idx < 0) {
            return null;
        }
        String part = intentUrl.substring(idx + 23);
        int end = part.indexOf(';');
        if (end > 0) {
            part = part.substring(0, end);
        }
        return Uri.decode(part);
    }
}
