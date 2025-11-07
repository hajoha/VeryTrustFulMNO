/*
 *  SPDX-FileCopyrightText: 2023 Peter Hasse <peter.hasse@fokus.fraunhofer.de>
 *  SPDX-FileCopyrightText: 2023 Johann Hackler <johann.hackler@fokus.fraunhofer.de>
 *  SPDX-FileCopyrightText: 2023 Fraunhofer FOKUS
 *
 *  SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package de.foo.bar.VeryTrustfulMNO;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import android.media.AudioRecord;
import android.widget.Toast;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CallFragment extends Fragment {
    public CallFragment() {
        super(R.layout.fragment_call);
    }
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private File outputFile;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NavController navController = NavHostFragment.findNavController(this);
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                navController.navigate(R.id.HomeFragment);
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void requestMicPermission() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.RECORD_AUDIO
        ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {

            androidx.activity.result.ActivityResultLauncher<String> micPermissionLauncher =
                    registerForActivityResult(
                            new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
                            isGranted -> {

                            }
                    );

            micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO);
        }
    }

    private void writeWavHeader(FileOutputStream out, int totalAudioLen, int sampleRate,
                                int channels, int bitsPerSample) throws IOException {

        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int totalDataLen = totalAudioLen + 36;

        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.put("RIFF".getBytes());
        header.putInt(totalDataLen);
        header.put("WAVE".getBytes());
        header.put("fmt ".getBytes());
        header.putInt(16);
        header.putShort((short) 1); // PCM
        header.putShort((short) channels);
        header.putInt(sampleRate);
        header.putInt(byteRate);
        header.putShort((short) (channels * bitsPerSample / 8));
        header.putShort((short) bitsPerSample);
        header.put("data".getBytes());
        header.putInt(totalAudioLen);

        out.getChannel().position(0);
        out.write(header.array());
    }
    private void startRecording() {
        // Check for microphone permission
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {
            return; // Permission not granted
        }

        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

        audioRecord = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build())
                .setBufferSizeInBytes(bufferSize)
                .build();

        outputFile = new File(
                requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "recording_" + System.currentTimeMillis() + ".wav"
        );

        try {
            audioRecord.startRecording();
        } catch (SecurityException e) {
            e.printStackTrace();
            audioRecord.release();
            audioRecord = null;
            return;
        }

        isRecording = true;

        new Thread(() -> {
            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                byte[] header = new byte[44];
                out.write(header);

                byte[] buffer = new byte[bufferSize];
                int totalAudioLen = 0;

                while (isRecording) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        out.write(buffer, 0, read);
                        totalAudioLen += read;
                    }
                }

                writeWavHeader(out, totalAudioLen, sampleRate, 1, 16);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void stopRecording() {
        if (audioRecord == null) return;

        isRecording = false;

        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {}

        try {
            audioRecord.stop();
        } catch (IllegalStateException ignored) {}

        audioRecord.release();
        audioRecord = null;
    }

    private void uploadRecording(File audioFile, String uploadUrl, UploadCallback callback) {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();

            MediaType mediaType = MediaType.parse("audio/wav");

            RequestBody fileBody = RequestBody.create(audioFile, mediaType);

            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", audioFile.getName(), fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body().string());
                } else {
                    callback.onError("Server error: " + response.code());
                }
            } catch (IOException e) {
                callback.onError("Upload failed: " + e.getMessage());
            }
        }).start();
    }

    public interface UploadCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    private void deleteFile(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requestMicPermission();

        ImageView greenButton = view.findViewById(R.id.green_call_btn);
        ImageView redButton = view.findViewById(R.id.red_call_btn);
        TextView callText = view.findViewById(R.id.calling_text);

        greenButton.setOnClickListener(v -> {
            greenButton.setVisibility(View.INVISIBLE);
            redButton.setVisibility(View.VISIBLE);
            callText.setVisibility(View.VISIBLE);
            startRecording();
        });

        redButton.setOnClickListener(v -> {
            redButton.setVisibility(View.INVISIBLE);
            callText.setVisibility(View.INVISIBLE);
            greenButton.setVisibility(View.VISIBLE);
            stopRecording();

            uploadRecording(outputFile, "http://87.106.144.118:8000/", new UploadCallback() {
                @Override
                public void onSuccess(String response) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Upload OK!", Toast.LENGTH_SHORT).show() //just for debugging
                    );
                    deleteFile(outputFile);
                }

                @Override
                public void onError(String error) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Upload failed: " + error, Toast.LENGTH_LONG).show() //just for debugging
                    );
                }
            });
        });


    }
}