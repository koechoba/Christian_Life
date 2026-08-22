package com.app.myapp.p770845;

import android.app.Activity;
import android.graphics.Color;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

final class UiStyleHelper {

    static final int SYSTEM_BAR_COLOR = Color.WHITE;
    static final int SYSTEM_TOOLBAR_TEXT = Color.BLACK;

    private UiStyleHelper() {}

    static int contrastTextColor(int bg) {
        int y = (299 * Color.red(bg) + 587 * Color.green(bg) + 114 * Color.blue(bg)) / 1000;
        return y >= 128 ? Color.BLACK : Color.WHITE;
    }

    static int parseColorSafe(String hex, int fallback) {
        try {
            if (hex == null || hex.isEmpty()) return fallback;
            String c = hex.trim();
            if (!c.startsWith("#")) c = "#" + c;
            return Color.parseColor(c);
        } catch (Exception e) {
            return fallback;
        }
    }

    /** When enabled: custom top bar color. When off: phone system default (white bars). */
    static void applySystemBars(Activity activity, int primaryColor, boolean customColorEnabled) {
        if (activity == null) {
            return;
        }
        if (!customColorEnabled) {
            applySystemDefaults(activity);
            return;
        }
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        WindowCompat.setDecorFitsSystemWindows(window, true);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(primaryColor);
        window.setNavigationBarColor(SYSTEM_BAR_COLOR);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                window, window.getDecorView());
        if (controller != null) {
            boolean lightStatusBg = contrastTextColor(primaryColor) == Color.BLACK;
            controller.setAppearanceLightStatusBars(lightStatusBg);
            controller.setAppearanceLightNavigationBars(true);
        }
    }

    static void applySystemDefaults(Activity activity) {
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        WindowCompat.setDecorFitsSystemWindows(window, true);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(SYSTEM_BAR_COLOR);
        window.setNavigationBarColor(SYSTEM_BAR_COLOR);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }
    }
}
