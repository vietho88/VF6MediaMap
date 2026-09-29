package com.vf6.mediamap;

import android.Manifest;
import android.app.Activity;
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
    private EditText youtubeUrl;
    private EditText webUrl;
    private EditText mapUrl;
    private Spinner contentMode;
    private Spinner ratio;
    private Spinner scale;
    private CheckBox autoResume;
    private CheckBox keepPlaying;
    private CheckBox autoFullscreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("VF6 MediaMap");

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(28));
        root.setBackgroundColor(Color.rgb(20, 20, 24));
        scroll.addView(root);

        root.addView(text("VF6 MediaMap 1.1", 26, true));
        root.addView(text("YouTube/Web + bản đồ trong cùng giao diện Android Auto. Các tính năng video chỉ nên dùng khi xe đã đỗ.", 15, false), lpMatchWrap(dp(8)));

        root.addView(text("Nội dung bên trái", 14, true), lpMatchWrap(dp(20)));
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

        root.addView(text("Map URL", 14, true), lpMatchWrap(dp(16)));
        mapUrl = input(Prefs.map(this));
        root.addView(mapUrl, lpMatchWrap(dp(6)));

        root.addView(text("Tỷ lệ nội dung / Map", 14, true), lpMatchWrap(dp(16)));
        ratio = new Spinner(this);
        String[] ratios = {"50 / 50", "60 / 40", "70 / 30", "40 / 60", "30 / 70"};
        ratio.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ratios));
        int current = Prefs.ratio(this);
        if (current == 50) ratio.setSelection(0);
        else if (current == 60) ratio.setSelection(1);
        else if (current == 70) ratio.setSelection(2);
        else if (current == 40) ratio.setSelection(3);
        else ratio.setSelection(4);
        root.addView(ratio, lpMatchWrap(dp(6)));

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

        autoFullscreen = check("Thử tự fullscreen video khi ở Parked Split", Prefs.autoFullscreen(this));
        root.addView(autoFullscreen, lpMatchWrap(dp(6)));

        Button save = button("LƯU CẤU HÌNH");
        save.setOnClickListener(v -> savePrefs());
        root.addView(save, lpMatchWrap(dp(22)));

        Button preview = button("XEM THỬ TRÊN ĐIỆN THOẠI");
        preview.setOnClickListener(v -> {
            savePrefs();
            startActivity(new Intent(this, PreviewActivity.class));
        });
        root.addView(preview, lpMatchWrap(dp(10)));

        Button reset = button("KHÔI PHỤC MẶC ĐỊNH");
        reset.setOnClickListener(v -> resetDefaults());
        root.addView(reset, lpMatchWrap(dp(10)));

        TextView note = text(
                "Điều khiển mới: Voice Search, Play/Pause, Next, Previous, Fullscreen, Swap và đổi tỷ lệ. " +
                        "Android Auto cần bật Developer mode của Android Auto → Unknown sources. " +
                        "Màn xe mặc định mở Drive Safe (Map toàn màn hình).",
                13, false);
        note.setTextColor(Color.LTGRAY);
        root.addView(note, lpMatchWrap(dp(22)));

        setContentView(scroll);
        requestPermissionsIfNeeded();
    }

    private void savePrefs() {
        int[] ratios = {50, 60, 70, 40, 30};
        int[] scales = {75, 90, 100, 110, 125};
        Prefs.save(this,
                youtubeUrl.getText().toString(),
                webUrl.getText().toString(),
                mapUrl.getText().toString(),
                contentMode.getSelectedItemPosition(),
                ratios[ratio.getSelectedItemPosition()],
                autoResume.isChecked(),
                keepPlaying.isChecked(),
                autoFullscreen.isChecked(),
                scales[scale.getSelectedItemPosition()]);
        Toast.makeText(this, "Đã lưu", Toast.LENGTH_SHORT).show();
    }

    private void resetDefaults() {
        Prefs.get(this).edit().clear().apply();
        recreate();
    }

    private void requestPermissionsIfNeeded() {
        List<String> missing = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
            missing.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
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
