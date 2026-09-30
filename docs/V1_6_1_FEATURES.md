# v1.6.1 controls

## Quick mic search

- Tap `←` once: normal browser/media back action after the double-tap window.
- Tap `←` twice within 420 ms: microphone search.
- Voice search keeps the existing Parked Video safety restriction.

## VML health/open button

The side rail shows `VML✓`, `VML?`, or `VML×`.

- `VML✓`: recent VietMap widget update (<=30 s) or the VietMap process is visible.
- `VML?`: VietMap is installed but active state is not observable/recent. Tap opens VietMap Live.
- `VML×`: VietMap Live is not installed/enabled.
- Long press: force-open VietMap Live.

This is intentionally a best-effort check because Android restricts inspection of services owned by another app.
