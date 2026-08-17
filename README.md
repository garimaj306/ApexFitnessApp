# Apex Fit — Android Fitness Tracking Application (Google Fit + Strava Hybrid)

A production-ready native Android application built in **Java** for **Android Studio**, featuring a 5-tab Bottom Navigation architecture, real-time Cloud Firestore synchronization, live GPS workout tracking HUD with Google Polyline compression, decoupled sleep sessions, and high-performance visual analytics.

---

## 📱 Application Architecture & 5-Tab Overview

### 1. Tab 1: Today (Main Dashboard)
* **Greeting & Streak Header**: Real-time user greeting and streak badge.
* **Hero Progress Ring (`CircularProgressRingView.java`)**: Hardware-accelerated custom Canvas circular progress ring tracking current steps against the daily target goal.
* **2x2 Secondary Metrics Grid**:
  * **Calories Burnt**: Real-time active calorie expenditure (kcal).
  * **Active Streak**: Consecutive active streak days.
  * **Hydration Tracker**: Current water intake (ml) with a **one-tap `+250ml` quick-log action** that atomically increments Firestore.
  * **Recorded Weight**: Last recorded weight in kg.
* **Recent Activity Card**: Recent workout summary with polyline route indicators and stats.
* **Last Night's Sleep Card**: Total sleep duration and quality score.
* **Materialized Cache Pattern**: Subscribes directly to `users/{userId}/daily_summaries/{dateString}` via Firestore websocket listeners (`addSnapshotListener`) for sub-second, instant dashboard loading.

### 2. Tab 2: Workout (Live Tracking Engine)
* **Pre-Workout Screen**: Map centered on GPS coordinates, activity mode chips (`Run`, `Walk`, `Cycle`, `Hike`), and large **START WORKOUT** CTA.
* **Active Workout HUD**: High-contrast outdoor HUD displaying:
  * **Elapsed Time** (`00:00:00`)
  * **Distance** in kilometers (`0.00 km`)
  * **Instantaneous Pace** in minutes/km ($\text{Pace} = \frac{\text{Time}}{\text{Distance}}$)
  * **Live Heart Rate** (BPM)
  * **Calories Burnt** (METs formula calculation)
* **Live Route Drawing**: Draws dynamic Google Maps polylines as you move.
* **Finish Workout Flow**: Encodes GPS coordinates with the **Google Polyline 5-decimal algorithm**, writes to `users/{userId}/workouts/{workoutId}`, updates `daily_summaries/{today}`, and displays the celebration summary dialog.

### 3. Tab 3: Vitals (Health Records)
* **Wearable Sync Indicator**: Health Connect / HealthKit live sync indicator.
* **24-Hour Heart Rate Sparkline**: Cubic bezier trend line chart (via MPAndroidChart) showing resting HR, peak HR, and continuous 24h readings.
* **Decoupled Sleep Sessions**: Single source of truth for sleep from `users/{userId}/sleep_sessions/{sleepId}` with `SleepStageBarView` breakdown (Deep, REM, and Light sleep stages).
* **Floating Action Button (FAB)**: Modal bottom sheet for logging Weight (kg) and Water (ml).

### 4. Tab 4: Insights (Analytics & Heatmaps)
* **Segmented Switcher**: `[ Activity Trends ]` vs. `[ Route Heatmap ]`.
* **Activity Trends**: Weekly steps bar chart and 14-day cumulative distance charts with daily averages.
* **Route Heatmap**: Full interactive map view fetching all historical `workouts` from Firestore, decoding their polylines, and overlaying multi-hued heat trails.

### 5. Tab 5: Profile & Settings
* **Athlete Profile**: Baseline biometric attributes (Height, Weight, Gender, Primary Goal).
* **Daily Step Target Goal Slider**: Interactive slider (4,000 to 25,000 steps) with live Firestore syncing.
* **1-Click Cloud Data Seeder**: Automatically populates 14 days of realistic daily summaries, sleep sessions, vitals, and polyline workout trails to Cloud Firestore.

---

## 🗄️ Cloud Firestore NoSQL Schema

```
firestore
 └── users/{userId}
      ├── email: string
      ├── displayName: string
      ├── targetStepGoal: number
      ├── heightCm: number
      ├── startingWeightKg: number
      ├── primaryGoal: string
      │
      ├── daily_summaries/{dateString} (e.g. "2026-08-15") [Materialized Cache]
      │    ├── date: string
      │    ├── stepsCount: number
      │    ├── caloriesBurntKcal: number
      │    ├── distanceTravelledKm: number
      │    ├── activeDurationMinutes: number
      │    ├── waterIntakeMl: number
      │    ├── lastRecordedWeightKg: number
      │    └── currentStreakDays: number
      │
      ├── workouts/{workoutId}
      │    ├── activityType: string ("Run" | "Walk" | "Cycle" | "Hike")
      │    ├── startTime: Timestamp
      │    ├── finishTime: Timestamp
      │    ├── durationSeconds: number
      │    ├── totalDistanceKm: number
      │    ├── caloriesBurntKcal: number
      │    └── polylineRoute: string (Google Encoded Polyline)
      │
      ├── sleep_sessions/{sleepId} [Decoupled Single Source of Truth]
      │    ├── startTime: Timestamp
      │    ├── endTime: Timestamp
      │    ├── totalDurationMinutes: number
      │    ├── deepDurationMinutes: number
      │    ├── remDurationMinutes: number
      │    ├── lightDurationMinutes: number
      │    └── source: string
      │
      └── vitals/{vitalId}
           ├── recordedAt: Timestamp
           ├── value: number
           ├── vitalsType: string ("Heart_Rate" | "Weight" | "Water")
           ├── unit: string ("BPM" | "kg" | "ml")
           └── sourceDeviceName: string
```

