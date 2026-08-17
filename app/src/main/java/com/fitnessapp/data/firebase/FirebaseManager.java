package com.fitnessapp.data.firebase;

import android.content.Context;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.PersistentCacheSettings;

/**
 * Firebase Manager: Initializes Firebase Auth and Firestore with Offline Persistence Cache.
 */
public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private static FirebaseManager instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private boolean isOfflineMode = false;

    private FirebaseManager(Context context) {
        // Initialize Firebase App if not already initialized
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context);
        }

        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();

        try {
            // Enable offline persistent cache for snappy sub-second performance
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder()
                            .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                            .build())
                    .build();
            this.firestore.setFirestoreSettings(settings);
        } catch (Exception e) {
            Log.w(TAG, "Firestore settings already applied or running in local mode: " + e.getMessage());
        }
    }

    public static synchronized FirebaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseManager(context.getApplicationContext());
        }
        return instance;
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public FirebaseFirestore getFirestore() {
        return firestore;
    }

    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            return user.getUid();
        }
        // Fallback default demo user ID
        return "demo_athlete_apex";
    }

    public boolean isUserAuthenticated() {
        return auth.getCurrentUser() != null;
    }

    public boolean isOfflineMode() {
        return isOfflineMode;
    }

    public void setOfflineMode(boolean offlineMode) {
        this.isOfflineMode = offlineMode;
    }
}
