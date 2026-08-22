package com.app.myapp.p770845;

import android.content.Context;

/**
 * Reads URL/content written at export — tries every asset file name.
 */
public final class ExportAssets {

    private static volatile String cachedUrl = "";
    private static volatile String cachedType = "";

    private ExportAssets() {}

    public static void preload(Context context) {
        if (context == null) {
            return;
        }
        cachedUrl = readUrl(context);
        cachedType = LaunchConfig.readAsset(context, "export_content_type.txt");
    }

    public static void clearCache() {
        cachedUrl = "";
        cachedType = "";
    }

    public static String getUrl(Context context) {
        if (!cachedUrl.isEmpty()) {
            return cachedUrl;
        }
        if (context != null) {
            cachedUrl = readUrl(context);
        }
        return cachedUrl;
    }

    public static String getContentType(Context context) {
        if (!cachedType.isEmpty()) {
            return cachedType;
        }
        if (context != null) {
            cachedType = LaunchConfig.readAsset(context, "export_content_type.txt");
        }
        return cachedType;
    }

    private static String readUrl(Context context) {
        for (String name : new String[]{"export_url.txt", "url.txt", "main_url.txt"}) {
            String v = LaunchConfig.readAsset(context, name);
            if (!v.isEmpty()) {
                return v.trim();
            }
        }
        return "";
    }
}
