package com.doktorarbeitbeham;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.doktorarbeitbeham.util.Converter;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class ViewScreenshotsActivity extends AppCompatActivity {
    private static final String TAG = "ViewScreenshots";
    private RecyclerView recyclerView;
    private ScreenshotAdapter adapter;
    private List<Screenshot> screenshots = new ArrayList<>();
    private byte[] key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_screenshots);

        // Get encryption key from preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String keyHex = prefs.getString("key", "");
        key = Converter.hexStringToByteArray(keyHex);

        recyclerView = findViewById(R.id.screenshotsRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ScreenshotAdapter();
        recyclerView.setAdapter(adapter);

        loadScreenshots();
    }

    private void loadScreenshots() {
        File encryptDir = new File(getExternalFilesDir(null), "encrypt");
        File[] files = encryptDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));

        if (files != null) {
            Arrays.sort(files, (f1, f2) -> f2.getName().compareTo(f1.getName())); // Sort by name descending
            for (File file : files) {
                Screenshot screenshot = new Screenshot(file);
                screenshots.add(screenshot);
            }
            adapter.notifyDataSetChanged();
        }
    }

    private class ScreenshotAdapter extends RecyclerView.Adapter<ScreenshotViewHolder> {
        @Override
        public ScreenshotViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.screenshot_item, parent, false);
            return new ScreenshotViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ScreenshotViewHolder holder, int position) {
            Screenshot screenshot = screenshots.get(position);
            try {
                Bitmap bitmap = decryptImage(screenshot.file);
                if (bitmap != null) {
                    holder.imageView.setImageBitmap(bitmap);
                    holder.timeText.setText(screenshot.getTimestamp());
                } else {
                    holder.timeText.setText("Failed to decrypt image");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error decrypting image: " + e.getMessage(), e);
                holder.timeText.setText("Error: " + e.getMessage());
            }
        }

        @Override
        public int getItemCount() {
            return screenshots.size();
        }
    }

    private Bitmap decryptImage(File file) throws Exception {
        String fname = file.getName().substring(9); // Skip hash prefix
        byte[] ivBytes = Arrays.copyOfRange(getSHA(fname), 0, 7);
        byte[] encryptedData = new byte[(int) file.length()];

        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(encryptedData);
        }

        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, ivBytes);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);

        try {
            byte[] decryptedData = cipher.doFinal(encryptedData);
            return BitmapFactory.decodeByteArray(decryptedData, 0, decryptedData.length);
        } catch (Exception e) {
            Log.e(TAG, "Decryption failed for " + file.getName(), e);
            return null;
        }
    }

    private byte[] getSHA(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    private static class Screenshot {
        File file;
        String timestamp;

        Screenshot(File file) {
            this.file = file;
            // Parse timestamp from filename format: hash_yyyy_MM_dd_HH_mm_ss_descriptor.png
            String[] parts = file.getName().split("_");
            if (parts.length >= 7) {
                timestamp = String.format("%s-%s-%s %s:%s:%s",
                        parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]);
            }
        }

        String getTimestamp() {
            return timestamp;
        }
    }

    private static class ScreenshotViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView timeText;

        ScreenshotViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.screenshotImage);
            timeText = view.findViewById(R.id.screenshotTime);
        }
    }
}