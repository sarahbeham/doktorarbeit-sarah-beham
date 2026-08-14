package com.doktorarbeitbeham.notifications;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.doktorarbeitbeham.MainActivity;
import com.doktorarbeitbeham.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CaptureNotifications {
    public static final int CAPTURE_NOTIFICATION_ID = 101;
    private static final String CHANNEL_ID = "sarah_beham_doktoarbeit";
    private static final String CAPTURE_CHANNEL_ID = "capture-channel";
    private static final String REMINDER_CHANNEL_ID = "reminder-channel";

    @SuppressLint("SimpleDateFormat")
    private static final DateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

    private final Context context;
    private final NotificationManager notificationManager;

    public CaptureNotifications(Context context) {
        this.context = context;
        this.notificationManager = context.getSystemService(NotificationManager.class);
        createChannels();
    }

    @SuppressLint("ObsoleteSdkInt")
    public void createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        // Service channel
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Sarah Beham Doktorarbeit Service Channel",
                NotificationManager.IMPORTANCE_HIGH
        );
        serviceChannel.setShowBadge(true);
        serviceChannel.setSound(null, null);
        serviceChannel.enableVibration(false);
        serviceChannel.enableLights(false);
        notificationManager.createNotificationChannel(serviceChannel);

        // Updates channel
        NotificationChannel updateChannel = new NotificationChannel(
                CAPTURE_CHANNEL_ID,
                "Sarah Beham Doktorarbeit Updates Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        updateChannel.setShowBadge(true);
        updateChannel.setSound(null, null);
        updateChannel.enableVibration(false);
        updateChannel.enableLights(false);
        notificationManager.createNotificationChannel(updateChannel);

        // Reminder channel
        NotificationChannel reminderChannel = new NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Sarah Beham Doktorarbeit Reminder Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        reminderChannel.setShowBadge(true);
        updateChannel.setSound(null, null);
        reminderChannel.enableVibration(false);
        reminderChannel.enableLights(false);
        notificationManager.createNotificationChannel(reminderChannel);
    }

    @SuppressLint("ObsoleteSdkInt")
    public void notifyImageCaptured() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        if (!hasNotificationPermission()) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CAPTURE_CHANNEL_ID)
                .setSmallIcon(R.drawable.dna)
                .setContentTitle("Sarah Beham Doktorarbeit: Es wurde gerade ein Screenshot aufgenommen!")
                .setContentText("Zeitpunkt: " + sdf.format(new Date()))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSound(null) // Explicitly disable sound
                .setVibrate(null) // Disable vibration
                .setAutoCancel(true);

        notificationManager.notify(2, builder.build());
    }

    private Notification getCaptureStatusNotification(Service service, String title, String subtitle) {
        if (!hasNotificationPermission()) {
            throw new SecurityException("Notification permission not granted");
        }

        Intent notificationIntent = new Intent(context, MainActivity.class);

        int intentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                intentFlags
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.dna)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSound(null) // Explicitly disable sound
                .setVibrate(null) // Disable vibration
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE);

        return builder.build();
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @SuppressLint("ObsoleteSdkInt")
    public void notifyCaptureStopped(Service service) {
        Notification notification = getCaptureStatusNotification(
                service,
                "Doktorarbeit Sarah Beham läuft aktuell nicht.",
                "Bitte starte die App neu"
        );
        service.startForeground(CAPTURE_NOTIFICATION_ID, notification);
    }

    @SuppressLint("ObsoleteSdkInt")
    public Notification getCaptureStartedNotification(Service foregroundService) {
        return getCaptureStatusNotification(
                foregroundService,
                "Doktorarbeit: Screenshotaufnahme ist aktuell aktiviert",
                "Wenn dieses Notification verschwindet, starte bitte die App neu"
        );
    }
}