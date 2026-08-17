package com.fitnessapp.data.models;

import com.google.firebase.Timestamp;
import java.io.Serializable;

/**
 * Decoupled Sleep Session Model (Single source of truth for sleep)
 * Firestore Path: users/{userId}/sleep_sessions/{sleepId}
 */
public class SleepSession implements Serializable {
    private String sleepId;
    private Timestamp startTime;
    private Timestamp endTime;
    private int totalDurationMinutes;
    private int remDurationMinutes;
    private int deepDurationMinutes;
    private int lightDurationMinutes;
    private String source; // e.g. "Health Connect", "Wear OS", "Manual"

    public SleepSession() {
        this.startTime = Timestamp.now();
        this.endTime = Timestamp.now();
        this.totalDurationMinutes = 450; // ~7.5 hours
        this.remDurationMinutes = 95;
        this.deepDurationMinutes = 110;
        this.lightDurationMinutes = 245;
        this.source = "Health Connect";
    }

    public SleepSession(String sleepId, Timestamp startTime, Timestamp endTime,
                        int totalDurationMinutes, int remDurationMinutes, int deepDurationMinutes,
                        int lightDurationMinutes, String source) {
        this.sleepId = sleepId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalDurationMinutes = totalDurationMinutes;
        this.remDurationMinutes = remDurationMinutes;
        this.deepDurationMinutes = deepDurationMinutes;
        this.lightDurationMinutes = lightDurationMinutes;
        this.source = source;
    }

    public String getFormattedTotalDuration() {
        int hours = totalDurationMinutes / 60;
        int mins = totalDurationMinutes % 60;
        return String.format("%dh %02dm", hours, mins);
    }

    // Getters and Setters
    public String getSleepId() { return sleepId; }
    public void setSleepId(String sleepId) { this.sleepId = sleepId; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public int getTotalDurationMinutes() { return totalDurationMinutes; }
    public void setTotalDurationMinutes(int totalDurationMinutes) { this.totalDurationMinutes = totalDurationMinutes; }

    public int getRemDurationMinutes() { return remDurationMinutes; }
    public void setRemDurationMinutes(int remDurationMinutes) { this.remDurationMinutes = remDurationMinutes; }

    public int getDeepDurationMinutes() { return deepDurationMinutes; }
    public void setDeepDurationMinutes(int deepDurationMinutes) { this.deepDurationMinutes = deepDurationMinutes; }

    public int getLightDurationMinutes() { return lightDurationMinutes; }
    public void setLightDurationMinutes(int lightDurationMinutes) { this.lightDurationMinutes = lightDurationMinutes; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
