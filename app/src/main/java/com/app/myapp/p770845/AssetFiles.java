package com.app.myapp.p770845;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Reads export assets — tries root path first, then legacy {@code assets/} subfolder.
 */
public final class AssetFiles {

    private AssetFiles() {}

    public static String readText(Context context, String name) {
        if (context == null || name == null || name.isEmpty()) {
            return "";
        }
        String direct = readPath(context, name);
        if (!direct.isEmpty()) {
            return direct;
        }
        if (!name.startsWith("assets/")) {
            return readPath(context, "assets/" + name);
        }
        return "";
    }

    private static String readPath(Context context, String path) {
        try (InputStream in = context.getAssets().open(path);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
            }
            return sb.toString().trim();
        } catch (Exception ignored) {
            return "";
        }
    }
}
