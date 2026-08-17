package com.fitnessapp.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.fitnessapp.R;
import com.fitnessapp.data.models.UserProfile;
import com.fitnessapp.viewmodels.MainViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Tab 5: Profile & Settings.
 * Displays User Athlete Profile, Physical Baseline Attributes,
 * Interactive Daily Step Goal Slider, and 1-Click Cloud Data Seeder.
 */
public class ProfileFragment extends Fragment {

    private MainViewModel viewModel;

    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvStepGoalValue;
    private Slider sliderStepGoal;

    private TextView tvProfileHeight;
    private TextView tvProfileWeight;
    private TextView tvProfileGoal;

    private MaterialButton btnSeedData;
    private MaterialButton btnSignOut;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        initViews(view);
        setupObservers();
        setupListeners();
    }

    private void initViews(View view) {
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfileEmail = view.findViewById(R.id.tv_profile_email);
        tvStepGoalValue = view.findViewById(R.id.tv_step_goal_value);
        sliderStepGoal = view.findViewById(R.id.slider_step_goal);

        tvProfileHeight = view.findViewById(R.id.tv_profile_height);
        tvProfileWeight = view.findViewById(R.id.tv_profile_weight);
        tvProfileGoal = view.findViewById(R.id.tv_profile_goal);

        btnSeedData = view.findViewById(R.id.btn_seed_data);
        btnSignOut = view.findViewById(R.id.btn_sign_out);
    }

    private void setupObservers() {
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                bindProfile(profile);
            }
        });
    }

    private void bindProfile(UserProfile profile) {
        if (profile.getDisplayName() != null) tvProfileName.setText(profile.getDisplayName());
        if (profile.getEmail() != null) tvProfileEmail.setText(profile.getEmail());

        int goal = profile.getTargetStepGoal() > 0 ? profile.getTargetStepGoal() : 10000;
        tvStepGoalValue.setText(NumberFormat.getNumberInstance(Locale.US).format(goal));
        sliderStepGoal.setValue(goal);

        tvProfileHeight.setText(String.format(Locale.US, "%.0f cm", profile.getHeightCm()));
        tvProfileWeight.setText(String.format(Locale.US, "%.1f kg", profile.getStartingWeightKg()));
        if (profile.getPrimaryGoal() != null) tvProfileGoal.setText(profile.getPrimaryGoal());
    }

    private void setupListeners() {
        sliderStepGoal.addOnChangeListener((slider, value, fromUser) -> {
            int newGoal = (int) value;
            tvStepGoalValue.setText(NumberFormat.getNumberInstance(Locale.US).format(newGoal));
            if (fromUser) {
                viewModel.updateTargetStepGoal(newGoal);
            }
        });

        btnSeedData.setOnClickListener(v -> {
            viewModel.seedDemoData();
            Toast.makeText(getContext(), "✓ 14-Day History, Routes & Sleep Seeded to Cloud Firestore!", Toast.LENGTH_LONG).show();
        });

        btnSignOut.setOnClickListener(v -> {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
            android.content.Intent authIntent = new android.content.Intent(requireActivity(), com.fitnessapp.ui.AuthActivity.class);
            authIntent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(authIntent);
            requireActivity().finish();
        });
    }
}
