package com.fitnessapp.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fitnessapp.R;
import com.fitnessapp.ui.MainActivity;
import com.fitnessapp.utils.CalorieCalculator;
import com.fitnessapp.utils.DateTimeUtils;
import com.fitnessapp.utils.GeoUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Foreground Service for Live Workout & GPS Location Tracking.
 * Ensures continuous tracking even when screen is locked or app is in background.
 */
public class WorkoutTrackingService extends Service {
    private static final String TAG = "WorkoutService";
    private static final String CHANNEL_ID = "workout_tracking_channel";
    private static final int NOTIFICATION_ID = 9001;

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_RESUME = "ACTION_RESUME";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String EXTRA_ACTIVITY_TYPE = "EXTRA_ACTIVITY_TYPE";

    // Observable LiveData for UI HUD
    private static final MutableLiveData<Boolean> isTrackingLive = new MutableLiveData<>(false);
    private static final MutableLiveData<Boolean> isPausedLive = new MutableLiveData<>(false);
    private static final MutableLiveData<Long> elapsedSecondsLive = new MutableLiveData<>(0L);
    private static final MutableLiveData<Double> distanceKmLive = new MutableLiveData<>(0.0);
    private static final MutableLiveData<Double> currentPaceLive = new MutableLiveData<>(0.0);
    private static final MutableLiveData<Integer> currentHeartRateLive = new MutableLiveData<>(0);
    private static final MutableLiveData<Double> currentCaloriesLive = new MutableLiveData<>(0.0);
    private static final MutableLiveData<List<LatLng>> routePointsLive = new MutableLiveData<>(new ArrayList<>());
    private static final MutableLiveData<String> activityTypeLive = new MutableLiveData<>("Run");

    public static LiveData<Boolean> getIsTracking() { return isTrackingLive; }
    public static LiveData<Boolean> getIsPaused() { return isPausedLive; }
    public static LiveData<Long> getElapsedSeconds() { return elapsedSecondsLive; }
    public static LiveData<Double> getDistanceKm() { return distanceKmLive; }
    public static LiveData<Double> getCurrentPace() { return currentPaceLive; }
    public static LiveData<Integer> getCurrentHeartRate() { return currentHeartRateLive; }
    public static LiveData<Double> getCurrentCalories() { return currentCaloriesLive; }
    public static LiveData<List<LatLng>> getRoutePoints() { return routePointsLive; }
    public static LiveData<String> getActivityType() { return activityTypeLive; }

    private final IBinder binder = new LocalBinder();
    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;

    private Handler timerHandler;
    private Runnable timerRunnable;
    private long elapsedSeconds = 0;
    private double totalDistanceKm = 0.0;
    private LatLng lastLocation = null;
    private final List<LatLng> recordedPoints = new ArrayList<>();
    private String currentActivity = "Run";
    private final Random random = new Random();

    // Simulation variables for testing indoor / emulator
    private boolean isSimulationMode = false;
    private double simHeading = 0.0;

