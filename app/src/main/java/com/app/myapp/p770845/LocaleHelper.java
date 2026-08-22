package com.app.myapp.p770845;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/** Keeps exported WebView apps in English only. */
final class LocaleHelper {

    private LocaleHelper() {}

    static void forceEnglish(Context context) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"));
    }
}
