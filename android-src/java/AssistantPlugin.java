package com.vibe.jarvis;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@CapacitorPlugin(
    name = "Assistant",
    permissions = {
        @Permission(alias = "contacts", strings = { Manifest.permission.READ_CONTACTS }),
        @Permission(alias = "call", strings = { Manifest.permission.CALL_PHONE }),
        @Permission(alias = "calendar", strings = { Manifest.permission.READ_CALENDAR }),
        @Permission(alias = "notif", strings = { "android.permission.POST_NOTIFICATIONS" })
    }
)
public class AssistantPlugin extends Plugin {

    private TextToSpeech tts;
    private boolean ttsReady = false;
    private int langResult = -99;
    private static boolean torchOn = false;

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

    // ---------------- voice ----------------
    @PluginMethod
    public void speak(PluginCall call) {
        if (!ttsReady) { call.reject("TTS not ready"); return; }
        String text = call.getString("text", "");
        String lang = call.getString("lang", "hi-IN");
        int r = tts.setLanguage(Locale.forLanguageTag(lang));
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            call.reject("Voice missing for " + lang);
            return;
        }
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

    // ---------------- mode / apps / urls ----------------
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
    public void openSettings(PluginCall call) {
        String which = call.getString("which", "");
        String action;
        switch (which) {
            case "wifi": action = Settings.ACTION_WIFI_SETTINGS; break;
            case "bluetooth": action = Settings.ACTION_BLUETOOTH_SETTINGS; break;
            case "display": action = Settings.ACTION_DISPLAY_SETTINGS; break;
            case "sound": action = Settings.ACTION_SOUND_SETTINGS; break;
            case "location": action = Settings.ACTION_LOCATION_SOURCE_SETTINGS; break;
            default: action = Settings.ACTION_SETTINGS;
        }
        try {
            Intent i = new Intent(action);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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

    // ---------------- alarm / timer / reminder ----------------
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
    public void setTimer(PluginCall call) {
        try {
            Intent i = new Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_LENGTH, call.getInt("seconds", 60))
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
    public void scheduleReminder(PluginCall call) {
        try {
            long when = call.getData().getLong("time");
            Context ctx = getContext();
            Intent i = new Intent(ctx, ReminderReceiver.class)
                .putExtra("title", call.getString("title", "Jarvis"))
                .putExtra("body", call.getString("body", ""))
                .putExtra("url", call.getString("url", ""));
            int id = (int) (when % 100000000L);
            PendingIntent pi = PendingIntent.getBroadcast(ctx, id, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Intent launch = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
            PendingIntent showPi = PendingIntent.getActivity(ctx, id, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(when, showPi), pi);
            call.resolve();
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    // ---------------- permissions ----------------
    @PluginMethod
    public void requestPerms(PluginCall call) {
        requestPermissionForAliases(new String[] { "contacts", "call", "calendar", "notif" }, call, "permsDone");
    }

    @PermissionCallback
    private void permsDone(PluginCall call) {
        JSObject r = new JSObject();
        r.put("contacts", getPermissionState("contacts") == PermissionState.GRANTED);
        r.put("call", getPermissionState("call") == PermissionState.GRANTED);
        r.put("calendar", getPermissionState("calendar") == PermissionState.GRANTED);
        r.put("notif", getPermissionState("notif") == PermissionState.GRANTED);
        call.resolve(r);
    }

    // ---------------- contacts / call ----------------
    @PluginMethod
    public void getContacts(PluginCall call) {
        JSObject r = new JSObject();
        JSArray arr = new JSArray();
        if (getPermissionState("contacts") != PermissionState.GRANTED) {
            r.put("granted", false);
            r.put("contacts", arr);
            call.resolve(r);
            return;
        }
        Map<String, String> map = new LinkedHashMap<>();
        Cursor c = getContext().getContentResolver().query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            new String[] { ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER },
            null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
        if (c != null) {
            while (c.moveToNext()) {
                String name = c.getString(0);
                String num = c.getString(1);
                if (name != null && num != null && !map.containsKey(name)) map.put(name, num);
            }
            c.close();
        }
        for (Map.Entry<String, String> e : map.entrySet()) {
            JSObject o = new JSObject();
            o.put("name", e.getKey());
            o.put("number", e.getValue());
            arr.put(o);
        }
        r.put("granted", true);
        r.put("contacts", arr);
        call.resolve(r);
    }

    @PluginMethod
    public void call(PluginCall call) {
        try {
            String number = call.getString("number", "");
            boolean can = getPermissionState("call") == PermissionState.GRANTED;
            Intent i = new Intent(can ? Intent.ACTION_CALL : Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number)));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            call.resolve();
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    // ---------------- calendar ----------------
    @PluginMethod
    public void todayEvents(PluginCall call) {
        JSObject r = new JSObject();
        JSArray arr = new JSArray();
        if (getPermissionState("calendar") != PermissionState.GRANTED) {
            r.put("granted", false);
            r.put("events", arr);
            call.resolve(r);
            return;
        }
        Calendar s = Calendar.getInstance();
        s.set(Calendar.HOUR_OF_DAY, 0); s.set(Calendar.MINUTE, 0); s.set(Calendar.SECOND, 0); s.set(Calendar.MILLISECOND, 0);
        Calendar e = (Calendar) s.clone();
        e.add(Calendar.DAY_OF_YEAR, 1);
        Uri.Builder b = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(b, s.getTimeInMillis());
        ContentUris.appendId(b, e.getTimeInMillis());
        Cursor c = getContext().getContentResolver().query(
            b.build(),
            new String[] { CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.ALL_DAY },
            null, null, CalendarContract.Instances.BEGIN + " ASC");
        if (c != null) {
            while (c.moveToNext()) {
                JSObject o = new JSObject();
                o.put("title", c.getString(0));
                o.put("begin", c.getLong(1));
                o.put("allDay", c.getInt(2) == 1);
                arr.put(o);
            }
            c.close();
        }
        r.put("granted", true);
        r.put("events", arr);
        call.resolve(r);
    }

    // ---------------- phone control ----------------
    @PluginMethod
    public void torch(PluginCall call) {
        try {
            CameraManager cm = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
            String id = null;
            for (String s : cm.getCameraIdList()) {
                CameraCharacteristics ch = cm.getCameraCharacteristics(s);
                Boolean flash = ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facing = ch.get(CameraCharacteristics.LENS_FACING);
                if (flash != null && flash && facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) { id = s; break; }
            }
            if (id == null) { call.reject("Flash not found"); return; }
            boolean on = call.hasOption("on") ? call.getBoolean("on", true) : !torchOn;
            cm.setTorchMode(id, on);
            torchOn = on;
            JSObject r = new JSObject();
            r.put("on", on);
            call.resolve(r);
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void volume(PluginCall call) {
        AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int cur = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        String mode = call.getString("mode", "up");
        int step = Math.max(1, Math.round(max / 6f));
        int target = cur;
        switch (mode) {
            case "up": target = Math.min(max, cur + step); break;
            case "down": target = Math.max(0, cur - step); break;
            case "max": target = max; break;
            case "mute": target = 0; break;
            case "set": target = Math.max(0, Math.min(max, Math.round(call.getInt("percent", 50) * max / 100f))); break;
        }
        am.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI);
        JSObject r = new JSObject();
        r.put("percent", Math.round(target * 100f / max));
        call.resolve(r);
    }

    @PluginMethod
    public void brightness(PluginCall call) {
        Context ctx = getContext();
        JSObject r = new JSObject();
        if (!Settings.System.canWrite(ctx)) {
            try {
                Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + ctx.getPackageName()));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
            } catch (Exception ignored) {}
            r.put("needsPermission", true);
            call.resolve(r);
            return;
        }
        int cur = Settings.System.getInt(ctx.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 128);
        int pct = call.hasOption("percent") ? call.getInt("percent", 50) : Math.round(cur * 100f / 255f) + call.getInt("delta", 0);
        pct = Math.max(2, Math.min(100, pct));
        Settings.System.putInt(ctx.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        Settings.System.putInt(ctx.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, Math.round(pct * 255f / 100f));
        r.put("percent", pct);
        call.resolve(r);
    }

    @PluginMethod
    public void battery(PluginCall call) {
        Intent b = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int status = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        JSObject r = new JSObject();
        r.put("percent", Math.round(level * 100f / scale));
        r.put("charging", status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);
        call.resolve(r);
    }
}
