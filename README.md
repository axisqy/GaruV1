# Garu — Build Guide

Local Android AI assistant. Runs 100% on-device using Phi-3-mini Q4 via llama.cpp.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog or newer |
| Android NDK | r26b or newer |
| CMake | 3.22+ |
| JDK | 17 |

---

## Step 1 — Compile libllama.so

The Java code bridges to llama.cpp via JNI. You need to compile the native library once.

```bash
# 1. Clone llama.cpp
git clone https://github.com/ggerganov/llama.cpp
cd llama.cpp

# 2. Build static library for Android ARM64
mkdir build-android && cd build-android
cmake .. \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-29 \
  -DLLAMA_BUILD_TESTS=OFF \
  -DLLAMA_BUILD_EXAMPLES=OFF
make -j$(nproc) llama

# 3. Compile the JNI bridge (llama-jni.cpp is in app/src/main/cpp/)
$ANDROID_NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android29-clang++ \
  -shared -fPIC -O2 \
  -I /path/to/llama.cpp \
  -I /path/to/llama.cpp/common \
  /path/to/garu/app/src/main/cpp/llama-jni.cpp \
  /path/to/llama.cpp/build-android/libllama.a \
  -o libllama.so \
  -llog -landroid

# 4. Place the output file
cp libllama.so /path/to/garu/app/src/main/jniLibs/arm64-v8a/libllama.so
```

> **Alternative**: Use the prebuilt `.so` from the [llama.cpp Android example](https://github.com/ggerganov/llama.cpp/tree/master/examples/llama.android).

---

## Step 2 — Open in Android Studio

1. Open the `garu/` folder as a project.
2. Wait for Gradle sync to complete.
3. Build → Make Project.

---

## Step 3 — Build the APK

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires signing config in build.gradle)
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/`

---

## Model

The app downloads **Phi-3-mini-4k-instruct Q4** (~2.4 GB) from HuggingFace on first launch:

```
https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf
```

The model is stored in the app's private files directory (`context.getFilesDir()`).  
Internet is only required for this one-time download.

---

## Project Structure

```
app/src/main/
├── java/com/garu/app/
│   ├── llm/
│   │   ├── LlamaContext.java      ← JNI declarations
│   │   ├── GaruInference.java     ← Prompt builder + inference wrapper
│   │   └── ModelDownloader.java   ← HuggingFace download with progress
│   ├── model/
│   │   ├── Message.java           ← Room entity
│   │   ├── MessageDao.java        ← Room DAO
│   │   └── AppDatabase.java       ← Room database singleton
│   └── ui/
│       ├── SplashActivity.java    ← Routes to FirstLaunch or Chat
│       ├── FirstLaunchActivity.java ← Download screen
│       ├── ChatActivity.java      ← Main chat screen
│       ├── ChatViewModel.java     ← LiveData + inference orchestration
│       ├── MessageAdapter.java    ← RecyclerView adapter
│       └── SettingsActivity.java  ← Clear history, re-download
├── cpp/
│   └── llama-jni.cpp              ← JNI bridge source (compile → libllama.so)
└── jniLibs/arm64-v8a/
    └── libllama.so                ← Place compiled binary here
```

---

## Inference Settings

Per spec, these are hardcoded and not exposed to the user:

| Parameter | Value |
|-----------|-------|
| Temperature | 0.2 |
| Top-P | 0.9 |
| Repeat penalty | 1.1 |
| Max tokens | 512 |

---

## Device Requirements

- Android 10+ (API 29)
- ARM64 processor
- 8 GB RAM (6 GB minimum)
- 6 GB free storage (2.5 minimum but maybe more cuz of the unpacking and stuff idk lol)
