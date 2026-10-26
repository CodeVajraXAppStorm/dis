package com.vibe.jarvis;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent in) {
        String title = in.getStringExtra("title");
        String body = in.getStringExtra("body");
        String url = in.getStringExtra("url");
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("jarvis_reminders", "Jarvis reminders", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(ch);
        }
        Intent open;
        if (url != null && !url.isEmpty()) {
            open = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        } else {
            open = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        }
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        int id = (int) (System.currentTimeMillis() % 1000000000L);
        PendingIntent pi = PendingIntent.getActivity(c, id, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, "jarvis_reminders") : new Notification.Builder(c);
        b.setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pi)
            .setAutoCancel(true);
        if (Build.VERSION.SDK_INT < 26) b.setPriority(Notification.PRIORITY_HIGH);
        nm.notify(id, b.build());
    }
}
