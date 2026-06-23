package com.garu.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.garu.app.R;

/**
 * Main menu screen. Displayed after splash.
 * User can choose to start chat, view settings, or manage models.
 */
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Garu");
        }

        Button startChatBtn = findViewById(R.id.btn_start_chat);
        Button settingsBtn = findViewById(R.id.btn_settings);

        // Check permissions when they click Start Chat
        startChatBtn.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                openChatActivity();
            } else {
                requestStoragePermission();
            }
        });

        settingsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private boolean checkStoragePermission() {
        // Automatically manages storage restrictions on newer Android versions (13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
                   ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES},
                    PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    // Runs immediately when the user interacts with the permission popup box
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission given! Proceed to open chat
                openChatActivity();
            } else {
                // Permission denied notice
                Toast.makeText(this, "Storage permission is required to read your local 2.30GB LLM file.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openChatActivity() {
        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
        java.io.File modelFile = new java.io.File(getFilesDir(), "Phi-3-mini-4k-instruct-q4.gguf");
        intent.putExtra("MODEL_PATH", modelFile.getAbsolutePath());
        startActivity(intent);
    }

