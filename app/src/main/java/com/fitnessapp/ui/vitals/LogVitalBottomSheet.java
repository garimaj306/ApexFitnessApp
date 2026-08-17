package com.fitnessapp.ui.vitals;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.fitnessapp.R;
import com.fitnessapp.viewmodels.VitalsViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Modal Bottom Sheet for Logging Weight (kg) or Water (ml).
 */
public class LogVitalBottomSheet extends BottomSheetDialogFragment {

    private VitalsViewModel viewModel;
    private RadioButton rbWeight;
    private RadioButton rbWater;
    private TextInputEditText etVitalValue;
    private MaterialButton btnSaveVital;

    public static LogVitalBottomSheet newInstance() {
        return new LogVitalBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_log_vital, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(VitalsViewModel.class);

        rbWeight = view.findViewById(R.id.rb_weight);
        rbWater = view.findViewById(R.id.rb_water);
        etVitalValue = view.findViewById(R.id.et_vital_value);
        btnSaveVital = view.findViewById(R.id.btn_save_vital);

        btnSaveVital.setOnClickListener(v -> {
            String input = etVitalValue.getText() != null ? etVitalValue.getText().toString().trim() : "";
            if (TextUtils.isEmpty(input)) {
                Toast.makeText(getContext(), "Please enter a value", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double val = Double.parseDouble(input);
                if (rbWeight.isChecked()) {
                    viewModel.logWeight(val);
                    Toast.makeText(getContext(), "Weight logged: " + val + " kg", Toast.LENGTH_SHORT).show();
                } else {
                    viewModel.logWater(val);
                    Toast.makeText(getContext(), "Water logged: " + (int) val + " ml", Toast.LENGTH_SHORT).show();
                }
                dismiss();
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid number format", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
