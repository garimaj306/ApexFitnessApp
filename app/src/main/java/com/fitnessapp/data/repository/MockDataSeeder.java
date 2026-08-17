package com.fitnessapp.data.repository;

import com.fitnessapp.data.models.DailySummary;
import com.fitnessapp.data.models.SleepSession;
import com.fitnessapp.data.models.UserProfile;
import com.fitnessapp.data.models.VitalRecord;
import com.fitnessapp.data.models.Workout;
import com.fitnessapp.utils.DateTimeUtils;
import com.fitnessapp.utils.PolylineEncoder;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Mock Data Seeder: Populates rich, realistic 14-day history, sleep stages,
 * heart rate trends, and polyline workouts for immediate evaluation in Android Studio.
 */
public class MockDataSeeder {

    public static UserProfile createDefaultProfile(String userId) {
        UserProfile profile = new UserProfile(userId, "athlete@apexfit.com", "Alex Vance");
        profile.setTargetStepGoal(10000);
        profile.setStartingWeightKg(72.5);
        profile.setHeightCm(178.0);
        profile.setGender("Male");
        profile.setPrimaryGoal("Half Marathon Training & Vitals Optimization");
        List<String> prefs = new ArrayList<>();
        prefs.add("Running");
        prefs.add("Cycling");
        prefs.add("Trail Hiking");
        profile.setActivityPreferences(prefs);
        return profile;
    }

    public static DailySummary createTodaySummary() {
        DailySummary summary = new DailySummary(DateTimeUtils.getTodayDateKey());
        summary.setStepsCount(7420);
        summary.setCaloriesBurntKcal(520.0);
        summary.setDistanceTravelledKm(5.8);
        summary.setActiveDurationMinutes(48);
        summary.setWaterIntakeMl(1750);
        summary.setLastRecordedWeightKg(72.3);
        summary.setCurrentStreakDays(7);
        summary.setLastUpdatedAt(Timestamp.now());
        return summary;
    }

    public static List<DailySummary> generatePast14DaysSummaries() {
        List<DailySummary> list = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        Random random = new Random(42);

        for (int i = 13; i >= 0; i--) {
            Calendar dayCal = (Calendar) cal.clone();
            dayCal.add(Calendar.DAY_OF_YEAR, -i);
            String dateKey = DateTimeUtils.formatDateKey(dayCal.getTime());

            DailySummary summary = new DailySummary(dateKey);
            long steps = 8000 + random.nextInt(4500);
            double dist = (steps * 0.75) / 1000.0;
            double calories = 400 + random.nextInt(350);
            int activeMins = (int) (dist * 9.5);
            int water = 1500 + random.nextInt(1250);

            summary.setStepsCount(steps);
            summary.setDistanceTravelledKm(Math.round(dist * 10.0) / 10.0);
            summary.setCaloriesBurntKcal(calories);
            summary.setActiveDurationMinutes(activeMins);
            summary.setWaterIntakeMl(water);
            summary.setLastRecordedWeightKg(72.0 + (random.nextDouble() * 0.8 - 0.4));
            summary.setCurrentStreakDays(7 - (i % 7) + 1);
            summary.setLastUpdatedAt(new Timestamp(dayCal.getTime()));

            list.add(summary);
        }
        return list;
    }

