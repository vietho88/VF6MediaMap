package com.vf6.mediamap;

import android.os.Bundle;
import android.view.View;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarUiController;

public class CarMainActivity extends CarActivity {
    private SplitBrowserView view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setIgnoreConfigChanges(0xFFFFFFFF);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        CarUiController c = getCarUiController();
        if (c != null) {
            try {
                c.getStatusBarController().showTitle();
                c.getStatusBarController().setTitle("VF6 MediaMap");
            } catch (Throwable ignored) {
            }
        }

        view = new SplitBrowserView(this, false);
        setContentView(view);
    }

    @Override public void onResume() { super.onResume(); if (view != null) view.onResume(); }
    @Override public void onPause() { if (view != null) view.onPause(); super.onPause(); }
    @Override public void onDestroy() { if (view != null) view.destroy(); super.onDestroy(); }
}
