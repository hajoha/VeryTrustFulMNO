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
                // ------- SERVER-AUFRUF ÜBERSPRINGEN -------
                // sendLoginRequest(userEmail, password); // Auskommentiert

                // Stattdessen: Erfolg simulieren und direkt zurück navigieren
                Toast.makeText(requireContext(), "Login successful", Toast.LENGTH_SHORT).show();

                // Zurück zum Einstellungs-Menü
                NavHostFragment.findNavController(this).popBackStack(R.id.settingsFragment, false);
            }
        });
    }

    // Die gesamte OkHttp-Logik (sendLoginRequest, handleResponse, handleError)
    // wird hierher verschoben und bleibt wie von uns erstellt.

    private void sendLoginRequest(String email, String password) {
        executor.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String json = "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}";
                MediaType JSON = MediaType.get("application/json; charset=utf-8");
                RequestBody requestBody = RequestBody.create(json, JSON);

                Request request = new Request.Builder()
                        .url("https://unser-sicherer-server.de/api/v1/sync-login")
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
                // Nach Erfolg zurück zum Einstellungs-Menü
                NavHostFragment.findNavController(this).popBackStack(R.id.settingsFragment, false);
            } else {
                Toast.makeText(requireContext(), "Fehler: " + response.message(), Toast.LENGTH_SHORT).show();
            }
            response.close();
        });
    }

    private void handleError(Exception e) {
        mainThreadHandler.post(() -> {
            if (!isAdded() || getContext() == null) {
                return;
            }
            Toast.makeText(requireContext(), "Netzwerkfehler: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}