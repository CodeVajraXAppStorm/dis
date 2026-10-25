package com.vibe.jarvis;

import android.content.Intent;
import android.speech.RecognitionService;

// Dummy: Android requires one for the app to appear in the "Digital assistant" list.
public class JarvisRecognitionService extends RecognitionService {
    @Override protected void onStartListening(Intent intent, Callback callback) {}
    @Override protected void onCancel(Callback callback) {}
    @Override protected void onStopListening(Callback callback) {}
}
