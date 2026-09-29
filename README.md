# VF6 MediaMap v1.2.0

Experimental Android Auto media + native-map app built around the Fermata-style AAuto `CarActivity` bridge.

## What changed in v1.2.0

The right pane is no longer `google.com/maps` inside a WebView. It is now a real Android native map view based on osmdroid/OpenStreetMap.

Default parked layout:

```
+-----------------------------+-------------------+
| YouTube / Web / TV portal   | Native map        |
| WebView                     | OpenStreetMap     |
| 60%                         | 40%               |
+-----------------------------+-------------------+
```

Default car startup remains **Drive Safe**, where the native map fills the content area.

## Native map features

- Native pan and pinch zoom; no Google Maps WebView prompt.
- GPS/current-position overlay.
- `GPS` button to center on the phone location.
- `+` and `-` buttons for large touch-friendly zoom controls.
- Long-press anywhere on the map to pin a destination.
- `NAV` button hands the pinned point to Google Maps/navigation if installed.
- Voice search can target the map.
- Android Geocoder first, then a Nominatim fallback for place search.
- Remembers last map center and zoom.
- OpenStreetMap attribution is rendered on the map.
- No Google Maps API key is required.

When parked in split mode, tap the map before pressing the toolbar microphone to make the microphone target the map. Tap the YouTube/Web pane to make it target media search again.

## Media features kept from v1.1.x

- YouTube or configurable Web/TV portal in the left WebView.
- Restore the last media page and playback position.
- Optional playback-recovery watchdog.
- Android MediaSession Play/Pause/Next/Previous actions.
- Voice search for YouTube/Web.
- Fullscreen HTML5 video support in phone preview / compatible activity contexts.
- 50/50, 60/40, 70/30 and swap controls.
- Hardware-accelerated WebView and selectable page scale.

## Important behavior

The native map is OpenStreetMap-based; it is not the Google Maps APK embedded inside the app. Android does not provide a supported way for this app to embed the real Google Maps Activity as half of its own layout.

`NAV` intentionally opens the installed navigation application as a separate app/screen for turn-by-turn navigation.

## Build on GitHub Actions

The workflow downloads Fermata's current `aauto.aar` bridge from the official Fermata repository and builds with:

- JDK 17
- Android SDK 36
- AGP 9.2.1
- Gradle 9.4.1
- osmdroid 6.1.20 from Maven Central

After a successful run, download:

`VF6MediaMap-v1.2.0-debug-apk`

Inside is `app-debug.apk`.

## Install / test

1. Install `app-debug.apk` on the phone normally first.
2. Open **VF6 MediaMap** and grant Location + Microphone.
3. Tap **XEM THU TREN DIEN THOAI** and confirm that YouTube and the native map both render.
4. On the map, test drag, pinch, GPS, long-press and NAV.
5. Enable Android Auto developer mode and **Unknown sources**.
6. Connect to the VF6 and open **VF6 MediaMap**.
7. If the icon does not appear, reinstall the same APK with the sideload method that already works on the phone, such as KingInstaller/AAAD.

## Map data and service notes

- Map rendering library: osmdroid 6.1.20.
- Base map data/tiles: OpenStreetMap contributors.
- Search fallback: OpenStreetMap Nominatim, only after Android Geocoder fails.
- This configuration is appropriate for personal/experimental testing. A widely distributed production app should use a tile/geocoding provider and usage plan appropriate for its traffic volume.

## Safety

Video/web UI is intended for use while parked. Keep **Drive Safe** active while the vehicle is moving and use the map/navigation and media transport controls appropriate for driving.

## Licensing

This project downloads Fermata's Android Auto bridge binary (`aauto.aar`) from the official Fermata repository. Fermata is GPL-3.0 licensed. Review and comply with the applicable GPL obligations when redistributing a built APK/source derivative.

osmdroid is distributed under its own open-source license. OpenStreetMap data is provided under ODbL and requires attribution.

Official Fermata repository: https://github.com/AndreyPavlenko/Fermata
osmdroid artifact: org.osmdroid:osmdroid-android:6.1.20
