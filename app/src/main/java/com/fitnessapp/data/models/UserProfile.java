package com.fitnessapp.data.models;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * User Profile Document Model
 * Firestore Path: users/{userId}
 */
public class UserProfile implements Serializable {
    private String userId;
    private String email;
    private String displayName;
    private Timestamp createdAt;
    private int targetStepGoal;
    private Timestamp birthdate;
    private String gender;
    private double heightCm;
    private double startingWeightKg;
    private String primaryGoal;
    private List<String> activityPreferences;

    public UserProfile() {
        this.activityPreferences = new ArrayList<>();
        this.targetStepGoal = 10000;
        this.heightCm = 175.0;
        this.startingWeightKg = 72.5;
        this.gender = "Not Specified";
        this.primaryGoal = "Improve Endurance & Health";
        this.createdAt = Timestamp.now();
    }

    public UserProfile(String userId, String email, String displayName) {
        this();
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public int getTargetStepGoal() { return targetStepGoal; }
    public void setTargetStepGoal(int targetStepGoal) { this.targetStepGoal = targetStepGoal; }

    public Timestamp getBirthdate() { return birthdate; }
    public void setBirthdate(Timestamp birthdate) { this.birthdate = birthdate; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public double getHeightCm() { return heightCm; }
    public void setHeightCm(double heightCm) { this.heightCm = heightCm; }

    public double getStartingWeightKg() { return startingWeightKg; }
    public void setStartingWeightKg(double startingWeightKg) { this.startingWeightKg = startingWeightKg; }

    public String getPrimaryGoal() { return primaryGoal; }
    public void setPrimaryGoal(String primaryGoal) { this.primaryGoal = primaryGoal; }

    public List<String> getActivityPreferences() { return activityPreferences; }
    public void setActivityPreferences(List<String> activityPreferences) { this.activityPreferences = activityPreferences; }
}
