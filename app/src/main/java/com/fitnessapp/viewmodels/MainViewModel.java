package com.fitnessapp.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.fitnessapp.data.models.DailySummary;
import com.fitnessapp.data.models.SleepSession;
import com.fitnessapp.data.models.UserProfile;
import com.fitnessapp.data.models.Workout;
import com.fitnessapp.data.repository.FitnessRepository;

import java.util.List;

/**
 * Shared Main View Model for Dashboard & User State.
 */
public class MainViewModel extends AndroidViewModel {
    private final FitnessRepository repository;

    public MainViewModel(@NonNull Application application) {
        super(application);
        this.repository = FitnessRepository.getInstance(application);
    }

    public LiveData<DailySummary> getTodaySummary() {
        return repository.getTodaySummary();
    }

    public LiveData<UserProfile> getUserProfile() {
        return repository.getUserProfile();
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

    public void updateTargetStepGoal(int goal) {
        repository.updateTargetStepGoal(goal);
    }

    public void seedDemoData() {
        repository.seedAllDataToCloud();
    }
}
