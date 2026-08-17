package com.fitnessapp.ui.insights;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.fitnessapp.R;
import com.fitnessapp.data.models.DailySummary;
import com.fitnessapp.viewmodels.InsightsViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tab 4: Insights (Activity Trends & Route Heatmaps).
 * Segmented switcher between weekly/monthly bar analytics and
 * multi-trail historical decoded polyline heatmap rendering.
 */
public class InsightsFragment extends Fragment implements OnMapReadyCallback {

    private InsightsViewModel viewModel;

    private TabLayout tabLayoutInsights;
    private NestedScrollView layoutTrendsView;
    private FrameLayout layoutHeatmapView;

    private BarChart chartWeeklySteps;
    private BarChart chartDistanceTrends;
    private TextView tvWeeklyStepsAvg;
    private TextView tvTotalDistanceStat;

    private MapView mapViewHeatmap;
    private GoogleMap googleMapHeatmap;
    private TextView tvHeatmapRoutesCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_insights, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(InsightsViewModel.class);

        initViews(view);
        mapViewHeatmap.onCreate(savedInstanceState);
        mapViewHeatmap.getMapAsync(this);

        setupSegmentSwitcher();
        setupChartStyling();
        setupObservers();
    }

    private void initViews(View view) {
        tabLayoutInsights = view.findViewById(R.id.tab_layout_insights);
        layoutTrendsView = view.findViewById(R.id.layout_trends_view);
        layoutHeatmapView = view.findViewById(R.id.layout_heatmap_view);

        chartWeeklySteps = view.findViewById(R.id.chart_weekly_steps);
        chartDistanceTrends = view.findViewById(R.id.chart_distance_trends);
        tvWeeklyStepsAvg = view.findViewById(R.id.tv_weekly_steps_avg);
        tvTotalDistanceStat = view.findViewById(R.id.tv_total_distance_stat);

        mapViewHeatmap = view.findViewById(R.id.map_view_heatmap);
        tvHeatmapRoutesCount = view.findViewById(R.id.tv_heatmap_routes_count);
    }

    private void setupSegmentSwitcher() {
        tabLayoutInsights.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    layoutTrendsView.setVisibility(View.VISIBLE);
                    layoutHeatmapView.setVisibility(View.GONE);
                } else {
                    layoutTrendsView.setVisibility(View.GONE);
                    layoutHeatmapView.setVisibility(View.VISIBLE);
                    renderHeatmapRoutes();
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupChartStyling() {
        // Configure Weekly Steps Bar Chart
        chartWeeklySteps.getDescription().setEnabled(false);
        chartWeeklySteps.getLegend().setEnabled(false);
        chartWeeklySteps.setDrawGridBackground(false);
        chartWeeklySteps.setFitBars(true);

        XAxis xSteps = chartWeeklySteps.getXAxis();
        xSteps.setPosition(XAxis.XAxisPosition.BOTTOM);
        xSteps.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        xSteps.setDrawGridLines(false);

        chartWeeklySteps.getAxisLeft().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        chartWeeklySteps.getAxisLeft().setGridColor(ContextCompat.getColor(requireContext(), R.color.divider));
        chartWeeklySteps.getAxisRight().setEnabled(false);

        // Configure Monthly Distance Bar Chart
        chartDistanceTrends.getDescription().setEnabled(false);
        chartDistanceTrends.getLegend().setEnabled(false);
        chartDistanceTrends.setDrawGridBackground(false);
        chartDistanceTrends.setFitBars(true);

        XAxis xDist = chartDistanceTrends.getXAxis();
        xDist.setPosition(XAxis.XAxisPosition.BOTTOM);
        xDist.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        xDist.setDrawGridLines(false);

        chartDistanceTrends.getAxisLeft().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        chartDistanceTrends.getAxisLeft().setGridColor(ContextCompat.getColor(requireContext(), R.color.divider));
        chartDistanceTrends.getAxisRight().setEnabled(false);
    }

    private void setupObservers() {
        viewModel.getHistoricalSummaries().observe(getViewLifecycleOwner(), summaries -> {
            if (summaries != null && !summaries.isEmpty()) {
                populateTrends(summaries);
            } else {
                tvWeeklyStepsAvg.setText("No activity history yet • Start tracking today");
                tvTotalDistanceStat.setText("Total: 0.0 km logged");
                chartWeeklySteps.clear();
                chartDistanceTrends.clear();
            }
        });
    }

    private void populateTrends(List<DailySummary> summaries) {
        List<BarEntry> stepEntries = new ArrayList<>();
        List<BarEntry> distEntries = new ArrayList<>();
        long totalSteps = 0;
        double totalDist = 0.0;

        int startIndex = Math.max(0, summaries.size() - 7);
        int idx = 0;

        for (int i = startIndex; i < summaries.size(); i++) {
            DailySummary s = summaries.get(i);
            stepEntries.add(new BarEntry(idx, s.getStepsCount()));
            totalSteps += s.getStepsCount();
            idx++;
        }

        for (int i = 0; i < summaries.size(); i++) {
            DailySummary s = summaries.get(i);
            distEntries.add(new BarEntry(i, (float) s.getDistanceTravelledKm()));
            totalDist += s.getDistanceTravelledKm();
        }

        long avgSteps = totalSteps / Math.max(1, (summaries.size() - startIndex));
        tvWeeklyStepsAvg.setText(String.format(Locale.US, "Avg: %,d steps/day • 7 Days Logged", avgSteps));
        tvTotalDistanceStat.setText(String.format(Locale.US, "Total: %.1f km logged across %d days", totalDist, summaries.size()));

        // Populate Steps Chart
        BarDataSet stepDataSet = new BarDataSet(stepEntries, "Steps");
        stepDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.google_fit_cyan));
        stepDataSet.setDrawValues(false);
        chartWeeklySteps.setData(new BarData(stepDataSet));
        chartWeeklySteps.animateY(800);

        // Populate Distance Chart
        BarDataSet distDataSet = new BarDataSet(distEntries, "Distance");
        distDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.strava_orange));
        distDataSet.setDrawValues(false);
        chartDistanceTrends.setData(new BarData(distDataSet));
        chartDistanceTrends.animateY(800);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMapHeatmap = map;
        googleMapHeatmap.getUiSettings().setCompassEnabled(true);
        renderHeatmapRoutes();
    }

    private void renderHeatmapRoutes() {
        if (googleMapHeatmap == null) return;
        googleMapHeatmap.clear();

        List<List<LatLng>> routes = viewModel.getAllDecodedRoutes();
        if (routes.isEmpty()) {
            tvHeatmapRoutesCount.setText("No recorded GPS routes yet. Complete a workout to generate heat trails!");
            return;
        }

        tvHeatmapRoutesCount.setText(String.format(Locale.US, "Rendering %d historical GPS routes from Cloud Firestore", routes.size()));

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        int[] heatColors = new int[]{
                ContextCompat.getColor(requireContext(), R.color.strava_orange),
                ContextCompat.getColor(requireContext(), R.color.google_fit_cyan),
                ContextCompat.getColor(requireContext(), R.color.google_fit_green),
                ContextCompat.getColor(requireContext(), R.color.strava_orange_light)
        };

        int colorIdx = 0;
        for (List<LatLng> path : routes) {
            PolylineOptions options = new PolylineOptions()
                    .addAll(path)
                    .width(12f)
                    .color(heatColors[colorIdx % heatColors.length])
                    .geodesic(true);
            googleMapHeatmap.addPolyline(options);

            for (LatLng p : path) {
                boundsBuilder.include(p);
            }
            colorIdx++;
        }

        try {
            googleMapHeatmap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
        } catch (Exception e) {
            if (!routes.get(0).isEmpty()) {
                googleMapHeatmap.animateCamera(CameraUpdateFactory.newLatLngZoom(routes.get(0).get(0), 14f));
            }
        }
    }

    // MapView Lifecycle Management
    @Override public void onStart() { super.onStart(); if (mapViewHeatmap != null) mapViewHeatmap.onStart(); }
    @Override public void onResume() { super.onResume(); if (mapViewHeatmap != null) mapViewHeatmap.onResume(); }
    @Override public void onPause() { if (mapViewHeatmap != null) mapViewHeatmap.onPause(); super.onPause(); }
    @Override public void onStop() { if (mapViewHeatmap != null) mapViewHeatmap.onStop(); super.onStop(); }
    @Override public void onDestroy() { if (mapViewHeatmap != null) mapViewHeatmap.onDestroy(); super.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapViewHeatmap != null) mapViewHeatmap.onLowMemory(); }
    @Override public void onSaveInstanceState(@NonNull Bundle outState) { super.onSaveInstanceState(outState); if (mapViewHeatmap != null) mapViewHeatmap.onSaveInstanceState(outState); }
}
