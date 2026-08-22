package com.app.myapp.p770845;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import java.io.InputStream;

final class BrandingAssets {

    private BrandingAssets() {}

    static void loadSplashLogo(Context context, ImageView target) {
        if (target == null) {
            return;
        }
        target.setScaleType(ImageView.ScaleType.FIT_CENTER);
        target.setAdjustViewBounds(true);

        Bitmap bmp = decodeAsset(context, "brand_splash.png");
        if (bmp == null) {
            bmp = decodeAsset(context, "brand_icon.png");
        }
        if (bmp != null) {
            target.setImageBitmap(bmp);
            target.setVisibility(android.view.View.VISIBLE);
            return;
        }
        try {
            target.setImageResource(R.drawable.splash_logo);
            target.setVisibility(android.view.View.VISIBLE);
        } catch (Exception ignored) {
            target.setVisibility(android.view.View.GONE);
        }
    }

    static Drawable appIconDrawable(Context context) {
        Bitmap bmp = decodeAsset(context, "brand_icon.png");
        if (bmp == null) {
            bmp = decodeAsset(context, "brand_splash.png");
        }
        if (bmp != null) {
            return new BitmapDrawable(context.getResources(), bmp);
        }
        return null;
    }

    private static Bitmap decodeAsset(Context context, String name) {
        Bitmap bmp = decodeAssetPath(context, name);
        if (bmp != null) {
            return bmp;
        }
        return decodeAssetPath(context, "assets/" + name);
    }

    private static Bitmap decodeAssetPath(Context context, String path) {
        try (InputStream in = context.getAssets().open(path)) {
            return BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            return null;
        }
    }
}
