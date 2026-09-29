package com.vf6.mediamap;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQ_BIND_WIDGET = 6407;
    private static final int REQ_CONFIGURE_WIDGET = 6408;

    private EditText youtubeUrl;
    private EditText webUrl;
    private Spinner contentMode;
    private Spinner scale;
    private CheckBox autoResume;
    private CheckBox keepPlaying;
    private CheckBox autoFullscreen;

    private TextView widgetStatus;
    private Spinner widgetProviderSpinner;
    private Spinner widgetSizeSpinner;
    private Spinner widgetPositionSpinner;
    private final List<AppWidgetProviderInfo> widgetProviders = new ArrayList<>();
    private AppWidgetHost widgetHost;
    private int pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("VF6 MediaMap");
        widgetHost = new AppWidgetHost(this, VietMapWidgetHost.HOST_ID);

        if (savedInstanceState != null) {
            pendingWidgetId = savedInstanceState.getInt("pendingWidgetId", AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(28));
        root.setBackgroundColor(Color.rgb(20, 20, 24));
        scroll.addView(root);

        root.addView(text("VF6 MediaMap 1.6.0", 26, true));
        root.addView(text(
                "YouTube/Web toàn màn hình + VietMap Live widget. v1.6 có watchdog tự phục hồi, " +
                        "layout riêng cho điện thoại/Android Auto và preset Compact/Expanded.",
                15, false), lpMatchWrap(dp(8)));

        root.addView(text("Nội dung", 14, true), lpMatchWrap(dp(20)));
        contentMode = new Spinner(this);
        contentMode.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"YouTube", "Web / TV portal"}));
        contentMode.setSelection(Prefs.contentMode(this));
        root.addView(contentMode, lpMatchWrap(dp(6)));

        root.addView(text("YouTube URL", 14, true), lpMatchWrap(dp(16)));
        youtubeUrl = input(Prefs.youtube(this));
        root.addView(youtubeUrl, lpMatchWrap(dp(6)));

        root.addView(text("Web / TV portal URL", 14, true), lpMatchWrap(dp(16)));
        webUrl = input(Prefs.web(this));
        root.addView(webUrl, lpMatchWrap(dp(6)));

        root.addView(text("Tỷ lệ hiển thị WebView", 14, true), lpMatchWrap(dp(16)));
        scale = new Spinner(this);
        int[] scaleValues = {75, 90, 100, 110, 125};
        String[] scaleLabels = {"75%", "90%", "100%", "110%", "125%"};
        scale.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, scaleLabels));
        int currentScale = Prefs.webScale(this);
        int selectedScale = 2;
        for (int i = 0; i < scaleValues.length; i++) if (scaleValues[i] == currentScale) selectedScale = i;
        scale.setSelection(selectedScale);
        root.addView(scale, lpMatchWrap(dp(6)));

        autoResume = check("Tự khôi phục video/trang gần nhất", Prefs.autoResume(this));
        root.addView(autoResume, lpMatchWrap(dp(18)));
        keepPlaying = check("Tự phục hồi nếu video bị pause ngoài ý muốn", Prefs.keepPlaying(this));
        root.addView(keepPlaying, lpMatchWrap(dp(6)));
        autoFullscreen = check("Thử tự fullscreen video ở chế độ Parked Video", Prefs.autoFullscreen(this));
        root.addView(autoFullscreen, lpMatchWrap(dp(6)));

        root.addView(text("VietMap Live widget host (thử nghiệm)", 18, true), lpMatchWrap(dp(28)));
        root.addView(text(
                "Mục này tận dụng VietMap Live đang cài trên điện thoại. VF6 MediaMap chỉ host RemoteViews/AppWidget nếu VietMap có xuất bản widget Android chuẩn; không giả lập dữ liệu VietMap.",
                13, false), lpMatchWrap(dp(6)));

        widgetStatus = text("Đang kiểm tra VietMap…", 13, false);
        widgetStatus.setTextColor(Color.LTGRAY);
        root.addView(widgetStatus, lpMatchWrap(dp(10)));

        root.addView(text("Widget provider", 14, true), lpMatchWrap(dp(14)));
        widgetProviderSpinner = new Spinner(this);
        root.addView(widgetProviderSpinner, lpMatchWrap(dp(6)));

        root.addView(text("Preset overlay trên điện thoại", 14, true), lpMatchWrap(dp(14)));
        widgetSizeSpinner = new Spinner(this);
        widgetSizeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Compact 300×92dp", "Expanded 460×150dp"}));
        widgetSizeSpinner.setSelection(Prefs.vietMapWidgetPreset(this, true));
        root.addView(widgetSizeSpinner, lpMatchWrap(dp(6)));

        root.addView(text("Vị trí overlay", 14, true), lpMatchWrap(dp(14)));
        widgetPositionSpinner = new Spinner(this);
        widgetPositionSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Trên phải", "Trên trái", "Dưới phải", "Dưới trái"}));
        widgetPositionSpinner.setSelection(Prefs.vietMapWidgetPosition(this, true));
        root.addView(widgetPositionSpinner, lpMatchWrap(dp(6)));

        Button refreshWidget = button("QUÉT LẠI WIDGET VIETMAP");
        refreshWidget.setOnClickListener(v -> refreshWidgetProviders());
        root.addView(refreshWidget, lpMatchWrap(dp(12)));

        Button bindWidget = button("KẾT NỐI WIDGET VIETMAP");
        bindWidget.setOnClickListener(v -> beginWidgetBinding());
        root.addView(bindWidget, lpMatchWrap(dp(8)));

        Button removeWidget = button("GỠ WIDGET ĐÃ KẾT NỐI");
        removeWidget.setOnClickListener(v -> removeBoundWidget());
        root.addView(removeWidget, lpMatchWrap(dp(8)));

        Button resetPhoneLayout = button("RESET LAYOUT VIETMAP - ĐIỆN THOẠI");
        resetPhoneLayout.setOnClickListener(v -> {
            Prefs.resetVietMapLayout(this, true);
            widgetSizeSpinner.setSelection(Prefs.vietMapWidgetPreset(this, true));
            widgetPositionSpinner.setSelection(Prefs.vietMapWidgetPosition(this, true));
            Toast.makeText(this, "Đã reset layout VietMap trên điện thoại.", Toast.LENGTH_SHORT).show();
        });
        root.addView(resetPhoneLayout, lpMatchWrap(dp(8)));

        Button resetCarLayout = button("RESET LAYOUT VIETMAP - ANDROID AUTO");
        resetCarLayout.setOnClickListener(v -> {
            Prefs.resetVietMapLayout(this, false);
            Toast.makeText(this, "Đã reset layout VietMap trên Android Auto.", Toast.LENGTH_SHORT).show();
        });
        root.addView(resetCarLayout, lpMatchWrap(dp(8)));

        Button save = button("LƯU CẤU HÌNH");
        save.setOnClickListener(v -> savePrefs());
        root.addView(save, lpMatchWrap(dp(24)));

        Button preview = button("XEM THỬ TRÊN ĐIỆN THOẠI");
        preview.setOnClickListener(v -> {
            savePrefs();
            startActivity(new Intent(this, PreviewActivity.class));
        });
        root.addView(preview, lpMatchWrap(dp(10)));

        Button reset = button("KHÔI PHỤC CẤU HÌNH MEDIA");
        reset.setOnClickListener(v -> {
            Prefs.get(this).edit()
                    .remove(Prefs.YOUTUBE_URL)
                    .remove(Prefs.WEB_URL)
                    .remove(Prefs.CONTENT_MODE)
                    .remove(Prefs.AUTO_RESUME)
                    .remove(Prefs.KEEP_PLAYING)
                    .remove(Prefs.AUTO_FULLSCREEN)
                    .remove(Prefs.WEB_SCALE)
                    .apply();
            recreate();
        });
        root.addView(reset, lpMatchWrap(dp(10)));

        TextView note = text(
                "Preview và CarActivity dùng cùng appWidgetId nhưng lưu vị trí/kích thước riêng. " +
                        "VM C/VM X đổi nhanh Compact/Expanded; giữ lâu nút preset để reset layout của màn hiện tại. " +
                        "Watchdog chỉ reload khi widget bị mất cây view/provider, không reload theo timer bình thường.",
                13, false);
        note.setTextColor(Color.LTGRAY);
        root.addView(note, lpMatchWrap(dp(22)));

        setContentView(scroll);
        refreshWidgetProviders();
        requestPermissionsIfNeeded();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("pendingWidgetId", pendingWidgetId);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_BIND_WIDGET) {
            int id = pendingWidgetId;
            if (data != null) id = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            if (resultCode == RESULT_OK && id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                pendingWidgetId = id;
                finishWidgetBindingOrConfigure(id);
            } else {
                deleteWidgetId(id);
                pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
                refreshWidgetProviders();
            }
            return;
        }

        if (requestCode == REQ_CONFIGURE_WIDGET) {
            int id = pendingWidgetId;
            if (data != null) id = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            if (resultCode == RESULT_OK && id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                persistBoundWidget(id);
            } else {
                deleteWidgetId(id);
                pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
                refreshWidgetProviders();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (widgetHost != null) {
            try {
                widgetHost.startListening();
            } catch (Throwable ignored) {
            }
        }
        if (widgetStatus != null) refreshWidgetProviders();
    }

    @Override
    protected void onPause() {
        if (widgetHost != null) {
            try {
                widgetHost.stopListening();
            } catch (Throwable ignored) {
            }
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (widgetHost != null) {
            try {
                widgetHost.stopListening();
            } catch (Throwable ignored) {
            }
        }
        super.onDestroy();
    }

    private void refreshWidgetProviders() {
        widgetProviders.clear();
        widgetProviders.addAll(VietMapWidgetHost.findVietMapProviders(this));
        List<String> labels = new ArrayList<>();
        for (AppWidgetProviderInfo info : widgetProviders) {
            labels.add(VietMapWidgetHost.providerLabel(this, info) + "\n" + info.provider.flattenToShortString());
        }
        if (labels.isEmpty()) labels.add("Không tìm thấy AppWidgetProvider của VietMap Live");
        widgetProviderSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));

        boolean installed = VietMapWidgetHost.isVietMapInstalled(this);
        int boundId = Prefs.vietMapWidgetId(this);
        AppWidgetProviderInfo boundInfo = null;
        if (boundId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            try {
                boundInfo = AppWidgetManager.getInstance(this).getAppWidgetInfo(boundId);
            } catch (Throwable ignored) {
            }
        }

        StringBuilder status = new StringBuilder();
        status.append("VietMap Live: ").append(installed ? "đã cài" : "không tìm thấy")
                .append(" • AppWidgetProvider: ").append(widgetProviders.size());
        if (boundInfo != null) {
            status.append("\nĐã bind: ").append(VietMapWidgetHost.providerLabel(this, boundInfo))
                    .append(" (#").append(boundId).append(")");
        } else if (boundId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            status.append("\nappWidgetId cũ không còn hợp lệ: #").append(boundId);
        }
        widgetStatus.setText(status.toString());
    }

    private void beginWidgetBinding() {
        saveWidgetLayoutPrefs();
        if (widgetProviders.isEmpty()) {
            Toast.makeText(this,
                    "Không thấy AppWidgetProvider của VietMap. Hãy cập nhật VietMap Live rồi bấm Quét lại.",
                    Toast.LENGTH_LONG).show();
            refreshWidgetProviders();
            return;
        }

        int index = Math.max(0, Math.min(widgetProviderSpinner.getSelectedItemPosition(), widgetProviders.size() - 1));
        AppWidgetProviderInfo info = widgetProviders.get(index);
        removeBoundWidget(false);

        int id = widgetHost.allocateAppWidgetId();
        pendingWidgetId = id;
        Bundle options = VietMapWidgetHost.optionsForPreset(Prefs.vietMapWidgetPreset(this, true));
        boolean allowed = false;
        try {
            allowed = AppWidgetManager.getInstance(this)
                    .bindAppWidgetIdIfAllowed(id, info.provider, options);
        } catch (Throwable ignored) {
        }

        if (allowed) {
            finishWidgetBindingOrConfigure(id);
            return;
        }

        Intent bindIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
        bindIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        bindIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider);
        bindIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options);
        try {
            startActivityForResult(bindIntent, REQ_BIND_WIDGET);
        } catch (Throwable error) {
            deleteWidgetId(id);
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
            Toast.makeText(this, "Android không cho mở màn hình bind widget: " + error.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }

    private void finishWidgetBindingOrConfigure(int id) {
        AppWidgetProviderInfo info = null;
        try {
            info = AppWidgetManager.getInstance(this).getAppWidgetInfo(id);
        } catch (Throwable ignored) {
        }
        if (info == null) {
            deleteWidgetId(id);
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
            Toast.makeText(this, "Không đọc được thông tin widget sau khi bind.", Toast.LENGTH_LONG).show();
            refreshWidgetProviders();
            return;
        }

        if (info.configure != null) {
            try {
                widgetHost.startAppWidgetConfigureActivityForResult(
                        this, id, 0, REQ_CONFIGURE_WIDGET,
                        VietMapWidgetHost.optionsForPreset(Prefs.vietMapWidgetPreset(this, true)));
                return;
            } catch (Throwable ignored) {
                // Some providers declare a configure component that cannot be started by third-party hosts.
                // Keep the bound widget and let RemoteViews decide what to render.
            }
        }
        persistBoundWidget(id);
    }

    private void persistBoundWidget(int id) {
        AppWidgetProviderInfo info = null;
        try {
            info = AppWidgetManager.getInstance(this).getAppWidgetInfo(id);
        } catch (Throwable ignored) {
        }
        String provider = info != null && info.provider != null ? info.provider.flattenToString() : "";
        Prefs.setVietMapWidget(this, id, provider);
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        Toast.makeText(this, "Đã kết nối widget VietMap. Bấm Xem thử trên điện thoại.", Toast.LENGTH_LONG).show();
        refreshWidgetProviders();
    }

    private void removeBoundWidget() {
        removeBoundWidget(true);
    }

    private void removeBoundWidget(boolean notify) {
        int id = Prefs.vietMapWidgetId(this);
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID) deleteWidgetId(id);
        Prefs.clearVietMapWidget(this);
        if (notify) Toast.makeText(this, "Đã gỡ widget khỏi VF6 MediaMap.", Toast.LENGTH_SHORT).show();
        refreshWidgetProviders();
    }

    private void deleteWidgetId(int id) {
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID || widgetHost == null) return;
        try {
            widgetHost.deleteAppWidgetId(id);
        } catch (Throwable ignored) {
        }
    }

    private void savePrefs() {
        int[] scales = {75, 90, 100, 110, 125};
        Prefs.save(this,
                youtubeUrl.getText().toString(),
                webUrl.getText().toString(),
                contentMode.getSelectedItemPosition(),
                autoResume.isChecked(),
                keepPlaying.isChecked(),
                autoFullscreen.isChecked(),
                scales[scale.getSelectedItemPosition()]);
        saveWidgetLayoutPrefs();
        Toast.makeText(this, "Đã lưu", Toast.LENGTH_SHORT).show();
    }

    private void saveWidgetLayoutPrefs() {
        int preset = widgetSizeSpinner.getSelectedItemPosition() == 1
                ? Prefs.WIDGET_PRESET_EXPANDED : Prefs.WIDGET_PRESET_COMPACT;
        Prefs.setVietMapWidgetPreset(this, true, preset);
        Prefs.setVietMapWidgetPosition(this, true, widgetPositionSpinner.getSelectedItemPosition());
        Prefs.clearVietMapWidgetCustomSize(this, true);
        int id = Prefs.vietMapWidgetId(this);
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
            try {
                AppWidgetManager.getInstance(this).updateAppWidgetOptions(
                        id, VietMapWidgetHost.optionsForPreset(preset));
            } catch (Throwable ignored) {
            }
        }
    }

    private void requestPermissionsIfNeeded() {
        List<String> missing = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.RECORD_AUDIO);
        }
        if (!missing.isEmpty()) requestPermissions(missing.toArray(new String[0]), 42);
    }

    private EditText input(String value) {
        EditText e = new EditText(this);
        e.setText(value);
        e.setSingleLine(true);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(Color.GRAY);
        e.setBackgroundColor(Color.rgb(45, 45, 52));
        e.setPadding(dp(12), 0, dp(12), 0);
        return e;
    }

    private CheckBox check(String label, boolean checked) {
        CheckBox c = new CheckBox(this);
        c.setText(label);
        c.setTextColor(Color.WHITE);
        c.setChecked(checked);
        c.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        return c;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setMinHeight(dp(52));
        return b;
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(Color.WHITE);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        t.setGravity(Gravity.START);
        return t;
    }

    private LinearLayout.LayoutParams lpMatchWrap(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = top;
        return p;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
