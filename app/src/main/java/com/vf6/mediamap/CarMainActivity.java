package com.vf6.mediamap;

import android.os.Bundle;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarUiController;

public class CarMainActivity extends CarActivity {
    private SplitBrowserView view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setIgnoreConfigChanges(0xFFFFFFFF);

        // CarActivity is not a normal android.app.Activity, so do not call
        // getWindow(). Use the Android Auto UI controller instead. This is
        // the same approach used by current Fermata Auto.
        CarUiController controller = getCarUiController();
        if (controller != null) {
            try {
                controller.getStatusBarController().hideAppHeader();
            } catch (Throwable ignored) {
            }
            try {
                controller.getMenuController().hideMenuButton();
            } catch (Throwable ignored) {
            }
        }

        view = new SplitBrowserView(this, false);
        setContentView(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (view != null) view.onResume();
    }

    @Override
    public void onPause() {
        if (view != null) view.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (view != null) view.destroy();
        super.onDestroy();
    }
}
