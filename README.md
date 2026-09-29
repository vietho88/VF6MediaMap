# VF6 MediaMap v1.5.2

## v1.5.2 - edge-handle-only control reveal

This release keeps the v1.5.1 Android Auto discovery compatibility and changes only the control-rail reveal behavior:

- Tapping YouTube/Web/VietMap no longer opens the side controls.
- When the rail auto-hides, only the small `›` handle at the left edge can reveal it.
- Touches outside that handle pass directly to the underlying media/widget.
- The reveal handle is widened slightly to 28dp for a more reliable touch target on the car display.
- The rail still auto-hides after about 2.8 seconds.
- `VM E` still pins the rail open while editing VietMap; `VM ✓` saves/locks the widget and restores auto-hide.


## v1.5.1 - Android Auto discovery compatibility

This release keeps the v1.5.0 UI and adds discovery metadata closer to current Fermata Auto: `media + service + projection`, an explicit enabled/icon on `CarService`, the AA application theme, and the automotive feature declaration used by Fermata Auto.

Important: this improves manifest compatibility only. Modern Android Auto can still hide sideloaded apps depending on Android/AA version and installation method. It does not bypass Android Auto driving restrictions.

# VF6 MediaMap v1.5.0

Experimental Android Auto project focused on YouTube/Web plus the real VietMap Live Android widget hosted inside the same CarActivity.

## v1.5.0 changes

- Replaces the two-row top toolbar with a compact vertical control rail on the left, inspired by the UI reference supplied during testing.
- Controls automatically hide after 2.8 seconds of inactivity.
- When hidden, use only the slim left-edge `›` handle to reveal controls; media-area taps go to YouTube/Web/VietMap.
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

The rail auto-hides after about 2.8 seconds. Tap the slim left-edge `›` handle to reveal it. Tapping the video or VietMap does not reveal the rail.

## VietMap edit mode

1. Tap the slim left-edge `›` handle to reveal the rail.
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
VF6MediaMap-v1.5.2-debug-apk
```

## Safety

Video interaction is intended for parked use. Drive Safe hides video while leaving the VietMap widget visible.
