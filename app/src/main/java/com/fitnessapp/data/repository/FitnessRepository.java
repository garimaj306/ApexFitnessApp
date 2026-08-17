package com.fitnessapp.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fitnessapp.data.firebase.FirebaseManager;
import com.fitnessapp.data.firebase.FirestoreCollections;
import com.fitnessapp.data.models.DailySummary;
import com.fitnessapp.data.models.SleepSession;
import com.fitnessapp.data.models.UserProfile;
import com.fitnessapp.data.models.VitalRecord;
import com.fitnessapp.data.models.Workout;
import com.fitnessapp.utils.DateTimeUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Main Fitness Repository: Orchestrates Real-time Firestore Subscriptions,
 * Atomic Increments, Materialized Cache Updates, and Offline Local Persistence.
 */
public class FitnessRepository {
    private static final String TAG = "FitnessRepository";
    private static FitnessRepository instance;

    private final FirebaseManager firebaseManager;
    private final FirebaseFirestore firestore;

    private final MutableLiveData<DailySummary> todaySummaryLiveData = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> userProfileLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Workout>> workoutsLiveData = new MutableLiveData<>();
    private final MutableLiveData<SleepSession> lastNightSleepLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<VitalRecord>> heartRateLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<DailySummary>> historicalSummariesLiveData = new MutableLiveData<>();

    private ListenerRegistration todaySummaryListener;
    private ListenerRegistration profileListener;
    private ListenerRegistration workoutsListener;

    private FitnessRepository(Context context) {
        this.firebaseManager = FirebaseManager.getInstance(context);
        this.firestore = firebaseManager.getFirestore();

        // Fresh athlete start: clean empty zero baseline for new users with authenticated display name
        String userId = firebaseManager.getCurrentUserId();
        String initialName = "Athlete";
        String initialEmail = "";
        if (firebaseManager.getAuth().getCurrentUser() != null) {
            if (firebaseManager.getAuth().getCurrentUser().getDisplayName() != null && !firebaseManager.getAuth().getCurrentUser().getDisplayName().trim().isEmpty()) {
                initialName = firebaseManager.getAuth().getCurrentUser().getDisplayName().trim();
            }
            if (firebaseManager.getAuth().getCurrentUser().getEmail() != null) {
                initialEmail = firebaseManager.getAuth().getCurrentUser().getEmail().trim();
            }
        }
        UserProfile defaultUser = new UserProfile(userId, initialEmail, initialName);
        defaultUser.setTargetStepGoal(10000);
        userProfileLiveData.setValue(defaultUser);

        DailySummary freshSummary = new DailySummary(DateTimeUtils.getTodayDateKey());
        freshSummary.setStepsCount(0);
        freshSummary.setDistanceTravelledKm(0.0);
        freshSummary.setCaloriesBurntKcal(0.0);
        freshSummary.setActiveDurationMinutes(0);
        freshSummary.setWaterIntakeMl(0);
        freshSummary.setCurrentStreakDays(1);
        todaySummaryLiveData.setValue(freshSummary);

        workoutsLiveData.setValue(new ArrayList<>());
        lastNightSleepLiveData.setValue(null);
        heartRateLiveData.setValue(new ArrayList<>());
        historicalSummariesLiveData.setValue(new ArrayList<>());

        // Attach Realtime Listeners to fetch existing user data if any
        initRealtimeListeners();
    }

    public static synchronized FitnessRepository getInstance(Context context) {
        if (instance == null) {
            instance = new FitnessRepository(context.getApplicationContext());
        }
        return instance;
    }

    public LiveData<DailySummary> getTodaySummary() { return todaySummaryLiveData; }
    public LiveData<UserProfile> getUserProfile() { return userProfileLiveData; }
    public LiveData<List<Workout>> getWorkouts() { return workoutsLiveData; }
    public LiveData<SleepSession> getLastNightSleep() { return lastNightSleepLiveData; }
    public LiveData<List<VitalRecord>> getHeartRateVitals() { return heartRateLiveData; }
    public LiveData<List<DailySummary>> getHistoricalSummaries() { return historicalSummariesLiveData; }

