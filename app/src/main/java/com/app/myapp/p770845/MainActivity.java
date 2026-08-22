package com.app.myapp.p770845;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.ads.AdView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "GeneratedWebView";
    private static final int MAX_SPLASH_WAIT_MS = 400;
    private static final int BLANK_GUARD_MS = 700;

    private WebView webView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private FrameLayout bannerContainer;
    private FrameLayout splashOverlay;
    private LinearLayout mainContent;
    private AdView bannerAdView;
    private Button btnRewardedAd;
    private AppConfig config;
    private WebViewHistoryHelper historyHelper;
    private MaterialToolbar toolbar;
    private boolean adsUiReady;
    private boolean mainUiReady;
    private boolean splashDismissed;
    private boolean firstPageReady;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable splashTimeoutRunnable;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            ExportAssets.preload(this);
            config = AppConfig.load(this);
            LaunchConfig.applyTo(config, this);
            setContentView(R.layout.activity_main);
            splashOverlay = findViewById(R.id.splashOverlay);
            mainContent = findViewById(R.id.mainContent);
            bannerContainer = findViewById(R.id.bannerContainer);
            btnRewardedAd = findViewById(R.id.btnRewardedAd);

            if (mainContent != null) {
                mainContent.setVisibility(View.VISIBLE);
            }

            setupSplashScreen();
            applyHeaderTheme();
            beginFastWebViewLoad();
            scheduleSplashTimeout();

            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (splashOverlay != null && splashOverlay.getVisibility() == View.VISIBLE) {
                        finish();
                        return;
                    }
                    if (historyHelper != null) {
                        if (historyHelper.navigateBack()) {
                            return;
                        }
                        finish();
                        return;
                    }
                    finish();
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "Startup failed", t);
            showFatalFallback(t);
        }
    }

    private void setupSplashScreen() {
        if (splashOverlay == null || config == null) return;
        int bg = UiStyleHelper.parseColorSafe(config.splashBgColor, Color.WHITE);
        splashOverlay.setBackgroundColor(bg);

        ImageView splashLogo = findViewById(R.id.splashLogo);
        BrandingAssets.loadSplashLogo(this, splashLogo);

        TextView splashAppName = findViewById(R.id.splashAppName);
        if (splashAppName != null) {
            String title = config.appName != null && !config.appName.isEmpty()
                    ? config.appName : getString(R.string.app_name);
            splashAppName.setText(title);
            splashAppName.setTextColor(UiStyleHelper.contrastTextColor(bg));
        }
    }

    /** Start WebView behind splash immediately — page loads while splash is visible. */
    private void beginFastWebViewLoad() {
        setupWebView();
        loadMainContent();
        finishMainUiSetup();
        scheduleBlankGuard();
    }

    private void scheduleSplashTimeout() {
        int wait = config != null ? config.splashDurationMs : 0;
        wait = Math.min(Math.max(wait, 0), MAX_SPLASH_WAIT_MS);
        if (splashTimeoutRunnable != null) {
            handler.removeCallbacks(splashTimeoutRunnable);
        }
        splashTimeoutRunnable = this::dismissSplashOverlay;
        if (wait <= 0) {
            handler.post(splashTimeoutRunnable);
        } else {
            handler.postDelayed(splashTimeoutRunnable, wait);
        }
    }

    private void onFirstPageReady() {
        if (firstPageReady) {
            return;
        }
        firstPageReady = true;
        dismissSplashOverlay();
        startAdsEarly();
    }

    private void dismissSplashOverlay() {
        if (splashDismissed || isFinishing()) {
            return;
        }
        splashDismissed = true;
        if (splashTimeoutRunnable != null) {
            handler.removeCallbacks(splashTimeoutRunnable);
        }
        if (splashOverlay != null) {
            splashOverlay.setVisibility(View.GONE);
        }
        if (!mainUiReady) {
            finishMainUiSetup();
        }
    }

    private void finishMainUiSetup() {
        if (mainUiReady) {
            return;
        }
        mainUiReady = true;
        try {
            if (config == null) {
                config = AppConfig.load(this);
            }
            setupToolbar();
        } catch (Throwable t) {
            Log.w(TAG, "Toolbar setup skipped", t);
        }
        try {
            setupSwipeRefresh();
        } catch (Throwable t) {
            Log.w(TAG, "SwipeRefresh setup skipped", t);
        }
        try {
            setupBottomNav();
        } catch (Throwable t) {
            Log.w(TAG, "Bottom nav setup skipped", t);
        }
    }

    private void scheduleBlankGuard() {
        handler.postDelayed(() -> {
            if (webView == null || isFinishing()) {
                return;
            }
            String current = webView.getUrl();
            if (current == null || current.isEmpty()
                    || "about:blank".equals(current)
                    || current.startsWith("data:text/html")) {
                Log.w(TAG, "Blank guard reload");
                loadMainContent();
            }
        }, BLANK_GUARD_MS);
    }

    private void startAdsEarly() {
        if (adsUiReady) {
            return;
        }
        config = AppConfig.load(this);
        LaunchConfig.applyTo(config, this);
        if (config == null || !config.adsEnabled) {
            return;
        }
        adsUiReady = true;
        AdsBootstrap.start(this, config, bannerContainer, btnRewardedAd, () -> {
            AppConfig adCfg = AppConfig.load(MainActivity.this);
            if (adCfg != null && adCfg.showInterstitial()) {
                AdPreloader.showInterstitial(MainActivity.this, adCfg, null);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null && !isFinishing()) {
            String current = webView.getUrl();
            if (current == null || current.isEmpty()
                    || "about:blank".equals(current)
                    || current.startsWith("data:text/html")) {
                loadMainContent();
            }
        }
        if (bannerContainer != null) {
            for (int i = 0; i < bannerContainer.getChildCount(); i++) {
                View child = bannerContainer.getChildAt(i);
                if (child instanceof AdView) {
                    bannerAdView = (AdView) child;
                    try {
                        bannerAdView.resume();
                    } catch (Exception ignored) {
                    }
                    break;
                }
            }
        }
        if (!adsUiReady && config != null) {
            startAdsEarly();
        } else if (config != null) {
            AdsBootstrap.retryBannerIfEmpty(this, config, bannerContainer);
        }
    }

    private void applyHeaderTheme() {
        if (config == null) {
            return;
        }
        int primary = UiStyleHelper.parseColorSafe(config.primaryColor, Color.parseColor("#6750A4"));
        UiStyleHelper.applySystemBars(this, primary, config.statusBarColorEnabled);
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        if (toolbar == null || config == null) return;
        int primary = UiStyleHelper.parseColorSafe(config.primaryColor, Color.parseColor("#6750A4"));
        boolean customColor = config.statusBarColorEnabled;
        int toolbarBg = customColor ? primary : UiStyleHelper.SYSTEM_BAR_COLOR;
        int titleColor = customColor ? UiStyleHelper.contrastTextColor(primary) : UiStyleHelper.SYSTEM_TOOLBAR_TEXT;
        UiStyleHelper.applySystemBars(this, primary, customColor);

        if (!config.toolbarShow) {
            toolbar.setVisibility(View.GONE);
            toolbar.setNavigationIcon(null);
            attachHistoryHelper(null, titleColor);
            return;
        }

        toolbar.setVisibility(View.VISIBLE);
        toolbar.setBackgroundColor(toolbarBg);
        toolbar.setTitleTextColor(titleColor);
        toolbar.setTitle(config.toolbarTitle != null && !config.toolbarTitle.trim().isEmpty()
                ? config.toolbarTitle.trim()
                : config.appName);

        MaterialToolbar backToolbar = null;
        if (config.toolbarBack) {
            toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
            Drawable nav = toolbar.getNavigationIcon();
            if (nav != null) {
                androidx.core.graphics.drawable.DrawableCompat.setTint(nav, titleColor);
            }
            backToolbar = toolbar;
        } else {
            toolbar.setNavigationIcon(null);
            toolbar.setNavigationOnClickListener(null);
        }
        attachHistoryHelper(backToolbar, titleColor);
    }

    private void attachHistoryHelper(MaterialToolbar backToolbar, int titleColor) {
        String home = LoadUrlHelper.resolveHomeUrl(config);
        historyHelper = new WebViewHistoryHelper(home);
        historyHelper.attach(webView, backToolbar, titleColor, new WebViewHistoryHelper.NavigationCallbacks() {
            @Override
            public void loadHome() {
                loadMainContent();
            }

            @Override
            public void exitApp() {
                finish();
            }
        });
    }

    private void setupWebView() {
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        if (webView == null || config == null) return;

        WebViewEngine.applySettings(webView, config);
        WebViewEngine.attachClients(webView, config, progressBar, new WebViewEngine.Callbacks() {
            @Override
            public void onRetry() {
                loadMainContent();
            }

            @Override
            public void onPageFinished(String url) {
                if (historyHelper != null) {
                    historyHelper.onPageFinished(url);
                }
                injectCustomCode();
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
                if (url == null || !url.contains("error.html")) {
                    onFirstPageReady();
                }
            }

            @Override
            public void onPageVisible() {
                onFirstPageReady();
            }
        });
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        View footerDivider = findViewById(R.id.footerDivider);
        if (nav == null || config == null) return;
        if (!config.bottomNavEnabled) {
            nav.setVisibility(View.GONE);
            if (footerDivider != null) footerDivider.setVisibility(View.GONE);
            return;
        }
        nav.setVisibility(View.VISIBLE);
        if (footerDivider != null) footerDivider.setVisibility(View.VISIBLE);
        android.view.Menu menu = nav.getMenu();
        setMenuVisible(menu, R.id.nav_home, config.navHomeEnabled);
        setMenuVisible(menu, R.id.nav_back, config.navBackEnabled);
        setMenuVisible(menu, R.id.nav_forward, config.navForwardEnabled);
        setMenuVisible(menu, R.id.nav_refresh, config.navRefreshEnabled);
        setMenuVisible(menu, R.id.nav_share, config.navShareEnabled);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadMainContent();
                if (historyHelper != null) {
                    historyHelper.resetStack(LoadUrlHelper.resolveHomeUrl(config));
                }
                return true;
            }
            if (id == R.id.nav_back) {
                if (historyHelper != null) {
                    historyHelper.navigateBack();
                }
                return true;
            }
            if (id == R.id.nav_forward) {
                if (webView != null && webView.canGoForward()) webView.goForward();
                return true;
            }
            if (id == R.id.nav_refresh) {
                loadMainContent();
                return true;
            }
            if (id == R.id.nav_share) {
                shareCurrentPage();
                return true;
            }
            return false;
        });
        if (config.navHomeEnabled) {
            nav.setSelectedItemId(R.id.nav_home);
        }
    }

    private static void setMenuVisible(android.view.Menu menu, int id, boolean visible) {
        if (menu == null) return;
        android.view.MenuItem item = menu.findItem(id);
        if (item != null) item.setVisible(visible);
    }

    private void shareCurrentPage() {
        try {
            String text = config != null ? config.getPlayStoreShareUrl() : "";
            if (text == null || text.isEmpty()) {
                text = getPackageName() != null
                        ? "https://play.google.com/store/apps/details?id=" + getPackageName()
                        : (config != null ? config.mainUrl : "");
            }
            String appLabel = config != null && config.appName != null ? config.appName : getString(R.string.app_name);
            String message = appLabel + "\n" + text;
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, message);
            startActivity(Intent.createChooser(share, getString(R.string.nav_share)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.nav_share, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSwipeRefresh() {
        swipeRefresh = findViewById(R.id.swipeRefresh);
        if (swipeRefresh == null || config == null) return;
        if (config.pullRefresh) {
            swipeRefresh.setEnabled(true);
            swipeRefresh.setOnRefreshListener(() -> {
                loadMainContent();
                swipeRefresh.setRefreshing(false);
            });
        } else {
            swipeRefresh.setEnabled(false);
        }
    }

    private void loadMainContent() {
        if (webView == null) return;
        if (config == null) {
            config = AppConfig.load(this);
            LaunchConfig.applyTo(config, this);
        }
        if (config == null) return;

        ExportAssets.clearCache();
        ExportAssets.preload(this);
        LaunchConfig.applyTo(config, this);

        webView.setBackgroundColor(Color.WHITE);

        try {
            ContentLoader.load(webView, this, config);
            String home = resolveHistoryHome();
            if (historyHelper != null && !home.isEmpty()) {
                historyHelper.resetStack(home);
            }
        } catch (Exception e) {
            Log.e(TAG, "Load failed", e);
            if (config.customErrorPage) {
                try {
                    webView.loadUrl("file:///android_asset/error.html");
                } catch (Exception ignored) {
                }
            }
        }
    }

    private String resolveHistoryHome() {
        String type = config.contentType != null ? config.contentType.trim() : "url";
        if ("html_code".equals(type)) {
            return LoadUrlHelper.htmlCodeBaseUrl(config);
        }
        if ("html_file".equals(type)) {
            return "file:///android_asset/index.html";
        }
        String url = LaunchConfig.resolveLoadUrl(this, config);
        if (url.isEmpty()) {
            url = LoadUrlHelper.resolveWebsiteUrl(this, config);
        }
        return url;
    }

    private void injectCustomCode() {
        if (webView == null || config == null) return;
        try {
            if (config.injectCss != null && !config.injectCss.isEmpty()) {
                String css = config.injectCss.replace("\\", "\\\\").replace("'", "\\'");
                webView.evaluateJavascript(
                        "(function(){var s=document.createElement('style');s.textContent='"
                                + css + "';document.head.appendChild(s);})();", null);
            }
            if (config.injectJs != null && !config.injectJs.trim().isEmpty()) {
                webView.evaluateJavascript(config.injectJs, null);
            }
        } catch (Exception ignored) {
        }
    }

    private void showFatalFallback(Throwable cause) {
        try {
            if (config == null) config = AppConfig.load(this);
            setContentView(R.layout.activity_main);
            splashOverlay = findViewById(R.id.splashOverlay);
            mainContent = findViewById(R.id.mainContent);
            if (splashOverlay != null) splashOverlay.setVisibility(View.GONE);
            if (mainContent != null) mainContent.setVisibility(View.VISIBLE);
            webView = findViewById(R.id.webView);
            if (webView != null && config != null) {
                beginFastWebViewLoad();
                return;
            }
        } catch (Throwable t) {
            Log.e(TAG, "Fallback failed", t);
        }
        try {
            webView = new WebView(this);
            setContentView(webView);
            webView.getSettings().setJavaScriptEnabled(true);
            if (config != null) {
                setupWebView();
                loadMainContent();
                return;
            } else {
                webView.loadData("<h3>Startup error</h3><p>Re-export from Christian Life App Builder.</p>",
                        "text/html", "UTF-8");
            }
        } catch (Throwable t) {
            finish();
        }
    }

    @Override
    protected void onPause() {
        if (bannerAdView != null) {
            try {
                bannerAdView.pause();
            } catch (Exception ignored) {
            }
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (bannerAdView != null) {
            try {
                bannerAdView.destroy();
            } catch (Exception ignored) {
            }
        }
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
