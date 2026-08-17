package com.fitnessapp.data.models;

import com.google.firebase.Timestamp;
import java.io.Serializable;

/**
 * High-Resolution GPS Coordinate Point Model
 * Firestore Path: users/{userId}/workouts/{workoutId}/gps_points/{pointId}
 */
public class GpsPoint implements Serializable {
    private String pointId;
    private Timestamp timestamp;
    private double latitude;
    private double longitude;
    private double currentSpeedMps;
    private double elevationM;

    public GpsPoint() {
        this.timestamp = Timestamp.now();
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.currentSpeedMps = 0.0;
        this.elevationM = 0.0;
    }

    public GpsPoint(double latitude, double longitude, double currentSpeedMps, double elevationM) {
        this.timestamp = Timestamp.now();
        this.latitude = latitude;
        this.longitude = longitude;
        this.currentSpeedMps = currentSpeedMps;
        this.elevationM = elevationM;
    }

    // Getters and Setters
    public String getPointId() { return pointId; }
    public void setPointId(String pointId) { this.pointId = pointId; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getCurrentSpeedMps() { return currentSpeedMps; }
    public void setCurrentSpeedMps(double currentSpeedMps) { this.currentSpeedMps = currentSpeedMps; }

    public double getElevationM() { return elevationM; }
    public void setElevationM(double elevationM) { this.elevationM = elevationM; }
}
