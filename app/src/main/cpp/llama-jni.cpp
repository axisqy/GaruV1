/**
 * llama-jni.cpp
 *
 * JNI bridge between GaruInference (Java) and llama.cpp.
 *
 * ── HOW TO BUILD ────────────────────────────────────────────────────────────
 *
 * 1. Clone llama.cpp:
 *      git clone https://github.com/ggerganov/llama.cpp
 *
 * 2. Build for Android ARM64 using the NDK:
 *      cd llama.cpp
 *      mkdir build-android && cd build-android
 *      cmake .. \
 *        -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
 *        -DANDROID_ABI=arm64-v8a \
 *        -DANDROID_PLATFORM=android-29 \
 *        -DLLAMA_BUILD_TESTS=OFF \
 *        -DLLAMA_BUILD_EXAMPLES=OFF
 *      make -j$(nproc) llama
 *
 * 3. Compile this file into a shared library:
 *      $NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android29-clang++ \
 *        -shared -fPIC -O2 \
 *        -I /path/to/llama.cpp \
 *        -I /path/to/llama.cpp/common \
 *        llama-jni.cpp \
 *        /path/to/llama.cpp/build-android/libllama.a \
 *        -o libllama.so \
 *        -llog -landroid
 *
 * 4. Copy libllama.so to:
 *      app/src/main/jniLibs/arm64-v8a/libllama.so
 *
 * ── REFERENCE ───────────────────────────────────────────────────────────────
 * llama.cpp Android example:
 *   https://github.com/ggerganov/llama.cpp/tree/master/examples/llama.android
 * ────────────────────────────────────────────────────────────────────────────
 */

#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

// llama.cpp public header
#include "llama.h"

#define TAG "GaruLLM"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" {

// ── loadModel ────────────────────────────────────────────────────────────────
JNIEXPORT jlong JNICALL
Java_com_garu_app_llm_LlamaContext_loadModel(
        JNIEnv* env, jobject /* this */, jstring modelPath) {

    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Loading model: %s", path);

    llama_backend_init();

    llama_model_params params = llama_model_default_params();
    params.n_gpu_layers = 0;   // CPU only — GPU offload not guaranteed on all ARMs

    llama_model* model = llama_load_model_from_file(path, params);
    env->ReleaseStringUTFChars(modelPath, path);

    if (!model) {
        LOGE("Failed to load model");
        return 0L;
    }

    LOGI("Model loaded successfully");
    return reinterpret_cast<jlong>(model);
}

// ── createContext ────────────────────────────────────────────────────────────
JNIEXPORT jlong JNICALL
Java_com_garu_app_llm_LlamaContext_createContext(
        JNIEnv* /* env */, jobject /* this */, jlong modelPtr) {

    auto* model = reinterpret_cast<llama_model*>(modelPtr);

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx    = 4096;
    ctx_params.n_batch  = 512;
    ctx_params.n_threads = 4;    // adjust based on device cores

    llama_context* ctx = llama_new_context_with_model(model, ctx_params);
    if (!ctx) {
        LOGE("Failed to create context");
        return 0L;
    }

    return reinterpret_cast<jlong>(ctx);
}

// ── completion ───────────────────────────────────────────────────────────────
JNIEXPORT jstring JNICALL
Java_com_garu_app_llm_LlamaContext_completion(
        JNIEnv* env, jobject /* this */,
        jlong contextPtr,
        jstring jPrompt,
        jfloat temperature,
        jfloat topP,
        jfloat repeatPenalty,
        jint maxTokens) {

    auto* ctx = reinterpret_cast<llama_context*>(contextPtr);
    const llama_model* model = llama_get_model(ctx);

    const char* promptCStr = env->GetStringUTFChars(jPrompt, nullptr);
    std::string prompt(promptCStr);
    env->ReleaseStringUTFChars(jPrompt, promptCStr);

    // Tokenize
    const int n_prompt = -llama_tokenize(model, prompt.c_str(), prompt.size(),
                                          nullptr, 0, true, true);
    std::vector<llama_token> tokens(n_prompt);
    llama_tokenize(model, prompt.c_str(), prompt.size(),
                   tokens.data(), tokens.size(), true, true);

    // Sampling params
    llama_sampling_params sparams;
    sparams.temp          = temperature;
    sparams.top_p         = topP;
    sparams.penalty_repeat = repeatPenalty;

    llama_sampling_context* sampler = llama_sampling_init(sparams);

    // Decode prompt
    llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size(), 0, 0);
    llama_decode(ctx, batch);

    // Generate
    std::string result;
    result.reserve(512);

    for (int i = 0; i < maxTokens; i++) {
        llama_token token = llama_sampling_sample(sampler, ctx, nullptr);
        llama_sampling_accept(sampler, ctx, token, true);

        if (llama_token_is_eog(model, token)) break;

        char buf[256];
        int n = llama_token_to_piece(model, token, buf, sizeof(buf), 0, true);
        if (n > 0) result.append(buf, n);

        // Stop on Phi-3 end-of-turn tag
        if (result.find("<|end|>") != std::string::npos) {
            size_t pos = result.find("<|end|>");
            result = result.substr(0, pos);
            break;
        }

        llama_batch next = llama_batch_get_one(&token, 1, tokens.size() + i, 0);
        llama_decode(ctx, next);
    }

    llama_sampling_free(sampler);
    llama_kv_cache_clear(ctx);

    return env->NewStringUTF(result.c_str());
}

// ── freeContext ──────────────────────────────────────────────────────────────
JNIEXPORT void JNICALL
Java_com_garu_app_llm_LlamaContext_freeContext(
        JNIEnv* /* env */, jobject /* this */, jlong contextPtr) {
    auto* ctx = reinterpret_cast<llama_context*>(contextPtr);
    if (ctx) llama_free(ctx);
}

// ── freeModel ────────────────────────────────────────────────────────────────
JNIEXPORT void JNICALL
Java_com_garu_app_llm_LlamaContext_freeModel(
        JNIEnv* /* env */, jobject /* this */, jlong modelPtr) {
    auto* model = reinterpret_cast<llama_model*>(modelPtr);
    if (model) llama_free_model(model);
    llama_backend_free();
}

} // extern "C"
