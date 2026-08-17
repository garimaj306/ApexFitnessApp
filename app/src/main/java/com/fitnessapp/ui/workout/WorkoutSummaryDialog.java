package com.fitnessapp.ui.workout;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.fitnessapp.R;
import com.fitnessapp.data.models.Workout;
import com.fitnessapp.utils.DateTimeUtils;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

/**
 * Post-Workout Celebration & Detailed Metrics Dialog.
 */
public class WorkoutSummaryDialog extends DialogFragment {

    private static final String ARG_WORKOUT = "ARG_WORKOUT";
    private Workout workout;

    public static WorkoutSummaryDialog newInstance(Workout workout) {
        WorkoutSummaryDialog dialog = new WorkoutSummaryDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_WORKOUT, workout);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_FitnessApp);
        if (getArguments() != null) {
            workout = (Workout) getArguments().getSerializable(ARG_WORKOUT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_workout_summary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvActivityType = view.findViewById(R.id.tv_dialog_activity_type);
        TextView tvTime = view.findViewById(R.id.tv_dialog_time);
        TextView tvDistance = view.findViewById(R.id.tv_dialog_distance);
        TextView tvPace = view.findViewById(R.id.tv_dialog_pace);
        TextView tvCalories = view.findViewById(R.id.tv_dialog_calories);
        MaterialButton btnClose = view.findViewById(R.id.btn_dialog_close);

        if (workout != null) {
            tvActivityType.setText(workout.getActivityType() + " Session Saved");
            tvTime.setText(DateTimeUtils.formatDuration(workout.getDurationSeconds()));
            tvDistance.setText(String.format(Locale.US, "%.2f km", workout.getTotalDistanceKm()));
            tvPace.setText(workout.getFormattedPace() + " /km");
            tvCalories.setText(String.format(Locale.US, "%.0f kcal", workout.getCaloriesBurntKcal()));
        }

        btnClose.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
