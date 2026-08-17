package com.fitnessapp.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.fitnessapp.data.models.DailySummary;
import com.fitnessapp.data.models.Workout;
import com.fitnessapp.data.repository.FitnessRepository;
import com.fitnessapp.utils.PolylineEncoder;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * View Model for Tab 4: Insights (Analytics Trends & Route Heatmaps).
 */
public class InsightsViewModel extends AndroidViewModel {
    private final FitnessRepository repository;

    public InsightsViewModel(@NonNull Application application) {
        super(application);
        this.repository = FitnessRepository.getInstance(application);
    }

    public LiveData<List<DailySummary>> getHistoricalSummaries() {
        return repository.getHistoricalSummaries();
    }

    public LiveData<List<Workout>> getHistoricalWorkouts() {
        return repository.getWorkouts();
    }

    /**
     * Decodes all historical polyline routes into a combined list of paths for heatmap rendering.
     */
    public List<List<LatLng>> getAllDecodedRoutes() {
        List<List<LatLng>> allRoutes = new ArrayList<>();
        List<Workout> workouts = repository.getWorkouts().getValue();
        if (workouts != null) {
            for (Workout w : workouts) {
                if (w.getPolylineRoute() != null && !w.getPolylineRoute().isEmpty()) {
                    List<LatLng> decoded = PolylineEncoder.decode(w.getPolylineRoute());
                    if (!decoded.isEmpty()) {
                        allRoutes.add(decoded);
                    }
                }
            }
        }
        return allRoutes;
    }
}
