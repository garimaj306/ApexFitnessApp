package com.fitnessapp.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.fitnessapp.data.firebase.FirestoreCollections;
import com.fitnessapp.data.models.DailySummary;
import com.fitnessapp.data.models.SleepSession;
import com.fitnessapp.data.models.UserProfile;
import com.fitnessapp.data.models.VitalRecord;
import com.fitnessapp.data.models.Workout;
import com.fitnessapp.utils.DateTimeUtils;
import com.fitnessapp.utils.PolylineEncoder;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Production-Grade 14-Day Mock Data Seeder using Cloud Firestore WriteBatch.
 * Strictly non-destructive: Populates subcollections without overwriting root user profile identity.
 */
public class DataSeeder {

    private static final String TAG = "DataSeeder";

    public interface SeedCallback {
        void onSuccess();
        void onFailure(@NonNull Exception e);
    }

    /**
     * Seeds 14-day historical summaries, realistic workouts, sleep records, and vitals
     * using atomic Firestore WriteBatches with attached success/failure listeners.
     */
    public static void seedUserData(@NonNull FirebaseFirestore firestore,
                                    @NonNull String userId,
                                    @NonNull UserProfile existingProfile,
                                    SeedCallback callback) {

        if (userId.trim().isEmpty()) {
            Log.e(TAG, "Cannot seed data: Empty userId");
            if (callback != null) callback.onFailure(new IllegalArgumentException("Invalid User ID"));
            return;
        }

        WriteBatch batch = firestore.batch();

        // 1. Non-destructive User Profile Update (Preserves real displayName and email)
        Map<String, Object> safeProfileUpdates = new HashMap<>();
        safeProfileUpdates.put("userId", userId);
        if (existingProfile.getEmail() != null && !existingProfile.getEmail().isEmpty()) {
            safeProfileUpdates.put("email", existingProfile.getEmail());
        }
        if (existingProfile.getDisplayName() != null && !existingProfile.getDisplayName().isEmpty()) {
            safeProfileUpdates.put("displayName", existingProfile.getDisplayName());
        }
        safeProfileUpdates.put("targetStepGoal", existingProfile.getTargetStepGoal() > 0 ? existingProfile.getTargetStepGoal() : 10000);
        safeProfileUpdates.put("heightCm", 178.0);
        safeProfileUpdates.put("startingWeightKg", 72.5);
        safeProfileUpdates.put("primaryGoal", "Half Marathon Training & Vitals Optimization");

        batch.set(
                firestore.collection(FirestoreCollections.USERS).document(userId),
                safeProfileUpdates,
                SetOptions.merge()
        );

        // 2. 14-Day Historical Daily Summaries
        List<DailySummary> summaries = generate14DaySummaries();
        for (DailySummary summary : summaries) {
            batch.set(
                    firestore.collection(FirestoreCollections.USERS)
                            .document(userId)
                            .collection(FirestoreCollections.DAILY_SUMMARIES)
                            .document(summary.getDate()),
                    summary,
                    SetOptions.merge()
            );
        }

        // 3. Realistic Workouts with Polylines
        List<Workout> workouts = generateRealisticWorkouts();
        for (Workout workout : workouts) {
            batch.set(
                    firestore.collection(FirestoreCollections.USERS)
                            .document(userId)
                            .collection(FirestoreCollections.WORKOUTS)
                            .document(workout.getWorkoutId()),
                    workout
            );
        }

        // 4. Sleep Sessions
        List<SleepSession> sleepSessions = generateSleepSessions();
        for (SleepSession sleep : sleepSessions) {
            batch.set(
                    firestore.collection(FirestoreCollections.USERS)
                            .document(userId)
                            .collection(FirestoreCollections.SLEEP_SESSIONS)
                            .document(sleep.getSleepId()),
                    sleep
            );
        }

        // 5. 24h Heart Rate and Hydration Vitals
        List<VitalRecord> vitals = generateVitals();
        for (VitalRecord vital : vitals) {
            batch.set(
                    firestore.collection(FirestoreCollections.USERS)
                            .document(userId)
                            .collection(FirestoreCollections.VITALS)
                            .document(vital.getVitalId()),
                    vital
            );
        }

        // 6. Atomic Batch Commit with Success and Error Handlers
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Successfully seeded 14-day history to Cloud Firestore for user: " + userId);
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "❌ Cloud Firestore batch seed failed: " + e.getMessage(), e);
                    if (callback != null) callback.onFailure(e);
                });
    }

    public static List<DailySummary> generate14DaySummaries() {
        List<DailySummary> list = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        Random random = new Random(42);

        for (int i = 13; i >= 0; i--) {
            Calendar dayCal = (Calendar) cal.clone();
            dayCal.add(Calendar.DAY_OF_YEAR, -i);
            String dateKey = DateTimeUtils.formatDateKey(dayCal.getTime());

            DailySummary summary = new DailySummary(dateKey);
            int baseSteps = 6000 + random.nextInt(6500);
            double baseDistance = (baseSteps * 0.75) / 1000.0;
            double baseCalories = (baseSteps * 0.045) + 300 + random.nextInt(150);

            summary.setStepsCount(baseSteps);
            summary.setDistanceTravelledKm(Math.round(baseDistance * 100.0) / 100.0);
            summary.setCaloriesBurntKcal(Math.round(baseCalories * 10.0) / 10.0);
            summary.setActiveDurationMinutes(35 + random.nextInt(40));
            summary.setWaterIntakeMl(1500 + (random.nextInt(6) * 250));
            summary.setLastRecordedWeightKg(72.5 - (i * 0.05) + (random.nextDouble() * 0.2));
            summary.setCurrentStreakDays(14 - i);
            summary.setLastUpdatedAt(new Timestamp(dayCal.getTime()));
            list.add(summary);
        }
        return list;
    }

    public static List<Workout> generateRealisticWorkouts() {
        List<Workout> workouts = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        // Workout 1: Morning Run
        cal.add(Calendar.DAY_OF_YEAR, 0);
        cal.set(Calendar.HOUR_OF_DAY, 6);
        cal.set(Calendar.MINUTE, 30);
        Date runStart = cal.getTime();
        Date runEnd = new Date(runStart.getTime() + 1920 * 1000); // 32 mins

        List<LatLng> runRoute = new ArrayList<>();
        runRoute.add(new LatLng(37.7749, -122.4194));
        runRoute.add(new LatLng(37.7770, -122.4170));
        runRoute.add(new LatLng(37.7800, -122.4150));
        runRoute.add(new LatLng(37.7820, -122.4180));
        runRoute.add(new LatLng(37.7790, -122.4210));

        workouts.add(new Workout(
                UUID.randomUUID().toString(),
                "Run",
                new Timestamp(runStart),
                new Timestamp(runEnd),
                1920,
                5.4,
                410.0,
                PolylineEncoder.encode(runRoute)
        ));

        // Workout 2: Evening Ride
        cal.add(Calendar.DAY_OF_YEAR, -1);
        cal.set(Calendar.HOUR_OF_DAY, 18);
        Date rideStart = cal.getTime();
        Date rideEnd = new Date(rideStart.getTime() + 2700 * 1000);

        List<LatLng> rideRoute = new ArrayList<>();
        rideRoute.add(new LatLng(37.7850, -122.4080));
        rideRoute.add(new LatLng(37.7900, -122.4020));
        rideRoute.add(new LatLng(37.7950, -122.4050));
        rideRoute.add(new LatLng(37.7910, -122.4120));

        workouts.add(new Workout(
                UUID.randomUUID().toString(),
                "Cycle",
                new Timestamp(rideStart),
                new Timestamp(rideEnd),
                2700,
                14.2,
                530.0,
                PolylineEncoder.encode(rideRoute)
        ));

        return workouts;
    }

    public static List<SleepSession> generateSleepSessions() {
        List<SleepSession> sessions = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        cal.set(Calendar.HOUR_OF_DAY, 22);
        cal.set(Calendar.MINUTE, 45);
        Date start = cal.getTime();

        Calendar endCal = (Calendar) cal.clone();
        endCal.add(Calendar.DAY_OF_YEAR, 1);
        endCal.set(Calendar.HOUR_OF_DAY, 6);
        endCal.set(Calendar.MINUTE, 30);
        Date end = endCal.getTime();

        SleepSession session = new SleepSession(
                UUID.randomUUID().toString(),
                new Timestamp(start),
                new Timestamp(end),
                465, // 7h 45m
                95,
                110,
                260,
                "Health Connect (Synced)"
        );

        sessions.add(session);
        return sessions;
    }

    public static List<VitalRecord> generateVitals() {
        List<VitalRecord> vitals = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        // 24-hour Heart Rate Sparkline Samples
        for (int h = 0; h < 24; h += 2) {
            Calendar hCal = (Calendar) cal.clone();
            hCal.set(Calendar.HOUR_OF_DAY, h);
            int rate = (h >= 0 && h <= 6) ? (52 + (h % 3) * 2) : (68 + (h * 2) % 35);
            vitals.add(new VitalRecord(
                    UUID.randomUUID().toString(),
                    new Timestamp(hCal.getTime()),
                    rate,
                    "Heart_Rate",
                    "BPM",
                    "Continuous Sensor"
            ));
        }

        // Hydration Logs
        vitals.add(new VitalRecord(
                UUID.randomUUID().toString(),
                Timestamp.now(),
                500,
                "Water",
                "ml",
                "Morning Hydration"
        ));

        return vitals;
    }
}
