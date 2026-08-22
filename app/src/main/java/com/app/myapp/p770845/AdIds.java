package com.app.myapp.p770845;

/** Google AdMob official test IDs — used in all exported apps until you add real IDs. */
public final class AdIds {

    public static final String APP_ID = "ca-app-pub-3940256099942544~3347511713";
    public static final String BANNER = "ca-app-pub-3940256099942544/6300978111";
    public static final String INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    public static final String REWARDED = "ca-app-pub-3940256099942544/5224354917";
    public static final String APP_OPEN = "ca-app-pub-3940256099942544/9257395921";

    private AdIds() {}

    private static final String GOOGLE_TEST_PUBLISHER = "3940256099942544";

    public static String validAppId(String id) {
        return isRealAppId(id) ? id.trim() : APP_ID;
    }

    public static String validBanner(String id) {
        return isRealUnitId(id) ? id.trim() : BANNER;
    }

    public static String validInterstitial(String id) {
        return isRealUnitId(id) ? id.trim() : INTERSTITIAL;
    }

    public static String validRewarded(String id) {
        return isRealUnitId(id) ? id.trim() : REWARDED;
    }

    public static String validAppOpen(String id) {
        return isRealUnitId(id) ? id.trim() : APP_OPEN;
    }

    public static boolean isGoogleTestId(String id) {
        return id != null && id.contains(GOOGLE_TEST_PUBLISHER);
    }

    public static boolean isRealAppId(String id) {
        if (id == null) {
            return false;
        }
        String t = id.trim();
        return t.matches("ca-app-pub-\\d+~\\S+") && !t.contains("ADMOB") && !isGoogleTestId(t);
    }

    public static String publisherFromAppId(String appId) {
        if (appId == null) {
            return "";
        }
        int pub = appId.indexOf("pub-");
        int tilde = appId.indexOf('~');
        if (pub < 0 || tilde <= pub + 4) {
            return "";
        }
        return appId.substring(pub + 4, tilde);
    }

    public static String publisherFromUnitId(String unitId) {
        if (unitId == null) {
            return "";
        }
        int pub = unitId.indexOf("pub-");
        int slash = unitId.indexOf('/');
        if (pub < 0 || slash <= pub + 4) {
            return "";
        }
        return unitId.substring(pub + 4, slash);
    }

    public static boolean samePublisher(String appId, String unitId) {
        String a = publisherFromAppId(appId);
        String u = publisherFromUnitId(unitId);
        return !a.isEmpty() && a.equals(u);
    }

    public static boolean isRealUnitId(String id) {
        if (id == null) {
            return false;
        }
        String t = id.trim();
        return t.matches("ca-app-pub-\\d+/\\S+")
                && !t.contains("ADMOB")
                && !t.contains("BANNER")
                && !t.contains("INTERSTITIAL")
                && !t.contains("REWARDED")
                && !isGoogleTestId(t);
    }
}
