package com.garu.app.llm;

/**
 * JNI bridge to llama.cpp native library.
 *
 * BUILD NOTE: The developer must compile llama.cpp for Android ARM64 and produce:
 *   app/src/main/jniLibs/arm64-v8a/libllama.so
 *
 * Reference implementation:
 *   https://github.com/ggerganov/llama.cpp/tree/master/examples/llama.android
 *
 * The native methods below match the JNI function signatures expected by
 * the llama.cpp Android example (llama-android.cpp).
 */
public class LlamaContext {

    static {
        System.loadLibrary("llama");
    }

    private long contextPointer = 0;

    // ── Native declarations ──────────────────────────────────────────────────

    /** Load a GGUF model from disk. Returns a native pointer (stored as long). */
    public native long loadModel(String modelPath);

    /** Create a context for the loaded model. Returns context pointer. */
    public native long createContext(long modelPointer);

    /** Run a completion. Blocks until done; returns the full response string. */
    public native String completion(
            long contextPointer,
            String prompt,
            float temperature,
            float topP,
            float repeatPenalty,
            int maxTokens
    );

    /** Free native memory. Must be called when done. */
    public native void freeContext(long contextPointer);
    public native void freeModel(long modelPointer);

    // ── Java wrapper ─────────────────────────────────────────────────────────

    private long modelPointer = 0;

    public boolean load(String modelPath) {
        modelPointer = loadModel(modelPath);
        if (modelPointer == 0) return false;
        contextPointer = createContext(modelPointer);
        return contextPointer != 0;
    }

    public String generate(String prompt) {
        if (contextPointer == 0) return null;
        return completion(
                contextPointer,
                prompt,
                0.2f,   // temperature — low for factual output
                0.9f,   // top_p
                1.1f,   // repeat_penalty
                512     // max_tokens
        );
    }

    public void release() {
        if (contextPointer != 0) { freeContext(contextPointer); contextPointer = 0; }
        if (modelPointer   != 0) { freeModel(modelPointer);     modelPointer   = 0; }
    }
}
