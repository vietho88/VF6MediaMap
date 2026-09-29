# VF6 MediaMap v1.6.0

## VietMap watchdog

The widget host now runs a conservative health check every 8 seconds while listening.
It does **not** reload VietMap just because no new speed/warning value arrived. A reload
is triggered only after two consecutive unhealthy checks such as:

- the bound AppWidget provider disappeared;
- the host view was lost;
- the RemoteViews tree became empty;
- the child host unexpectedly detached or lost size while its container still has size.

This avoids the old flash/reload loop while still recovering from provider/process restarts.

## Separate Phone / Car layouts

VietMap position and custom size are now stored separately for:

- phone Preview (`*_phone` preferences);
- Android Auto CarActivity (`*_car` preferences).

The same bound `appWidgetId` is reused, but each surface applies its own overlay geometry.
Editing VM on the phone no longer overwrites the Android Auto position/size, and vice versa.

## Compact / Expanded presets

The old S/M/L quick control is replaced by two practical presets:

- `VM C` — Compact: 300 x 92 dp;
- `VM X` — Expanded: 460 x 150 dp.

Tap the button to switch preset for the current surface. Long-press the same button to
reset the current surface layout to Compact at the top-right. `VM E` still enables free
drag + pinch resize and `VM ✓` saves the custom layout.
