/* Pfad: app/src/main/java/de/foo/bar/VeryTrustfulMNO/ui/sync/LoginPasswordFragment.java */
package de.foo.bar.VeryTrustfulMNO.ui.sync;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.foo.bar.VeryTrustfulMNO.R;
import de.foo.bar.VeryTrustfulMNO.databinding.FragmentLoginPasswordBinding;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginPasswordFragment extends Fragment {

    private FragmentLoginPasswordBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private String userEmail; // Variable zum Speichern der E-Mail

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginPasswordBinding.inflate(inflater, container, false);

        // E-Mail aus den Navigations-Argumenten holen
        if (getArguments() != null) {
            userEmail = LoginPasswordFragmentArgs.fromBundle(getArguments()).getEmail();
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // E-Mail im TextView anzeigen
        binding.tvUserEmailDisplay.setText(userEmail);

        // Der "Anmelden"-Button
        binding.loginButton.setOnClickListener(v -> {

            String password = binding.etPassword.getText().toString();

            if (password.isEmpty()) {
                Toast.makeText(requireContext(), "Passwort darf nicht leer sein", Toast.LENGTH_SHORT).show();
            } else {
                // 1. Simulations-Code ist weg
                // 2. Echter Server-Aufruf ist aktiv:
                sendLoginRequest(userEmail, password);
            }
        });
    }

    private void sendLoginRequest(String email, String password) {
        executor.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                // JSON erstellen
                String json = "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}";
                MediaType JSON = MediaType.get("application/json; charset=utf-8");
                RequestBody requestBody = RequestBody.create(json, JSON);

                // WICHTIG: Hier steht deine WLAN-IP.
                // Falls du wieder den Emulator auf dem PC nutzt, nimm: http://10.0.2.2:5000/api/v1/sync-login
                Request request = new Request.Builder()
                        .url("http://192.168.178.126:5000/api/v1/sync-login")
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();
                handleResponse(response);
            } catch (IOException e) {
                e.printStackTrace();
                handleError(e);
            }
        });
    }

    private void handleResponse(Response response) {
        mainThreadHandler.post(() -> {
            if (!isAdded() || getContext() == null) {
                return;
            }
            if (response.isSuccessful()) {
                Toast.makeText(requireContext(), "Sync erfolgreich aktiviert!", Toast.LENGTH_LONG).show();
                // Erst JETZT, nach Erfolg, springen wir zurück
                NavHostFragment.findNavController(this).popBackStack(R.id.settingsFragment, false);
            } else {
                Toast.makeText(requireContext(), "Server-Fehler: " + response.message(), Toast.LENGTH_SHORT).show();
            }
            response.close();
        });
    }

    private void handleError(Exception e) {
        mainThreadHandler.post(() -> {
            if (!isAdded() || getContext() == null) {
                return;
            }
            // Fehler anzeigen (z.B. wenn der Server aus ist)
            Toast.makeText(requireContext(), "Verbindungsfehler: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}