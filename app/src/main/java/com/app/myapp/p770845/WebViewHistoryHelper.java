package com.app.myapp.p770845;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.appcompat.R;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Back navigation for WebView + HTML (loadDataWithBaseURL has no reliable canGoBack()).
 */
public final class WebViewHistoryHelper {

    private static final String SPA_HOOK_JS =
            "(function(){if(window.__wvHistoryHook)return;"
                    + "window.__wvHistoryHook=true;"
                    + "function n(){if(window.AndroidHistory)AndroidHistory.onNavigate(location.href);}"
                    + "var ps=history.pushState;history.pushState=function(){ps.apply(history,arguments);n();};"
                    + "var rs=history.replaceState;history.replaceState=function(){rs.apply(history,arguments);n();};"
                    + "window.addEventListener('popstate',n);"
                    + "window.addEventListener('hashchange',n);"
                    + "document.addEventListener('click',function(e){"
                    + "var a=e.target;while(a&&a.tagName!=='A')a=a.parentElement;"
                    + "if(a&&a.href)setTimeout(function(){n();},50);},true);})();";

    public interface NavigationCallbacks {
        void loadHome();

        void exitApp();
    }

    private final List<String> pageStack = new ArrayList<>();
    private final String homeUrl;
    private WebView webView;
    private MaterialToolbar toolbar;
    private NavigationCallbacks callbacks;
    private int titleColor = 0xFFFFFFFF;

    public WebViewHistoryHelper(String homeUrl) {
        this.homeUrl = normalizeUrl(homeUrl);
        pageStack.add(this.homeUrl);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void attach(WebView webView, MaterialToolbar toolbar, int titleColor,
                       NavigationCallbacks callbacks) {
        this.webView = webView;
        this.toolbar = toolbar;
        this.titleColor = titleColor;
        this.callbacks = callbacks;
        if (webView != null) {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.addJavascriptInterface(new HistoryBridge(), "AndroidHistory");
        }
        if (toolbar != null) {
            bindToolbarBack();
        }
    }

    public void onPageFinished(String url) {
        String normalized = normalizeUrl(url);
        if (normalized.isEmpty()) {
            return;
        }
        if ("about:blank".equals(normalized)) {
            if (pageStack.isEmpty()) {
                pageStack.add(homeUrl);
            }
            injectSpaHook();
            return;
        }
        if (normalized.startsWith("data:")) {
            if (pageStack.isEmpty()) {
                pageStack.add(homeUrl);
            }
            injectSpaHook();
            return;
        }
        if (isHomeUrl(normalized)) {
            normalized = homeUrl;
        }
        if (pageStack.isEmpty()) {
            pageStack.add(normalized);
        } else {
            String last = pageStack.get(pageStack.size() - 1);
            if (!last.equals(normalized)) {
                pageStack.add(normalized);
            }
        }
        injectSpaHook();
    }

    /**
     * @return true if back was handled inside WebView; false = caller should close app
     */
    public boolean navigateBack() {
        if (webView == null) {
            return false;
        }

        if (pageStack.size() > 1) {
            pageStack.remove(pageStack.size() - 1);
            openUrl(pageStack.get(pageStack.size() - 1));
            return true;
        }

        if (webView.canGoBack()) {
            webView.goBack();
            return true;
        }

        if (tryInPageHistoryBack()) {
            return true;
        }

        if (isAtHome()) {
            return false;
        }

        if (callbacks != null) {
            callbacks.loadHome();
        } else {
            openUrl(homeUrl);
        }
        resetStack(homeUrl);
        return true;
    }

    public boolean canNavigateBack() {
        if (webView == null) {
            return false;
        }
        return pageStack.size() > 1 || webView.canGoBack() || !isAtHome();
    }

    public void resetStack(String url) {
        pageStack.clear();
        String u = normalizeUrl(url);
        if (u.isEmpty()) {
            u = homeUrl;
        } else if (isHomeUrl(u)) {
            u = homeUrl;
        }
        pageStack.add(u);
    }

    private boolean tryInPageHistoryBack() {
        if (webView == null) {
            return false;
        }
        String url = webView.getUrl();
        if (url == null || !url.contains("#")) {
            return false;
        }
        String current = normalizeUrl(url);
        if (!stripFragment(current).equals(stripFragment(homeUrl))) {
            return false;
        }
        webView.evaluateJavascript("history.back()", null);
        return true;
    }

    private void openUrl(String url) {
        if (isHomeUrl(url)) {
            if (callbacks != null) {
                callbacks.loadHome();
            } else if (webView != null) {
                webView.loadUrl(homeUrl);
            }
        } else if (webView != null) {
            webView.loadUrl(url);
        }
    }

    private void bindToolbarBack() {
        if (toolbar == null) {
            return;
        }
        TypedValue backIcon = new TypedValue();
        if (toolbar.getContext().getTheme().resolveAttribute(R.attr.homeAsUpIndicator, backIcon, true)) {
            toolbar.setNavigationIcon(backIcon.resourceId);
        }
        toolbar.setNavigationOnClickListener(v -> {
            if (!navigateBack() && callbacks != null) {
                callbacks.exitApp();
            }
        });
        Drawable icon = toolbar.getNavigationIcon();
        if (icon != null) {
            DrawableCompat.setTint(icon, titleColor);
        }
        toolbar.setNavigationContentDescription(
                toolbar.getContext().getString(com.app.myapp.p770845.R.string.nav_back));
    }

    private void injectSpaHook() {
        if (webView != null) {
            webView.evaluateJavascript(SPA_HOOK_JS, null);
        }
    }

    private boolean isAtHome() {
        if (webView == null) {
            return true;
        }
        if (pageStack.size() > 1) {
            return false;
        }
        String current = normalizeUrl(webView.getUrl());
        if (current.isEmpty() || "about:blank".equals(current)) {
            return true;
        }
        if (current.startsWith("data:")) {
            return true;
        }
        if (current.contains("#") && !homeUrl.contains("#")) {
            return false;
        }
        return isHomeUrl(current);
    }

    private boolean isHomeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return true;
        }
        String n = normalizeUrl(url);
        String h = normalizeUrl(homeUrl);
        if (n.equals(h)) {
            return true;
        }
        return stripFragment(n).equals(stripFragment(h));
    }

    private static String stripFragment(String url) {
        int hash = url.indexOf('#');
        return hash >= 0 ? url.substring(0, hash) : url;
    }

    private static String normalizeUrl(String url) {
        if (url == null) {
            return "";
        }
        String u = url.trim();
        if (u.isEmpty()) {
            return "";
        }
        if (u.endsWith("/") && u.length() > 8) {
            u = u.substring(0, u.length() - 1);
        }
        return u.toLowerCase(Locale.US);
    }

    private final class HistoryBridge {
        @JavascriptInterface
        public void onNavigate(String url) {
            if (webView == null) {
                return;
            }
            webView.post(() -> onPageFinished(url));
        }
    }
}