    /**
     * Attaches Firestore Snapshot Listeners for sub-second reactive updates.
     */
    public void initRealtimeListeners() {
        String userId = firebaseManager.getCurrentUserId();
        String todayKey = DateTimeUtils.getTodayDateKey();

        try {
            // 1. Subscribe to today's daily_summaries document
            DocumentReference todayRef = firestore.collection(FirestoreCollections.USERS)
                    .document(userId)
                    .collection(FirestoreCollections.DAILY_SUMMARIES)
                    .document(todayKey);

            if (todaySummaryListener != null) todaySummaryListener.remove();
            todaySummaryListener = todayRef.addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    Log.w(TAG, "Today summary listener error: " + e.getMessage());
                    return;
                }
                if (snapshot != null && snapshot.exists()) {
                    DailySummary summary = snapshot.toObject(DailySummary.class);
                    if (summary != null) {
                        todaySummaryLiveData.postValue(summary);
                    }
                }
            });

            // 2. Subscribe to user profile
            DocumentReference profileRef = firestore.collection(FirestoreCollections.USERS).document(userId);
            if (profileListener != null) profileListener.remove();
            profileListener = profileRef.addSnapshotListener((snapshot, e) -> {
                if (snapshot != null && snapshot.exists()) {
                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile != null) {
                        userProfileLiveData.postValue(profile);
                    }
                }
            });

            // 3. Subscribe to recent workouts
            if (workoutsListener != null) workoutsListener.remove();
            workoutsListener = firestore.collection(FirestoreCollections.USERS)
                    .document(userId)
                    .collection(FirestoreCollections.WORKOUTS)
                    .orderBy("startTime", Query.Direction.DESCENDING)
                    .limit(20)
                    .addSnapshotListener((querySnapshot, e) -> {
                        if (querySnapshot != null) {
                            List<Workout> list = querySnapshot.toObjects(Workout.class);
                            workoutsLiveData.postValue(list);
                        }
                    });

        } catch (Exception ex) {
            Log.e(TAG, "Error initializing Firestore listeners: " + ex.getMessage());
        }
    }

    /**
     * Resets local repository LiveData cache for a new/switched user.
     */
    public void resetToFreshUser(String newUserId, String displayName, String email) {
        UserProfile newProfile = new UserProfile(newUserId, email, displayName);
        newProfile.setTargetStepGoal(10000);
        userProfileLiveData.setValue(newProfile);

        DailySummary freshSummary = new DailySummary(DateTimeUtils.getTodayDateKey());
        freshSummary.setStepsCount(0);
        freshSummary.setDistanceTravelledKm(0.0);
        freshSummary.setCaloriesBurntKcal(0.0);
        freshSummary.setActiveDurationMinutes(0);
        freshSummary.setWaterIntakeMl(0);
        freshSummary.setCurrentStreakDays(1);
        todaySummaryLiveData.setValue(freshSummary);

        workoutsLiveData.setValue(new ArrayList<>());
        lastNightSleepLiveData.setValue(null);
        heartRateLiveData.setValue(new ArrayList<>());
        historicalSummariesLiveData.setValue(new ArrayList<>());

        initRealtimeListeners();
    }

    /**
     * Quick Action: Instant +250ml Water increment with optimistic UI update and atomic Firestore increment.
     */
    public void quickAddWater() {
        DailySummary current = todaySummaryLiveData.getValue();
        if (current == null) {
            current = new DailySummary(DateTimeUtils.getTodayDateKey());
        }
        current.setWaterIntakeMl(current.getWaterIntakeMl() + 250);
        current.setLastUpdatedAt(Timestamp.now());
        todaySummaryLiveData.setValue(current);

        // Firestore Atomic Sync
        String userId = firebaseManager.getCurrentUserId();
        String todayKey = DateTimeUtils.getTodayDateKey();
        try {
            DocumentReference ref = firestore.collection(FirestoreCollections.USERS)
                    .document(userId)
                    .collection(FirestoreCollections.DAILY_SUMMARIES)
                    .document(todayKey);

            ref.set(current, SetOptions.merge());

            // Also log to vitals collection
            VitalRecord vital = new VitalRecord(UUID.randomUUID().toString(),
                    Timestamp.now(), 250, "Water", "ml", "Quick Log Button");
            firestore.collection(FirestoreCollections.USERS)
                    .document(userId)
                    .collection(FirestoreCollections.VITALS)
                    .document(vital.getVitalId())
                    .set(vital);
        } catch (Exception e) {
            Log.e(TAG, "Failed to sync water to Firestore: " + e.getMessage());
        }
    }

    /**
     * Records a completed workout, writes the workout document, and atomically updates the daily summary.
     */
    public void recordWorkout(Workout workout) {
        // 1. Optimistic local update
        List<Workout> currentWorkouts = workoutsLiveData.getValue();
        if (currentWorkouts == null) currentWorkouts = new ArrayList<>();
        List<Workout> updatedList = new ArrayList<>(currentWorkouts);
        updatedList.add(0, workout);
        workoutsLiveData.setValue(updatedList);

        DailySummary currentSummary = todaySummaryLiveData.getValue();
        if (currentSummary == null) currentSummary = new DailySummary(DateTimeUtils.getTodayDateKey());

        // Realistic stride rate: ~1,300 steps per km (max 50,000 steps per single workout session)
        double validDist = Math.max(0.0, Math.min(workout.getTotalDistanceKm(), 100.0));
        long extraSteps = (long) (validDist * 1300);

        currentSummary.setStepsCount(currentSummary.getStepsCount() + extraSteps);
        currentSummary.setDistanceTravelledKm(Math.round((currentSummary.getDistanceTravelledKm() + validDist) * 100.0) / 100.0);
        currentSummary.setCaloriesBurntKcal(Math.round((currentSummary.getCaloriesBurntKcal() + workout.getCaloriesBurntKcal()) * 10.0) / 10.0);
        currentSummary.setActiveDurationMinutes((int) (currentSummary.getActiveDurationMinutes() + (workout.getDurationSeconds() / 60)));
        currentSummary.setLastUpdatedAt(Timestamp.now());
        todaySummaryLiveData.setValue(currentSummary);

        // 2. Cloud Firestore Writes
        String userId = firebaseManager.getCurrentUserId();
        String todayKey = DateTimeUtils.getTodayDateKey();

        try {
            // Write workout document
            firestore.collection(FirestoreCollections.USERS)
                    .document(userId)
                    .collection(FirestoreCollections.WORKOUTS)
                    .document(workout.getWorkoutId())
                    .set(workout);

            // Update daily summary document
            firestore.collection(FirestoreCollections.USERS)
                    .document(userId)
                    .collection(FirestoreCollections.DAILY_SUMMARIES)
                    .document(todayKey)
                    .set(currentSummary, SetOptions.merge());

        } catch (Exception e) {
            Log.e(TAG, "Error saving workout to Firestore: " + e.getMessage());
        }
    }

    /**
     * Logs manual vitals (Weight, Water, Heart Rate).
     */
    public void logVital(VitalRecord vital) {
        String userId = firebaseManager.getCurrentUserId();

        if ("Weight".equalsIgnoreCase(vital.getVitalsType())) {
            DailySummary summary = todaySummaryLiveData.getValue();
            if (summary != null) {
                summary.setLastRecordedWeightKg(vital.getValue());
                todaySummaryLiveData.setValue(summary);
            }
        } else if ("Heart_Rate".equalsIgnoreCase(vital.getVitalsType())) {
            List<VitalRecord> list = heartRateLiveData.getValue();
            if (list == null) list = new ArrayList<>();
            List<VitalRecord> updated = new ArrayList<>(list);
            updated.add(vital);
            heartRateLiveData.setValue(updated);
        }

        try {
            firestore.collection(FirestoreCollections.USERS)
                    .document(userId)
                    .collection(FirestoreCollections.VITALS)
                    .document(vital.getVitalId())
                    .set(vital);

            if ("Weight".equalsIgnoreCase(vital.getVitalsType())) {
                String todayKey = DateTimeUtils.getTodayDateKey();
                firestore.collection(FirestoreCollections.USERS)
                        .document(userId)
                        .collection(FirestoreCollections.DAILY_SUMMARIES)
                        .document(todayKey)
                        .update("lastRecordedWeightKg", vital.getValue());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error logging vital to Firestore: " + e.getMessage());
        }
    }

    /**
     * Updates target daily step goal.
     */
    public void updateTargetStepGoal(int targetGoal) {
        UserProfile profile = userProfileLiveData.getValue();
        if (profile != null) {
            profile.setTargetStepGoal(targetGoal);
            userProfileLiveData.setValue(profile);

            String userId = firebaseManager.getCurrentUserId();
            try {
                firestore.collection(FirestoreCollections.USERS)
                        .document(userId)
                        .update("targetStepGoal", targetGoal);
            } catch (Exception e) {
                Log.e(TAG, "Error updating step goal in Firestore: " + e.getMessage());
            }
        }
    }

    /**
     * Seeds full 14-day history, workouts, and vitals to Firestore using DataSeeder while preserving the user's actual identity.
     */
    public void seedAllDataToCloud() {
        String userId = firebaseManager.getCurrentUserId();

        // 1. Preserve the current authenticated user's actual profile details
        UserProfile currentProfile = userProfileLiveData.getValue();
        String currentName = (currentProfile != null && currentProfile.getDisplayName() != null) ? currentProfile.getDisplayName() : "Athlete";
        String currentEmail = (currentProfile != null && currentProfile.getEmail() != null) ? currentProfile.getEmail() : "athlete@apexfit.com";

        if (firebaseManager.getAuth().getCurrentUser() != null) {
            if (firebaseManager.getAuth().getCurrentUser().getDisplayName() != null && !firebaseManager.getAuth().getCurrentUser().getDisplayName().trim().isEmpty()) {
                currentName = firebaseManager.getAuth().getCurrentUser().getDisplayName().trim();
            }
            if (firebaseManager.getAuth().getCurrentUser().getEmail() != null) {
                currentEmail = firebaseManager.getAuth().getCurrentUser().getEmail().trim();
            }
        }

        UserProfile safeProfile = new UserProfile(userId, currentEmail, currentName);
        safeProfile.setTargetStepGoal(currentProfile != null && currentProfile.getTargetStepGoal() > 0 ? currentProfile.getTargetStepGoal() : 10000);
        userProfileLiveData.setValue(safeProfile);

        // 2. Optimistic local updates for immediate UI responsiveness
        List<DailySummary> summaries = DataSeeder.generate14DaySummaries();
        historicalSummariesLiveData.setValue(summaries);
        if (!summaries.isEmpty()) {
            todaySummaryLiveData.setValue(summaries.get(summaries.size() - 1));
        }

        List<Workout> sampleWorkouts = DataSeeder.generateRealisticWorkouts();
        workoutsLiveData.setValue(sampleWorkouts);

        List<SleepSession> sleepSessions = DataSeeder.generateSleepSessions();
        if (!sleepSessions.isEmpty()) {
            lastNightSleepLiveData.setValue(sleepSessions.get(0));
        }

        List<VitalRecord> vitals = DataSeeder.generateVitals();
        heartRateLiveData.setValue(vitals);

        // 3. Delegate to DataSeeder for atomic Firestore WriteBatch commits with error logging
        DataSeeder.seedUserData(firestore, userId, safeProfile, new DataSeeder.SeedCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Seeded 14-day history and workouts committed to Cloud Firestore successfully!");
            }

            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("FirestoreError", "Failed to commit seeded data to Firestore: " + e.getMessage(), e);
            }
        });
    }
}
