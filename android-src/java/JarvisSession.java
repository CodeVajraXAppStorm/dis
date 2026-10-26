package com.vibe.jarvis;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.voice.VoiceInteractionSession;

public class JarvisSession extends VoiceInteractionSession {
    public static JarvisSession current;

    public JarvisSession(Context context) {
        super(context);
    }

    @Override
    public void onShow(Bundle args, int showFlags) {
        super.onShow(args, showFlags);
        current = this;
        final Intent i = new Intent(getContext(), AssistActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        try {
            startAssistantActivity(i);
        } catch (Exception e) {
            try { getContext().startActivity(i); } catch (Exception ignored) {}
        }
        // Agar kisi dusri app (Gallery etc.) ke upar activity shuru nahi hui, to dobara seedha shuru karo.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!AssistActivity.running) {
                try { getContext().startActivity(i); } catch (Exception ignored) {}
            }
        }, 700);
    }

    @Override
    public void onHide() {
        super.onHide();
        if (current == this) current = null;
    }
}
