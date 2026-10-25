package com.vibe.jarvis;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.AlarmClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@CapacitorPlugin(name = "Assistant")
public class AssistantPlugin extends Plugin {

    private TextToSpeech tts;
    private boolean ttsReady = false;
    private int langResult = -99;

    @Override
    public void load() {
        tts = new TextToSpeech(getContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int r = tts.setLanguage(new Locale("hi", "IN"));
                langResult = r;
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault());
                }
                tts.setPitch(0.9f);
                tts.setSpeechRate(1.0f);
                ttsReady = true;
            }
        });
    }

    @Override
    protected void handleOnDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.handleOnDestroy();
    }

    @PluginMethod
    public void speak(PluginCall call) {
        if (!ttsReady) { call.reject("TTS not ready"); return; }
        String text = call.getString("text", "");
        final String id = UUID.randomUUID().toString();
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String u) {}
            @Override public void onDone(String u) { if (id.equals(u)) call.resolve(); }
            @Override public void onError(String u) { call.reject("tts error"); }
        });
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id);
    }

    @PluginMethod
    public void stopSpeaking(PluginCall call) {
        if (tts != null) tts.stop();
        call.resolve();
    }

    @PluginMethod
    public void ttsInfo(PluginCall call) {
        JSObject o = new JSObject();
        o.put("ready", ttsReady);
        o.put("hindi", langResult >= TextToSpeech.LANG_AVAILABLE);
        o.put("engine", tts != null ? tts.getDefaultEngine() : "none");
        call.resolve(o);
    }

    @PluginMethod
    public void installVoice(PluginCall call) {
        try {
            Intent i = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            call.resolve();
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void getMode(PluginCall call) {
        JSObject r = new JSObject();
        r.put("assist", getActivity() instanceof AssistActivity);
        call.resolve(r);
    }

    @PluginMethod
    public void getApps(PluginCall call) {
        PackageManager pm = getContext().getPackageManager();
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(i, 0);
        JSArray arr = new JSArray();
        for (ResolveInfo ri : list) {
            JSObject o = new JSObject();
            o.put("name", ri.loadLabel(pm).toString());
            o.put("pkg", ri.activityInfo.packageName);
            arr.put(o);
        }
        JSObject r = new JSObject();
        r.put("apps", arr);
        call.resolve(r);
    }

    @PluginMethod
    public void openApp(PluginCall call) {
        String pkg = call.getString("pkg");
        Intent i = getContext().getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) { call.reject("App not found"); return; }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(i);
        call.resolve();
    }

    @PluginMethod
    public void openUrl(PluginCall call) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(call.getString("url")));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            call.resolve();
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void setAlarm(PluginCall call) {
        try {
            Intent i = new Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, call.getInt("hour", 0))
                .putExtra(AlarmClock.EXTRA_MINUTES, call.getInt("minute", 0))
                .putExtra(AlarmClock.EXTRA_MESSAGE, call.getString("label", "Jarvis"))
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            call.resolve();
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void close(PluginCall call) {
        call.resolve();
        getActivity().runOnUiThread(() -> getActivity().finishAndRemoveTask());
    }
}
