# VF6 MediaMap v1.6.1

Experimental Android Auto media UI based on the existing VF6 MediaMap project.

## New in 1.6.1

- Double-tap the **Back (←)** button in the side rail to start quick microphone search.
- A single Back tap is delayed ~420 ms so the app can distinguish it from a double tap.
- New **VML** status button in the side rail:
  - `VML✓`: VietMap Live appears active (recent widget update or visible process).
  - `VML?`: activity/service status cannot be confirmed; tap to open VietMap Live on the phone.
  - `VML×`: VietMap Live is not installed/enabled.
- Long-press **VML** to force-open VietMap Live.
- The phone settings screen also has **KIỂM TRA / MỞ VIETMAP LIVE**.

## Important Android limitation

Modern Android does not reliably let one normal app inspect another app's running services. VF6 MediaMap therefore uses a best-effort combination of recent VietMap widget `RemoteViews` updates and visible process information. If status is stale/unknown, the VML button launches VietMap Live so its warning/navigation engine can be started.

## Existing v1.6 features

- YouTube/Web full screen.
- VietMap AppWidget host overlay.
- Separate phone and Android Auto widget layouts.
- Compact/Expanded presets.
- Drag/pinch VM edit mode.
- Auto-hide left controls with edge handle.
- Widget watchdog and transparent-background pass.

Build with the included GitHub Actions workflow.
