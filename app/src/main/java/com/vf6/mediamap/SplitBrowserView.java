package com.vf6.mediamap;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

final class SplitBrowserView extends LinearLayout {
    private static final long MONITOR_MS = 4000L;
    private static final long CONTROLS_HIDE_MS = 2800L;
    private static final long BACK_DOUBLE_TAP_MS = 420L;
    private static final long VML_STATUS_REFRESH_MS = 5000L;
    private static final long VML_FRESH_UPDATE_MS = 30000L;

    private final Context context;
    private final boolean phonePreview;
    private final TouchStage stage;
    private final WebView content;
    private final TextView safeCover;
    private final VietMapWidgetHost vietMapWidget;
    private LinearLayout controlPanel;
    private TextView controlHandle;
    private boolean controlsVisible = true;
    private Button modeButton;
    private Button widgetButton;
    private Button positionButton;
    private Button sizeButton;
    private Button voiceButton;
    private Button playPauseButton;
    private Button vmlButton;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideControlsRunnable = this::hideControls;
    private long lastBackTapAt;
    private Runnable pendingBackAction;

    private final Runnable vmlStatusRunnable = new Runnable() {
        @Override
        public void run() {
            if (destroyed) return;
            updateVmlButton();
            handler.postDelayed(this, VML_STATUS_REFRESH_MS);
        }
    };

    private boolean safeMode;
    private boolean desiredPlaying;
    private boolean fullscreenAttempted;
    private boolean destroyed;
    private View fullscreenView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;
    private SpeechRecognizer speechRecognizer;
    private MediaSession mediaSession;

    private final Runnable playbackMonitor = new Runnable() {
        @Override
        public void run() {
            if (!destroyed) {
                String keep = Prefs.keepPlaying(context) && desiredPlaying && !safeMode
                        ? "if(v.paused){var p=v.play();if(p&&p.catch){p.catch(function(){});}}" : "";
                String js = "(function(){try{" +
                        "var v=document.querySelector('video');" +
                        "if(!v){return;}" + keep +
                        "AndroidBridge.playback(location.href,Math.floor((v.currentTime||0)*1000)," +
                        "Math.floor((v.duration||0)*1000),!v.paused,document.title||'');" +
                        "}catch(e){}})();";
                content.evaluateJavascript(js, null);
            }
            handler.postDelayed(this, MONITOR_MS);
        }
    };

    SplitBrowserView(Context context, boolean phonePreview) {
        super(context);
        this.context = context;
        this.phonePreview = phonePreview;
        this.safeMode = !phonePreview;
        this.desiredPlaying = Prefs.lastWasPlaying(context);

        setOrientation(VERTICAL);
        setBackgroundColor(Color.BLACK);
        setKeepScreenOn(true);

        stage = new TouchStage(context);
        stage.setBackgroundColor(Color.BLACK);
        addView(stage, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));

