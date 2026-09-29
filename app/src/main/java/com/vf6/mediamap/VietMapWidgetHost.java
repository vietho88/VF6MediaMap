package com.vf6.mediamap;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Build;
import android.os.Process;
import android.util.SizeF;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class VietMapWidgetHost extends FrameLayout {
    static final int HOST_ID = 6406;
    static final String VIETMAP_PACKAGE = "vn.vietmap.live";

    private final Context context;
    private final AppWidgetHost host;
    private AppWidgetHostView hostView;
    private boolean listening;

    VietMapWidgetHost(Context context) {
        super(context);
        this.context = context;
        this.host = new AppWidgetHost(context, HOST_ID);
        setClipChildren(true);
        setClipToPadding(true);
        setBackgroundColor(Color.TRANSPARENT);
        reload();
    }

    static boolean isVietMapInstalled(Context context) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(VIETMAP_PACKAGE, 0);
            return info.enabled;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static List<AppWidgetProviderInfo> findVietMapProviders(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        List<AppWidgetProviderInfo> result = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= 26) {
            try {
                List<AppWidgetProviderInfo> exact = manager.getInstalledProvidersForPackage(
                        VIETMAP_PACKAGE, Process.myUserHandle());
                if (exact != null) result.addAll(exact);
            } catch (Throwable ignored) {
            }
        }

        if (result.isEmpty()) {
            List<AppWidgetProviderInfo> all;
            try {
                all = manager.getInstalledProviders();
            } catch (Throwable ignored) {
                all = Collections.emptyList();
            }
            for (AppWidgetProviderInfo info : all) {
                if (info == null || info.provider == null) continue;
                String pkg = info.provider.getPackageName();
                if (VIETMAP_PACKAGE.equals(pkg) || pkg.toLowerCase().contains("vietmap")) {
                    result.add(info);
                }
            }
        }

        result.sort(Comparator.comparing((AppWidgetProviderInfo info) -> providerLabel(context, info)));
        return result;
    }

    static String providerLabel(Context context, AppWidgetProviderInfo info) {
        if (info == null) return "Unknown";
        try {
            CharSequence label = info.loadLabel(context.getPackageManager());
            if (label != null && label.length() > 0) return label.toString();
        } catch (Throwable ignored) {
        }
        return info.provider == null ? "Unknown" : info.provider.flattenToShortString();
    }

    static Bundle optionsForSize(Context context, int size) {
        int widthDp;
        int heightDp;
        if (size == Prefs.WIDGET_SMALL) {
            widthDp = 260;
            heightDp = 86;
        } else if (size == Prefs.WIDGET_LARGE) {
            widthDp = 480;
            heightDp = 156;
        } else {
            widthDp = 360;
            heightDp = 112;
        }
        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);
        if (Build.VERSION.SDK_INT >= 31) {
            ArrayList<SizeF> sizes = new ArrayList<>();
            sizes.add(new SizeF(widthDp, heightDp));
            options.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, sizes);
        }
        return options;
    }

    static int widthDpForSize(int size) {
        if (size == Prefs.WIDGET_SMALL) return 260;
        if (size == Prefs.WIDGET_LARGE) return 480;
        return 360;
    }

    static int heightDpForSize(int size) {
        if (size == Prefs.WIDGET_SMALL) return 86;
        if (size == Prefs.WIDGET_LARGE) return 156;
        return 112;
    }

    void reload() {
        removeAllViews();
        hostView = null;

        if (!Prefs.vietMapWidgetEnabled(context)) {
            addPlaceholder("VietMap widget đang ẩn");
            return;
        }

        int id = Prefs.vietMapWidgetId(context);
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            addPlaceholder("Chưa kết nối widget VietMap\nMở VF6 MediaMap trên điện thoại → Kết nối widget");
            return;
        }

        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        AppWidgetProviderInfo info;
        try {
            info = manager.getAppWidgetInfo(id);
        } catch (Throwable ignored) {
            info = null;
        }
        if (info == null) {
            addPlaceholder("Widget VietMap không còn hợp lệ\nHãy kết nối lại trên điện thoại");
            return;
        }

        try {
            manager.updateAppWidgetOptions(id, optionsForSize(context, Prefs.vietMapWidgetSize(context)));
            hostView = host.createView(context, id, info);
            hostView.setAppWidget(id, info);
            hostView.setPadding(0, 0, 0, 0);
            addView(hostView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        } catch (Throwable error) {
            addPlaceholder("Không render được VietMap widget\n" + error.getClass().getSimpleName());
        }
    }

    void startListening() {
        if (listening) return;
        try {
            host.startListening();
            listening = true;
        } catch (Throwable ignored) {
        }
    }

    void stopListening() {
        if (!listening) return;
        try {
            host.stopListening();
        } catch (Throwable ignored) {
        }
        listening = false;
    }

    void destroy() {
        stopListening();
        removeAllViews();
        hostView = null;
    }

    private void addPlaceholder(String message) {
        TextView text = new TextView(context);
        text.setText(message);
        text.setTextColor(Color.WHITE);
        text.setTextSize(12);
        text.setGravity(Gravity.CENTER);
        text.setPadding(dp(8), dp(5), dp(8), dp(5));
        text.setBackgroundColor(Color.argb(205, 32, 32, 36));
        addView(text, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
