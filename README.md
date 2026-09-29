# VF6 MediaMap v1.1

Experimental Android Auto media + map app inspired by the open architecture used by Fermata Auto and by publicly documented UX ideas from apps such as CarView Auto.

## Main idea

A single Android Auto `CarActivity` renders two web panes directly. It does **not** use a MediaProjection -> ImageReader -> Bitmap mirror loop for its core experience.

Default parked layout:

```
+-----------------------------+-------------------+
| YouTube / Web / TV portal   | Google Maps       |
| 60%                         | 40%               |
+-----------------------------+-------------------+
```

Default car startup is **Drive Safe**: Maps is full screen while existing media audio may continue in the background. Use **Parked Split** for video/web UI only after the vehicle is parked.

## New in 1.1

- Choose left pane mode: **YouTube** or **Web / TV portal**.
- Restore the last media page and playback position.
- Optional playback-recovery watchdog if a video gets paused unexpectedly.
- Android `MediaSession` transport actions:
  - Play / Pause
  - Next
  - Previous
  These can receive standard media-button / steering-wheel events when Android routes them to the active media session.
- Voice search:
  - Drive Safe -> Google Maps search.
  - Parked YouTube -> YouTube search.
  - Parked Web -> Google web search.
- Fullscreen HTML5 video support.
- Optional attempt to enter fullscreen automatically in Parked Split.
- Better handling for `intent://` and browser fallback URLs to reduce `ERR_UNKNOWN_URL_SCHEME` failures.
- Hardware accelerated WebView and selectable 75%-125% page scale.
- Persistent cookies and geolocation for map/web use.
- 50/50, 60/40, 70/30 and swap controls.

## CarView study

See `docs/CARVIEW_RESEARCH.md`.

The public CarView website advertises YouTube, resume/auto play, voice and steering-wheel control, Maps/VietMap multitasking, TV/Web Player, screen share and display persistence. It does **not** publish source code or identify itself as a Fermata fork, so this project does not claim that relationship and does not copy CarView code.

## Important limitations

1. YouTube/Google can change their web UI at any time. DOM-based Next/Previous/Play controls may need maintenance after site changes.
2. Google account login inside Android WebView may be restricted; playback without login should be tested first.
3. This is an experimental sideloaded Android Auto projection app, not an approved Play Store Android Auto category.
4. Fullscreen requests can be blocked by WebView/site user-gesture policy. The manual **Full** button has the best chance of succeeding.
5. Voice recognition requires microphone permission on the phone.
6. Media-button/steering-wheel delivery depends on the phone/head-unit routing media keys to this app's active `MediaSession`.
7. Video/web UI is intended for parked use. When driving, keep **Drive Safe** active and use audio/navigation controls only.

## Build on GitHub Actions

The workflow downloads Fermata's current `aauto.aar` bridge from the official Fermata repository and builds with:

- JDK 17
- Android SDK 36
- AGP 9.2.1
- Gradle 9.4.1

After a successful run, download:

`VF6MediaMap-v1.1-debug-apk`

Inside is `app-debug.apk`.

## Install / test

1. Install `app-debug.apk` on the phone.
2. Open **VF6 MediaMap**.
3. Grant Location and Microphone if you want Maps current-position and voice search.
4. Configure YouTube/Web URL, map URL, layout and playback options.
5. Use **XEM THỬ TRÊN ĐIỆN THOẠI** first.
6. In Android Auto settings enable Android Auto developer mode and **Unknown sources**.
7. Connect to the VF6 and open **VF6 MediaMap**.
8. If the icon does not appear, reinstall the same APK using the sideload method that already works on the phone (for example KingInstaller/AAAD), then reconnect Android Auto.
9. Keep **Drive Safe** while moving. Switch to **Parked Split** only when parked.

## Licensing

This project does not copy Fermata's media-player source. Its GitHub Actions build downloads Fermata's Android Auto bridge binary (`aauto.aar`) from the official Fermata repository, which is GPL-3.0 licensed. If you redistribute a built APK, review and comply with applicable GPL obligations and provide corresponding source as required.

Official Fermata repository: https://github.com/AndreyPavlenko/Fermata
