package com.doktorarbeitbeham;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.google.common.util.concurrent.ListenableFuture;
import com.doktorarbeitbeham.services.capture.CaptureActivity;
import com.doktorarbeitbeham.services.capture.CaptureService;
import com.doktorarbeitbeham.services.upload.SenderWorker;
import com.doktorarbeitbeham.services.upload.UploadScheduler;
import com.doktorarbeitbeham.services.upload.UploadService;
import com.doktorarbeitbeham.util.InternetConnection;

import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = 1;
    private SwitchCompat switchCapture;
    private Timer numImageRefreshTimer;
    private TextView captureState;
    private Boolean recordingState;
    private TextView numImagesText;
    private Button uploadButton;
    private SwitchCompat instagramPermissionToggle;
    private SwitchCompat batteryOptimizationToggle;

    private final ServiceConnection captureServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            CaptureService.LocalBinder localBinder = (CaptureService.LocalBinder) iBinder;
            CaptureService captureService = localBinder.getService();
            if (captureService.isCapturing()) {
                captureState.setText(getResources().getString(R.string.capture_state_on));
                captureState.setTextColor(getResources().getColor(R.color.light_sea_green));
                switchCapture.setEnabled(true);
                switchCapture.setChecked(true);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };
    private TextView numUploadText;
    private UploadService uploadService;
    private final ServiceConnection uploadServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            UploadService.LocalBinder localBinder = (UploadService.LocalBinder) iBinder;
            uploadService = localBinder.getService();
            if (uploadService.status == UploadService.Status.SENDING) {
                numUploadText.setText("Uploading: " + uploadService.numUploaded + "/" + uploadService.numTotal);
            } else {
                numUploadText.setText(uploadService.status.toString());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.viewScreenshotsOption) {
            openScreenshotViewer();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setSupportActionBar(findViewById(R.id.mainToolbar));

        // Notification
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                // Use reflection to get POST_NOTIFICATIONS constant
                Class<?> manifestPermissionClass = Class.forName("android.Manifest$permission");
                String postNotificationsPermission = (String) manifestPermissionClass
                        .getField("POST_NOTIFICATIONS")
                        .get(null);

                if (ActivityCompat.checkSelfPermission(this,
                        postNotificationsPermission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{postNotificationsPermission},
                            1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Cancel any existing work
        WorkManager.getInstance(this).cancelAllWork();
        ListenableFuture<List<WorkInfo>> send_periodic1 =
                WorkManager.getInstance(this).getWorkInfosByTag("send_periodic");
        try {
            System.out.println("SENDPERIODIC: " + send_periodic1.get());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set up periodic work request for uploads
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build();
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                SenderWorker.class,
                1,
                TimeUnit.HOURS)
                .addTag("send_periodic")
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("send_periodic", ExistingPeriodicWorkPolicy.REPLACE,
                        workRequest);

        ListenableFuture<List<WorkInfo>> send_periodic =
                WorkManager.getInstance(this).getWorkInfosByTag("send_periodic");
        try {
            System.out.println("SENDPERIODIC: " + send_periodic.get());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        // Auto-register with the participant keys supplied at build time via
        // local.properties (participantKey / participantHash). One build per participant.
        if (!prefs.contains("state")
                && !BuildConfig.PARTICIPANT_KEY.isEmpty()
                && !BuildConfig.PARTICIPANT_HASH.isEmpty()) {
            editor.putInt("state", 1);  // Set as registered
            editor.putString("key", BuildConfig.PARTICIPANT_KEY);
            editor.putString("hash", BuildConfig.PARTICIPANT_HASH);
            editor.apply();
        }

        recordingState = prefs.getBoolean("recordingState", false);

        captureState = findViewById(R.id.captureState);
        switchCapture = findViewById(R.id.switchCapture);
        numImagesText = findViewById(R.id.imageNumber);
        uploadButton = findViewById(R.id.uploadButton);
        instagramPermissionToggle = findViewById(R.id.instagramPermissionToggle);
        batteryOptimizationToggle = findViewById(R.id.batteryOptimizationToggle);
        numUploadText = findViewById(R.id.uploadNumber);

        switchCapture.setChecked(recordingState);

        switchCapture.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) {
                return;
            }
            if (isChecked) {
                Log.d("MainActivity", "pressed switch button!");
                editor.putBoolean("recordingState", true);
                editor.apply();
                startCapture();
                captureState.setText(getResources().getString(R.string.capture_state_on));
                captureState.setTextColor(getResources().getColor(R.color.light_sea_green));
            } else {
                Intent intent = new Intent(MainActivity.this, StopCaptureActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });

        instagramPermissionToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !checkUsageStatsPermission()) {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            } else if (!isChecked && checkUsageStatsPermission()) {
                Toast.makeText(this, "Bitte aktiviere die App-Nutzungsstatistik", Toast.LENGTH_LONG).show();
                buttonView.setChecked(true);
            }
        });

        batteryOptimizationToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestBatteryOptimizationExemption();
            } else {
                Toast.makeText(this, "Bitte deaktiviere die App-Batterieoptimierung", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);
            }
        });

        uploadButton.setOnClickListener(v -> {
            if (!InternetConnection.checkWiFiConnection(getApplicationContext())) {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Alert");
                alertDialog.setMessage("Bilder ohne WIFI hochladen?");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Upload",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                UploadScheduler.startUpload(getApplicationContext(), true);
                                Toast.makeText(getApplicationContext(), "Uploading...",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Abbrechen",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();

            } else {
                UploadScheduler.startUpload(getApplicationContext(), false);
                Toast.makeText(getApplicationContext(), "Uploading...", Toast.LENGTH_SHORT).show();
            }
        });

        File f_image = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath() +
                File.separator + "images");
        File f_encrypt = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath() +
                File.separator + "encrypt");
        if (!f_image.exists()) f_image.mkdir();
        if (!f_encrypt.exists()) f_encrypt.mkdir();
        Log.i(TAG, "f_image: " + f_image.getAbsolutePath());
    }

    private void startCapture() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String key = prefs.getString("key", "");
        String hash = prefs.getString("hash", "");

        if (key == null || key.isEmpty() || hash == null || hash.isEmpty()) {
            Toast.makeText(this,
                    "Participant key/hash missing. Configure local.properties and reinstall the app.",
                    Toast.LENGTH_LONG).show();
            switchCapture.setChecked(false);
            return;
        }

        Intent serviceIntent = new Intent(this, CaptureActivity.class);
        startActivity(serviceIntent);
    }

    private void openScreenshotViewer() {
        Intent intent = new Intent(this, ViewScreenshotsActivity.class);
        startActivity(intent);
    }

    protected void stopCapture() {
        Intent serviceIntent = new Intent(this, CaptureService.class);
        stopService(serviceIntent);
    }

    public void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivityForResult(intent, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            }
        } else {
            Toast.makeText(this, "Batterieeinstellungen sind auf diesem Gerät nicht vorhanden. Bitte manuell deaktivieren", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateToggleStates() {
        // Check and update Instagram media usage permission toggle
        boolean hasUsageAccess = checkUsageStatsPermission();
        instagramPermissionToggle.setChecked(hasUsageAccess);

        // Check and update battery optimization toggle
        boolean isBatteryOptimizationDisabled = checkBatteryOptimizationDisabled();
        batteryOptimizationToggle.setChecked(isBatteryOptimizationDisabled);
    }

    private boolean checkUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private boolean checkBatteryOptimizationDisabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            return pm.isIgnoringBatteryOptimizations(packageName);
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update toggle states
        updateToggleStates();

        captureState.setText(getResources().getString(R.string.capture_state_off));
        switchCapture.setEnabled(true);
        switchCapture.setChecked(false);
        Intent screenCaptureIntent = new Intent(this, CaptureService.class);
        bindService(screenCaptureIntent, captureServiceConnection, 0);

        Intent intent = new Intent(this, UploadService.class);
        bindService(intent, uploadServiceConnection, 0);

        numImageRefreshTimer = new Timer();
        numImageRefreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        File outputDir =
                                new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + File.separator + "encrypt");
                        File[] files = outputDir.listFiles();
                        if (files == null) return;
                        int numImages = files.length;
                        float bytesTotal = Stream.of(files).mapToLong(File::length).sum();
                        numImagesText.setText(String.format("Anzahl Screenshots: %d (%sMB)",
                                numImages, String.format("%.2f", bytesTotal / 1024 / 1024)));
                        Log.i(TAG, "Image Number:" + numImages);
                        if (uploadService != null) {
                            if (uploadService.status == UploadService.Status.SENDING) {
                                numUploadText.setText("Wird hochgeladen: " + uploadService.numUploaded + "/" + uploadService.numTotal);
                            } else if (uploadService.status == UploadService.Status.SUCCESS) {
                                numUploadText.setText("Erfolgreich hochgeladen " + uploadService.numUploaded + " Screenshots am " + uploadService.lastActivityTime);
                            } else if (uploadService.status == UploadService.Status.FAILED) {
                                numUploadText.setText("Fehler beim hochladen " + uploadService.numToUpload + " Screenshots am " + uploadService.lastActivityTime + " with code " + uploadService.errorCode);
                            } else {
                                numUploadText.setText(uploadService.status.toString());
                            }
                        }
                    }
                });
            }
        }, 500, 5000);
    }

    // This needs to be here so that onResume is called at the correct time.
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        numImageRefreshTimer.cancel();
        unbindService(captureServiceConnection);
        unbindService(uploadServiceConnection);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) {
            if (resultCode == RESULT_OK) {
                // The user has granted the exemption
                Toast.makeText(this, "Batterieoptimierung deaktiviert", Toast.LENGTH_SHORT).show();
            } else {
                // The user has denied the exemption
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Batterieoptimierung")
                        .setMessage("Die App funktioniert im Hintergrund nur, wenn Batterie-Optimierungen für diese App deaktiviert sind.")
                        .setPositiveButton("Einstellungen öffnen", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                            startActivity(intent);
                        })
                        .setNegativeButton("OK", (dialog, which) -> dialog.dismiss())
                        .show();            }
        }
    }
}

