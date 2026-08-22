package com.app.myapp.p770845;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Safe URL handling for WebView — never prefixes https:// on file:// or asset paths.
 */
public final class LoadUrlHelper {

    private static final String ASSET_INDEX = "file:///android_asset/index.html";
    /** Shell template placeholder — not a real user URL. */
    private static final String SHELL_PLACEHOLDER_URL = "https://www.google.com";

    private LoadUrlHelper() {}

    public static boolean isWebUrl(String url) {
        if (url == null) {
            return false;
        }
        String u = url.trim().toLowerCase();
        return u.startsWith("http://") || u.startsWith("https://");
    }

    public static boolean isFileUrl(String url) {
        if (url == null) {
            return false;
        }
        return url.trim().toLowerCase().startsWith("file://");
    }

    /** Normalizes website URLs only; leaves file:// and other schemes unchanged. */
    public static String normalizeWebUrl(String url) {
        if (url == null) {
            return "";
        }
        String u = url.trim();
        if (u.isEmpty()) {
            return "";
        }
        if (isWebUrl(u) || isFileUrl(u) || u.startsWith("about:") || u.startsWith("data:")) {
            return u;
        }
        return "https://" + u;
    }

    public static String resolveHomeUrl(AppConfig config) {
        if (config == null) {
            return "";
        }
        String type = config.contentType != null ? config.contentType.trim() : "url";
        if ("html_file".equals(type)) {
            return ASSET_INDEX;
        }
        if ("html_code".equals(type)) {
            return htmlCodeBaseUrl(config);
        }
        String web = resolveWebsiteUrl(config);
        return web.isEmpty() ? ASSET_INDEX : web;
    }

    public static String resolveWebsiteUrl(AppConfig config) {
        return resolveWebsiteUrl(null, config);
    }

    /** Website URL for WebView — export_url.txt wins (written at APK build time). */
    public static String resolveWebsiteUrl(android.content.Context context, AppConfig config) {
        if (context != null) {
            String exported = readExportUrl(context);
            if (!exported.isEmpty()) {
                if (exported.startsWith("http://") || exported.startsWith("https://")) {
                    return normalizeWebUrl(exported);
                }
                if (exported.startsWith("file://")) {
                    return exported;
                }
            }
        }
        if (config == null) {
            return "";
        }
        String url = config.mainUrl != null ? config.mainUrl.trim() : "";
        if (url.isEmpty() || ASSET_INDEX.equals(url) || isShellPlaceholder(url)) {
            return "";
        }
        return normalizeWebUrl(url);
    }

    static boolean isShellPlaceholder(String url) {
        if (url == null) {
            return true;
        }
        String u = url.trim();
        return u.isEmpty() || SHELL_PLACEHOLDER_URL.equalsIgnoreCase(u);
    }

    static String readExportUrl(android.content.Context context) {
        if (context == null) {
            return "";
        }
        return ExportAssets.getUrl(context);
    }

    public static String htmlCodeBaseUrl(AppConfig config) {
        if (config != null && isWebUrl(config.mainUrl)) {
            return config.mainUrl.trim();
        }
        return "https://app.local/";
    }
}
