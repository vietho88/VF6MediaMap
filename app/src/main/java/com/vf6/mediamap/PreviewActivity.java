package com.vf6.mediamap;

import android.app.Activity;
import android.os.Bundle;

public class PreviewActivity extends Activity {
    private SplitBrowserView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = new SplitBrowserView(this, true);
        setContentView(view);
    }

    @Override protected void onResume() { super.onResume(); view.onResume(); }
    @Override protected void onPause() { view.onPause(); super.onPause(); }
    @Override protected void onDestroy() { view.destroy(); super.onDestroy(); }
}
