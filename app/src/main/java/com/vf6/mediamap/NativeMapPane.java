package com.vf6.mediamap;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

final class NativeMapPane extends FrameLayout {
    interface InteractionListener {
        void onMapInteraction();
    }

    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MapView mapView;
    private final MyLocationNewOverlay locationOverlay;
    private final InteractionListener interactionListener;
    private Marker destinationMarker;
    private GeoPoint destination;
    private String destinationLabel = "";
    private boolean destroyed;

    NativeMapPane(Context context, InteractionListener interactionListener) {
        super(context);
        this.context = context;
        this.interactionListener = interactionListener;

        Configuration.getInstance().setUserAgentValue(context.getPackageName());

        mapView = new MapView(context);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setTilesScaledToDpi(true);
        mapView.setMinZoomLevel(3.0);
        mapView.setMaxZoomLevel(19.0);
        addView(mapView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        double lat = Prefs.mapLatitude(context);
        double lon = Prefs.mapLongitude(context);
        double zoom = Prefs.mapZoom(context);
        mapView.getController().setZoom(zoom);
        mapView.getController().setCenter(new GeoPoint(lat, lon));

        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), mapView);
        locationOverlay.setDrawAccuracyEnabled(true);
        mapView.getOverlays().add(locationOverlay);
        mapView.getOverlays().add(new CopyrightOverlay(context));
        mapView.getOverlays().add(0, new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                notifyInteraction();
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                notifyInteraction();
                setDestination(p, "Pinned destination", true);
                Toast.makeText(context, "Destination pinned. Tap NAV to open navigation.", Toast.LENGTH_SHORT).show();
                return true;
            }
        }));

        mapView.setOnTouchListener((View v, MotionEvent event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) notifyInteraction();
            return false;
        });

        addControls();
        enableLocationIfAllowed(true);
    }

    private void addControls() {
        LinearLayout controls = new LinearLayout(context);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(dp(4), dp(4), dp(4), dp(4));

        Button locate = controlButton("GPS");
        locate.setOnClickListener(v -> {
            notifyInteraction();
            enableLocationIfAllowed(true);
        });
        controls.addView(locate, new LinearLayout.LayoutParams(dp(58), dp(46)));

        Button plus = controlButton("+");
        plus.setOnClickListener(v -> {
            notifyInteraction();
            mapView.getController().zoomIn();
        });
        controls.addView(plus, topMargin(dp(58), dp(46), dp(4)));

        Button minus = controlButton("-");
        minus.setOnClickListener(v -> {
            notifyInteraction();
            mapView.getController().zoomOut();
        });
        controls.addView(minus, topMargin(dp(58), dp(46), dp(4)));

        Button nav = controlButton("NAV");
        nav.setOnClickListener(v -> {
            notifyInteraction();
            openNavigation();
        });
        controls.addView(nav, topMargin(dp(58), dp(46), dp(4)));

        LayoutParams cp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.END | Gravity.CENTER_VERTICAL);
        cp.rightMargin = dp(8);
        addView(controls, cp);

        TextView hint = new TextView(context);
        hint.setText("Native map - long press to pin destination");
        hint.setTextSize(10);
        hint.setTextColor(0xFFFFFFFF);
        hint.setBackgroundColor(0x99000000);
        hint.setPadding(dp(7), dp(3), dp(7), dp(3));
        LayoutParams hp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
        hp.leftMargin = dp(6);
        hp.bottomMargin = dp(6);
        addView(hint, hp);
    }

    private LinearLayout.LayoutParams topMargin(int w, int h, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.topMargin = top;
        return p;
    }

    private Button controlButton(String label) {
        Button b = new Button(context);
        b.setText(label);
        b.setTextSize(11);
        b.setAllCaps(false);
        b.setPadding(0, 0, 0, 0);
        return b;
    }

    private void notifyInteraction() {
        if (interactionListener != null) interactionListener.onMapInteraction();
    }

    void search(String query) {
        if (query == null || query.trim().isEmpty()) return;
        notifyInteraction();
        final String q = query.trim();
        Toast.makeText(context, "Searching map: " + q, Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            GeoPoint found = null;
            String label = q;
            try {
                if (Geocoder.isPresent()) {
                    Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                    List<Address> results = geocoder.getFromLocationName(q, 5);
                    if (results != null && !results.isEmpty()) {
                        Address a = results.get(0);
                        found = new GeoPoint(a.getLatitude(), a.getLongitude());
                        String line = a.getAddressLine(0);
                        if (line != null && !line.isEmpty()) label = line;
                    }
                }
            } catch (IOException | RuntimeException ignored) {
            }

            if (found == null) {
                SearchResult fallback = searchNominatim(q);
                if (fallback != null) {
                    found = fallback.point;
                    label = fallback.label;
                }
            }

            final GeoPoint point = found;
            final String finalLabel = label;
            main.post(() -> {
                if (destroyed) return;
                if (point == null) {
                    Toast.makeText(context, "No map result found.", Toast.LENGTH_SHORT).show();
                    return;
                }
                setDestination(point, finalLabel, true);
                mapView.getController().setZoom(Math.max(mapView.getZoomLevelDouble(), 15.5));
            });
        }, "VF6MapGeocoder").start();
    }

    private SearchResult searchNominatim(String query) {
        HttpURLConnection connection = null;
        try {
            String language = Locale.getDefault().toLanguageTag();
            String encoded = URLEncoder.encode(query, "UTF-8");
            URL url = new URL("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&accept-language="
                    + URLEncoder.encode(language, "UTF-8") + "&q=" + encoded);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(7000);
            connection.setRequestProperty("User-Agent", "VF6MediaMap/1.2.0 (Android)");
            connection.setRequestProperty("Accept", "application/json");
            if (connection.getResponseCode() != 200) return null;

            StringBuilder json = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null && json.length() < 262144) json.append(line);
            }
            JSONArray array = new JSONArray(json.toString());
            if (array.length() == 0) return null;
            JSONObject item = array.getJSONObject(0);
            double lat = Double.parseDouble(item.getString("lat"));
            double lon = Double.parseDouble(item.getString("lon"));
            String label = item.optString("display_name", query);
            return new SearchResult(new GeoPoint(lat, lon), label);
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static final class SearchResult {
        final GeoPoint point;
        final String label;

        SearchResult(GeoPoint point, String label) {
            this.point = point;
            this.label = label;
        }
    }

    private void setDestination(GeoPoint point, String label, boolean animate) {
        destination = point;
        destinationLabel = label == null ? "Destination" : label;
        if (destinationMarker == null) {
            destinationMarker = new Marker(mapView);
            destinationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(destinationMarker);
        }
        destinationMarker.setPosition(point);
        destinationMarker.setTitle(destinationLabel);
        destinationMarker.showInfoWindow();
        if (animate) mapView.getController().animateTo(point);
        mapView.invalidate();
    }

    private void enableLocationIfAllowed(boolean centerNow) {
        boolean granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!granted) {
            if (centerNow) Toast.makeText(context, "Grant Location permission in the phone app first.", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            locationOverlay.enableMyLocation();
            if (centerNow) {
                locationOverlay.enableFollowLocation();
                locationOverlay.runOnFirstFix(() -> main.post(() -> {
                    GeoPoint p = locationOverlay.getMyLocation();
                    if (p != null && !destroyed) {
                        mapView.getController().animateTo(p);
                        if (mapView.getZoomLevelDouble() < 16.0) mapView.getController().setZoom(16.0);
                    }
                }));
            }
        } catch (Throwable ignored) {
        }
    }

    void refresh() {
        mapView.getTileProvider().clearTileCache();
        mapView.invalidate();
        enableLocationIfAllowed(false);
    }

    void openNavigation() {
        GeoPoint point = destination;
        if (point == null) {
            point = locationOverlay.getMyLocation();
        }
        if (point == null) {
            Toast.makeText(context, "Pin or search a destination first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String coordinates = point.getLatitude() + "," + point.getLongitude();
        try {
            Intent maps = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + coordinates));
            maps.setPackage("com.google.android.apps.maps");
            maps.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(maps);
            return;
        } catch (Throwable ignored) {
        }

        try {
            Uri geo = Uri.parse("geo:0,0?q=" + Uri.encode(coordinates + " " + destinationLabel));
            Intent fallback = new Intent(Intent.ACTION_VIEW, geo);
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(fallback);
        } catch (Throwable ignored) {
            Toast.makeText(context, "No navigation app is available.", Toast.LENGTH_SHORT).show();
        }
    }

    void onResume() {
        if (!destroyed) {
            mapView.onResume();
            enableLocationIfAllowed(false);
        }
    }

    void onPause() {
        saveCamera();
        if (!destroyed) mapView.onPause();
    }

    void destroy() {
        if (destroyed) return;
        destroyed = true;
        saveCamera();
        try {
            locationOverlay.disableFollowLocation();
            locationOverlay.disableMyLocation();
        } catch (Throwable ignored) {
        }
        try { mapView.onPause(); } catch (Throwable ignored) { }
        try { mapView.getOverlays().clear(); } catch (Throwable ignored) { }
        try { mapView.getTileProvider().detach(); } catch (Throwable ignored) { }
        removeView(mapView);
        main.removeCallbacksAndMessages(null);
    }

    private void saveCamera() {
        try {
            IGeoPoint c = mapView.getMapCenter();
            if (c != null) Prefs.saveMapCamera(context, c.getLatitude(), c.getLongitude(), mapView.getZoomLevelDouble());
        } catch (Throwable ignored) {
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
