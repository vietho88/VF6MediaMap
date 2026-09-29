package com.vf6.mediamap;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    static final String YOUTUBE_URL = "youtube_url";
    static final String WEB_URL = "web_url";
    static final String CONTENT_MODE = "content_mode";
    static final String AUTO_RESUME = "auto_resume";
    static final String KEEP_PLAYING = "keep_playing";
    static final String AUTO_FULLSCREEN = "auto_fullscreen";
    static final String WEB_SCALE = "web_scale";
    static final String LAST_CONTENT_URL = "last_content_url";
    static final String LAST_POSITION_MS = "last_position_ms";
    static final String LAST_DURATION_MS = "last_duration_ms";
    static final String LAST_WAS_PLAYING = "last_was_playing";
    static final String LAST_TITLE = "last_title";

    static final String VIETMAP_WIDGET_ID = "vietmap_widget_id";
    static final String VIETMAP_WIDGET_PROVIDER = "vietmap_widget_provider";
    static final String VIETMAP_WIDGET_ENABLED = "vietmap_widget_enabled";
    static final String VIETMAP_WIDGET_SIZE = "vietmap_widget_size";
    static final String VIETMAP_WIDGET_POSITION = "vietmap_widget_position";

    static final int MODE_YOUTUBE = 0;
    static final int MODE_WEB = 1;

    static final int WIDGET_SMALL = 0;
    static final int WIDGET_MEDIUM = 1;
    static final int WIDGET_LARGE = 2;

    static final int POS_TOP_RIGHT = 0;
    static final int POS_TOP_LEFT = 1;
    static final int POS_BOTTOM_RIGHT = 2;
    static final int POS_BOTTOM_LEFT = 3;

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences("vf6_mediamap", Context.MODE_PRIVATE);
    }

    static String youtube(Context c) {
        return get(c).getString(YOUTUBE_URL, "https://m.youtube.com");
    }

    static String web(Context c) {
        return get(c).getString(WEB_URL, "https://www.google.com");
    }

    static int contentMode(Context c) {
        return get(c).getInt(CONTENT_MODE, MODE_YOUTUBE);
    }

    static String contentUrl(Context c) {
        return contentMode(c) == MODE_WEB ? web(c) : youtube(c);
    }

    static boolean autoResume(Context c) {
        return get(c).getBoolean(AUTO_RESUME, true);
    }

    static boolean keepPlaying(Context c) {
        return get(c).getBoolean(KEEP_PLAYING, false);
    }

    static boolean autoFullscreen(Context c) {
        return get(c).getBoolean(AUTO_FULLSCREEN, false);
    }

    static int webScale(Context c) {
        return get(c).getInt(WEB_SCALE, 100);
    }

    static String lastContentUrl(Context c) {
        return get(c).getString(LAST_CONTENT_URL, "");
    }

    static long lastPositionMs(Context c) {
        return get(c).getLong(LAST_POSITION_MS, 0L);
    }

    static boolean lastWasPlaying(Context c) {
        return get(c).getBoolean(LAST_WAS_PLAYING, false);
    }

    static String lastTitle(Context c) {
        return get(c).getString(LAST_TITLE, "");
    }

    static int vietMapWidgetId(Context c) {
        return get(c).getInt(VIETMAP_WIDGET_ID, android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    static String vietMapWidgetProvider(Context c) {
        return get(c).getString(VIETMAP_WIDGET_PROVIDER, "");
    }

    static boolean vietMapWidgetEnabled(Context c) {
        return get(c).getBoolean(VIETMAP_WIDGET_ENABLED, true);
    }

    static int vietMapWidgetSize(Context c) {
        return get(c).getInt(VIETMAP_WIDGET_SIZE, WIDGET_MEDIUM);
    }

    static int vietMapWidgetPosition(Context c) {
        return get(c).getInt(VIETMAP_WIDGET_POSITION, POS_TOP_RIGHT);
    }

    static void save(Context c, String youtube, String web, int contentMode,
                     boolean autoResume, boolean keepPlaying,
                     boolean autoFullscreen, int webScale) {
        get(c).edit()
                .putString(YOUTUBE_URL, normalizeUrl(youtube, "https://m.youtube.com"))
                .putString(WEB_URL, normalizeUrl(web, "https://www.google.com"))
                .putInt(CONTENT_MODE, contentMode == MODE_WEB ? MODE_WEB : MODE_YOUTUBE)
                .putBoolean(AUTO_RESUME, autoResume)
                .putBoolean(KEEP_PLAYING, keepPlaying)
                .putBoolean(AUTO_FULLSCREEN, autoFullscreen)
                .putInt(WEB_SCALE, Math.max(75, Math.min(125, webScale)))
                .apply();
    }

    static void setVietMapWidget(Context c, int appWidgetId, String provider) {
        get(c).edit()
                .putInt(VIETMAP_WIDGET_ID, appWidgetId)
                .putString(VIETMAP_WIDGET_PROVIDER, provider == null ? "" : provider)
                .putBoolean(VIETMAP_WIDGET_ENABLED, true)
                .apply();
    }

    static void clearVietMapWidget(Context c) {
        get(c).edit()
                .remove(VIETMAP_WIDGET_ID)
                .remove(VIETMAP_WIDGET_PROVIDER)
                .apply();
    }

    static void setVietMapWidgetEnabled(Context c, boolean enabled) {
        get(c).edit().putBoolean(VIETMAP_WIDGET_ENABLED, enabled).apply();
    }

    static void setVietMapWidgetSize(Context c, int size) {
        int value = Math.max(WIDGET_SMALL, Math.min(WIDGET_LARGE, size));
        get(c).edit().putInt(VIETMAP_WIDGET_SIZE, value).apply();
    }

    static void setVietMapWidgetPosition(Context c, int position) {
        int value = Math.max(POS_TOP_RIGHT, Math.min(POS_BOTTOM_LEFT, position));
        get(c).edit().putInt(VIETMAP_WIDGET_POSITION, value).apply();
    }

    static void savePlayback(Context c, String url, long positionMs, long durationMs,
                             boolean playing, String title) {
        SharedPreferences.Editor e = get(c).edit()
                .putString(LAST_CONTENT_URL, url == null ? "" : url)
                .putLong(LAST_POSITION_MS, Math.max(0L, positionMs))
                .putLong(LAST_DURATION_MS, Math.max(0L, durationMs))
                .putBoolean(LAST_WAS_PLAYING, playing);
        if (title != null) e.putString(LAST_TITLE, title);
        e.apply();
    }

    static String normalizeUrl(String value, String fallback) {
        if (value == null) return fallback;
        String s = value.trim();
        if (s.isEmpty()) return fallback;
        if (!s.startsWith("https://") && !s.startsWith("http://")) s = "https://" + s;
        return s;
    }
}
