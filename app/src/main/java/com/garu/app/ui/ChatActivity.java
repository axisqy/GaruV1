package com.garu.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.garu.app.R;
import com.garu.app.llm.ModelDownloader;

public class ChatActivity extends AppCompatActivity {

    private ChatViewModel viewModel;
    private MessageAdapter adapter;
    private EditText inputField;
    private ImageButton sendBtn;
    private View loadingIndicator;
    private View modelInitIndicator;
    private boolean modelInitialized = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Garu Chat");
        }

        // Check if model exists before initializing
        if (!ModelDownloader.isModelDownloaded(this)) {
            showModelDownloadDialog();
            return;
        }

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        
        // RecyclerView
        RecyclerView recycler = findViewById(R.id.recycler_messages);
        adapter = new MessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recycler.setLayoutManager(layoutManager);
        recycler.setAdapter(adapter);

        // Input
        inputField = findViewById(R.id.et_input);
        sendBtn = findViewById(R.id.btn_send);
        loadingIndicator = findViewById(R.id.loading_indicator);
        modelInitIndicator = findViewById(R.id.model_init_indicator);

        // Show model loading indicator
        modelInitIndicator.setVisibility(View.VISIBLE);
        sendBtn.setEnabled(false);

        // Initialize model asynchronously (lazy load on first message)
        viewModel.initModel(() -> {
            mainHandler.post(() -> {
                modelInitialized = true;
                modelInitIndicator.setVisibility(View.GONE);
                sendBtn.setEnabled(true);
                Toast.makeText(ChatActivity.this, "AI ready!", Toast.LENGTH_SHORT).show();
            });
        });

        // Observe messages
        viewModel.messages.observe(this, messages -> {
            adapter.setMessages(messages);
            if (!messages.isEmpty()) {
                recycler.scrollToPosition(messages.size() - 1);
            }
        });

        // Loading indicator
        viewModel.isGenerating().observe(this, generating -> {
            loadingIndicator.setVisibility(generating ? View.VISIBLE : View.GONE);
            sendBtn.setEnabled(!generating && modelInitialized);
        });

        sendBtn.setOnClickListener(v -> sendMessage());
    }

    private void showModelDownloadDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Model Required")
                .setMessage("The AI model needs to be downloaded first. Return to settings to download it.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    Intent intent = new Intent(ChatActivity.this, SettingsActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Back", (dialog, which) -> {
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void sendMessage() {
        if (!modelInitialized) {
            Toast.makeText(this, "Model is still loading...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String text = inputField.getText().toString().trim();
        if (text.isEmpty()) return;
        inputField.setText("");
        viewModel.sendMessage(text);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
