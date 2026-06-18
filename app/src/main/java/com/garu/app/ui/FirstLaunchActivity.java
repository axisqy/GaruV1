package com.garu.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.garu.app.R;
import com.garu.app.llm.ModelDownloader;

import java.io.File;

public class FirstLaunchActivity extends AppCompatActivity {

    private Button downloadBtn;
    private ProgressBar progressBar;
    private TextView statusText;
    private ModelDownloader downloader;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_launch);

        downloadBtn  = findViewById(R.id.btn_download);
        progressBar  = findViewById(R.id.progress_bar);
        statusText   = findViewById(R.id.tv_status);

        downloader = new ModelDownloader(this);

        downloadBtn.setOnClickListener(v -> startDownload());
    }

    private void startDownload() {
        downloadBtn.setEnabled(false);
        downloadBtn.setText("Downloading…");
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        statusText.setText("0%");

        downloader.download(new ModelDownloader.DownloadCallback() {
            @Override
            public void onProgress(int percent) {
                mainHandler.post(() -> {
                    progressBar.setProgress(percent);
                    statusText.setText(percent + "%");
                });
            }

            @Override
            public void onComplete(File modelFile) {
                mainHandler.post(() -> {
                    statusText.setText("Done!");
                    Intent intent = new Intent(FirstLaunchActivity.this, ChatActivity.class);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    downloadBtn.setEnabled(true);
                    downloadBtn.setText("Retry Download");
                    statusText.setText("Error: " + message);
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (downloader != null) downloader.cancel();
    }
}
