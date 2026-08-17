package com.fitnessapp.utils;

/**
 * Calorie estimation engine using Metabolic Equivalent of Task (MET) values.
 */
public class CalorieCalculator {

    public static double calculateCalories(String activityType, double userWeightKg, long durationSeconds) {
        if (durationSeconds <= 0) return 0.0;
        double weight = userWeightKg > 20 ? userWeightKg : 70.0;
        double hours = durationSeconds / 3600.0;
        double met;

        switch (activityType.toLowerCase()) {
            case "run":
                met = 9.8;
                break;
            case "cycle":
                met = 7.5;
                break;
            case "hike":
                met = 6.0;
                break;
            case "walk":
            default:
                met = 3.8;
                break;
        }

        return met * weight * hours;
    }
}
