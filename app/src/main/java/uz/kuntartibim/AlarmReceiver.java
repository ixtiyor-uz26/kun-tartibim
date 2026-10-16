package uz.kuntartibim;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        Scheduler.ensureChannel(c);
        String title = intent.getStringExtra("title");
        String text = intent.getStringExtra("text");
        int id = intent.getIntExtra("id", 0);

        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 1000 + id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = new Notification.Builder(c, Scheduler.CHANNEL)
                .setSmallIcon(R.drawable.ic_notify)
                .setColor(Color.parseColor("#0d6b56"))
                .setContentTitle(title == null ? "Kun tartibim" : title)
                .setAutoCancel(true)
                .setContentIntent(pi);
        if (text != null && !text.isEmpty()) {
            b.setContentText(text);
            b.setStyle(new Notification.BigTextStyle().bigText(text));
        }
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            try { nm.notify(2000 + id, b.build()); } catch (SecurityException ignored) { }
        }
    }
}
