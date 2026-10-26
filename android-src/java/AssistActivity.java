package com.vibe.jarvis;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.WindowManager;
import com.getcapacitor.BridgeActivity;

public class AssistActivity extends BridgeActivity {
    public static volatile boolean running = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(AssistantPlugin.class);
        super.onCreate(savedInstanceState);
        running = true;
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getBridge() != null && getBridge().getWebView() != null) {
            getBridge().getWebView().setBackgroundColor(Color.TRANSPARENT);
        }
    }

    // Jab screen se hat jaye to band ho jaye, taaki agli baar naya session shuru ho.
    @Override
    public void onStop() {
        super.onStop();
        if (!isFinishing()) {
            finishAndRemoveTask();
        }
    }

    @Override
    protected void onDestroy() {
        running = false;
        super.onDestroy();
        JarvisSession s = JarvisSession.current;
        if (s != null) {
            try { s.hide(); } catch (Exception ignored) {}
        }
    }
}
