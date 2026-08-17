package com.fitnessapp.data.models;

import com.google.firebase.Timestamp;
import java.io.Serializable;

/**
 * Workout Session Document Model
 * Firestore Path: users/{userId}/workouts/{workoutId}
 */
public class Workout implements Serializable {
    private String workoutId;
    private String activityType; // "Run", "Walk", "Cycle", "Hike"
    private Timestamp startTime;
    private Timestamp finishTime;
    private long durationSeconds;
    private double totalDistanceKm;
    private double caloriesBurntKcal;
    private String polylineRoute; // Encoded Google Polyline string

    public Workout() {
        this.activityType = "Run";
        this.startTime = Timestamp.now();
        this.finishTime = Timestamp.now();
        this.durationSeconds = 0;
        this.totalDistanceKm = 0.0;
        this.caloriesBurntKcal = 0.0;
        this.polylineRoute = "";
    }

    public Workout(String workoutId, String activityType, Timestamp startTime, Timestamp finishTime,
                   long durationSeconds, double totalDistanceKm, double caloriesBurntKcal, String polylineRoute) {
        this.workoutId = workoutId;
        this.activityType = activityType;
        this.startTime = startTime;
        this.finishTime = finishTime;
        this.durationSeconds = durationSeconds;
        this.totalDistanceKm = totalDistanceKm;
        this.caloriesBurntKcal = caloriesBurntKcal;
        this.polylineRoute = polylineRoute;
    }

    // Calculated Helper: Average Pace (minutes per km)
    public double getAveragePaceMinPerKm() {
        if (totalDistanceKm <= 0.001) return 0.0;
        double minutes = durationSeconds / 60.0;
        return minutes / totalDistanceKm;
    }

    public String getFormattedPace() {
        double pace = getAveragePaceMinPerKm();
        if (pace <= 0 || Double.isInfinite(pace) || Double.isNaN(pace)) return "--'--\"";
        int mins = (int) pace;
        int secs = (int) ((pace - mins) * 60);
        return String.format("%d'%02d\"", mins, secs);
    }

    // Getters and Setters
    public String getWorkoutId() { return workoutId; }
    public void setWorkoutId(String workoutId) { this.workoutId = workoutId; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getFinishTime() { return finishTime; }
    public void setFinishTime(Timestamp finishTime) { this.finishTime = finishTime; }

    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }

    public double getTotalDistanceKm() { return totalDistanceKm; }
    public void setTotalDistanceKm(double totalDistanceKm) { this.totalDistanceKm = totalDistanceKm; }

    public double getCaloriesBurntKcal() { return caloriesBurntKcal; }
    public void setCaloriesBurntKcal(double caloriesBurntKcal) { this.caloriesBurntKcal = caloriesBurntKcal; }

    public String getPolylineRoute() { return polylineRoute; }
    public void setPolylineRoute(String polylineRoute) { this.polylineRoute = polylineRoute; }
}
