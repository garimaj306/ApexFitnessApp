package com.fitnessapp;

import android.app.Application;
import com.fitnessapp.data.firebase.FirebaseManager;

/**
 * Application Entry Point: Initializes Firebase Services and Global Singletons.
 */
public class FitnessApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase with persistent offline caching
        FirebaseManager.getInstance(this);
    }
}