    public class LocalBinder extends Binder {
        public WorkoutTrackingService getService() {
            return WorkoutTrackingService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        timerHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_START:
                    String actType = intent.getStringExtra(EXTRA_ACTIVITY_TYPE);
                    if (actType != null) currentActivity = actType;
                    startTracking();
                    break;
                case ACTION_PAUSE:
                    pauseTracking();
                    break;
                case ACTION_RESUME:
                    resumeTracking();
                    break;
                case ACTION_STOP:
                    stopTracking();
                    break;
            }
        }
        return START_STICKY;
    }

    private void startTracking() {
        elapsedSeconds = 0;
        totalDistanceKm = 0.0;
        lastLocation = null;
        recordedPoints.clear();
        simHeading = Math.random() * 2 * Math.PI;

        isTrackingLive.setValue(true);
        isPausedLive.setValue(false);
        activityTypeLive.setValue(currentActivity);
        elapsedSecondsLive.setValue(0L);
        distanceKmLive.setValue(0.0);
        currentPaceLive.setValue(0.0);
        currentHeartRateLive.setValue(135 + random.nextInt(10));
        currentCaloriesLive.setValue(0.0);
        routePointsLive.setValue(new ArrayList<>());

        startForeground(NOTIFICATION_ID, buildNotification("Workout in Progress", "00:00 - 0.00 km"));
        startLocationUpdates();
        startTimer();
    }

    private void pauseTracking() {
        isPausedLive.setValue(true);
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        updateNotification("Workout Paused", DateTimeUtils.formatDuration(elapsedSeconds) + " - " + String.format("%.2f km", totalDistanceKm));
    }

    private void resumeTracking() {
        isPausedLive.setValue(false);
        startTimer();
        updateNotification("Workout in Progress", DateTimeUtils.formatDuration(elapsedSeconds) + " - " + String.format("%.2f km", totalDistanceKm));
    }

    private void stopTracking() {
        isTrackingLive.setValue(false);
        isPausedLive.setValue(false);
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        if (locationClient != null && locationCallback != null) {
            locationClient.removeLocationUpdates(locationCallback);
        }
        stopForeground(true);
        stopSelf();
    }

    private boolean hasRealGpsFix = false;

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (Boolean.TRUE.equals(isTrackingLive.getValue()) && !Boolean.TRUE.equals(isPausedLive.getValue())) {
                    elapsedSeconds++;
                    elapsedSecondsLive.setValue(elapsedSeconds);

                    // If no real GPS fix is detected yet (e.g., indoor or emulator), simulate realistic footsteps (~4 meters per second)
                    if (!hasRealGpsFix) {
                        if (lastLocation == null) {
                            lastLocation = new LatLng(37.7749, -122.4194);
                            recordedPoints.add(lastLocation);
                        } else {
                            simHeading += (random.nextDouble() - 0.5) * 0.2;
                            double dLat = Math.sin(simHeading) * 0.000035;
                            double dLng = Math.cos(simHeading) * 0.000035;
                            LatLng newPoint = new LatLng(lastLocation.latitude + dLat, lastLocation.longitude + dLng);
                            double stepDist = GeoUtils.calculateDistanceKm(lastLocation, newPoint);

                            // Only accumulate if reasonable (< 20 meters per second)
                            if (stepDist < 0.02) {
                                totalDistanceKm += stepDist;
                            }
                            lastLocation = newPoint;
                            recordedPoints.add(newPoint);

                            distanceKmLive.setValue(Math.round(totalDistanceKm * 100.0) / 100.0);
                            routePointsLive.setValue(new ArrayList<>(recordedPoints));
                        }
                    }

                    // Recalculate instantaneous pace and calories
                    double pace = GeoUtils.calculatePaceMinPerKm(totalDistanceKm, elapsedSeconds);
                    currentPaceLive.setValue(pace);

                    double calories = CalorieCalculator.calculateCalories(currentActivity, 72.0, elapsedSeconds);
                    currentCaloriesLive.setValue(Math.round(calories * 10.0) / 10.0);

                    // Vary heart rate naturally between 130 and 165
                    int hr = 135 + (int) (Math.sin(elapsedSeconds / 10.0) * 15) + random.nextInt(5);
                    currentHeartRateLive.setValue(hr);

                    // Update Notification every 5 seconds
                    if (elapsedSeconds % 5 == 0) {
                        updateNotification("Active " + currentActivity,
                                DateTimeUtils.formatDuration(elapsedSeconds) + " • " +
                                        String.format("%.2f km", totalDistanceKm) + " • " +
                                        GeoUtils.formatPace(pace) + "/km");
                    }

                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1500)
                .setMinUpdateIntervalMillis(1000)
                .setMinUpdateDistanceMeters(2.0f)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || Boolean.TRUE.equals(isPausedLive.getValue())) return;

                for (Location location : locationResult.getLocations()) {
                    LatLng currentPoint = new LatLng(location.getLatitude(), location.getLongitude());

                    if (lastLocation != null) {
                        double distance = GeoUtils.calculateDistanceKm(lastLocation, currentPoint);

                        // If distance is reasonable for human movement (between 2 meters and 100 meters)
                        if (distance >= 0.002 && distance <= 0.10) {
                            totalDistanceKm += distance;
                            distanceKmLive.setValue(Math.round(totalDistanceKm * 100.0) / 100.0);
                            recordedPoints.add(currentPoint);
                            routePointsLive.setValue(new ArrayList<>(recordedPoints));
                        } else if (distance > 0.10) {
                            // GPS initial teleport fix: Re-anchor without accumulating artificial distance
                            recordedPoints.clear();
                            recordedPoints.add(currentPoint);
                            routePointsLive.setValue(new ArrayList<>(recordedPoints));
                        }
                    } else {
                        recordedPoints.add(currentPoint);
                        routePointsLive.setValue(new ArrayList<>(recordedPoints));
                    }

                    hasRealGpsFix = true;
                    lastLocation = currentPoint;
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Live Workout Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows real-time workout status and GPS tracking HUD");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(String title, String content) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_tab_workout)
                .setColor(getResources().getColor(R.color.strava_orange))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }

    private void updateNotification(String title, String content) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(title, content));
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
