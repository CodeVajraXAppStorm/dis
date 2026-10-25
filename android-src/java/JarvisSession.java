package com.vibe.jarvis;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.voice.VoiceInteractionSession;
import android.widget.Toast;

public class JarvisSession extends VoiceInteractionSession {
    public JarvisSession(Context context) {
        super(context);
    }

    @Override
    public void onShow(Bundle args, int showFlags) {
        super.onShow(args, showFlags);
        Toast.makeText(getContext(), "Jarvis सेशन चालू", Toast.LENGTH_SHORT).show();
        Intent i = new Intent(getContext(), AssistActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        try {
            startAssistantActivity(i);
        } catch (Exception e) {
            try { getContext().startActivity(i); } catch (Exception ignored) {}
        }
        // do not finish immediately; give the system time to start the activity
        new Handler(Looper.getMainLooper()).postDelayed(this::hide, 1500);
    }
}
