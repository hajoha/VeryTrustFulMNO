package de.foo.bar.VeryTrustfulMNO;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;

import de.foo.bar.VeryTrustfulMNO.Preferences.SPType;
import de.foo.bar.VeryTrustfulMNO.Preferences.SharedPreferencesGrouper;

public class WifiCallFragment extends Fragment {

    private static final String TAG = "WifiCallFragment";

    // Root permission list container (we update this)
    private LinearLayout permissionsListContainer;
    private SharedPreferencesGrouper spg;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                if (fineGranted != null && fineGranted) {
                    onPermissionGranted();
                } else {
                    Log.d(TAG, "Permission denied");
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NavController navController = NavHostFragment.findNavController(this);
        requireActivity().getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        navController.navigate(R.id.HomeFragment);
                    }
                }
        );
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_wifi_call, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        LinearLayout root = view.findViewById(R.id.wifi_call_layout);
        spg = SharedPreferencesGrouper.getInstance(requireContext());
        // Persistent container used for dynamic updates
        permissionsListContainer = new LinearLayout(getContext());
        permissionsListContainer.setOrientation(LinearLayout.VERTICAL);
        permissionsListContainer.setVisibility(View.GONE);
        root.addView(buildStatusTextView());
        root.addView(buildRequestButton());

        root.addView(permissionsListContainer);
        // Initial UI state
        updatePermissionsList();
    }

    // ------------------------------------------------------------------------
    // Permission Handling
    // ------------------------------------------------------------------------

    private boolean isGranted(String permission) {
        return ContextCompat.checkSelfPermission(
                requireContext(), permission
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        boolean fineMissing = !isGranted(Manifest.permission.ACCESS_FINE_LOCATION);
        Log.d(TAG, "fineLocationMissing=" + fineMissing);

        if (fineMissing) {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            });
        } else {
            // Permission already granted, safe to continue
            onPermissionGranted();
        }
    }

    private void onPermissionGranted() {
        Log.d(TAG, "requestPermission: enabling logging settings for WiFi Calling");
        new AlertDialog.Builder(getContext())
                .setTitle("WiFi Calling Enabled!")
                        .setMessage("Thanks for enabling WiFi Calling, now you can get called in the unlikely case with no reception of the mobile network!")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss()).show();
        spg.getSharedPreference(SPType.logging_sp).edit()
                .putBoolean("enable_logging", true)
                .putBoolean("enable_influx", true)
                .putString("influx_URL", "https://influxv2.johann-hackler.com")
                .putString("measurement_name", "omnt")
                .putString("influx_token", "zj2sEEAfov-zLLh9HUsQQQPxjZviPeuQbvmZRDoZ5nNN3UF-CZvUlR45e8u2pNsIwXUiAWewNswAHySAUV8RXQ==")
                .putString("influx_org", "home")
                .putString("influx_bucket", "mobile_sec")
                .putBoolean("log_wifi_data", true)
                .apply();

        updatePermissionsList();
    }


    private void updatePermissionsList() {
        // Clear old UI
        permissionsListContainer.removeAllViews();

        // Re-add rows with updated values
        permissionsListContainer.addView(buildPermissionRow(
                "Access Fine Location Permission: ",
                isGranted(Manifest.permission.ACCESS_FINE_LOCATION)
        ));

        // Keep this in UI if you want to *display* it — but note it's not runtime
        permissionsListContainer.addView(buildPermissionRow(
                "Change WiFi State Permission (install-time): ",
                isGranted(Manifest.permission.CHANGE_WIFI_STATE)
        ));
    }

    private View buildPermissionRow(String label, boolean granted) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView labelView = new TextView(getContext());
        labelView.setText(label);

        TextView statusView = new TextView(getContext());
        statusView.setText(granted ? "GRANTED" : "NOT GRANTED");

        row.addView(labelView);
        row.addView(statusView);
        return row;
    }

    private TextView buildStatusTextView() {
        TextView status = new TextView(getContext());
        status.setText("To enable WiFi Calling, we need specific permissions. Please grant them.");
        return status;
    }

    private MaterialButton buildRequestButton() {
        MaterialButton button = new MaterialButton(getContext());
        button.setText("Enable WiFi Calling");
        button.setOnClickListener(v -> requestPermission());
        button.setOnLongClickListener(v -> {
            Log.d(TAG, "Request button long-pressed: refreshing UI.");
            updatePermissionsList();
            boolean isGone = permissionsListContainer.getVisibility() == View.GONE;
            permissionsListContainer.setVisibility(isGone ? View.VISIBLE : View.GONE);
            return true;
        });
        return button;
    }
}
