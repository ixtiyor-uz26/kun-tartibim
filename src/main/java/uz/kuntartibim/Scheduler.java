package uz.kuntartibim;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

public final class Scheduler {
    static final String CHANNEL = "kun_tartibi";
    private static final String PREFS = "kun";
    private static final int MAX = 400;

    private Scheduler() { }

    static void ensureChannel(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null || nm.getNotificationChannel(CHANNEL) != null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Kun tartibi eslatmalari", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Namoz, ish va mashg'ulot vaqtlari");
        ch.enableVibration(true);
        ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
        nm.createNotificationChannel(ch);
    }

    static void save(Context c, String json) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("items", json).apply();
    }

    static synchronized void scheduleAll(Context c) {
        ensureChannel(c);
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        int old = p.getInt("count", 0);
        for (int i = 0; i < old; i++) {
            PendingIntent pi = PendingIntent.getBroadcast(c, i, new Intent(c, AlarmReceiver.class),
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) { am.cancel(pi); pi.cancel(); }
        }

        int n = 0;
        try {
            JSONArray arr = new JSONArray(p.getString("items", "[]"));
            long now = System.currentTimeMillis();
            boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
            for (int i = 0; i < arr.length() && n < MAX; i++) {
                JSONObject o = arr.getJSONObject(i);
                long at = o.getLong("at");
                if (at <= now) continue;
                Intent it = new Intent(c, AlarmReceiver.class);
                it.putExtra("title", o.optString("title", "Kun tartibim"));
                it.putExtra("text", o.optString("text", ""));
                it.putExtra("id", n);
                it.putExtra("kin", o.optString("kin", ""));
                it.putExtra("tel", o.optString("tel", ""));
                PendingIntent pi = PendingIntent.getBroadcast(c, n, it,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                n++;
            }
        } catch (Exception ignored) { }
        p.edit().putInt("count", n).apply();
    }
}
