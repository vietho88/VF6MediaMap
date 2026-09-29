# v1.5 control rail

The media UI uses a left-side vertical rail layered inside the same FrameLayout as the WebView and VietMap AppWidgetHostView.

Behavior:

- Visible on entry, then hides after 2800 ms of inactivity.
- Hidden state leaves a narrow left-edge handle.
- The first tap on the media stage while hidden reveals the rail and consumes that gesture so the underlying WebView/widget is not accidentally activated.
- Any normal interaction while visible restarts the hide timer.
- VietMap edit mode disables auto-hide until `VM ✓` is pressed.
- HTML5 fullscreen is inserted into the stage instead of the Activity decor so both VietMap and the control rail can remain above fullscreen media.
