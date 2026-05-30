package com.example.cardioflow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.cardioflow.R;
import com.example.cardioflow.data.DataManager;
import com.example.cardioflow.models.Thresholds;

public class ThresholdsConfigFragment extends Fragment {
    private String patientId;
    private EditText etHrMin, etHrMax, etSpo2Min, etTempMin, etTempMax, etHumMin, etHumMax, etPersist, etActivityInterval;
    private Button btnSave;

    public static ThresholdsConfigFragment newInstance(String patientId) {
        ThresholdsConfigFragment fragment = new ThresholdsConfigFragment();
        Bundle args = new Bundle();
        args.putString("patientId", patientId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            patientId = getArguments().getString("patientId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_thresholds_config, container, false);
        etHrMin = view.findViewById(R.id.et_hr_min);
        etHrMax = view.findViewById(R.id.et_hr_max);
        etSpo2Min = view.findViewById(R.id.et_spo2_min);
        etTempMin = view.findViewById(R.id.et_temp_min);
        etTempMax = view.findViewById(R.id.et_temp_max);
        etHumMin = view.findViewById(R.id.et_hum_min);
        etHumMax = view.findViewById(R.id.et_hum_max);
        etPersist = view.findViewById(R.id.et_persist_seconds);
        etActivityInterval = view.findViewById(R.id.et_activity_interval);
        btnSave = view.findViewById(R.id.btn_save);

        loadThresholds();
        btnSave.setOnClickListener(v -> saveThresholds());
        return view;
    }

    private void loadThresholds() {
        Thresholds t = DataManager.getInstance(requireContext()).getThresholdsForPatient(patientId);
        if (t != null) {
            etHrMin.setText(String.valueOf(t.getHrMin()));
            etHrMax.setText(String.valueOf(t.getHrMax()));
            etSpo2Min.setText(String.valueOf(t.getSpo2Min()));
            etTempMin.setText(String.valueOf(t.getTempMin()));
            etTempMax.setText(String.valueOf(t.getTempMax()));
            etHumMin.setText(String.valueOf(t.getHumMin()));
            etHumMax.setText(String.valueOf(t.getHumMax()));
            etPersist.setText(String.valueOf(t.getPersistSeconds()));
            etActivityInterval.setText(String.valueOf(t.getActivityIntervalMinutes()));
        }
    }

    private void saveThresholds() {
        try {
            Thresholds t = new Thresholds();
            t.setPatientId(patientId);
            t.setHrMin(Integer.parseInt(etHrMin.getText().toString()));
            t.setHrMax(Integer.parseInt(etHrMax.getText().toString()));
            t.setSpo2Min(Integer.parseInt(etSpo2Min.getText().toString()));
            t.setTempMin(Double.parseDouble(etTempMin.getText().toString()));
            t.setTempMax(Double.parseDouble(etTempMax.getText().toString()));
            t.setHumMin(Double.parseDouble(etHumMin.getText().toString()));
            t.setHumMax(Double.parseDouble(etHumMax.getText().toString()));
            t.setPersistSeconds(Integer.parseInt(etPersist.getText().toString()));
            t.setActivityIntervalMinutes(Integer.parseInt(etActivityInterval.getText().toString()));
            DataManager.getInstance(requireContext()).updateThresholds(t);
            Toast.makeText(getContext(), "Configurări salvate", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Completați corect toate câmpurile numerice", Toast.LENGTH_SHORT).show();
        }
    }
}