    public static List<Workout> generateSampleWorkouts() {
        List<Workout> workouts = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        // Sample Base Coordinate (e.g. San Francisco Golden Gate Park area / Central Park)
        LatLng baseLoc = new LatLng(37.7694, -122.4862);

        // 1. Morning Tempo Run (Today)
        List<LatLng> route1 = createScenicRoute(baseLoc, 0.008, 25);
        Workout w1 = new Workout(
                "w_today_run",
                "Run",
                new Timestamp(new Date(System.currentTimeMillis() - 3600000L * 4)),
                new Timestamp(new Date(System.currentTimeMillis() - 3600000L * 3 - 180000L)),
                2280L, // 38 mins
                6.4,
                510.0,
                PolylineEncoder.encode(route1)
        );
        workouts.add(w1);

        // 2. Evening Cycle (Yesterday)
        List<LatLng> route2 = createScenicRoute(new LatLng(37.7750, -122.4700), 0.015, 35);
        Workout w2 = new Workout(
                "w_yesterday_cycle",
                "Cycle",
                new Timestamp(new Date(System.currentTimeMillis() - 86400000L - 7200000L)),
                new Timestamp(new Date(System.currentTimeMillis() - 86400000L - 3600000L)),
                3600L, // 60 mins
                18.2,
                640.0,
                PolylineEncoder.encode(route2)
        );
        workouts.add(w2);

        // 3. Trail Hike (3 Days Ago)
        List<LatLng> route3 = createScenicRoute(new LatLng(37.7600, -122.4900), 0.010, 30);
        Workout w3 = new Workout(
                "w_trail_hike",
                "Hike",
                new Timestamp(new Date(System.currentTimeMillis() - 86400000L * 3)),
                new Timestamp(new Date(System.currentTimeMillis() - 86400000L * 3 + 5400000L)),
                5400L, // 90 mins
                7.8,
                580.0,
                PolylineEncoder.encode(route3)
        );
        workouts.add(w3);

        // 4. Recovery Walk (5 Days Ago)
        List<LatLng> route4 = createScenicRoute(new LatLng(37.7800, -122.4600), 0.005, 20);
        Workout w4 = new Workout(
                "w_recovery_walk",
                "Walk",
                new Timestamp(new Date(System.currentTimeMillis() - 86400000L * 5)),
                new Timestamp(new Date(System.currentTimeMillis() - 86400000L * 5 + 2400000L)),
                2400L, // 40 mins
                3.5,
                195.0,
                PolylineEncoder.encode(route4)
        );
        workouts.add(w4);

        return workouts;
    }

    public static SleepSession createLastNightSleep() {
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_YEAR, -1);
        start.set(Calendar.HOUR_OF_DAY, 23);
        start.set(Calendar.MINUTE, 15);

        Calendar end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, 7);
        end.set(Calendar.MINUTE, 5);

        int totalMins = 470; // 7h 50m
        int deep = 115;      // ~24%
        int rem = 100;       // ~21%
        int light = 255;     // ~55%

        return new SleepSession("sleep_last_night",
                new Timestamp(start.getTime()),
                new Timestamp(end.getTime()),
                totalMins, rem, deep, light,
                "Health Connect (Synced)");
    }

    public static List<VitalRecord> generate24HourHeartRate() {
        List<VitalRecord> records = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, -24);
        Random random = new Random(101);

        for (int i = 0; i < 24; i++) {
            cal.add(Calendar.HOUR_OF_DAY, 1);
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            double hr;

            // Sleeping hours (0 to 6 AM): 52-62 BPM
            if (hour >= 0 && hour <= 6) {
                hr = 54 + random.nextInt(8);
            }
            // Workout hour (7 to 8 AM): 140-165 BPM
            else if (hour >= 7 && hour <= 8) {
                hr = 145 + random.nextInt(20);
            }
            // Daytime resting & active: 68-92 BPM
            else {
                hr = 70 + random.nextInt(22);
            }

            records.add(new VitalRecord(
                    "hr_" + i,
                    new Timestamp(cal.getTime()),
                    hr,
                    "Heart_Rate",
                    "BPM",
                    "Wear OS Optical Sensor"
            ));
        }

        return records;
    }

    private static List<LatLng> createScenicRoute(LatLng center, double spread, int points) {
        List<LatLng> list = new ArrayList<>();
        double angle = 0;
        double r = spread;
        for (int i = 0; i < points; i++) {
            angle += 0.35;
            double lat = center.latitude + Math.sin(angle) * r;
            double lng = center.longitude + Math.cos(angle) * r * 1.3;
            list.add(new LatLng(lat, lng));
        }
        return list;
    }
}
