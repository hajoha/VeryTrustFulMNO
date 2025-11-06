/*
 *  SPDX-FileCopyrightText: 2023 Peter Hasse <peter.hasse@fokus.fraunhofer.de>
 *  SPDX-FileCopyrightText: 2023 Johann Hackler <johann.hackler@fokus.fraunhofer.de>
 *  SPDX-FileCopyrightText: 2023 Fraunhofer FOKUS
 *
 *  SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package de.foo.bar.VeryTrustfulMNO;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

public class CallFragment extends Fragment {
    public CallFragment() {
        super(R.layout.fragment_call);
    }

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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView greenButton = view.findViewById(R.id.green_call_btn);
        ImageView redButton = view.findViewById(R.id.red_call_btn);
        TextView callText = view.findViewById(R.id.calling_text);

        greenButton.setOnClickListener(v -> {
            greenButton.setVisibility(View.INVISIBLE);
            redButton.setVisibility(View.VISIBLE);
            callText.setVisibility(View.VISIBLE);
        });

        redButton.setOnClickListener(v -> {
            redButton.setVisibility(View.INVISIBLE);
            callText.setVisibility(View.INVISIBLE);
            greenButton.setVisibility(View.VISIBLE);
        });


    }
}