        content = webView();
        stage.addView(content, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        safeCover = new TextView(context);
        safeCover.setText("DRIVE SAFE\nVietMap widget vẫn hoạt động");
        safeCover.setTextColor(Color.LTGRAY);
        safeCover.setTextSize(20);
        safeCover.setGravity(Gravity.CENTER);
        safeCover.setBackgroundColor(Color.BLACK);
        stage.addView(safeCover, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        vietMapWidget = new VietMapWidgetHost(context, phonePreview);
        stage.addView(vietMapWidget, widgetLayoutParams());

        controlPanel = buildControlPanel();
        FrameLayout.LayoutParams controlsLp = new FrameLayout.LayoutParams(dp(78),
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL);
        controlsLp.leftMargin = dp(8);
        stage.addView(controlPanel, controlsLp);

        controlHandle = buildControlHandle();
        FrameLayout.LayoutParams handleLp = new FrameLayout.LayoutParams(dp(28), dp(78),
                Gravity.START | Gravity.CENTER_VERTICAL);
        stage.addView(controlHandle, handleLp);
        controlHandle.setVisibility(GONE);

        stage.post(() -> {
            applyWidgetLayout();
            showControls();
        });

        setupMediaSession();
        setupSpeechRecognizer();

        String firstUrl = Prefs.contentUrl(context);
        if (Prefs.autoResume(context)) {
            String last = Prefs.lastContentUrl(context);
            if (last != null && (last.startsWith("https://") || last.startsWith("http://"))) firstUrl = last;
        }
        content.loadUrl(firstUrl);
        applyMode();
        handler.postDelayed(playbackMonitor, MONITOR_MS);
        handler.post(vmlStatusRunnable);
    }

    private LinearLayout buildControlPanel() {
        LinearLayout rail = new LinearLayout(context);
        rail.setOrientation(VERTICAL);
        rail.setGravity(Gravity.CENTER);
        rail.setPadding(dp(4), dp(6), dp(4), dp(6));
        rail.setElevation(dp(8));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(205, 28, 46, 66));
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), Color.argb(190, 68, 150, 220));
        rail.setBackground(bg);

        modeButton = railButton(safeMode ? "VIDEO" : "SAFE", "Drive Safe / Parked Video");
        modeButton.setOnClickListener(v -> { toggleSafeMode(); keepControlsAlive(); });
        rail.addView(modeButton, railButtonParams());

        voiceButton = railButton("MIC", "Voice search");
        voiceButton.setOnClickListener(v -> { startVoiceSearch(); keepControlsAlive(); });
        rail.addView(voiceButton, railButtonParams());

        playPauseButton = railButton("▶", "Play or pause");
        playPauseButton.setOnClickListener(v -> { toggleVideoPlayback(); keepControlsAlive(); });
        rail.addView(playPauseButton, railButtonParams());

        Button next = railButton("⏭", "Next media");
        next.setOnClickListener(v -> { nextMedia(); keepControlsAlive(); });
        rail.addView(next, railButtonParams());

        Button back = railButton("←", "Back. Double tap for quick voice search");
        back.setOnClickListener(v -> handleBackButtonTap());
        rail.addView(back, railButtonParams());

        Button fullscreen = railButton("⛶", "Fullscreen video");
        fullscreen.setOnClickListener(v -> { requestContentFullscreen(); keepControlsAlive(); });
        rail.addView(fullscreen, railButtonParams());

        widgetButton = railButton(Prefs.vietMapWidgetEnabled(context) ? "VM" : "VM×",
                "Show or hide VietMap widget");
        widgetButton.setOnClickListener(v -> { toggleWidget(); keepControlsAlive(); });
        rail.addView(widgetButton, railButtonParams());

        vmlButton = railButton("VML?", "Check or open VietMap Live on the phone");
        vmlButton.setOnClickListener(v -> { checkOrOpenVietMap(); keepControlsAlive(); });
        vmlButton.setOnLongClickListener(v -> {
            boolean opened = VietMapWidgetHost.openVietMap(context);
            Toast.makeText(context, opened ? "Đang mở VietMap Live trên điện thoại…"
                    : "Không mở được VietMap Live.", Toast.LENGTH_SHORT).show();
            keepControlsAlive();
            return true;
        });
        rail.addView(vmlButton, railButtonParams());

        positionButton = railButton("VM E", "Edit VietMap position and size");
        positionButton.setOnClickListener(v -> toggleWidgetEditMode());
        rail.addView(positionButton, railButtonParams());

        sizeButton = railButton(presetLabel(Prefs.vietMapWidgetPreset(context, phonePreview)),
                "VietMap Compact / Expanded preset");
        sizeButton.setOnClickListener(v -> { toggleWidgetPreset(); keepControlsAlive(); });
        sizeButton.setOnLongClickListener(v -> {
            Prefs.resetVietMapLayout(context, phonePreview);
            sizeButton.setText(presetLabel(Prefs.vietMapWidgetPreset(context, phonePreview)));
            vietMapWidget.reload();
            applyWidgetLayout();
            Toast.makeText(context, phonePreview ? "Đã reset layout VietMap trên điện thoại."
                    : "Đã reset layout VietMap trên Android Auto.", Toast.LENGTH_SHORT).show();
            keepControlsAlive();
            return true;
        });
        rail.addView(sizeButton, railButtonParams());

        Button reload = railButton("↻", "Reload");
        reload.setOnClickListener(v -> {
            content.reload();
            vietMapWidget.reload();
            keepControlsAlive();
        });
        rail.addView(reload, railButtonParams());

        return rail;
    }

    private TextView buildControlHandle() {
        TextView handle = new TextView(context);
        handle.setText("›");
        handle.setTextColor(Color.WHITE);
        handle.setTextSize(22);
        handle.setGravity(Gravity.CENTER);
        handle.setContentDescription("Show controls");
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(185, 28, 46, 66));
        float r = dp(12);
        bg.setCornerRadii(new float[]{0,0,r,r,r,r,0,0});
        handle.setBackground(bg);
        handle.setOnClickListener(v -> showControls());
        return handle;
    }

    private LinearLayout.LayoutParams railButtonParams() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40));
    }

    private Button railButton(String text, String description) {
        Button b = new Button(context);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(text.length() > 3 ? 11 : 18);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setPadding(0, 0, 0, 0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setContentDescription(description);
        return b;
    }

    private void onStageInteraction() {
        if (!controlsVisible) return;
        scheduleControlsHide();
    }

    private void keepControlsAlive() {
        showControls();
    }

    private void showControls() {
        if (destroyed || controlPanel == null || controlHandle == null) return;
        handler.removeCallbacks(hideControlsRunnable);
        controlsVisible = true;
        controlHandle.setVisibility(GONE);
        controlPanel.animate().cancel();
        controlPanel.setVisibility(VISIBLE);
        controlPanel.setAlpha(1f);
        controlPanel.setTranslationX(0f);
        bringOverlayControlsToFront();
        scheduleControlsHide();
    }

    private void scheduleControlsHide() {
        handler.removeCallbacks(hideControlsRunnable);
        if (destroyed) return;
        if (vietMapWidget != null && vietMapWidget.isEditMode()) return;
        handler.postDelayed(hideControlsRunnable, CONTROLS_HIDE_MS);
    }

    private void hideControls() {
        if (controlPanel == null || controlHandle == null) return;
        if (vietMapWidget != null && vietMapWidget.isEditMode()) return;
        if (!controlsVisible) return;
        controlsVisible = false;
        controlPanel.animate().cancel();
        controlPanel.animate()
                .alpha(0f)
                .translationX(-dp(16))
                .setDuration(170L)
                .withEndAction(() -> {
                    if (!controlsVisible) {
                        controlPanel.setVisibility(INVISIBLE);
                        controlHandle.setVisibility(VISIBLE);
                        controlHandle.bringToFront();
                    }
                })
                .start();
    }

    private void bringOverlayControlsToFront() {
        if (controlPanel != null) controlPanel.bringToFront();
        if (controlHandle != null && controlHandle.getVisibility() == VISIBLE) controlHandle.bringToFront();
    }

    private WebView webView() {
        WebView w = new WebView(context);
        w.setBackgroundColor(Color.BLACK);
        w.setOverScrollMode(View.OVER_SCROLL_NEVER);
        w.setInitialScale(Prefs.webScale(context));

        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (android.os.Build.VERSION.SDK_INT >= 23) s.setOffscreenPreRaster(true);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, true);
        w.addJavascriptInterface(new PlaybackBridge(), "AndroidBridge");

        w.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleNavigation(view, request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleNavigation(view, Uri.parse(url));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                fullscreenAttempted = false;
                handler.postDelayed(() -> restorePlaybackIfNeeded(view, url), 1200L);
            }
        });

        w.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                showCustomView(view, callback);
            }

            @Override
            public void onHideCustomView() {
                hideCustomView();
            }
        });
        return w;
    }

    private boolean handleNavigation(WebView view, Uri uri) {
        if (uri == null) return true;
        String scheme = uri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) return false;

        if ("intent".equalsIgnoreCase(scheme)) {
            try {
                Intent intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(intent);
                    return true;
                } catch (Throwable openError) {
                    String fallback = intent.getStringExtra("browser_fallback_url");
                    if (fallback != null && (fallback.startsWith("https://") || fallback.startsWith("http://"))) {
                        view.loadUrl(fallback);
                    }
                    return true;
                }
            } catch (Throwable ignored) {
                return true;
            }
        }

        try {
            Intent external = new Intent(Intent.ACTION_VIEW, uri);
            external.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(external);
        } catch (Throwable ignored) {
            Toast.makeText(context, "Không tìm thấy ứng dụng xử lý liên kết này.", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void restorePlaybackIfNeeded(WebView view, String url) {
        if (!Prefs.autoResume(context)) return;
        String last = Prefs.lastContentUrl(context);
        long positionMs = Prefs.lastPositionMs(context);
        if (last == null || last.isEmpty() || positionMs < 1500L) return;
        if (!sameMediaPage(last, url)) return;

        boolean shouldPlay = Prefs.lastWasPlaying(context) && !safeMode;
        desiredPlaying = shouldPlay;
        double seconds = positionMs / 1000.0;
        String js = "(function(){var v=document.querySelector('video');if(!v)return;" +
                "try{if(Math.abs((v.currentTime||0)-" + seconds + ")>2){v.currentTime=" + seconds + ";}}catch(e){}" +
                (shouldPlay ? "try{var p=v.play();if(p&&p.catch)p.catch(function(){});}catch(e){}" : "") +
                "})()";
        view.evaluateJavascript(js, null);
    }

    private boolean sameMediaPage(String a, String b) {
        try {
            Uri ua = Uri.parse(a);
            Uri ub = Uri.parse(b);
            if (ua.getHost() == null || ub.getHost() == null) return a.equals(b);
            if (!ua.getHost().equalsIgnoreCase(ub.getHost())) return false;
            String qa = ua.getQueryParameter("v");
            String qb = ub.getQueryParameter("v");
            if (qa != null || qb != null) return qa != null && qa.equals(qb);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void toggleSafeMode() {
        safeMode = !safeMode;
        if (!safeMode && !phonePreview) {
            Toast.makeText(context, "Parked Video: chỉ dùng video khi xe đã đỗ.", Toast.LENGTH_LONG).show();
        }
        if (safeMode) {
            desiredPlaying = false;
            pauseMedia();
            hideCustomView();
        }
        applyMode();
    }

    private void applyMode() {
        safeCover.setVisibility(safeMode ? VISIBLE : GONE);
        content.setVisibility(safeMode ? INVISIBLE : VISIBLE);
        modeButton.setText(safeMode ? "VIDEO" : "SAFE");
        applyWidgetLayout();
    }

    private void toggleWidget() {
        boolean enabled = !Prefs.vietMapWidgetEnabled(context);
        Prefs.setVietMapWidgetEnabled(context, enabled);
        widgetButton.setText(enabled ? "VM" : "VM×");
        vietMapWidget.reload();
        applyWidgetLayout();
    }

    private void toggleWidgetEditMode() {
        boolean edit = !vietMapWidget.isEditMode();
        vietMapWidget.setEditMode(edit);
        positionButton.setText(edit ? "VM ✓" : "VM E");
        if (edit) {
            showControls();
            handler.removeCallbacks(hideControlsRunnable);
            Toast.makeText(context,
                    phonePreview
                            ? "VM Edit (Phone): kéo 1 ngón, pinch 2 ngón. VM ✓ để lưu riêng layout điện thoại."
                            : "VM Edit (Car): kéo 1 ngón, pinch 2 ngón. VM ✓ để lưu riêng layout Android Auto.",
                    Toast.LENGTH_LONG).show();
        } else {
            scheduleControlsHide();
        }
    }

    private void toggleWidgetPreset() {
        int current = Prefs.vietMapWidgetPreset(context, phonePreview);
        int preset = current == Prefs.WIDGET_PRESET_COMPACT
                ? Prefs.WIDGET_PRESET_EXPANDED : Prefs.WIDGET_PRESET_COMPACT;
        Prefs.setVietMapWidgetPreset(context, phonePreview, preset);
        Prefs.clearVietMapWidgetCustomSize(context, phonePreview);
        sizeButton.setText(presetLabel(preset));
        vietMapWidget.reload();
        applyWidgetLayout();
        Toast.makeText(context, preset == Prefs.WIDGET_PRESET_COMPACT
                ? "VietMap Compact" : "VietMap Expanded", Toast.LENGTH_SHORT).show();
    }

    private String positionLabel(int pos) {
        if (pos == Prefs.POS_TOP_LEFT) return "VM ↖";
        if (pos == Prefs.POS_BOTTOM_RIGHT) return "VM ↘";
        if (pos == Prefs.POS_BOTTOM_LEFT) return "VM ↙";
        return "VM ↗";
    }

    private String presetLabel(int preset) {
        return preset == Prefs.WIDGET_PRESET_EXPANDED ? "VM X" : "VM C";
    }

    private FrameLayout.LayoutParams widgetLayoutParams() {
        int preset = Prefs.vietMapWidgetPreset(context, phonePreview);
        int customWidthDp = Prefs.vietMapWidgetWidthDp(context, phonePreview);
        int customHeightDp = Prefs.vietMapWidgetHeightDp(context, phonePreview);
        int width = dp(customWidthDp > 0 ? customWidthDp : VietMapWidgetHost.widthDpForPreset(preset));
        int height = dp(customHeightDp > 0 ? customHeightDp : VietMapWidgetHost.heightDpForPreset(preset));

        int stageWidth = stage == null ? 0 : stage.getWidth();
        int stageHeight = stage == null ? 0 : stage.getHeight();
        float customX = Prefs.vietMapWidgetX(context, phonePreview);
        float customY = Prefs.vietMapWidgetY(context, phonePreview);

        if (stageWidth > 0 && stageHeight > 0 && customX >= 0f && customY >= 0f) {
            width = Math.min(width, stageWidth);
            height = Math.min(height, stageHeight);
            int freeX = Math.max(0, stageWidth - width);
            int freeY = Math.max(0, stageHeight - height);
            FrameLayout.LayoutParams custom = new FrameLayout.LayoutParams(width, height, Gravity.TOP | Gravity.START);
            custom.leftMargin = Math.round(freeX * Math.max(0f, Math.min(1f, customX)));
            custom.topMargin = Math.round(freeY * Math.max(0f, Math.min(1f, customY)));
            return custom;
        }

        int gravity;
        switch (Prefs.vietMapWidgetPosition(context, phonePreview)) {
            case Prefs.POS_TOP_LEFT:
                gravity = Gravity.TOP | Gravity.START;
                break;
            case Prefs.POS_BOTTOM_RIGHT:
                gravity = Gravity.BOTTOM | Gravity.END;
                break;
            case Prefs.POS_BOTTOM_LEFT:
                gravity = Gravity.BOTTOM | Gravity.START;
                break;
            case Prefs.POS_TOP_RIGHT:
            default:
                gravity = Gravity.TOP | Gravity.END;
                break;
        }
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(width, height, gravity);
        p.setMargins(dp(8), dp(8), dp(8), dp(8));
        return p;
    }

    private void applyWidgetLayout() {
        if (vietMapWidget == null) return;
        vietMapWidget.setVisibility(Prefs.vietMapWidgetEnabled(context) ? VISIBLE : GONE);
        vietMapWidget.setLayoutParams(widgetLayoutParams());
        vietMapWidget.bringToFront();
        vietMapWidget.requestTransparentPasses();
        bringOverlayControlsToFront();
    }

    private void handleBackButtonTap() {
        keepControlsAlive();
        long now = SystemClock.elapsedRealtime();
        if (lastBackTapAt > 0L && now - lastBackTapAt <= BACK_DOUBLE_TAP_MS) {
            if (pendingBackAction != null) handler.removeCallbacks(pendingBackAction);
            pendingBackAction = null;
            lastBackTapAt = 0L;
            startVoiceSearch();
            return;
        }

        lastBackTapAt = now;
        pendingBackAction = () -> {
            lastBackTapAt = 0L;
            pendingBackAction = null;
            performSingleBackAction();
            keepControlsAlive();
        };
        handler.postDelayed(pendingBackAction, BACK_DOUBLE_TAP_MS);
    }

    private void performSingleBackAction() {
        if (content.canGoBack()) content.goBack();
        else previousMedia();
    }

    private void updateVmlButton() {
        if (vmlButton == null) return;
        if (!VietMapWidgetHost.isVietMapInstalled(context)) {
            vmlButton.setText("VML×");
            vmlButton.setContentDescription("VietMap Live is not installed");
            return;
        }

        boolean freshWidget = vietMapWidget.hasRecentRemoteViewsUpdate(VML_FRESH_UPDATE_MS);
        boolean visibleProcess = VietMapWidgetHost.isVietMapProcessVisible(context);
        if (freshWidget || visibleProcess) {
            vmlButton.setText("VML✓");
            vmlButton.setContentDescription("VietMap Live appears active");
        } else {
            vmlButton.setText("VML?");
            vmlButton.setContentDescription("VietMap Live status unknown or inactive. Tap to open it");
        }
    }

    private void checkOrOpenVietMap() {
        updateVmlButton();
        if (!VietMapWidgetHost.isVietMapInstalled(context)) {
            Toast.makeText(context, "Không tìm thấy VietMap Live trên điện thoại.", Toast.LENGTH_LONG).show();
            return;
        }

        boolean freshWidget = vietMapWidget.hasRecentRemoteViewsUpdate(VML_FRESH_UPDATE_MS);
        boolean visibleProcess = VietMapWidgetHost.isVietMapProcessVisible(context);
        if (freshWidget || visibleProcess) {
            long ageMs = vietMapWidget.remoteViewsUpdateAgeMs();
            String detail = ageMs >= 0L
                    ? " • widget cập nhật " + Math.max(0L, ageMs / 1000L) + " giây trước"
                    : "";
            Toast.makeText(context, "VietMap Live có vẻ đang hoạt động" + detail + ".", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean opened = VietMapWidgetHost.openVietMap(context);
        if (opened) {
            Toast.makeText(context,
                    "VML chưa có dấu hiệu cập nhật. Đang mở VietMap Live trên điện thoại; khởi động cảnh báo/dẫn đường rồi quay lại VF6 MediaMap.",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "Không mở được VietMap Live.", Toast.LENGTH_LONG).show();
        }
    }

    private void toggleVideoPlayback() {
        if (safeMode) {
            Toast.makeText(context, "Chuyển sang Parked Video khi xe đã đỗ.", Toast.LENGTH_SHORT).show();
            return;
        }
        desiredPlaying = !desiredPlaying;
        String js = "(function(){var v=document.querySelector('video');if(!v)return 'no-video';" +
                "if(v.paused){var p=v.play();if(p&&p.catch)p.catch(function(){});return 'play';}" +
                "v.pause();return 'pause';})()";
        content.evaluateJavascript(js, value -> {
            if (Prefs.autoFullscreen(context) && desiredPlaying) requestContentFullscreen();
        });
    }

    private void nextMedia() {
        if (safeMode) return;
        desiredPlaying = true;
        String js = "(function(){" +
                "var b=document.querySelector('.ytp-next-button,[aria-label*=Next],[aria-label*=next],[aria-label*=Tiếp]');" +
                "if(b){b.click();return 'clicked';}" +
                "var v=document.querySelector('video');if(v&&isFinite(v.duration)){v.currentTime=Math.max(0,v.duration-0.25);}" +
                "return 'fallback';})()";
        content.evaluateJavascript(js, null);
    }

    private void previousMedia() {
        if (safeMode) return;
        String js = "(function(){" +
                "var b=document.querySelector('.ytp-prev-button,[aria-label*=Previous],[aria-label*=previous],[aria-label*=Trước]');" +
                "if(b){b.click();return 'clicked';}" +
                "var v=document.querySelector('video');if(v){v.currentTime=0;return 'restart';}" +
                "return 'none';})()";
        content.evaluateJavascript(js, null);
    }

    private void requestContentFullscreen() {
        if (safeMode) {
            Toast.makeText(context, "Fullscreen video chỉ dùng ở Parked Video.", Toast.LENGTH_SHORT).show();
            return;
        }
        String js = "(function(){var v=document.querySelector('video');if(!v)return 'no-video';" +
                "try{if(v.requestFullscreen){v.requestFullscreen();return 'fs';}" +
                "if(v.webkitRequestFullscreen){v.webkitRequestFullscreen();return 'fs';}" +
                "if(v.webkitEnterFullscreen){v.webkitEnterFullscreen();return 'fs';}}catch(e){}return 'unsupported';})()";
        content.evaluateJavascript(js, null);
    }

    private void showCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        if (fullscreenView != null) {
            callback.onCustomViewHidden();
            return;
        }
        fullscreenView = view;
        fullscreenCallback = callback;
        stage.addView(view, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        view.bringToFront();
        vietMapWidget.bringToFront();
        bringOverlayControlsToFront();
        showControls();
    }

    private void hideCustomView() {
        if (fullscreenView == null) return;
        ViewParent p = fullscreenView.getParent();
        if (p instanceof ViewGroup) ((ViewGroup) p).removeView(fullscreenView);
        fullscreenView = null;
        if (fullscreenCallback != null) fullscreenCallback.onCustomViewHidden();
        fullscreenCallback = null;
        if (!destroyed) {
            applyWidgetLayout();
            showControls();
        }
    }

    private void maybeAutoFullscreen() {
        if (!Prefs.autoFullscreen(context) || safeMode || fullscreenAttempted) return;
        fullscreenAttempted = true;
        handler.postDelayed(this::requestContentFullscreen, 450L);
    }

    private void setupMediaSession() {
        mediaSession = new MediaSession(context, "VF6MediaMap");
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { if (!safeMode) { desiredPlaying = true; playMedia(); } }
            @Override public void onPause() { desiredPlaying = false; pauseMedia(); }
            @Override public void onSkipToNext() { nextMedia(); }
            @Override public void onSkipToPrevious() { previousMedia(); }
        });
        mediaSession.setActive(true);
        updateMediaSession(false, Prefs.lastTitle(context));
    }

    private void playMedia() {
        content.evaluateJavascript("(function(){var v=document.querySelector('video');if(v){var p=v.play();if(p&&p.catch)p.catch(function(){});}})()", null);
    }

    private void pauseMedia() {
        content.evaluateJavascript("(function(){var v=document.querySelector('video');if(v)v.pause();})()", null);
    }

    private void updateMediaSession(boolean playing, String title) {
        if (mediaSession == null) return;
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE |
                PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT |
                PlaybackState.ACTION_SKIP_TO_PREVIOUS;
        PlaybackState state = new PlaybackState.Builder()
                .setActions(actions)
                .setState(playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED,
                        PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build();
        mediaSession.setPlaybackState(state);
        if (title != null && !title.isEmpty()) {
            mediaSession.setMetadata(new MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                    .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, title)
                    .build());
        }
        if (playPauseButton != null) playPauseButton.setText(playing ? "Ⅱ" : "▶");
    }

    private void setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return;
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { voiceButton.setText("…"); }
            @Override public void onBeginningOfSpeech() { }
            @Override public void onRmsChanged(float rmsdB) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() { }
            @Override public void onError(int error) { restoreVoiceLabel(); }
            @Override public void onResults(Bundle results) {
                restoreVoiceLabel();
                ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (list == null || list.isEmpty()) return;
                applyVoiceQuery(list.get(0));
            }
            @Override public void onPartialResults(Bundle partialResults) { }
            @Override public void onEvent(int eventType, Bundle params) { }
        });
    }

    private void startVoiceSearch() {
        if (safeMode) {
            Toast.makeText(context, "Voice Search media chỉ dùng ở Parked Video.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Hãy cấp quyền Microphone trong app trên điện thoại.", Toast.LENGTH_LONG).show();
            return;
        }
        if (speechRecognizer == null) {
            Toast.makeText(context, "Thiết bị không có Speech Recognizer.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Search media");
        speechRecognizer.startListening(i);
    }

    private void applyVoiceQuery(String query) {
        if (query == null || query.trim().isEmpty()) return;
        String q = Uri.encode(query.trim());
        if (Prefs.contentMode(context) == Prefs.MODE_YOUTUBE) {
            content.loadUrl("https://m.youtube.com/results?search_query=" + q);
        } else {
            content.loadUrl("https://www.google.com/search?q=" + q);
        }
    }

    private void restoreVoiceLabel() {
        if (voiceButton != null) voiceButton.setText("MIC");
    }

    void onResume() {
        content.onResume();
        vietMapWidget.startListening();
        vietMapWidget.reload();
        updateVmlButton();
        handler.removeCallbacks(vmlStatusRunnable);
        handler.post(vmlStatusRunnable);
        if (sizeButton != null) sizeButton.setText(presetLabel(Prefs.vietMapWidgetPreset(context, phonePreview)));
        applyWidgetLayout();
        if (mediaSession != null) mediaSession.setActive(true);
    }

    void onPause() {
        handler.removeCallbacks(vmlStatusRunnable);
        content.onPause();
        vietMapWidget.stopListening();
    }

    void destroy() {
        destroyed = true;
        handler.removeCallbacksAndMessages(null);
        hideCustomView();
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
        vietMapWidget.destroy();
        destroyWebView(content);
    }

    private static void destroyWebView(WebView w) {
        ViewGroup p = (ViewGroup) w.getParent();
        if (p != null) p.removeView(w);
        w.stopLoading();
        w.loadUrl("about:blank");
        w.clearHistory();
        w.removeAllViews();
        w.destroy();
    }

    private final class TouchStage extends FrameLayout {
        TouchStage(Context context) {
            super(context);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            // v1.6: never reveal the control rail from a tap on the media area.
            // When hidden, touches must pass through to YouTube/Web/VietMap. The
            // only way to reveal the rail is the dedicated left-edge handle.
            if (ev.getActionMasked() == MotionEvent.ACTION_DOWN && controlsVisible) {
                onStageInteraction();
            }
            return super.onInterceptTouchEvent(ev);
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private final class PlaybackBridge {
        @JavascriptInterface
        public void playback(String url, long positionMs, long durationMs, boolean playing, String title) {
            handler.post(() -> {
                Prefs.savePlayback(context, url, positionMs, durationMs, playing, title);
                desiredPlaying = playing || desiredPlaying && Prefs.keepPlaying(context);
                updateMediaSession(playing, title);
                if (playing) maybeAutoFullscreen();
            });
        }
    }
}
