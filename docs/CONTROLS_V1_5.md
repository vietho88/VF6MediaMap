# v1.5.2 control rail

The media UI uses a left-side vertical rail layered inside the same FrameLayout as the WebView and VietMap AppWidgetHostView.

Behavior:

- Visible on entry, then hides after 2800 ms of inactivity.
- Hidden state leaves a narrow left-edge `›` handle.
- **Only the `›` handle reveals the rail.**
- Tapping YouTube/Web/VietMap while the rail is hidden does not reveal it and is passed through to the underlying content.
- Normal interaction while the rail is visible restarts the hide timer.
- `VM E` enters VietMap edit mode and disables auto-hide.
- `VM ✓` saves/locks the VietMap widget and restores auto-hide.
- HTML5 fullscreen stays inside the media stage so VietMap and the control rail can remain layered above fullscreen media.

## Hidden state

```text
›            YouTube / Web content

                         [ VietMap ]
```

Tap `›` to reveal the controls.

## VietMap editing

1. Tap `›`.
2. Tap `VM E`.
3. Drag VietMap with one finger.
4. Pinch with two fingers to resize.
5. Tap `VM ✓` to save and lock.
