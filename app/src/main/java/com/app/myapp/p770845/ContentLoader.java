package com.app.myapp.p770845;

import android.content.Context;
import android.util.Log;
import android.webkit.WebView;

/**
 * Loads WebView content in exported APK — same rules as builder Run/Preview.
 */
public final class ContentLoader {

    private static final String TAG = "ContentLoader";
    private static final String ASSET_INDEX = "file:///android_asset/index.html";

    private ContentLoader() {}

    public static void load(WebView webView, Context context, AppConfig config) {
        if (webView == null || context == null) {
            return;
        }
        if (config == null) {
            config = AppConfig.load(context);
        }
        ExportAssets.clearCache();
        ExportAssets.preload(context);
        LaunchConfig.applyTo(config, context);

        String exportType = ExportAssets.getContentType(context);
        if (!exportType.isEmpty()) {
            config.contentType = exportType.trim();
        }

        String type = config.contentType != null ? config.contentType.trim() : "url";
        Log.i(TAG, "contentType=" + type + " mainUrl=" + safe(config.mainUrl));

        if ("html_code".equals(type)) {
            loadHtmlCode(webView, context, config);
            return;
        }
        if ("html_file".equals(type)) {
            loadHtmlFile(webView, context, config);
            return;
        }
        loadWebsite(webView, context, config);
    }

    private static void loadHtmlCode(WebView webView, Context context, AppConfig config) {
        String html = config.htmlContent;
        if (html == null || html.trim().isEmpty()) {
            html = LaunchConfig.readAsset(context, "index.html");
        }
        if (html != null && !html.trim().isEmpty()) {
            String base = LoadUrlHelper.htmlCodeBaseUrl(config);
            html = WebViewEngine.ensureMobileHtml(html);
            Log.i(TAG, "loadData html_code base=" + base);
            webView.loadDataWithBaseURL(base, html, "text/html", "UTF-8", null);
            return;
        }
        Log.i(TAG, "loadUrl asset index (html_code fallback)");
        webView.loadUrl(ASSET_INDEX);
    }

    private static void loadHtmlFile(WebView webView, Context context, AppConfig config) {
        String index = LaunchConfig.readAsset(context, "index.html");
        if (index != null && !index.trim().isEmpty()) {
            Log.i(TAG, "loadUrl asset index (html_file)");
            webView.loadUrl(ASSET_INDEX);
            return;
        }
        Log.w(TAG, "html_file missing index.html — fallback to html_code");
        loadHtmlCode(webView, context, config);
    }

    private static void loadWebsite(WebView webView, Context context, AppConfig config) {
        String url = resolveWebsiteUrl(context, config);
        if (!url.isEmpty()) {
            Log.i(TAG, "loadUrl " + url);
            webView.loadUrl(url);
            return;
        }
        Log.w(TAG, "No URL — showing setup message");
        webView.loadData(
                "<html><body style='font-family:sans-serif;padding:24px;color:#222'>"
                        + "<h2>" + escapeHtml(config.appName) + "</h2>"
                        + "<p>Website URL is missing. Open Christian Life App Builder → Step 2 → enter URL → export again.</p>"
                        + "</body></html>",
                "text/html", "UTF-8");
    }

    static String resolveWebsiteUrl(Context context, AppConfig config) {
        if (context != null) {
            String exported = ExportAssets.getUrl(context);
            if (!exported.isEmpty()) {
                if (exported.startsWith("http://") || exported.startsWith("https://")) {
                    return LoadUrlHelper.normalizeWebUrl(exported);
                }
                if (exported.startsWith("file://")) {
                    return exported;
                }
            }
        }
        if (config != null) {
            String fromConfig = LoadUrlHelper.resolveWebsiteUrl(context, config);
            if (!fromConfig.isEmpty()) {
                return fromConfig;
            }
            if (config.mainUrl != null && !config.mainUrl.trim().isEmpty()) {
                String raw = config.mainUrl.trim();
                if (!LoadUrlHelper.isShellPlaceholder(raw) && !ASSET_INDEX.equals(raw)) {
                    return LoadUrlHelper.normalizeWebUrl(raw);
                }
            }
        }
        return "";
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
