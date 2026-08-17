package com.fitnessapp.ui.today;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.fitnessapp.R;
import com.fitnessapp.data.models.DailySummary;
import com.fitnessapp.data.models.SleepSession;
import com.fitnessapp.data.models.UserProfile;
import com.fitnessapp.data.models.Workout;
import com.fitnessapp.ui.views.CircularProgressRingView;
import com.fitnessapp.utils.DateTimeUtils;
import com.fitnessapp.viewmodels.MainViewModel;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Tab 1: Today Dashboard.
 * Subscribes to the pre-aggregated Firestore document in daily_summaries for real-time, sub-second dashboard loading.
 */
public class TodayFragment extends Fragment {

    private com.fitnessapp.viewmodels.TodayViewModel viewModel;

    private TextView tvGreeting;
    private TextView tvUserName;
    private TextView tvDate;
    private TextView tvStreakBadge;

    private CircularProgressRingView heroProgressRing;
    private TextView tvRingDistanceCalories;

    private TextView tvMetricCalories;
    private TextView tvMetricStreak;
    private TextView tvMetricWater;
    private TextView tvMetricWeight;
    private View btnQuickWater;

    private TextView tvRecentActivityTitle;
    private TextView tvRecentActivityDetails;
    private TextView tvRecentActivityTime;
    private ImageView ivRecentActivityIcon;

    private TextView tvSleepDuration;
    private TextView tvSleepScore;
    private TextView tvSleepSchedule;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_today, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(com.fitnessapp.viewmodels.TodayViewModel.class);

        initViews(view);
        setupGreetingAndDate();
        setupObservers();
        setupListeners();
    }

    private void initViews(View view) {
        tvGreeting = view.findViewById(R.id.tv_greeting);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvDate = view.findViewById(R.id.tv_date);
        tvStreakBadge = view.findViewById(R.id.tv_streak_badge);

        heroProgressRing = view.findViewById(R.id.hero_progress_ring);
        tvRingDistanceCalories = view.findViewById(R.id.tv_ring_distance_calories);

        tvMetricCalories = view.findViewById(R.id.tv_metric_calories);
        tvMetricStreak = view.findViewById(R.id.tv_metric_streak);
        tvMetricWater = view.findViewById(R.id.tv_metric_water);
        tvMetricWeight = view.findViewById(R.id.tv_metric_weight);
        btnQuickWater = view.findViewById(R.id.btn_quick_water);

        tvRecentActivityTitle = view.findViewById(R.id.tv_recent_activity_title);
        tvRecentActivityDetails = view.findViewById(R.id.tv_recent_activity_details);
        tvRecentActivityTime = view.findViewById(R.id.tv_recent_activity_time);
        ivRecentActivityIcon = view.findViewById(R.id.iv_recent_activity_icon);

        tvSleepDuration = view.findViewById(R.id.tv_sleep_duration);
        tvSleepScore = view.findViewById(R.id.tv_sleep_score);
        tvSleepSchedule = view.findViewById(R.id.tv_sleep_schedule);
    }

    private void setupGreetingAndDate() {
        tvGreeting.setText(com.fitnessapp.utils.GreetingHelper.getGreetingPrefix());
        tvDate.setText(DateTimeUtils.formatDisplayDate(new Date()));
    }

    private void setupObservers() {
        // 1. Observe User Profile (Permanent persistent athlete name)
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            String name = "Athlete";
            if (profile != null && profile.getDisplayName() != null && !profile.getDisplayName().trim().isEmpty()) {
                name = profile.getDisplayName().trim();
            } else if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
                String authName = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
                if (authName != null && !authName.trim().isEmpty()) {
                    name = authName.trim();
                }
            }
            tvUserName.setText(name);
            tvGreeting.setText(com.fitnessapp.utils.GreetingHelper.getGreetingPrefix());
        });

        // 2. Observe Realtime Daily Summary (Materialized Cache)
        viewModel.getTodaySummary().observe(getViewLifecycleOwner(), summary -> {
            if (summary != null) {
                bindDailySummary(summary);
            }
        });

        // 3. Observe Workouts (Recent Activity Card)
        viewModel.getWorkouts().observe(getViewLifecycleOwner(), workouts -> {
            if (workouts != null && !workouts.isEmpty()) {
                bindRecentActivity(workouts.get(0));
            } else {
                tvRecentActivityTitle.setText("No Activities Yet");
                tvRecentActivityDetails.setText("Tap Workout to record your first run or walk!");
                tvRecentActivityTime.setText("Ready to track");
            }
        });

        // 4. Observe Sleep Session
        viewModel.getLastNightSleep().observe(getViewLifecycleOwner(), sleep -> {
            if (sleep != null) {
                bindSleepSession(sleep);
            } else {
                tvSleepDuration.setText("--h --m");
                tvSleepScore.setText("No Data");
                tvSleepSchedule.setText("Wearable sync or manual entry pending");
            }
        });
    }

    private void bindDailySummary(DailySummary summary) {
        UserProfile profile = viewModel.getUserProfile().getValue();
        int goal = profile != null ? profile.getTargetStepGoal() : 10000;

        // Hero Ring
        heroProgressRing.setProgress(summary.getStepsCount(), goal, true);
        tvRingDistanceCalories.setText(String.format(Locale.US, "%.1f km • %.0f kcal",
                summary.getDistanceTravelledKm(), summary.getCaloriesBurntKcal()));

        // 2x2 Secondary Grid
        tvMetricCalories.setText(String.format(Locale.US, "%.0f kcal", summary.getCaloriesBurntKcal()));
        tvMetricStreak.setText(String.format(Locale.US, "%d Days", summary.getCurrentStreakDays()));
        tvStreakBadge.setText(String.format(Locale.US, "%d Days", summary.getCurrentStreakDays()));
        tvMetricWater.setText(NumberFormat.getNumberInstance(Locale.US).format(summary.getWaterIntakeMl()) + " ml");
        tvMetricWeight.setText(String.format(Locale.US, "%.1f kg", summary.getLastRecordedWeightKg()));
    }

    private void bindRecentActivity(Workout workout) {
        tvRecentActivityTitle.setText(workout.getActivityType() + " Session");
        tvRecentActivityDetails.setText(String.format(Locale.US, "%.1f km • %s • %s",
                workout.getTotalDistanceKm(),
                DateTimeUtils.formatDuration(workout.getDurationSeconds()),
                workout.getFormattedPace() + "/km"));

        if (workout.getStartTime() != null) {
            tvRecentActivityTime.setText(DateTimeUtils.formatTime(workout.getStartTime().toDate()));
        }

        switch (workout.getActivityType().toLowerCase()) {
            case "cycle":
                ivRecentActivityIcon.setImageResource(R.drawable.ic_tab_workout);
                break;
            case "walk":
            case "hike":
            case "run":
            default:
                ivRecentActivityIcon.setImageResource(R.drawable.ic_tab_workout);
                break;
        }
    }

    private void bindSleepSession(SleepSession sleep) {
        tvSleepDuration.setText(sleep.getFormattedTotalDuration());
        tvSleepScore.setText("Quality: Optimal");
        tvSleepSchedule.setText(sleep.getSource() != null ? sleep.getSource() : "Synced from Health Connect");
    }

    private void setupListeners() {
        btnQuickWater.setOnClickListener(v -> {
            viewModel.quickAddWater();
            Toast.makeText(getContext(), "+250ml Water Logged & Synced!", Toast.LENGTH_SHORT).show();
        });
    }
}
