package com.fitnessapp.utils;

import java.util.Calendar;

/**
 * Utility helper for dynamic, time-of-day greeting prefix generation.
 */
public class GreetingHelper {

    /**
     * Determines the greeting prefix based on current device hour:
     * - 05:00 to 11:59 -> "Good morning, "
     * - 12:00 to 16:59 -> "Good afternoon, "
     * - 17:00 to 04:59 -> "Good evening, "
     */
    public static String getGreetingPrefix() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour < 12) {
            return "Good morning, ";
        } else if (hour >= 12 && hour < 17) {
            return "Good afternoon, ";
        } else {
            return "Good evening, ";
        }
    }

    /**
     * Formats full greeting string combining the time-of-day prefix with the athlete's name.
     */
    public static String formatGreeting(String athleteName) {
        String name = (athleteName != null && !athleteName.trim().isEmpty()) ? athleteName.trim() : "Athlete";
        return getGreetingPrefix() + name;
    }
}
