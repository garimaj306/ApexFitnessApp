package com.fitnessapp.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.fitnessapp.data.models.DailySummary;
import com.fitnessapp.data.models.SleepSession;
import com.fitnessapp.data.models.UserProfile;
import com.fitnessapp.data.models.Workout;
import com.fitnessapp.data.repository.FitnessRepository;
import com.fitnessapp.utils.GreetingHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

/**
 * ViewModel for Tab 1 (Today Dashboard).
 * Encapsulates live observation of daily summary, user profile, workouts, and dynamic greeting computation.
 */
public class TodayViewModel extends AndroidViewModel {

    private final FitnessRepository repository;
    private final LiveData<String> greetingLiveData;

    public TodayViewModel(@NonNull Application application) {
        super(application);
        this.repository = FitnessRepository.getInstance(application);

        // Dynamically compute greeting string without UI flicker
        this.greetingLiveData = Transformations.map(repository.getUserProfile(), profile -> {
            String name = "Athlete";
            if (profile != null && profile.getDisplayName() != null && !profile.getDisplayName().trim().isEmpty()) {
                name = profile.getDisplayName().trim();
            } else {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
                    name = user.getDisplayName().trim();
                }
            }
            return GreetingHelper.formatGreeting(name);
        });
    }

    public LiveData<DailySummary> getTodaySummary() {
        return repository.getTodaySummary();
    }

    public LiveData<UserProfile> getUserProfile() {
        return repository.getUserProfile();
    }

    public LiveData<String> getGreetingText() {
        return greetingLiveData;
    }

    public LiveData<List<Workout>> getRecentWorkouts() {
        return repository.getWorkouts();
    }

    public LiveData<List<Workout>> getWorkouts() {
        return repository.getWorkouts();
    }

    public LiveData<SleepSession> getLastNightSleep() {
        return repository.getLastNightSleep();
    }

    public void quickAddWater() {
        repository.quickAddWater();
    }

    public void seedDemoData() {
        repository.seedAllDataToCloud();
    }
}
