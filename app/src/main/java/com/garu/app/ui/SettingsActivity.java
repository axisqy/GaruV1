package com.garu.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.garu.app.BuildConfig;
import com.garu.app.R;
import com.garu.app.llm.ModelDownloader;
import com.garu.app.model.AppDatabase;

import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Version
        TextView tvVersion = findViewById(R.id.tv_version);
        tvVersion.setText("Version " + BuildConfig.VERSION_NAME);

        // Clear history
        Button btnClear = findViewById(R.id.btn_clear_history);
        btnClear.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setMessage("Clear all conversation history?")
                        .setPositiveButton("Clear", (d, w) -> {
                            Executors.newSingleThreadExecutor().execute(() ->
                                    AppDatabase.getInstance(this).messageDao().deleteAll());
                            Toast.makeText(this, "History cleared.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show()
        );

        // Re-download model
        Button btnRedownload = findViewById(R.id.btn_redownload_model);
        btnRedownload.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setMessage("Delete the current model and re-download it? (~2.4 GB)")
                        .setPositiveButton("Re-download", (d, w) -> {
                            ModelDownloader.getModelFile(this).delete();
                            startActivity(new Intent(this, FirstLaunchActivity.class));
                            finish();
                        })
                        .setNegativeButton("Cancel", null)
                        .show()
        );
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
