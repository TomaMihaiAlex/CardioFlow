package com.example.cardioflow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.net.ParseException;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cardioflow.R;
import com.example.cardioflow.adapters.AlertsAdapter;
import com.example.cardioflow.data.DataManager;
import com.example.cardioflow.models.Alert;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlertsFragment extends Fragment {
    private String patientId;
    private RecyclerView recyclerView;

    public static AlertsFragment newInstance(String patientId) {
        AlertsFragment fragment = new AlertsFragment();
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
        View view = inflater.inflate(R.layout.fragment_alerts, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        List<Alert> alerts = DataManager.getInstance(requireContext()).getAlertsForPatient(patientId);
        recyclerView.setAdapter(new AlertsAdapter(alerts));
        return view;
    }
}