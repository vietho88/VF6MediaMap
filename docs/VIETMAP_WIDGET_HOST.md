# VietMap Widget Host experiment

## Goal

Reuse the VietMap Live installation already present on the phone instead of duplicating its map, routing, camera and speed-limit data.

## Android mechanism

The experiment uses the platform widget host APIs:

- `AppWidgetManager.getInstalledProviders()` to discover providers.
- `AppWidgetHost.allocateAppWidgetId()` to allocate an ID owned by VF6 MediaMap.
- `AppWidgetManager.bindAppWidgetIdIfAllowed()` or `ACTION_APPWIDGET_BIND` for user-approved binding.
- `AppWidgetHostView` to render provider `RemoteViews`.
- `updateAppWidgetOptions()` to request compact/medium/large dimensions.

The app persists the bound `appWidgetId`, so `MainActivity`, `PreviewActivity`, and `CarMainActivity` can refer to the same widget instance.

## Hard boundary

This only works for a standard Android AppWidget. Android does not offer an API for one app to steal/reparent another app's arbitrary overlay window or the view tree of another process. An Android Auto widget implemented inside VietMap's own car service is also not automatically an AppWidget.

## Diagnostics

If provider discovery is empty, inspect the installed package rather than inventing provider class names:

```powershell
adb shell pm path vn.vietmap.live
adb shell cmd package query-receivers --components -a android.appwidget.action.APPWIDGET_UPDATE -p vn.vietmap.live
adb shell dumpsys package vn.vietmap.live > vietmap_package.txt
```
