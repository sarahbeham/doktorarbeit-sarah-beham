package com.doktorarbeitbeham.services.capture;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.doktorarbeitbeham.R;

public class RebootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, CaptureActivity.class);
        int intentFlags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intentFlags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        } else {
            intentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, serviceIntent,
                intentFlags);

        Notification notification =
                new Notification.Builder(context, "reminder-channel")
                        .setContentTitle("Bitte starte Doktorarbeit Sarah Beham neu")
                        .setContentText("Es scheint, dass du dein Gerät neugestartet hast. Bitte " +
                                "starte die App neu und starte die Aufzeichnung")
                        .setSmallIcon(R.drawable.dna)
                        .setContentIntent(pendingIntent)
                        .build();

        NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);
        notificationManager.notify(5, notification);
    }
}