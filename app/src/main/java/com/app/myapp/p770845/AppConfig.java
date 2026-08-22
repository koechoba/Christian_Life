package com.app.myapp.p770845;

import org.json.JSONObject;

import java.util.regex.Pattern;

public class AppConfig {

    private static final Pattern HEX_COLOR = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8}|[A-Fa-f0-9]{3})$");

    public String appName = "Christian Life";
    /** Play Store package — used for Share button link. */
    public String packageName = "";
    public String mainUrl = "";
    public String primaryColor = "#6200EE";
    public String splashBgColor = "#FFFFFF";
    public int splashDurationMs = 300;
    public boolean toolbarShow = true;
    public String toolbarTitle = "Christian Life";
    public boolean toolbarBack = false;
    public boolean statusBarColorEnabled = false;
    public boolean jsEnabled = true;
    public boolean domStorage = true;
    public boolean fileUpload = true;
    public boolean downloadManager = true;
    /** When true, http/https links stay inside the WebView (recommended). */
    public boolean externalLinksOpen = true;
    public boolean pullRefresh = true;
    public boolean zoomControl = true;
    public boolean cookiesEnabled = true;
    public String cacheMode = "default";
    public String userAgent = "";
    public String injectCss = "";
    public String injectJs = "";
    public boolean adsEnabled = true;
    /** When true, exported app uses Google test ad units (matches test manifest). */
    public boolean useTestAds = true;
    public boolean bannerAdEnabled = true;
    public boolean interstitialAdEnabled = true;
    public boolean rewardedAdEnabled = true;
    public boolean appOpenAdEnabled = true;
    public int interstitialIntervalSec = 60;
    public String admobAppId = AdIds.APP_ID;
    public String admobBannerId = AdIds.BANNER;
    public String admobInterstitialId = AdIds.INTERSTITIAL;
    public String admobRewardedId = AdIds.REWARDED;
    public String admobAppOpenId = AdIds.APP_OPEN;
    public String contentType = "url";
    public String htmlContent = "";
    public boolean bottomNavEnabled = true;
    public boolean navHomeEnabled = true;
    public boolean navBackEnabled = true;
    public boolean navForwardEnabled = true;
    public boolean navRefreshEnabled = true;
    public boolean navShareEnabled = true;
    public boolean rewardedButtonEnabled = true;
    public boolean loadingProgress = true;
    public boolean customErrorPage = true;
    public String privacyPolicyUrl = "";
    public boolean requireInternet = true;

    public static AppConfig load(android.content.Context context) {
        AppConfig config = new AppConfig();
        try {
            String json = AssetFiles.readText(context, "app_config.json");
            if (!json.isEmpty()) {
                JSONObject o = new JSONObject(json);
                config.appName = optStr(o, "appName", config.appName);
                config.packageName = optStr(o, "packageName", config.packageName);
                String studioPkg = optStr(o, "studioPackageName", "");
                if ((config.packageName == null || config.packageName.isEmpty())
                        && !studioPkg.isEmpty()) {
                    config.packageName = studioPkg;
                }
                config.mainUrl = optStr(o, "mainUrl", config.mainUrl);
                String urlAlt = optStr(o, "url", "");
                if ((config.mainUrl == null || config.mainUrl.isEmpty()) && !urlAlt.isEmpty()) {
                    config.mainUrl = urlAlt;
                }
                config.primaryColor = optStr(o, "primaryColor", config.primaryColor);
                config.splashBgColor = optStr(o, "splashBgColor", config.splashBgColor);
                config.splashDurationMs = o.optInt("splashDurationMs", config.splashDurationMs);
                config.toolbarShow = o.optBoolean("toolbarShow", config.toolbarShow);
                config.toolbarTitle = optStr(o, "toolbarTitle", config.toolbarTitle);
                config.toolbarBack = o.optBoolean("toolbarBack", config.toolbarBack);
                config.statusBarColorEnabled = o.optBoolean("statusBarColorEnabled", config.statusBarColorEnabled);
                config.jsEnabled = o.optBoolean("jsEnabled", config.jsEnabled);
                config.domStorage = o.optBoolean("domStorage", config.domStorage);
                config.fileUpload = o.optBoolean("fileUpload", config.fileUpload);
                config.downloadManager = o.optBoolean("downloadManager", config.downloadManager);
                config.externalLinksOpen = o.optBoolean("externalLinksOpen", config.externalLinksOpen);
                config.pullRefresh = o.optBoolean("pullRefresh", config.pullRefresh);
                config.zoomControl = o.optBoolean("zoomControl", config.zoomControl);
                config.cookiesEnabled = o.optBoolean("cookiesEnabled", config.cookiesEnabled);
                config.cacheMode = optStr(o, "cacheMode", config.cacheMode);
                config.userAgent = optStr(o, "userAgent", config.userAgent);
                config.injectCss = optStr(o, "injectCss", config.injectCss);
                config.injectJs = optStr(o, "injectJs", config.injectJs);
                config.adsEnabled = o.optBoolean("adsEnabled", true);
                config.useTestAds = o.optBoolean("useTestAds", true);
                config.bannerAdEnabled = o.optBoolean("bannerAdEnabled", config.adsEnabled);
                config.interstitialAdEnabled = o.optBoolean("interstitialAdEnabled", config.adsEnabled);
                config.rewardedAdEnabled = o.optBoolean("rewardedAdEnabled", config.adsEnabled);
                config.appOpenAdEnabled = o.optBoolean("appOpenAdEnabled", config.adsEnabled);
                config.interstitialIntervalSec = o.optInt("interstitialIntervalSec", 60);
                config.admobAppId = optStr(o, "admobAppId", config.admobAppId);
                config.admobBannerId = optStr(o, "admobBannerId", config.admobBannerId);
                config.admobInterstitialId = optStr(o, "admobInterstitialId", config.admobInterstitialId);
                config.admobRewardedId = optStr(o, "admobRewardedId", config.admobRewardedId);
                config.admobAppOpenId = optStr(o, "admobAppOpenId", config.admobAppOpenId);
                config.contentType = optStr(o, "contentType", config.contentType);
                config.htmlContent = optStr(o, "htmlContent", config.htmlContent);
                config.bottomNavEnabled = o.optBoolean("bottomNavEnabled", true);
                config.navHomeEnabled = o.optBoolean("navHomeEnabled", true);
                config.navBackEnabled = o.optBoolean("navBackEnabled", true);
                config.navForwardEnabled = o.optBoolean("navForwardEnabled", true);
                config.navRefreshEnabled = o.optBoolean("navRefreshEnabled", true);
                config.navShareEnabled = o.optBoolean("navShareEnabled", true);
                config.rewardedButtonEnabled = o.optBoolean("rewardedButtonEnabled", true);
                config.loadingProgress = o.optBoolean("loadingProgress", true);
                config.customErrorPage = o.optBoolean("customErrorPage", true);
                config.privacyPolicyUrl = optStr(o, "privacyPolicyUrl", config.privacyPolicyUrl);
                config.requireInternet = o.optBoolean("requireInternet", true);
            }
        } catch (Exception ignored) {
        }
        applyExportFallbacks(context, config);
        if ((config.packageName == null || config.packageName.isEmpty()) && context != null) {
            config.packageName = context.getPackageName();
        }
        AdsConfigSync.apply(context, config);
        return finalizeConfig(config);
    }

    public String getPlayStoreShareUrl() {
        if (packageName == null || packageName.trim().isEmpty()) {
            return "";
        }
        return "https://play.google.com/store/apps/details?id=" + packageName.trim();
    }

    private static void applyExportFallbacks(android.content.Context context, AppConfig config) {
        String fallbackType = AssetFiles.readText(context, "export_content_type.txt");
        if (!fallbackType.isEmpty()) {
            config.contentType = fallbackType.trim();
        }
        String fallbackUrl = AssetFiles.readText(context, "export_url.txt");
        if (!fallbackUrl.isEmpty()) {
            String trimmed = fallbackUrl.trim();
            config.mainUrl = trimmed;
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                if (!"html_code".equals(config.contentType) && !"html_file".equals(config.contentType)) {
                    config.contentType = "url";
                }
            } else if (trimmed.startsWith("file://")) {
                if ("file:///android_asset/index.html".equals(trimmed)) {
                    if ("html_code".equals(config.contentType) || "html_file".equals(config.contentType)) {
                        // keep html mode
                    } else {
                        config.contentType = "html_file";
                    }
                }
            }
        }
        if ("html_code".equals(config.contentType)
                && (config.htmlContent == null || config.htmlContent.trim().isEmpty())) {
            String index = AssetFiles.readText(context, "index.html");
            if (!index.isEmpty()) {
                config.htmlContent = index;
            }
        }
    }

    public boolean showBanner() {
        return adsEnabled && bannerAdEnabled;
    }

    public boolean showInterstitial() {
        return adsEnabled && interstitialAdEnabled;
    }

    public boolean showRewarded() {
        return adsEnabled && rewardedAdEnabled && rewardedButtonEnabled;
    }

    public boolean showAppOpen() {
        return adsEnabled && appOpenAdEnabled;
    }

    private static AppConfig finalizeConfig(AppConfig config) {
        if (config.appName == null || config.appName.trim().isEmpty()
                || false) {
            config.appName = "Christian Life";
        }
        if (config.toolbarTitle == null || config.toolbarTitle.trim().isEmpty()
                || false) {
            config.toolbarTitle = config.appName;
        }
        config.primaryColor = safeColor(config.primaryColor, "#6200EE");
        config.splashBgColor = safeColor(config.splashBgColor, "#FFFFFF");
        if (config.mainUrl == null || config.mainUrl.trim().isEmpty()) {
            config.mainUrl = "";
        }
        config.mainUrl = config.mainUrl != null ? config.mainUrl.trim() : "";
        if (config.contentType == null || config.contentType.trim().isEmpty()) {
            config.contentType = "url";
        } else {
            config.contentType = config.contentType.trim();
        }
        if (!"url".equals(config.contentType)
                && (config.htmlContent != null && !config.htmlContent.trim().isEmpty())
                && !"html_file".equals(config.contentType)) {
            config.contentType = "html_code";
        }
        if ("html_code".equals(config.contentType) && config.htmlContent == null) {
            config.htmlContent = "";
        }
        if (config.packageName != null) {
            config.packageName = config.packageName.trim();
        }
        if (!config.adsEnabled) {
            config.bannerAdEnabled = false;
            config.interstitialAdEnabled = false;
            config.rewardedAdEnabled = false;
            config.appOpenAdEnabled = false;
        }
        if (config.useTestAds || AdIds.isGoogleTestId(config.admobAppId)) {
            config.useTestAds = true;
            config.admobAppId = AdIds.APP_ID;
            config.admobBannerId = AdIds.BANNER;
            config.admobInterstitialId = AdIds.INTERSTITIAL;
            config.admobRewardedId = AdIds.REWARDED;
            config.admobAppOpenId = AdIds.APP_OPEN;
        } else {
            config.admobAppId = AdIds.validAppId(config.admobAppId);
            config.admobBannerId = AdIds.validBanner(config.admobBannerId);
            config.admobInterstitialId = AdIds.validInterstitial(config.admobInterstitialId);
            config.admobRewardedId = AdIds.validRewarded(config.admobRewardedId);
            config.admobAppOpenId = AdIds.validAppOpen(config.admobAppOpenId);
        }
        config.interstitialIntervalSec = Math.max(15, Math.min(config.interstitialIntervalSec, 600));
        config.splashDurationMs = Math.max(0, Math.min(config.splashDurationMs, 800));
        if (!config.navHomeEnabled && !config.navBackEnabled && !config.navForwardEnabled
                && !config.navRefreshEnabled && !config.navShareEnabled) {
            config.bottomNavEnabled = false;
        }
        return config;
    }

    static String safeColor(String hex, String fallback) {
        if (hex == null) return fallback;
        String c = hex.trim();
        if (c.isEmpty() || c.contains("COLOR") || c.contains("PRIMARY") || c.contains("SPLASH")) {
            return fallback;
        }
        if (!c.startsWith("#")) c = "#" + c;
        if (HEX_COLOR.matcher(c).matches()) return c;
        return fallback;
    }

    private static String optStr(JSONObject o, String key, String fallback) {
        if (!o.has(key) || o.isNull(key)) return fallback;
        String v = o.optString(key, fallback);
        return v != null ? v : fallback;
    }
}
