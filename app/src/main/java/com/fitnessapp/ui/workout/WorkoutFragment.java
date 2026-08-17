package com.fitnessapp.ui.workout;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.fitnessapp.R;
import com.fitnessapp.data.models.Workout;
import com.fitnessapp.utils.DateTimeUtils;
import com.fitnessapp.utils.GeoUtils;
import com.fitnessapp.viewmodels.WorkoutViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;
import java.util.Locale;

/**
 * Tab 2: Workout Live Tracking Engine.
 * Manages GPS coordinate streaming, high-contrast outdoor HUD,
 * polyline compression, and Cloud Firestore synchronization.
 */
public class WorkoutFragment extends Fragment implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private WorkoutViewModel viewModel;
    private MapView mapView;
    private GoogleMap googleMap;
    private Polyline activeRoutePolyline;

    private View layoutPreWorkout;
    private LinearLayout layoutActiveHud;

    private ChipGroup chipGroupActivities;
    private MaterialButton btnStartWorkout;

    private TextView tvHudActivityTitle;
    private TextView tvHudHeartRate;
    private TextView tvHudTime;
    private TextView tvHudDistance;
    private TextView tvHudPace;
    private TextView tvHudCalories;

    private MaterialButton btnPauseResume;
    private MaterialButton btnFinishWorkout;

    private String selectedActivityType = "Run";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_workout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(WorkoutViewModel.class);

        initViews(view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        setupActivitySelection();
        setupObservers();
        setupClickListeners();
        checkPermissions();
    }

    private void initViews(View view) {
        mapView = view.findViewById(R.id.map_view);
        layoutPreWorkout = view.findViewById(R.id.layout_pre_workout);
        layoutActiveHud = view.findViewById(R.id.layout_active_hud);

        chipGroupActivities = view.findViewById(R.id.chip_group_activities);
        btnStartWorkout = view.findViewById(R.id.btn_start_workout);

        tvHudActivityTitle = view.findViewById(R.id.tv_hud_activity_title);
        tvHudHeartRate = view.findViewById(R.id.tv_hud_heart_rate);
        tvHudTime = view.findViewById(R.id.tv_hud_time);
        tvHudDistance = view.findViewById(R.id.tv_hud_distance);
        tvHudPace = view.findViewById(R.id.tv_hud_pace);
        tvHudCalories = view.findViewById(R.id.tv_hud_calories);

        btnPauseResume = view.findViewById(R.id.btn_pause_resume);
        btnFinishWorkout = view.findViewById(R.id.btn_finish_workout);
    }

    private void setupActivitySelection() {
        chipGroupActivities.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip_run) selectedActivityType = "Run";
            else if (id == R.id.chip_walk) selectedActivityType = "Walk";
            else if (id == R.id.chip_cycle) selectedActivityType = "Cycle";
            else if (id == R.id.chip_hike) selectedActivityType = "Hike";
        });
    }

    private void setupObservers() {
        // Tracking state
        viewModel.isTracking().observe(getViewLifecycleOwner(), isTracking -> {
            if (Boolean.TRUE.equals(isTracking)) {
                layoutPreWorkout.setVisibility(View.GONE);
                layoutActiveHud.setVisibility(View.VISIBLE);
            } else {
                layoutPreWorkout.setVisibility(View.VISIBLE);
                layoutActiveHud.setVisibility(View.GONE);
            }
        });

        // Paused state
        viewModel.isPaused().observe(getViewLifecycleOwner(), isPaused -> {
            if (Boolean.TRUE.equals(isPaused)) {
                btnPauseResume.setText(R.string.resume_workout);
                btnPauseResume.setIconResource(R.drawable.ic_play);
            } else {
                btnPauseResume.setText(R.string.pause_workout);
                btnPauseResume.setIconResource(R.drawable.ic_pause);
            }
        });

        // Live HUD Metrics
        viewModel.getElapsedSeconds().observe(getViewLifecycleOwner(), seconds -> {
            tvHudTime.setText(DateTimeUtils.formatDuration(seconds != null ? seconds : 0L));
        });

        viewModel.getDistanceKm().observe(getViewLifecycleOwner(), distance -> {
            tvHudDistance.setText(String.format(Locale.US, "%.2f", distance != null ? distance : 0.0));
        });

        viewModel.getCurrentPace().observe(getViewLifecycleOwner(), pace -> {
            tvHudPace.setText(GeoUtils.formatPace(pace != null ? pace : 0.0));
        });

        viewModel.getCurrentHeartRate().observe(getViewLifecycleOwner(), bpm -> {
            tvHudHeartRate.setText((bpm != null ? bpm : 140) + " BPM");
        });

        viewModel.getCurrentCalories().observe(getViewLifecycleOwner(), kcal -> {
            tvHudCalories.setText(String.format(Locale.US, "%.0f kcal", kcal != null ? kcal : 0.0));
        });

        viewModel.getActivityType().observe(getViewLifecycleOwner(), type -> {
            tvHudActivityTitle.setText("ACTIVE " + (type != null ? type.toUpperCase() : "RUN"));
        });

        // Live Route Polyline Map Rendering
        viewModel.getRoutePoints().observe(getViewLifecycleOwner(), points -> {
            if (googleMap != null && points != null && !points.isEmpty()) {
                if (activeRoutePolyline == null) {
                    PolylineOptions polyOptions = new PolylineOptions()
                            .addAll(points)
                            .width(14f)
                            .color(getResources().getColor(R.color.strava_orange))
                            .geodesic(true);
                    activeRoutePolyline = googleMap.addPolyline(polyOptions);
                } else {
                    activeRoutePolyline.setPoints(points);
                }

                LatLng lastPoint = points.get(points.size() - 1);
                googleMap.animateCamera(CameraUpdateFactory.newLatLng(lastPoint));
            }
        });
    }

    private void setupClickListeners() {
        btnStartWorkout.setOnClickListener(v -> {
            if (activeRoutePolyline != null) {
                activeRoutePolyline.remove();
                activeRoutePolyline = null;
            }
            viewModel.startWorkout(selectedActivityType);
        });

        btnPauseResume.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(viewModel.isPaused().getValue())) {
                viewModel.resumeWorkout();
            } else {
                viewModel.pauseWorkout();
            }
        });

        btnFinishWorkout.setOnClickListener(v -> {
            Workout savedWorkout = viewModel.finishWorkout();
            if (activeRoutePolyline != null) {
                activeRoutePolyline.remove();
                activeRoutePolyline = null;
            }
            WorkoutSummaryDialog dialog = WorkoutSummaryDialog.newInstance(savedWorkout);
            dialog.show(getParentFragmentManager(), "WorkoutSummaryDialog");
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(false);

        LatLng initialLocation = new LatLng(37.7694, -122.4862); // Default SF park view
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 15f));

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
    }

    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (googleMap != null && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    googleMap.setMyLocationEnabled(true);
                }
            }
        }
    }

    // MapView Lifecycle Management
    @Override public void onStart() { super.onStart(); if (mapView != null) mapView.onStart(); }
    @Override public void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override public void onPause() { if (mapView != null) mapView.onPause(); super.onPause(); }
    @Override public void onStop() { if (mapView != null) mapView.onStop(); super.onStop(); }
    @Override public void onDestroy() { if (mapView != null) mapView.onDestroy(); super.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapView != null) mapView.onLowMemory(); }
    @Override public void onSaveInstanceState(@NonNull Bundle outState) { super.onSaveInstanceState(outState); if (mapView != null) mapView.onSaveInstanceState(outState); }
}
