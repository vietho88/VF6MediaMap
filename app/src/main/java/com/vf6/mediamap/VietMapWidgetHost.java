package com.vf6.mediamap;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.util.SizeF;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class VietMapWidgetHost extends FrameLayout {
    static final int HOST_ID = 6406;
    static final String VIETMAP_PACKAGE = "vn.vietmap.live";

    private static final int MIN_WIDTH_DP = 180;
    private static final int MIN_HEIGHT_DP = 64;

    private final Context context;
    private final TransparentWidgetHost host;
    private AppWidgetHostView hostView;
    private boolean listening;
    private boolean editMode;

    private float dragRawX;
    private float dragRawY;
    private int dragLeft;
    private int dragTop;
    private float pinchStartDistance;
    private int pinchStartWidth;
    private int pinchStartHeight;

    VietMapWidgetHost(Context context) {
        super(context);
        this.context = context;
        this.host = new TransparentWidgetHost(context, HOST_ID);
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
        return optionsForDimensions(widthDpForSize(size), heightDpForSize(size));
    }

    static Bundle optionsForDimensions(int widthDp, int heightDp) {
        widthDp = Math.max(MIN_WIDTH_DP, widthDp);
        heightDp = Math.max(MIN_HEIGHT_DP, heightDp);
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
            addPlaceholder("VietMap widget hidden");
            return;
        }

        int id = Prefs.vietMapWidgetId(context);
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            addPlaceholder("VietMap widget is not bound");
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
            addPlaceholder("VietMap widget binding is invalid");
            return;
        }

        try {
            int widthDp = Prefs.vietMapWidgetWidthDp(context);
            int heightDp = Prefs.vietMapWidgetHeightDp(context);
            if (widthDp <= 0 || heightDp <= 0) {
                int size = Prefs.vietMapWidgetSize(context);
                widthDp = widthDpForSize(size);
                heightDp = heightDpForSize(size);
            }
            manager.updateAppWidgetOptions(id, optionsForDimensions(widthDp, heightDp));
            hostView = host.createView(context, id, info);
            hostView.setAppWidget(id, info);
            hostView.setPadding(0, 0, 0, 0);
            hostView.setBackgroundColor(Color.TRANSPARENT);
            addView(hostView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            requestTransparentPasses();
        } catch (Throwable error) {
            addPlaceholder("Cannot render VietMap widget: " + error.getClass().getSimpleName());
        }
    }

    void setEditMode(boolean enabled) {
        editMode = enabled;
        if (enabled) {
            GradientDrawable border = new GradientDrawable();
            border.setColor(Color.TRANSPARENT);
            border.setStroke(dp(2), Color.YELLOW);
            setForeground(border);
        } else {
            setForeground(null);
            saveCustomLayout();
        }
        invalidate();
    }

    boolean isEditMode() {
        return editMode;
    }

    void requestTransparentPasses() {
        post(this::stripProviderBackgrounds);
        postDelayed(this::stripProviderBackgrounds, 120L);
        postDelayed(this::stripProviderBackgrounds, 600L);
        postDelayed(this::stripProviderBackgrounds, 1800L);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return editMode || super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!editMode) return super.onTouchEvent(event);
        ViewParent parentObject = getParent();
        if (!(parentObject instanceof View)) return true;
        View parent = (View) parentObject;
        FrameLayout.LayoutParams lp = asFrameLayoutParams();
        if (lp == null) return true;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragRawX = event.getRawX();
                dragRawY = event.getRawY();
                dragLeft = lp.leftMargin;
                dragTop = lp.topMargin;
                pinchStartDistance = 0f;
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() >= 2) {
                    pinchStartDistance = pointerDistance(event);
                    pinchStartWidth = getWidth();
                    pinchStartHeight = getHeight();
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() >= 2) {
                    float distance = pointerDistance(event);
                    if (pinchStartDistance <= 0f) {
                        pinchStartDistance = distance;
                        pinchStartWidth = getWidth();
                        pinchStartHeight = getHeight();
                    }
                    float scale = distance / Math.max(1f, pinchStartDistance);
                    int minW = dp(MIN_WIDTH_DP);
                    int minH = dp(MIN_HEIGHT_DP);
                    int maxW = Math.max(minW, parent.getWidth() - dp(8));
                    int maxH = Math.max(minH, parent.getHeight() - dp(8));
                    int width = clamp(Math.round(pinchStartWidth * scale), minW, maxW);
                    int height = clamp(Math.round(pinchStartHeight * scale), minH, maxH);
                    applyFrameBounds(lp.leftMargin, lp.topMargin, width, height, parent);
                } else {
                    int left = dragLeft + Math.round(event.getRawX() - dragRawX);
                    int top = dragTop + Math.round(event.getRawY() - dragRawY);
                    applyFrameBounds(left, top, getWidth(), getHeight(), parent);
                }
                return true;

            case MotionEvent.ACTION_POINTER_UP:
                pinchStartDistance = 0f;
                FrameLayout.LayoutParams now = asFrameLayoutParams();
                if (now != null) {
                    dragLeft = now.leftMargin;
                    dragTop = now.topMargin;
                }
                dragRawX = event.getRawX();
                dragRawY = event.getRawY();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                pinchStartDistance = 0f;
                saveCustomLayout();
                updateWidgetOptionsForBounds();
                requestTransparentPasses();
                return true;

            default:
                return true;
        }
    }

    void startListening() {
        if (listening) return;
        try {
            host.startListening();
            listening = true;
            requestTransparentPasses();
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

    private void applyFrameBounds(int left, int top, int width, int height, View parent) {
        int maxWidth = Math.max(dp(MIN_WIDTH_DP), parent.getWidth());
        int maxHeight = Math.max(dp(MIN_HEIGHT_DP), parent.getHeight());
        width = clamp(width, dp(MIN_WIDTH_DP), maxWidth);
        height = clamp(height, dp(MIN_HEIGHT_DP), maxHeight);
        left = clamp(left, 0, Math.max(0, parent.getWidth() - width));
        top = clamp(top, 0, Math.max(0, parent.getHeight() - height));

        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(width, height, Gravity.TOP | Gravity.START);
        p.leftMargin = left;
        p.topMargin = top;
        setLayoutParams(p);
    }

    private FrameLayout.LayoutParams asFrameLayoutParams() {
        ViewGroup.LayoutParams raw = getLayoutParams();
        if (raw instanceof FrameLayout.LayoutParams) return (FrameLayout.LayoutParams) raw;
        return null;
    }

    private void saveCustomLayout() {
        ViewParent parentObject = getParent();
        FrameLayout.LayoutParams lp = asFrameLayoutParams();
        if (!(parentObject instanceof View) || lp == null) return;
        View parent = (View) parentObject;
        int width = Math.max(1, getWidth());
        int height = Math.max(1, getHeight());
        int freeX = Math.max(1, parent.getWidth() - width);
        int freeY = Math.max(1, parent.getHeight() - height);
        float x = Math.max(0f, Math.min(1f, lp.leftMargin / (float) freeX));
        float y = Math.max(0f, Math.min(1f, lp.topMargin / (float) freeY));
        Prefs.setVietMapWidgetCustomLayout(context, x, y, pxToDp(width), pxToDp(height));
    }

    private void updateWidgetOptionsForBounds() {
        int id = Prefs.vietMapWidgetId(context);
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return;
        int widthDp = Math.max(MIN_WIDTH_DP, pxToDp(Math.max(1, getWidth())));
        int heightDp = Math.max(MIN_HEIGHT_DP, pxToDp(Math.max(1, getHeight())));
        try {
            AppWidgetManager.getInstance(context).updateAppWidgetOptions(
                    id, optionsForDimensions(widthDp, heightDp));
        } catch (Throwable ignored) {
        }
    }

    private void stripProviderBackgrounds() {
        if (hostView == null) return;
        hostView.setBackgroundColor(Color.TRANSPARENT);
        long rootArea = Math.max(1L, (long) Math.max(1, hostView.getWidth()) * Math.max(1, hostView.getHeight()));
        stripBackgroundsRecursive(hostView, rootArea, true);
    }

    private static void stripBackgroundsRecursive(View view, long rootArea, boolean root) {
        if (view == null) return;
        if (view instanceof ViewGroup) {
            long area = (long) Math.max(0, view.getWidth()) * Math.max(0, view.getHeight());
            if (!root && area >= rootArea / 10L && view.getBackground() != null) {
                view.setBackground(null);
            }
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                stripBackgroundsRecursive(group.getChildAt(i), rootArea, false);
            }
        }
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

    private float pointerDistance(MotionEvent event) {
        if (event.getPointerCount() < 2) return 0f;
        float dx = event.getX(0) - event.getX(1);
        float dy = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private int clamp(int value, int min, int max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int pxToDp(int value) {
        return Math.round(value / getResources().getDisplayMetrics().density);
    }

    private static final class TransparentWidgetHost extends AppWidgetHost {
        TransparentWidgetHost(Context context, int hostId) {
            super(context, hostId);
        }

        @Override
        protected AppWidgetHostView onCreateView(Context context, int appWidgetId,
                                                  AppWidgetProviderInfo appWidget) {
            return new TransparentHostView(context);
        }
    }

    private static final class TransparentHostView extends AppWidgetHostView {
        TransparentHostView(Context context) {
            super(context);
            setBackgroundColor(Color.TRANSPARENT);
            setPadding(0, 0, 0, 0);
        }

        @Override
        public void updateAppWidget(RemoteViews remoteViews) {
            super.updateAppWidget(remoteViews);
            setBackgroundColor(Color.TRANSPARENT);
            post(this::clearProviderChrome);
            postDelayed(this::clearProviderChrome, 120L);
            postDelayed(this::clearProviderChrome, 600L);
        }

        private void clearProviderChrome() {
            long rootArea = Math.max(1L, (long) Math.max(1, getWidth()) * Math.max(1, getHeight()));
            stripBackgroundsRecursive(this, rootArea, true);
        }
    }
}
