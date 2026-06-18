# Keep Room entities
-keep class com.garu.app.model.** { *; }

# Keep JNI bridge
-keep class com.garu.app.llm.LlamaContext { *; }

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
