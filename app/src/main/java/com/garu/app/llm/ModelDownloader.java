package com.garu.app.llm;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Downloads the Phi-3-mini-4k-instruct Q4 GGUF model from HuggingFace.
 * Model: microsoft/Phi-3-mini-4k-instruct-gguf  (~2.4 GB)
 * Direct CDN URL via HuggingFace resolve endpoint.
 */
public class ModelDownloader {

    private static final String TAG = "ModelDownloader";

    // Official Microsoft Phi-3 Mini 4K Q4 GGUF — ~2.4 GB
    public static final String MODEL_URL =
            "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/" +
            "Phi-3-mini-4k-instruct-q4.gguf";

    public static final String MODEL_FILENAME = "Phi-3-mini-4k-instruct-q4.gguf";

    /** Callback interface for download progress */
    public interface DownloadCallback {
        void onProgress(int percent);
        void onComplete(File modelFile);
        void onError(String message);
    }

    private final Context context;
    private final OkHttpClient client;
    private Call activeCall;

    public ModelDownloader(Context context) {
        this.context = context.getApplicationContext();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(0, java.util.concurrent.TimeUnit.SECONDS)  // no timeout for large file
                .build();
    }

    /** Returns the local File where the model is (or will be) stored. */
    public static File getModelFile(Context context) {
        return new File(context.getFilesDir(), MODEL_FILENAME);
    }

    /** Returns true if the model file already exists and has non-zero size. */
    public static boolean isModelDownloaded(Context context) {
        File f = getModelFile(context);
        return f.exists() && f.length() > 0;
    }

    /** Start downloading. Calls back on the calling thread's looper via Handler. */
    public void download(DownloadCallback callback) {
        Request request = new Request.Builder().url(MODEL_URL).build();
        activeCall = client.newCall(request);

        activeCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (!call.isCanceled()) {
                    callback.onError("Download failed: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("Server error: " + response.code());
                    return;
                }

                ResponseBody body = response.body();
                if (body == null) { callback.onError("Empty response body"); return; }

                long totalBytes = body.contentLength();
                File outputFile = getModelFile(context);
                // Write to temp file first, rename on success
                File tempFile = new File(context.getFilesDir(), MODEL_FILENAME + ".tmp");

                try (InputStream in = body.byteStream();
                     FileOutputStream out = new FileOutputStream(tempFile)) {

                    byte[] buf = new byte[8192];
                    long downloaded = 0;
                    int lastPercent = -1;
                    int read;

                    while ((read = in.read(buf)) != -1) {
                        out.write(buf, 0, read);
                        downloaded += read;
                        if (totalBytes > 0) {
                            int percent = (int) (downloaded * 100 / totalBytes);
                            if (percent != lastPercent) {
                                lastPercent = percent;
                                callback.onProgress(percent);
                            }
                        }
                    }

                    out.flush();

                } catch (IOException e) {
                    tempFile.delete();
                    callback.onError("Write error: " + e.getMessage());
                    return;
                }

                // Rename temp → final
                if (tempFile.renameTo(outputFile)) {
                    callback.onComplete(outputFile);
                } else {
                    tempFile.delete();
                    callback.onError("Failed to finalize model file.");
                }
            }
        });
    }

    public void cancel() {
        if (activeCall != null) activeCall.cancel();
    }
}
