package com.vf6.mediamap;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    static final String YOUTUBE_URL = "youtube_url";
    static final String WEB_URL = "web_url";
    static final String MAP_URL = "map_url";
    static final String CONTENT_MODE = "content_mode";
    static final String RATIO = "ratio";
    static final String SWAPPED = "swapped";
    static final String AUTO_RESUME = "auto_resume";
    static final String KEEP_PLAYING = "keep_playing";
    static final String AUTO_FULLSCREEN = "auto_fullscreen";
    static final String WEB_SCALE = "web_scale";
    static final String LAST_CONTENT_URL = "last_content_url";
    static final String LAST_POSITION_MS = "last_position_ms";
    static final String LAST_DURATION_MS = "last_duration_ms";
    static final String LAST_WAS_PLAYING = "last_was_playing";
    static final String LAST_TITLE = "last_title";
    static final String MAP_LAT = "map_lat";
    static final String MAP_LON = "map_lon";
    static final String MAP_ZOOM = "map_zoom";

    static final int MODE_YOUTUBE = 0;
    static final int MODE_WEB = 1;

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences("vf6_mediamap", Context.MODE_PRIVATE);
    }

    static String youtube(Context c) {
        return get(c).getString(YOUTUBE_URL, "https://m.youtube.com");
    }

    static String web(Context c) {
        return get(c).getString(WEB_URL, "https://www.google.com");
    }

    // Kept for migration from v1.1.x. The native map no longer loads this URL.
    static String map(Context c) {
        return get(c).getString(MAP_URL, "https://www.google.com/maps");
    }

    static int contentMode(Context c) {
        return get(c).getInt(CONTENT_MODE, MODE_YOUTUBE);
    }

    static String contentUrl(Context c) {
        return contentMode(c) == MODE_WEB ? web(c) : youtube(c);
    }

    static int ratio(Context c) {
        return get(c).getInt(RATIO, 60);
    }

    static boolean swapped(Context c) {
        return get(c).getBoolean(SWAPPED, false);
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

    static double mapLatitude(Context c) {
        return Double.longBitsToDouble(get(c).getLong(MAP_LAT, Double.doubleToRawLongBits(16.0)));
    }

    static double mapLongitude(Context c) {
        return Double.longBitsToDouble(get(c).getLong(MAP_LON, Double.doubleToRawLongBits(108.0)));
    }

    static double mapZoom(Context c) {
        return Double.longBitsToDouble(get(c).getLong(MAP_ZOOM, Double.doubleToRawLongBits(6.0)));
    }

    static void save(Context c, String youtube, String web, String legacyMap, int contentMode,
                     int ratio, boolean autoResume, boolean keepPlaying,
                     boolean autoFullscreen, int webScale) {
        get(c).edit()
                .putString(YOUTUBE_URL, normalizeUrl(youtube, "https://m.youtube.com"))
                .putString(WEB_URL, normalizeUrl(web, "https://www.google.com"))
                .putString(MAP_URL, normalizeUrl(legacyMap, "https://www.google.com/maps"))
                .putInt(CONTENT_MODE, contentMode == MODE_WEB ? MODE_WEB : MODE_YOUTUBE)
                .putInt(RATIO, Math.max(30, Math.min(70, ratio)))
                .putBoolean(AUTO_RESUME, autoResume)
                .putBoolean(KEEP_PLAYING, keepPlaying)
                .putBoolean(AUTO_FULLSCREEN, autoFullscreen)
                .putInt(WEB_SCALE, Math.max(75, Math.min(125, webScale)))
                .apply();
    }

    static void setSwapped(Context c, boolean swapped) {
        get(c).edit().putBoolean(SWAPPED, swapped).apply();
    }

    static void saveMapCamera(Context c, double lat, double lon, double zoom) {
        if (!Double.isFinite(lat) || !Double.isFinite(lon) || !Double.isFinite(zoom)) return;
        get(c).edit()
                .putLong(MAP_LAT, Double.doubleToRawLongBits(Math.max(-85.0, Math.min(85.0, lat))))
                .putLong(MAP_LON, Double.doubleToRawLongBits(Math.max(-180.0, Math.min(180.0, lon))))
                .putLong(MAP_ZOOM, Double.doubleToRawLongBits(Math.max(3.0, Math.min(19.0, zoom))))
                .apply();
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
