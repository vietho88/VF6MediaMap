# VF6 MediaMap v1.4.1

Experimental Android Auto project focused on YouTube/Web plus the real VietMap Live Android widget hosted inside the same CarActivity.

## v1.4.1 changes

- Keeps the real bound VietMap Live AppWidget.
- Best-effort transparent widget chrome: large provider ViewGroup backgrounds are cleared while small sign/speed elements are preserved.
- Adds free drag positioning.
- Adds pinch-to-resize while edit mode is active.
- Saves X/Y and custom widget width/height, so the layout is restored on the phone preview and Android Auto.
- Keeps S/M/L preset sizes as a fallback for head units that do not report multitouch reliably.

## Edit controls

Top toolbar:

- `VM Edit`: enter widget edit mode. A yellow border appears.
- One finger: drag the VietMap widget anywhere inside the media area.
- Two fingers: pinch to resize proportionally.
- `VM Lock`: exit edit mode and save the position/size.
- `VM S/M/L`: reset only the size to a preset; the custom position remains.
- `VM On/Off`: show or hide VietMap.

The transparent-background pass is intentionally best-effort. The widget belongs to VietMap and is delivered as RemoteViews, so a VietMap update may change its internal view hierarchy. v1.4.1 only strips backgrounds from large container views and does not modify VietMap data or icons.

## Architecture

```text
CarActivity / PreviewActivity
+-- FrameLayout
    +-- YouTube/Web WebView
    +-- VietMapWidgetHost
        +-- AppWidgetHostView (real VietMap Live widget)
```

## Build

GitHub Actions downloads Fermata's aauto.aar bridge and builds app-debug.apk.

Artifact:

```text
VF6MediaMap-v1.4.1-debug-apk
```

## Safety

Video interaction is intended for parked use. Drive Safe hides video while leaving the VietMap widget visible.
