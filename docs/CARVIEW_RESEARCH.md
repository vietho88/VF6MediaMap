# CarView feature study notes

Publicly advertised CarView capabilities reviewed from https://carviewapp.com/ and its public demo/update descriptions:

- YouTube on Android Auto.
- Restore/reopen the last YouTube video or playlist.
- Voice search / steering-wheel media control.
- Background audio while using Google Maps or VietMap.
- Web Player and TV/add-on style content.
- Optional phone screen sharing in the Full edition.
- Resolution/display-choice persistence.
- Auto-fullscreen options.
- Recovery from accidental/undesired playback pauses.
- Handling of `intent://` / unknown URL schemes in Web Player.
- Optimizations for smooth scrolling and reduced playback stutter.

Important: the CarView website does not publish its source code or state that it is a Fermata fork. Its feature set overlaps heavily with Fermata/CarStream-style Android Auto media apps, but that is not enough to prove source-level derivation. This project therefore re-implements behavior from public feature descriptions and uses only the openly licensed Fermata Android Auto bridge already documented in this repository.

## Implemented in VF6 MediaMap 1.1

- YouTube or generic Web/TV-portal content pane.
- Google Maps pane.
- 50/50, 60/40, 70/30 layouts and swap.
- Drive Safe mode: map only, while audio can continue in background.
- Last media URL and playback-position persistence.
- Optional playback auto-recovery/watchdog.
- Steering-wheel/media-button transport controls through Android MediaSession.
- Voice search for Maps, YouTube, or generic Web search.
- Fullscreen video handling and an optional auto-fullscreen attempt in Parked Split.
- `intent://` fallback handling to avoid common WebView ERR_UNKNOWN_URL_SCHEME failures.
- Hardware-accelerated WebView, no overscroll, persistent cookies, geolocation, and selectable page scale.

## Deliberately not cloned

- CarView licensing/donation/activation flow.
- Proprietary UI/assets.
- Any private CarView APK behavior or code.
- Forced video display intended to defeat driving-safety restrictions.

## Possible next steps

- Native IPTV/M3U + EPG using Media3/ExoPlayer rather than relying on a web portal.
- Favorites/bookmarks/history.
- More robust YouTube DOM adapters per page version.
- Optional screen-share module kept separate from the low-latency core.
- Better metadata/artwork extraction into MediaSession.
