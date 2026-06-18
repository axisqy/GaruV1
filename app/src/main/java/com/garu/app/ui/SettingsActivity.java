package com.garu.app.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.garu.app.R;
import com.garu.app.llm.ModelDownloader;
import com.garu.app.model.AppDatabase;

import java.io.File;
import java.util.concurrent.Executors;

/**
 * Settings screen allowing users to:
 * - Download or re-download the AI model
 * - Configure AI behavior via system prompts/rules
 * - Clear chat history
 */
public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private ModelDownloader downloader;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private Button downloadModelBtn;
    private Button clearHistoryBtn;
    private Button saveRulesBtn;
    private ProgressBar modelProgressBar;
    private TextView modelStatusText;
    private EditText systemPromptEdit;
    private TextView modelStatusIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        prefs = getSharedPreferences("garu_prefs", MODE_PRIVATE);
        downloader = new ModelDownloader(this);

        // UI Elements
        downloadModelBtn = findViewById(R.id.btn_download_model);
        clearHistoryBtn = findViewById(R.id.btn_clear_history);
        saveRulesBtn = findViewById(R.id.btn_save_rules);
        modelProgressBar = findViewById(R.id.model_progress_bar);
        modelStatusText = findViewById(R.id.tv_model_status);
        systemPromptEdit = findViewById(R.id.et_system_prompt);
        modelStatusIndicator = findViewById(R.id.tv_model_status_indicator);

        updateModelStatus();
        loadSavedRules();

        downloadModelBtn.setOnClickListener(v -> downloadModel());
        clearHistoryBtn.setOnClickListener(v -> clearChatHistory());
        saveRulesBtn.setOnClickListener(v -> saveRules());
    }

    private void updateModelStatus() {
        boolean downloaded = ModelDownloader.isModelDownloaded(this);
        if (downloaded) {
            File modelFile = ModelDownloader.getModelFile(this);
            long sizeMB = modelFile.length() / (1024 * 1024);
            modelStatusIndicator.setText("✓ Model Downloaded (" + sizeMB + " MB)");
            modelStatusIndicator.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            downloadModelBtn.setText("Re-download Model");
        } else {
            modelStatusIndicator.setText("✗ Model Not Downloaded");
            modelStatusIndicator.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            downloadModelBtn.setText("Download Model");
        }
    }

    private void downloadModel() {
        downloadModelBtn.setEnabled(false);
        modelProgressBar.setVisibility(View.VISIBLE);
        modelProgressBar.setProgress(0);
        modelStatusText.setText("0%");

        downloader.download(new ModelDownloader.DownloadCallback() {
            @Override
            public void onProgress(int percent) {
                mainHandler.post(() -> {
                    modelProgressBar.setProgress(percent);
                    modelStatusText.setText(percent + "%");
                });
            }

            @Override
            public void onComplete(File modelFile) {
                mainHandler.post(() -> {
                    modelProgressBar.setVisibility(View.GONE);
                    modelStatusText.setText("Download complete!");
                    Toast.makeText(SettingsActivity.this, "Model downloaded successfully!", Toast.LENGTH_SHORT).show();
                    updateModelStatus();
                    downloadModelBtn.setEnabled(true);
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    modelProgressBar.setVisibility(View.GONE);
                    modelStatusText.setText("Error: " + message);
                    downloadModelBtn.setEnabled(true);
                    Toast.makeText(SettingsActivity.this, "Download failed: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void clearChatHistory() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Clear Chat History")
                .setMessage("Are you sure? This cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> {
                    // Clear from database
                    Executors.newSingleThreadExecutor().execute(() ->
                            AppDatabase.getInstance(this).messageDao().deleteAll());
                    Toast.makeText(SettingsActivity.this, "Chat history cleared.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadSavedRules() {
        String savedPrompt = prefs.getString("system_prompt", "You are a helpful AI assistant.");
        systemPromptEdit.setText(savedPrompt);
    }

    private void saveRules() {
        String prompt = systemPromptEdit.getText().toString().trim();
        if (prompt.isEmpty()) {
            Toast.makeText(this, "System prompt cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("system_prompt", prompt);
        editor.apply();
        
        Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (downloader != null) {
            downloader.cancel();
        }
    }
}
