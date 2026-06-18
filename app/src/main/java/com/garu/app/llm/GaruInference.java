package com.garu.app.llm;

import android.content.Context;

import com.garu.app.model.Message;

import java.util.List;

/**
 * High-level inference wrapper.
 * Handles prompt formatting for Phi-3-mini (chat template) and
 * injects the Garu system prompt before every conversation.
 */
public class GaruInference {

    // ── Garu system prompt — non-editable per spec ───────────────────────────
    private static final String SYSTEM_PROMPT =
            "You are Garu, a local AI assistant. Follow these rules strictly:\n" +
            "1. NEVER guess missing information. If anything is unclear or missing, " +
                "identify ALL missing details and ask for them in ONE message.\n" +
            "2. Ask all questions at once. Never ask one at a time.\n" +
            "3. No filler words. No greetings. No enthusiasm. Every word must " +
                "serve the goal of solving the problem.\n" +
            "4. If you don't know something, say 'I don't know.' Ask for more " +
                "context. If the user also doesn't know, suggest where they " +
                "could find the answer.\n" +
            "5. Never make things up. Never hallucinate facts, names, or steps. " +
                "Accuracy is more important than sounding confident.\n" +
            "6. Stay focused on the problem. Stop when it's solved.";

    private final LlamaContext llama;

    public GaruInference() {
        llama = new LlamaContext();
    }

    /** Load the model. Returns true on success. */
    public boolean loadModel(String modelPath) {
        return llama.load(modelPath);
    }

    /**
     * Build a full Phi-3 chat prompt from conversation history and run inference.
     *
     * Phi-3 chat template:
     *   <|system|>\n{system}<|end|>\n
     *   <|user|>\n{user}<|end|>\n
     *   <|assistant|>\n{assistant}<|end|>\n
     *   ...
     *   <|assistant|>\n   ← model continues from here
     */
    public String generate(List<Message> history) {
        StringBuilder sb = new StringBuilder();

        // System turn
        sb.append("<|system|>\n").append(SYSTEM_PROMPT).append("<|end|>\n");

        // Conversation turns
        for (Message msg : history) {
            if (msg.isUser()) {
                sb.append("<|user|>\n").append(msg.getText()).append("<|end|>\n");
            } else {
                sb.append("<|assistant|>\n").append(msg.getText()).append("<|end|>\n");
            }
        }

        // Assistant turn start — model generates from here
        sb.append("<|assistant|>\n");

        return llama.generate(sb.toString());
    }

    public void release() {
        llama.release();
    }
}
