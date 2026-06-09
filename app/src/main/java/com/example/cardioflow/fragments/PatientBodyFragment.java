package com.example.cardioflow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.cardioflow.R;
import com.example.cardioflow.data.DataManager;
import com.example.cardioflow.models.Thresholds;
import com.example.cardioflow.utils.BodyPartDialog;

public class PatientBodyFragment extends Fragment {
    private String patientId;
    private Thresholds thresholds;

    public static PatientBodyFragment newInstance(String patientId) {
        PatientBodyFragment fragment = new PatientBodyFragment();
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
            thresholds = DataManager.getInstance(requireContext()).getThresholdsForPatient(patientId);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_body, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.view_heart_doctor).setOnClickListener(v -> 
            BodyPartDialog.show(requireContext(), BodyPartDialog.PartType.HEART, thresholds));
        view.findViewById(R.id.view_lungs_doctor).setOnClickListener(v -> 
            BodyPartDialog.show(requireContext(), BodyPartDialog.PartType.LUNGS, thresholds));
        view.findViewById(R.id.view_skin_doctor).setOnClickListener(v -> 
            BodyPartDialog.show(requireContext(), BodyPartDialog.PartType.SKIN, thresholds));
    }
}