# VF6 MediaMap v1.5.0

Experimental Android Auto project focused on YouTube/Web plus the real VietMap Live Android widget hosted inside the same CarActivity.

## v1.5.0 changes

- Replaces the two-row top toolbar with a compact vertical control rail on the left, inspired by the UI reference supplied during testing.
- Controls automatically hide after 2.8 seconds of inactivity.
- When hidden, the first tap anywhere in the media area only reveals the controls instead of activating YouTube underneath.
- A slim edge handle remains available on the left.
- The control rail stays visible while VietMap edit mode is active.
- VietMap edit is integrated directly into the rail: `VM E` enters edit mode and `VM ✓` saves/locks.
- One-finger drag moves the VietMap widget; two-finger pinch resizes it.
- Keeps `VM S/M/L` preset sizing as a fallback.
- Fullscreen WebView video is now hosted inside the media stage so the VietMap overlay and control rail can remain above it.
- Retains the v1.4.2 pre-draw transparency fix that prevents the VietMap widget card from flashing during frequent RemoteViews updates.

## Left control rail

From top to bottom:

- `VIDEO / SAFE`: parked-video / Drive Safe mode.
- `MIC`: voice search.
- `▶ / Ⅱ`: play or pause.
- `⏭`: next media.
- `←`: browser back; falls back to previous/restart media.
- `⛶`: request HTML5 fullscreen.
- `VM / VM×`: show or hide the VietMap widget.
- `VM E / VM ✓`: edit or lock VietMap.
- `VM S/M/L`: cycle VietMap preset size.
- `↻`: reload YouTube/Web and the VietMap widget.

The rail auto-hides after about 2.8 seconds. Tap the screen once to reveal it; that reveal tap is consumed so it does not also trigger the video underneath.

## VietMap edit mode

1. Tap the screen to reveal the rail.
2. Tap `VM E`.
3. Drag the VietMap widget with one finger.
4. Pinch with two fingers to resize.
5. Tap `VM ✓` to save and lock it.

Position and custom size are persisted and restored in PreviewActivity and CarActivity.

## Architecture

```text
CarActivity / PreviewActivity
+-- TouchStage (FrameLayout)
    +-- YouTube/Web WebView
    +-- optional HTML5 fullscreen view
    +-- VietMapWidgetHost
    |   +-- AppWidgetHostView (real VietMap Live widget)
    +-- auto-hide left control rail
    +-- slim reveal handle
```

## Build

GitHub Actions downloads Fermata's `aauto.aar` bridge and builds `app-debug.apk`.

Artifact:

```text
VF6MediaMap-v1.5.0-debug-apk
```

## Safety

Video interaction is intended for parked use. Drive Safe hides video while leaving the VietMap widget visible.
