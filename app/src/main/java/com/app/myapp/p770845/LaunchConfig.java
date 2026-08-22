package com.app.myapp.p770845;

import android.content.Context;

import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the website URL written at export time — never fails silently. */
public final class LaunchConfig {

    private static final Pattern MAIN_URL_JSON =
            Pattern.compile("\"mainUrl\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern URL_JSON =
            Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");

    private LaunchConfig() {}

    public static void applyTo(AppConfig config, Context context) {
        if (config == null || context == null) {
            return;
        }
        String exportType = ExportAssets.getContentType(context);
        if (!exportType.isEmpty()) {
            config.contentType = exportType.trim();
        }

        String exported = readAsset(context, "export_url.txt");
        if (!exported.isEmpty()) {
            config.mainUrl = exported.trim();
            if (exported.startsWith("http://") || exported.startsWith("https://")) {
                if (exportType.isEmpty() || "url".equals(exportType)) {
                    config.contentType = "url";
                }
            } else if (exported.startsWith("file://")) {
                if (exportType.isEmpty()) {
                    config.contentType = "html_file";
                }
            }
            return;
        }
        String fromJson = readUrlFromConfigJson(context);
        if (!fromJson.isEmpty()) {
            config.mainUrl = fromJson;
            if (exportType.isEmpty() || "url".equals(exportType)) {
                config.contentType = "url";
            }
        }
    }

    public static String resolveLoadUrl(Context context, AppConfig config) {
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
        if (config == null) {
            return "";
        }
        String type = config.contentType != null ? config.contentType.trim() : "url";
        if ("html_file".equals(type)) {
            return "file:///android_asset/index.html";
        }
        if ("html_code".equals(type)) {
            return "";
        }
        return LoadUrlHelper.resolveWebsiteUrl(config);
    }

    private static String readUrlFromConfigJson(Context context) {
        String json = readAsset(context, "app_config.json");
        if (json.isEmpty()) {
            return "";
        }
        try {
            JSONObject o = new JSONObject(json);
            String main = o.optString("mainUrl", "");
            if (main.isEmpty()) {
                main = o.optString("url", "");
            }
            if (!main.isEmpty() && !LoadUrlHelper.isShellPlaceholder(main)
                    && !"file:///android_asset/index.html".equals(main)) {
                return main.trim();
            }
        } catch (Exception ignored) {
            Matcher m = MAIN_URL_JSON.matcher(json);
            if (m.find()) {
                String v = m.group(1);
                if (!LoadUrlHelper.isShellPlaceholder(v)) {
                    return v;
                }
            }
            m = URL_JSON.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        }
        return "";
    }

    static String readAsset(Context context, String name) {
        return AssetFiles.readText(context, name);
    }
}
