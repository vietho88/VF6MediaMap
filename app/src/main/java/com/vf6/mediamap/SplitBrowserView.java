package com.vf6.mediamap;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
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

    private final Context context;
    private final boolean phonePreview;
    private final LinearLayout splitRow;
    private final FrameLayout contentPane;
    private final FrameLayout mapPane;
    private final WebView content;
    private final WebView map;
    private Button modeButton;
    private Button ratioButton;
    private Button voiceButton;
    private Button playPauseButton;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean safeMode;
    private boolean swapped;
    private int ratio;
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
                String keep = Prefs.keepPlaying(context) && desiredPlaying ?
                        "if(v.paused){var p=v.play();if(p&&p.catch){p.catch(function(){});}}" : "";
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
        this.ratio = Prefs.ratio(context);
        this.swapped = Prefs.swapped(context);
        this.safeMode = !phonePreview;
        this.desiredPlaying = Prefs.lastWasPlaying(context);

        setOrientation(VERTICAL);
        setBackgroundColor(Color.BLACK);
        setKeepScreenOn(true);

        addView(buildToolbar(), new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        splitRow = new LinearLayout(context);
        splitRow.setOrientation(HORIZONTAL);
        splitRow.setBackgroundColor(Color.BLACK);
        addView(splitRow, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));

        contentPane = pane(Prefs.contentMode(context) == Prefs.MODE_WEB ? "WEB / TV" : "YOUTUBE");
        mapPane = pane("MAP");
        content = webView(true);
        map = webView(false);
        contentPane.addView(content, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mapPane.addView(map, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        setupMediaSession();
        setupSpeechRecognizer();

        String firstUrl = Prefs.contentUrl(context);
        if (Prefs.autoResume(context)) {
            String last = Prefs.lastContentUrl(context);
            if (last != null && (last.startsWith("https://") || last.startsWith("http://"))) firstUrl = last;
        }
        content.loadUrl(firstUrl);
        map.loadUrl(Prefs.map(context));
        rebuildSplit();
        handler.postDelayed(playbackMonitor, MONITOR_MS);
    }

    private LinearLayout buildToolbar() {
        LinearLayout outer = new LinearLayout(context);
        outer.setOrientation(VERTICAL);
        outer.setBackgroundColor(Color.rgb(28, 28, 32));
        outer.setPadding(dp(3), dp(2), dp(3), dp(2));

        LinearLayout row1 = toolbarRow();
        modeButton = smallButton(safeMode ? "Parked Split" : "Drive Safe");
        modeButton.setOnClickListener(v -> toggleSafeMode());
        row1.addView(modeButton, weightButton(1.2f));

        ratioButton = smallButton(ratio + "/" + (100 - ratio));
        ratioButton.setOnClickListener(v -> cycleRatio());
        row1.addView(ratioButton, weightButton(0.8f));

        Button swap = smallButton("Swap");
        swap.setOnClickListener(v -> swap());
        row1.addView(swap, weightButton(0.8f));

        Button fullscreen = smallButton("Full");
        fullscreen.setOnClickListener(v -> requestContentFullscreen());
        row1.addView(fullscreen, weightButton(0.8f));
        outer.addView(row1, new LayoutParams(LayoutParams.MATCH_PARENT, dp(44)));

        LinearLayout row2 = toolbarRow();
        voiceButton = smallButton(safeMode ? "Mic Map" : "Mic YT");
        voiceButton.setOnClickListener(v -> startVoiceSearch());
        row2.addView(voiceButton, weightButton(1.05f));

        Button prev = smallButton("⏮");
        prev.setOnClickListener(v -> previousMedia());
        row2.addView(prev, weightButton(0.65f));

        playPauseButton = smallButton("▶/Ⅱ");
        playPauseButton.setOnClickListener(v -> toggleVideoPlayback());
        row2.addView(playPauseButton, weightButton(0.75f));

        Button next = smallButton("⏭");
        next.setOnClickListener(v -> nextMedia());
        row2.addView(next, weightButton(0.65f));

        Button reload = smallButton("Reload");
        reload.setOnClickListener(v -> {
            content.reload();
            map.reload();
        });
        row2.addView(reload, weightButton(0.9f));
        outer.addView(row2, new LayoutParams(LayoutParams.MATCH_PARENT, dp(44)));

        return outer;
    }

    private LinearLayout toolbarRow() {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private LayoutParams weightButton(float weight) {
        return new LayoutParams(0, LayoutParams.MATCH_PARENT, weight);
    }

    private FrameLayout pane(String label) {
        FrameLayout f = new FrameLayout(context);
        f.setBackgroundColor(Color.BLACK);
        TextView tag = new TextView(context);
        tag.setText(label);
        tag.setTextSize(10);
        tag.setTextColor(Color.WHITE);
        tag.setBackgroundColor(Color.argb(170, 0, 0, 0));
        tag.setPadding(dp(6), dp(2), dp(6), dp(2));
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        p.leftMargin = dp(4);
        p.topMargin = dp(4);
        f.addView(tag, p);
        return f;
    }

    private WebView webView(boolean isContent) {
        WebView w = new WebView(context);
        w.setBackgroundColor(Color.BLACK);
        w.setOverScrollMode(View.OVER_SCROLL_NEVER);
        w.setInitialScale(Prefs.webScale(context));

        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setGeolocationEnabled(true);
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

        if (isContent) w.addJavascriptInterface(new PlaybackBridge(), "AndroidBridge");

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
                if (isContent) {
                    fullscreenAttempted = false;
                    handler.postDelayed(() -> restorePlaybackIfNeeded(view, url), 1200L);
                }
            }
        });

        w.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                boolean granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                callback.invoke(origin, granted, false);
            }

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
                String fallback = intent.getStringExtra("browser_fallback_url");
                if (fallback != null && (fallback.startsWith("https://") || fallback.startsWith("http://"))) {
                    view.loadUrl(fallback);
                    return true;
                }
                if (phonePreview && intent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(intent);
                }
            } catch (Throwable ignored) {
            }
            return true;
        }

        if (phonePreview) {
            try {
                Intent external = new Intent(Intent.ACTION_VIEW, uri);
                if (external.resolveActivity(context.getPackageManager()) != null) context.startActivity(external);
            } catch (Throwable ignored) {
            }
        }
        return true;
    }

    private void restorePlaybackIfNeeded(WebView view, String url) {
        if (!Prefs.autoResume(context)) return;
        String last = Prefs.lastContentUrl(context);
        long positionMs = Prefs.lastPositionMs(context);
        if (last == null || last.isEmpty() || positionMs < 1500L) return;
        if (!sameMediaPage(last, url)) return;

        boolean shouldPlay = Prefs.lastWasPlaying(context);
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
            Toast.makeText(context, "Parked Split: chỉ dùng video khi xe đã đỗ.", Toast.LENGTH_LONG).show();
        }
        modeButton.setText(safeMode ? "Parked Split" : "Drive Safe");
        voiceButton.setText(safeMode ? "Mic Map" : (Prefs.contentMode(context) == Prefs.MODE_WEB ? "Mic Web" : "Mic YT"));
        if (safeMode) hideCustomView();
        rebuildSplit();
    }

    private void cycleRatio() {
        if (ratio == 50) ratio = 60;
        else if (ratio == 60) ratio = 70;
        else ratio = 50;
        Prefs.get(context).edit().putInt(Prefs.RATIO, ratio).apply();
        ratioButton.setText(ratio + "/" + (100 - ratio));
        rebuildSplit();
    }

    private void swap() {
        swapped = !swapped;
        Prefs.setSwapped(context, swapped);
        rebuildSplit();
    }

    private void rebuildSplit() {
        splitRow.removeAllViews();
        if (safeMode) {
            splitRow.addView(mapPane, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1));
            contentPane.setVisibility(GONE);
            mapPane.setVisibility(VISIBLE);
            return;
        }

        contentPane.setVisibility(VISIBLE);
        mapPane.setVisibility(VISIBLE);
        float firstWeight = ratio;
        float secondWeight = 100 - ratio;
        if (!swapped) {
            splitRow.addView(contentPane, new LayoutParams(0, LayoutParams.MATCH_PARENT, firstWeight));
            splitRow.addView(mapPane, new LayoutParams(0, LayoutParams.MATCH_PARENT, secondWeight));
        } else {
            splitRow.addView(mapPane, new LayoutParams(0, LayoutParams.MATCH_PARENT, secondWeight));
            splitRow.addView(contentPane, new LayoutParams(0, LayoutParams.MATCH_PARENT, firstWeight));
        }
    }

    private void toggleVideoPlayback() {
        desiredPlaying = !desiredPlaying;
        String js = "(function(){var v=document.querySelector('video');if(!v)return 'no-video';" +
                "if(v.paused){var p=v.play();if(p&&p.catch)p.catch(function(){});return 'play';}" +
                "v.pause();return 'pause';})()";
        content.evaluateJavascript(js, value -> {
            if (Prefs.autoFullscreen(context) && desiredPlaying && !safeMode) requestContentFullscreen();
        });
    }

    private void nextMedia() {
        desiredPlaying = true;
        String js = "(function(){" +
                "var b=document.querySelector('.ytp-next-button,[aria-label*=Next],[aria-label*=next],[aria-label*=Tiếp]');" +
                "if(b){b.click();return 'clicked';}" +
                "var v=document.querySelector('video');if(v&&isFinite(v.duration)){v.currentTime=Math.max(0,v.duration-0.25);}" +
                "return 'fallback';})()";
        content.evaluateJavascript(js, null);
    }

    private void previousMedia() {
        String js = "(function(){" +
                "var b=document.querySelector('.ytp-prev-button,[aria-label*=Previous],[aria-label*=previous],[aria-label*=Trước]');" +
                "if(b){b.click();return 'clicked';}" +
                "var v=document.querySelector('video');if(v){v.currentTime=0;return 'restart';}" +
                "return 'none';})()";
        content.evaluateJavascript(js, null);
    }

    private void requestContentFullscreen() {
        if (safeMode) {
            Toast.makeText(context, "Fullscreen video chỉ dùng ở Parked Split.", Toast.LENGTH_SHORT).show();
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
        if (!(context instanceof Activity)) {
            callback.onCustomViewHidden();
            return;
        }
        fullscreenView = view;
        fullscreenCallback = callback;
        Activity activity = (Activity) context;
        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        decor.addView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setVisibility(GONE);
    }

    private void hideCustomView() {
        if (fullscreenView == null) return;
        if (context instanceof Activity) {
            ViewGroup decor = (ViewGroup) ((Activity) context).getWindow().getDecorView();
            decor.removeView(fullscreenView);
        }
        fullscreenView = null;
        if (fullscreenCallback != null) fullscreenCallback.onCustomViewHidden();
        fullscreenCallback = null;
        setVisibility(VISIBLE);
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
            @Override public void onPlay() { desiredPlaying = true; playMedia(); }
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
        playPauseButton.setText(playing ? "Ⅱ" : "▶");
    }

    private void setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return;
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { voiceButton.setText("Nghe…"); }
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
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, safeMode ? "Tìm địa điểm" : "Tìm nội dung");
        speechRecognizer.startListening(i);
    }

    private void applyVoiceQuery(String query) {
        if (query == null || query.trim().isEmpty()) return;
        String q = Uri.encode(query.trim());
        if (safeMode) {
            map.loadUrl("https://www.google.com/maps/search/?api=1&query=" + q);
        } else if (Prefs.contentMode(context) == Prefs.MODE_YOUTUBE) {
            content.loadUrl("https://m.youtube.com/results?search_query=" + q);
        } else {
            content.loadUrl("https://www.google.com/search?q=" + q);
        }
    }

    private void restoreVoiceLabel() {
        voiceButton.setText(safeMode ? "Mic Map" : (Prefs.contentMode(context) == Prefs.MODE_WEB ? "Mic Web" : "Mic YT"));
    }

    void onResume() {
        content.onResume();
        map.onResume();
        if (mediaSession != null) mediaSession.setActive(true);
    }

    void onPause() {
        content.onPause();
        map.onPause();
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
        destroyWebView(content);
        destroyWebView(map);
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

    private Button smallButton(String text) {
        Button b = new Button(context);
        b.setText(text);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setPadding(dp(2), 0, dp(2), 0);
        return b;
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
