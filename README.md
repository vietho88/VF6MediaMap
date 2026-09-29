# VF6 MediaMap v1.6.0

Experimental Android/Android Auto project that combines a full-screen YouTube/WebView
with a hosted VietMap Live Android AppWidget when VietMap exposes an AppWidgetProvider.

## v1.6 highlights

- VietMap watchdog: conservative self-recovery if the bound provider/RemoteViews host breaks.
- Separate VietMap layout for Phone Preview and Android Auto.
- `VM C` Compact preset (300 x 92 dp) and `VM X` Expanded preset (460 x 150 dp).
- Long-press `VM C/VM X` to reset the current surface layout.
- `VM E` / `VM ✓`: drag with one finger, pinch with two fingers, then save/lock.
- Left control rail still auto-hides; only the small left-edge `›` handle reveals it.
- Background stripping remains pre-draw based to avoid the previous VietMap card flashing.

## Android Auto installation

On the test device used for this project, Fermata Auto is installed through KingInstaller.
If a normal APK install does not appear in Android Auto's launcher customization, install
this APK using the same sideload method that successfully exposes Fermata on that phone.

## Build

Push the project to GitHub. The included workflow builds with JDK 17, Android SDK 36 and
fetches Fermata's `aauto.aar` bridge before running `:app:assembleDebug`.

Artifact name:

`VF6MediaMap-v1.6.0-debug-apk`

APK path inside the artifact:

`app-debug.apk`

## Notes

This project is experimental and has not been validated end-to-end on every Android Auto
host. VietMap rendering depends on the installed VietMap Live version exposing a standard
Android AppWidgetProvider and on its RemoteViews behavior.

See `docs/V1_6_FEATURES.md` and `docs/VIETMAP_WIDGET_HOST.md` for implementation notes.

See also `docs/CONTROLS_V1_6.md`.
