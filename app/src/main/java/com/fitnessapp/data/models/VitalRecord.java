package com.fitnessapp.data.models;

import com.google.firebase.Timestamp;
import java.io.Serializable;

/**
 * Vital Metric Record Model (Heart Rate 24h trend, Manual Weight, Water logs)
 * Firestore Path: users/{userId}/vitals/{vitalId}
 */
public class VitalRecord implements Serializable {
    private String vitalId;
    private Timestamp recordedAt;
    private double value;
    private String vitalsType; // "Heart_Rate", "Weight", "Water"
    private String unit;       // "BPM", "kg", "ml"
    private String sourceDeviceName;

    public VitalRecord() {
        this.recordedAt = Timestamp.now();
        this.value = 70.0;
        this.vitalsType = "Heart_Rate";
        this.unit = "BPM";
        this.sourceDeviceName = "Wear OS Sensor";
    }

    public VitalRecord(String vitalId, Timestamp recordedAt, double value, String vitalsType, String unit, String sourceDeviceName) {
        this.vitalId = vitalId;
        this.recordedAt = recordedAt;
        this.value = value;
        this.vitalsType = vitalsType;
        this.unit = unit;
        this.sourceDeviceName = sourceDeviceName;
    }

    // Getters and Setters
    public String getVitalId() { return vitalId; }
    public void setVitalId(String vitalId) { this.vitalId = vitalId; }

    public Timestamp getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Timestamp recordedAt) { this.recordedAt = recordedAt; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public String getVitalsType() { return vitalsType; }
    public void setVitalsType(String vitalsType) { this.vitalsType = vitalsType; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getSourceDeviceName() { return sourceDeviceName; }
    public void setSourceDeviceName(String sourceDeviceName) { this.sourceDeviceName = sourceDeviceName; }
}
