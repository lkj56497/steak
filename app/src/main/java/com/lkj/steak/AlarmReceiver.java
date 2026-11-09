package com.lkj.steak;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

/** 화면이 꺼졌거나 다른 앱을 보고 있을 때 단계가 끝나면 알림을 울려요. */
public class AlarmReceiver extends BroadcastReceiver {
    private static final int MAX = 60;
    private static final String ACTION = "com.lkj.steak.ALARM";

    private static PendingIntent pending(Context c, int i, String title, String text) {
        Intent it = new Intent(c, AlarmReceiver.class);
        it.setAction(ACTION);
        if (title != null) {
            it.putExtra("title", title);
            it.putExtra("text", text);
        }
        return PendingIntent.getBroadcast(c, i, it,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static synchronized void schedule(Context c, String json) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        SharedPreferences p = c.getSharedPreferences("alarms", Context.MODE_PRIVATE);
        int old = p.getInt("n", 0);
        for (int i = 0; i < old; i++) am.cancel(pending(c, i, null, null));
        int n = 0;
        try {
            JSONArray a = new JSONArray(json);
            boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
            for (int i = 0; i < a.length() && i < MAX; i++) {
                JSONObject o = a.getJSONObject(i);
                long at = o.getLong("at");
                PendingIntent pi = pending(c, i, o.optString("title", "스테이크"), o.optString("text", ""));
                if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                n++;
            }
        } catch (Exception ignored) { }
        p.edit().putInt("n", n).apply();
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        if (MainActivity.foreground) return; // 앱이 열려 있으면 앱 안에서 소리를 내요
        MainActivity.createChannel(c);
        Intent open = new Intent(c, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent cp = PendingIntent.getActivity(c, 1000, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String title = intent.getStringExtra("title");
        String text = intent.getStringExtra("text");
        Notification nt = new Notification.Builder(c, MainActivity.CHANNEL)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(title == null ? "스테이크" : title)
                .setContentText(text == null ? "" : text)
                .setCategory(Notification.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(cp)
                .build();
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify((int) (System.currentTimeMillis() % 1000000), nt);
    }
}
