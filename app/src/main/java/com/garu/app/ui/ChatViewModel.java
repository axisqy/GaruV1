package com.garu.app.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.garu.app.llm.GaruInference;
import com.garu.app.llm.ModelDownloader;
import com.garu.app.model.AppDatabase;
import com.garu.app.model.Message;
import com.garu.app.model.MessageDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatViewModel extends AndroidViewModel {

    private final MessageDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private GaruInference inference;

    public final LiveData<List<Message>> messages;
    private final MutableLiveData<Boolean> generating = new MutableLiveData<>(false);

    public ChatViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        dao = db.messageDao();
        messages = dao.getAllMessages();
    }

    public LiveData<Boolean> isGenerating() { return generating; }

    /** Initialize the LLM. Call once after model is confirmed present. */
    public void initModel() {
        executor.execute(() -> {
            String modelPath = ModelDownloader.getModelFile(getApplication()).getAbsolutePath();
            inference = new GaruInference();
            boolean ok = inference.loadModel(modelPath);
            if (!ok) {
                // Surface error — model file may be corrupt
                inference = null;
            }
        });
    }

    /** Send a user message and generate a Garu response. */
    public void sendMessage(String userText) {
        if (generating.getValue() == Boolean.TRUE) return;

        Message userMsg = new Message(userText, true);
        executor.execute(() -> {
            dao.insert(userMsg);

            if (inference == null) {
                Message err = new Message("Model not loaded. Please restart the app.", false);
                dao.insert(err);
                return;
            }

            generating.postValue(true);

            // Build history for prompt
            List<Message> history = dao.getAllMessagesSync();

            String response = inference.generate(history);
            if (response == null || response.trim().isEmpty()) {
                response = "I don't know. Could you provide more context?";
            }

            Message garuMsg = new Message(response.trim(), false);
            dao.insert(garuMsg);
            generating.postValue(false);
        });
    }

    /** Wipe all messages from DB. */
    public void clearHistory() {
        executor.execute(dao::deleteAll);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.execute(() -> {
            if (inference != null) inference.release();
        });
        executor.shutdown();
    }
}
