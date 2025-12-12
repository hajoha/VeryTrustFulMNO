/* Pfad: app/src/main/java/de/foo/bar/VeryTrustfulMNO/ui/sync/LoginEmailFragment.java */
package de.foo.bar.VeryTrustfulMNO.ui.sync;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.util.Patterns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

// Wichtig: Importieren Sie die Navigations-Actions-Klasse, die Gradle generiert
import de.foo.bar.VeryTrustfulMNO.SettingPreferences.SettingsFragmentDirections;
import de.foo.bar.VeryTrustfulMNO.ui.sync.LoginEmailFragmentDirections;

import de.foo.bar.VeryTrustfulMNO.databinding.FragmentLoginEmailBinding;
import de.foo.bar.VeryTrustfulMNO.R;

public class LoginEmailFragment extends Fragment {

    // Binding-Referenz auf das NEUE Layout
    private FragmentLoginEmailBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflaten des E-Mail-Layouts
        binding = FragmentLoginEmailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Der "Weiter"-Button
        binding.loginButton.setOnClickListener(v -> {
            String input = binding.etEmail.getText().toString().trim();

            if (input.isEmpty()) {
                Toast.makeText(requireContext(), "Field must not be empty", Toast.LENGTH_SHORT).show();

                // NEUE PRÜFUNG: Ist es weder eine E-Mail NOCH eine Telefonnummer?
            } else if (!Patterns.EMAIL_ADDRESS.matcher(input).matches() && !Patterns.PHONE.matcher(input).matches()) {
                // Zeigt einen Fehler an, wenn beides fehlschlägt
                Toast.makeText(requireContext(), "Enter valid E-Mail or Phone Number", Toast.LENGTH_LONG).show();

            } else {
                // Eingabe ist gültig (entweder E-Mail oder Telefon)
                // Navigiere zum Passwort-Fragment und übergib die Eingabe
                LoginEmailFragmentDirections.ActionLoginEmailFragmentToLoginPasswordFragment action;
                action = LoginEmailFragmentDirections.actionLoginEmailFragmentToLoginPasswordFragment(input);

                NavHostFragment.findNavController(this).navigate(action);
            }
        });

        //TODO: Listener für "Konto erstellen" etc. hinzufügen)
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}