package com.garu.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.garu.app.R;

public class ChatActivity extends AppCompatActivity {

    private ChatViewModel viewModel;
    private MessageAdapter adapter;
    private EditText inputField;
    private ImageButton sendBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Garu");
        }

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        viewModel.initModel();

        // RecyclerView
        RecyclerView recycler = findViewById(R.id.recycler_messages);
        adapter = new MessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recycler.setLayoutManager(layoutManager);
        recycler.setAdapter(adapter);

        // Input
        inputField = findViewById(R.id.et_input);
        sendBtn    = findViewById(R.id.btn_send);

        // Observe messages
        viewModel.messages.observe(this, messages -> {
            adapter.setMessages(messages);
            if (!messages.isEmpty()) {
                recycler.scrollToPosition(messages.size() - 1);
            }
        });

        // Loading indicator
        View loadingIndicator = findViewById(R.id.loading_indicator);
        viewModel.isGenerating().observe(this, generating -> {
            loadingIndicator.setVisibility(generating ? View.VISIBLE : View.GONE);
            sendBtn.setEnabled(!generating);
        });

        sendBtn.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
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
