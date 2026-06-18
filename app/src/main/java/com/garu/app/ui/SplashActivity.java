package com.garu.app.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.garu.app.llm.ModelDownloader;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Always route to MainActivity (main menu)
        // From there, user can start chat or download model if needed
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
