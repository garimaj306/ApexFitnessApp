package com.fitnessapp.data.models;

import com.google.firebase.Timestamp;
import java.io.Serializable;

/**
 * Daily Summary Document Model (Materialized Cache for sub-second dashboard loading)
 * Firestore Path: users/{userId}/daily_summaries/{dateString} (e.g., "2026-08-15")
 */
public class DailySummary implements Serializable {
    private String date; // "YYYY-MM-DD"
    private long stepsCount;
    private double caloriesBurntKcal;
    private double distanceTravelledKm;
    private int activeDurationMinutes;
    private int waterIntakeMl;
    private double lastRecordedWeightKg;
    private int currentStreakDays;
    private Timestamp lastUpdatedAt;

    public DailySummary() {
        this.stepsCount = 0;
        this.caloriesBurntKcal = 0.0;
        this.distanceTravelledKm = 0.0;
        this.activeDurationMinutes = 0;
        this.waterIntakeMl = 0;
        this.lastRecordedWeightKg = 72.0;
        this.currentStreakDays = 1;
        this.lastUpdatedAt = Timestamp.now();
    }

    public DailySummary(String date) {
        this();
        this.date = date;
    }

    // Getters and Setters
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getStepsCount() { return stepsCount; }
    public void setStepsCount(long stepsCount) { this.stepsCount = stepsCount; }

    public double getCaloriesBurntKcal() { return caloriesBurntKcal; }
    public void setCaloriesBurntKcal(double caloriesBurntKcal) { this.caloriesBurntKcal = caloriesBurntKcal; }

    public double getDistanceTravelledKm() { return distanceTravelledKm; }
    public void setDistanceTravelledKm(double distanceTravelledKm) { this.distanceTravelledKm = distanceTravelledKm; }

    public int getActiveDurationMinutes() { return activeDurationMinutes; }
    public void setActiveDurationMinutes(int activeDurationMinutes) { this.activeDurationMinutes = activeDurationMinutes; }

    public int getWaterIntakeMl() { return waterIntakeMl; }
    public void setWaterIntakeMl(int waterIntakeMl) { this.waterIntakeMl = waterIntakeMl; }

    public double getLastRecordedWeightKg() { return lastRecordedWeightKg; }
    public void setLastRecordedWeightKg(double lastRecordedWeightKg) { this.lastRecordedWeightKg = lastRecordedWeightKg; }

    public int getCurrentStreakDays() { return currentStreakDays; }
    public void setCurrentStreakDays(int currentStreakDays) { this.currentStreakDays = currentStreakDays; }

    public Timestamp getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(Timestamp lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
}
