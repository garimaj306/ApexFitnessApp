package com.fitnessapp.viewmodels;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.fitnessapp.data.models.Workout;
import com.fitnessapp.data.repository.FitnessRepository;
import com.fitnessapp.services.WorkoutTrackingService;
import com.fitnessapp.utils.PolylineEncoder;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * View Model for Tab 2: Workout Live Tracking Engine.
 */
public class WorkoutViewModel extends AndroidViewModel {
    private final FitnessRepository repository;
    private final Context appContext;
    private long workoutStartTimeMs = 0;

    public WorkoutViewModel(@NonNull Application application) {
        super(application);
        this.appContext = application.getApplicationContext();
        this.repository = FitnessRepository.getInstance(application);
    }

    public LiveData<Boolean> isTracking() { return WorkoutTrackingService.getIsTracking(); }
    public LiveData<Boolean> isPaused() { return WorkoutTrackingService.getIsPaused(); }
    public LiveData<Long> getElapsedSeconds() { return WorkoutTrackingService.getElapsedSeconds(); }
    public LiveData<Double> getDistanceKm() { return WorkoutTrackingService.getDistanceKm(); }
    public LiveData<Double> getCurrentPace() { return WorkoutTrackingService.getCurrentPace(); }
    public LiveData<Integer> getCurrentHeartRate() { return WorkoutTrackingService.getCurrentHeartRate(); }
    public LiveData<Double> getCurrentCalories() { return WorkoutTrackingService.getCurrentCalories(); }
    public LiveData<List<LatLng>> getRoutePoints() { return WorkoutTrackingService.getRoutePoints(); }
    public LiveData<String> getActivityType() { return WorkoutTrackingService.getActivityType(); }

    public void startWorkout(String activityType) {
        workoutStartTimeMs = System.currentTimeMillis();
        Intent intent = new Intent(appContext, WorkoutTrackingService.class);
        intent.setAction(WorkoutTrackingService.ACTION_START);
        intent.putExtra(WorkoutTrackingService.EXTRA_ACTIVITY_TYPE, activityType);
        appContext.startService(intent);
    }

    public void pauseWorkout() {
        Intent intent = new Intent(appContext, WorkoutTrackingService.class);
        intent.setAction(WorkoutTrackingService.ACTION_PAUSE);
        appContext.startService(intent);
    }

    public void resumeWorkout() {
        Intent intent = new Intent(appContext, WorkoutTrackingService.class);
        intent.setAction(WorkoutTrackingService.ACTION_RESUME);
        appContext.startService(intent);
    }

    public Workout finishWorkout() {
        long elapsedSec = getElapsedSeconds().getValue() != null ? getElapsedSeconds().getValue() : 0L;
        double distKm = getDistanceKm().getValue() != null ? getDistanceKm().getValue() : 0.0;
        double calories = getCurrentCalories().getValue() != null ? getCurrentCalories().getValue() : 0.0;
        String actType = getActivityType().getValue() != null ? getActivityType().getValue() : "Run";
        List<LatLng> points = getRoutePoints().getValue();

        // Google Polyline Compression
        String encodedPolyline = PolylineEncoder.encode(points);

        Timestamp start = new Timestamp(new Date(workoutStartTimeMs > 0 ? workoutStartTimeMs : System.currentTimeMillis() - elapsedSec * 1000));
        Timestamp finish = Timestamp.now();

        Workout workout = new Workout(
                UUID.randomUUID().toString(),
                actType,
                start,
                finish,
                elapsedSec,
                distKm,
                calories,
                encodedPolyline
        );

        // Stop foreground service
        Intent intent = new Intent(appContext, WorkoutTrackingService.class);
        intent.setAction(WorkoutTrackingService.ACTION_STOP);
        appContext.startService(intent);

        // Persist to Cloud Firestore & Update Daily Summary
        repository.recordWorkout(workout);

        return workout;
    }
}
