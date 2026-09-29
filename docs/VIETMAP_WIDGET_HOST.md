# VietMap widget host

VF6 MediaMap hosts VietMap Live only when the installed VietMap package publishes a
standard Android `AppWidgetProvider`. It uses `AppWidgetHost`, `AppWidgetHostView` and
one bound `appWidgetId`.

## v1.6 behavior

- Phone Preview and CarActivity keep separate overlay x/y/width/height preferences.
- Compact and Expanded presets update AppWidget size options for the current surface.
- A conservative watchdog checks the widget while `AppWidgetHost.startListening()` is active.
- The watchdog reloads only after repeated structural failures; it does not use a periodic
  forced refresh, avoiding unnecessary flashes.
- Provider background stripping is performed synchronously/pre-draw to reduce the gray-card
  flash seen when VietMap updates its RemoteViews frequently.

Because the RemoteViews are owned by VietMap, not every provider background or internal
bitmap can be made transparent by the host.
