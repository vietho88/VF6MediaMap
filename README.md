# VF6 MediaMap v1.4.0

Experimental Android Auto project focused on **YouTube/Web + a hosted VietMap Live Android widget**.

## What changed from v1.3

The custom OpenStreetMap/OSRM pane has been removed. v1.4 tries a different architecture:

```text
CarActivity / PreviewActivity
└─ FrameLayout
   ├─ YouTube or Web WebView (full screen)
   └─ AppWidgetHostView (VietMap overlay)
```

The important part is that VF6 MediaMap does **not** recreate VietMap traffic data. It asks Android for any standard `AppWidgetProvider` published by the installed `vn.vietmap.live` package, lets the user bind that widget on the phone, and then hosts the same `appWidgetId` inside both the phone preview and the projected `CarActivity`.

## Why this is experimental

VietMap publicly advertises Android/Android Auto widget functionality, but that does not prove the implementation is a standard Android `AppWidgetProvider`. If VietMap's Android Auto widget is implemented only through its own car service/template, Android will not expose it to `AppWidgetManager` and a third-party app cannot host it this way.

v1.4 is deliberately diagnostic: the phone setup screen shows whether VietMap is installed and exactly how many VietMap `AppWidgetProvider`s Android exposes.

## Phone setup

1. Install/update **VietMap Live** and log in normally.
2. Install VF6 MediaMap v1.4.0.
3. Open VF6 MediaMap on the phone.
4. Under **VietMap Live widget host (thử nghiệm)** press **QUÉT LẠI WIDGET VIETMAP**.
5. If provider count is greater than 0, select a provider and press **KẾT NỐI WIDGET VIETMAP**.
6. Accept Android's widget binding/configuration screen if one appears.
7. Choose overlay size and position, then press **XEM THỬ TRÊN ĐIỆN THOẠI**.
8. If the real VietMap widget updates in Preview, connect Android Auto and open VF6 MediaMap.

## Overlay controls

Top toolbar:

- `Parked Video` / `Drive Safe`: switch between video view and the non-video driving view.
- `VM On/Off`: show/hide the hosted VietMap widget.
- `VM ↗ / ↖ / ↘ / ↙`: cycle widget corner.
- `VM S / M / L`: cycle 260x86, 360x112, and 480x156 dp widget sizes.
- `Full`: request HTML5 video full-screen where supported.

Second row keeps voice media search and media controls.

## Expected outcomes

### A. Provider count > 0 and Preview renders VietMap

This is the desired result. The same bound widget is then hosted by the Android Auto `CarActivity`.

### B. Provider count = 0

VietMap Live does not expose a standard AppWidget to third-party hosts on that build/device. Do not keep changing layout code: collect package diagnostics instead. Useful ADB commands:

```powershell
adb shell pm path vn.vietmap.live
adb shell cmd package query-receivers --components -a android.appwidget.action.APPWIDGET_UPDATE -p vn.vietmap.live
adb shell dumpsys package vn.vietmap.live > vietmap_package.txt
```

The next implementation should then target the actual exported integration mechanism found in the package instead of guessing.

### C. Preview works but Android Auto shows placeholder/blank widget

The widget is a standard AppWidget, but its `RemoteViews` or provider may restrict the projected host. In that case the APK and `vietmap_package.txt` are the most useful artifacts for the next iteration.

## Build

GitHub Actions downloads Fermata's `aauto.aar` bridge and builds `app-debug.apk`.

Artifact name:

```text
VF6MediaMap-v1.4.0-debug-apk
```

## Safety

Video interaction is intended for parked use. `Drive Safe` hides video while leaving the VietMap widget host visible.
