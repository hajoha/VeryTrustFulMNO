package de.foo.bar.VeryTrustfulMNO.WifiScanner;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.foo.bar.VeryTrustfulMNO.R;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WifiScannerFragment extends Fragment {

    private static final String TAG = "WifiScannerFragment";
    private static final String UPLOAD_URL = "https://vwhosnragmnkhkgjogoq.supabase.co/functions/v1/upload-image";

    private Button scanWifiButton;
    private Uri imageUri;

    // Launcher for taking a picture using the native camera app
    private final ActivityResultLauncher<Uri> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            isSuccess -> {
                if (isSuccess) {
                    Log.d(TAG, "Photo taken successfully. URI: " + imageUri);
                    File file = getFileFromUri(imageUri);
                    if (file != null) {
                        uploadImage(file, new UploadCallback() {
                            @Override
                            public void onSuccess(String response) {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() ->
                                            Toast.makeText(requireContext(), "Upload OK!", Toast.LENGTH_SHORT).show()
                                    );
                                }
                                cleanup(file, imageUri);
                            }

                            @Override
                            public void onError(String error) {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() ->
                                            Toast.makeText(requireContext(), "Upload failed: " + error, Toast.LENGTH_LONG).show()
                                    );
                                }
                                cleanup(file, imageUri);
                            }
                        });
                    }
                } else {
                    Log.d(TAG, "Photo capture was cancelled.");
                }
            });

    // Launcher for requesting camera permission
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    launchCamera();
                } else {
                    Toast.makeText(requireContext(), "Camera permission is required.", Toast.LENGTH_SHORT).show();
                }
            });

    public WifiScannerFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wifi_scanner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        scanWifiButton = view.findViewById(R.id.scan_wifi_button);

        scanWifiButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });
    }

    private void launchCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From an app");
        imageUri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (imageUri != null) {
            takePictureLauncher.launch(imageUri);
        } else {
            Toast.makeText(requireContext(), "Failed to create image file.", Toast.LENGTH_SHORT).show();
        }
    }

    private File getFileFromUri(Uri uri) {
        if (uri == null) return null;
        File tempFile = null;
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            tempFile = File.createTempFile("upload", ".jpg", requireContext().getCacheDir());
            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4 * 1024]; // 4k buffer
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to create temp file from URI", e);
            return null;
        }
        return tempFile;
    }


    private void uploadImage(File imageFile, UploadCallback callback) {
        new Thread(() -> {
            String supabaseAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ3aG9zbnJhZ21ua2hrZ2pvZ29xIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjMxMzU0MDgsImV4cCI6MjA3ODcxMTQwOH0.BlYUZDgsVbmGhRFzVK4orcHlutFrdk-bPY0A81V719o";
            OkHttpClient client = new OkHttpClient();
            MediaType mediaType = MediaType.parse("image/jpeg");
            RequestBody fileBody = RequestBody.create(imageFile, mediaType);

            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", imageFile.getName(), fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url(UPLOAD_URL)
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer " + supabaseAnonKey)
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body().string());
                } else {
                    callback.onError("Server error: " + response.code() + " " + response.message());
                }
            } catch (IOException e) {
                callback.onError("Upload failed: " + e.getMessage());
            }
        }).start();
    }

    private void cleanup(File file, Uri uri) {
        if (file != null && file.exists()) {
            if(file.delete()) {
                Log.d(TAG, "Temp file deleted: " + file.getAbsolutePath());
            }
        }
        if (uri != null) {
            requireContext().getContentResolver().delete(uri, null, null);
        }
    }

    public interface UploadCallback {
        void onSuccess(String response);
        void onError(String error);
    }
}