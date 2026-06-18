package com.garu.app.llm;

/**
 * JNI bridge to llama.cpp native library.
 */
public class LlamaContext {

    // ── Safety flag ─────────────────────────────────────────────
    private static boolean nativeAvailable = true;

    // ── Native library load ─────────────────────────────────────
    static {
        try {
            System.loadLibrary("llama");
            nativeAvailable = true;
        } catch (UnsatisfiedLinkError e) {
            android.util.Log.e("LlamaContext",
                    "libllama.so missing - native AI disabled");
            nativeAvailable = false;
        }
    }

    // ── Native pointers ─────────────────────────────────────────
    private long modelPointer = 0;
    private long contextPointer = 0;

    // ── Native methods ──────────────────────────────────────────
    public native long loadModel(String modelPath);

    public native long createContext(long modelPointer);

    public native String completion(
            long contextPointer,
            String prompt,
            float temperature,
            float topP,
            float repeatPenalty,
            int maxTokens
    );

    public native void freeContext(long contextPointer);

    public native void freeModel(long modelPointer);

    // ── Wrapper ─────────────────────────────────────────────────

    public boolean load(String modelPath) {

        if (!nativeAvailable) return false;

        modelPointer = loadModel(modelPath);
        if (modelPointer == 0) return false;

        contextPointer = createContext(modelPointer);
        return contextPointer != 0;
    }

    public String generate(String prompt) {

        if (!nativeAvailable || contextPointer == 0) return null;

        return completion(
                contextPointer,
                prompt,
                0.2f,
                0.9f,
                1.1f,
                512
        );
    }

    public void release() {

        if (contextPointer != 0) {
            freeContext(contextPointer);
            contextPointer = 0;
        }

        if (modelPointer != 0) {
            freeModel(modelPointer);
            modelPointer = 0;
        }
    }
}