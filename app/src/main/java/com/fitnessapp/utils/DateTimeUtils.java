package com.fitnessapp.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Date and Time utilities for formatting Firestore date keys and UI timestamps.
 */
public class DateTimeUtils {

    private static final SimpleDateFormat DATE_KEY_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("EEEE, MMM d", Locale.US);
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm a", Locale.US);

    public static String getTodayDateKey() {
        return DATE_KEY_FORMAT.format(new Date());
    }

    public static String formatDateKey(Date date) {
        return DATE_KEY_FORMAT.format(date);
    }

    public static String formatDisplayDate(Date date) {
        return DISPLAY_DATE_FORMAT.format(date);
    }

    public static String formatTime(Date date) {
        return TIME_FORMAT.format(date);
    }

    /**
     * Formats duration in seconds into "HH:MM:SS" or "MM:SS"
     */
    public static String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }
}
