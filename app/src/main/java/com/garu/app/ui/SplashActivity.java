package com.garu.app.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.garu.app.llm.ModelDownloader;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Route based on whether model is already downloaded
        if (ModelDownloader.isModelDownloaded(this)) {
            startActivity(new Intent(this, ChatActivity.class));
        } else {
            startActivity(new Intent(this, FirstLaunchActivity.class));
        }
        finish();
    }
}
