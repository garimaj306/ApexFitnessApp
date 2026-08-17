package com.fitnessapp.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.fitnessapp.data.models.SleepSession;
import com.fitnessapp.data.models.VitalRecord;
import com.fitnessapp.data.repository.FitnessRepository;
import com.google.firebase.Timestamp;

import java.util.List;
import java.util.UUID;

/**
 * View Model for Tab 3: Vitals & Health Records.
 */
public class VitalsViewModel extends AndroidViewModel {
    private final FitnessRepository repository;

    public VitalsViewModel(@NonNull Application application) {
        super(application);
        this.repository = FitnessRepository.getInstance(application);
    }

    public LiveData<List<VitalRecord>> getHeartRateVitals() {
        return repository.getHeartRateVitals();
    }

    public LiveData<SleepSession> getLastNightSleep() {
        return repository.getLastNightSleep();
    }

    public void logWeight(double weightKg) {
        VitalRecord vital = new VitalRecord(
                UUID.randomUUID().toString(),
                Timestamp.now(),
                weightKg,
                "Weight",
                "kg",
                "Manual Entry"
        );
        repository.logVital(vital);
    }

    public void logWater(double amountMl) {
        VitalRecord vital = new VitalRecord(
                UUID.randomUUID().toString(),
                Timestamp.now(),
                amountMl,
                "Water",
                "ml",
                "Manual Entry"
        );
        repository.logVital(vital);
    }
}
