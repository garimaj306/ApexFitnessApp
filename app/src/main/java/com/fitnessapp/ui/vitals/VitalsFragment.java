package com.fitnessapp.ui.vitals;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.fitnessapp.R;
import com.fitnessapp.data.models.SleepSession;
import com.fitnessapp.data.models.VitalRecord;
import com.fitnessapp.ui.views.SleepStageBarView;
import com.fitnessapp.viewmodels.VitalsViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tab 3: Vitals & Health Records.
 * Displays Wearable Sync Status, 24h Heart Rate Sparkline/Chart,
 * Decoupled Sleep Session Stage Breakdown, and FAB for Manual Logging.
 */
public class VitalsFragment extends Fragment {

    private VitalsViewModel viewModel;

    private TextView tvRestingHr;
    private TextView tvPeakHr;
    private LineChart chartHeartRate;

    private TextView tvVitalsSleepDuration;
    private SleepStageBarView viewSleepStages;
    private TextView tvStageDeep;
    private TextView tvStageRem;
    private TextView tvStageLight;

    private ExtendedFloatingActionButton fabLogVital;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vitals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(VitalsViewModel.class);

        initViews(view);
        setupHeartRateChartStyling();
        setupObservers();

        fabLogVital.setOnClickListener(v -> {
            LogVitalBottomSheet sheet = LogVitalBottomSheet.newInstance();
            sheet.show(getParentFragmentManager(), "LogVitalBottomSheet");
        });
    }

    private void initViews(View view) {
        tvRestingHr = view.findViewById(R.id.tv_resting_hr);
        tvPeakHr = view.findViewById(R.id.tv_peak_hr);
        chartHeartRate = view.findViewById(R.id.chart_heart_rate);

        tvVitalsSleepDuration = view.findViewById(R.id.tv_vitals_sleep_duration);
        viewSleepStages = view.findViewById(R.id.view_sleep_stages);
        tvStageDeep = view.findViewById(R.id.tv_stage_deep);
        tvStageRem = view.findViewById(R.id.tv_stage_rem);
        tvStageLight = view.findViewById(R.id.tv_stage_light);

        fabLogVital = view.findViewById(R.id.fab_log_vital);
    }

    private void setupHeartRateChartStyling() {
        chartHeartRate.getDescription().setEnabled(false);
        chartHeartRate.getLegend().setEnabled(false);
        chartHeartRate.setTouchEnabled(true);
        chartHeartRate.setDragEnabled(true);
        chartHeartRate.setScaleEnabled(false);
        chartHeartRate.setPinchZoom(false);
        chartHeartRate.setDrawGridBackground(false);

        XAxis xAxis = chartHeartRate.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(4f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int hour = (int) value % 24;
                return String.format(Locale.US, "%02d:00", hour);
            }
        });

        chartHeartRate.getAxisLeft().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        chartHeartRate.getAxisLeft().setDrawGridLines(true);
        chartHeartRate.getAxisLeft().setGridColor(ContextCompat.getColor(requireContext(), R.color.divider));
        chartHeartRate.getAxisLeft().setAxisMinimum(40f);
        chartHeartRate.getAxisLeft().setAxisMaximum(180f);

        chartHeartRate.getAxisRight().setEnabled(false);
    }

    private void setupObservers() {
        // 1. Observe 24h Heart Rate vitals
        viewModel.getHeartRateVitals().observe(getViewLifecycleOwner(), records -> {
            if (records != null && !records.isEmpty()) {
                populateHeartRateChart(records);
            } else {
                tvRestingHr.setText("-- BPM");
                tvPeakHr.setText("Peak: -- BPM");
                chartHeartRate.clear();
            }
        });

        // 2. Observe Sleep Session
        viewModel.getLastNightSleep().observe(getViewLifecycleOwner(), sleep -> {
            if (sleep != null) {
                populateSleepSession(sleep);
            } else {
                tvVitalsSleepDuration.setText("0h 00m Total");
                viewSleepStages.setStages(0, 0, 0);
                tvStageDeep.setText("0h 00m");
                tvStageRem.setText("0h 00m");
                tvStageLight.setText("0h 00m");
            }
        });
    }

    private void populateHeartRateChart(List<VitalRecord> records) {
        List<Entry> entries = new ArrayList<>();
        double minHr = 200;
        double maxHr = 0;

        for (int i = 0; i < records.size(); i++) {
            double val = records.get(i).getValue();
            entries.add(new Entry(i, (float) val));
            if (val < minHr) minHr = val;
            if (val > maxHr) maxHr = val;
        }

        tvRestingHr.setText(String.format(Locale.US, "%.0f BPM", minHr));
        tvPeakHr.setText(String.format(Locale.US, "Peak: %.0f BPM", maxHr));

        LineDataSet dataSet = new LineDataSet(entries, "Heart Rate");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.heart_rate_red));
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(requireContext(), R.color.heart_rate_red));
        dataSet.setFillAlpha(35);

        LineData lineData = new LineData(dataSet);
        chartHeartRate.setData(lineData);
        chartHeartRate.animateX(800);
    }

    private void populateSleepSession(SleepSession sleep) {
        tvVitalsSleepDuration.setText(sleep.getFormattedTotalDuration() + " Total");
        viewSleepStages.setStages(sleep.getDeepDurationMinutes(), sleep.getRemDurationMinutes(), sleep.getLightDurationMinutes());

        int deepH = sleep.getDeepDurationMinutes() / 60;
        int deepM = sleep.getDeepDurationMinutes() % 60;
        tvStageDeep.setText(String.format(Locale.US, "%dh %02dm", deepH, deepM));

        int remH = sleep.getRemDurationMinutes() / 60;
        int remM = sleep.getRemDurationMinutes() % 60;
        tvStageRem.setText(String.format(Locale.US, "%dh %02dm", remH, remM));

        int lightH = sleep.getLightDurationMinutes() / 60;
        int lightM = sleep.getLightDurationMinutes() % 60;
        tvStageLight.setText(String.format(Locale.US, "%dh %02dm", lightH, lightM));
    }
}
