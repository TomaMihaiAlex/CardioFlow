package com.example.cardioflow.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cardioflow.R;
import com.example.cardioflow.database.FirebaseManager;
import com.example.cardioflow.models.Measurement;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class PatientLiveFragment extends Fragment {
    private String patientId;
    private TextView tvHr, tvSpo2;
    private LineChart chartHr, chartEcg;
    private List<Entry> hrEntries = new ArrayList<>();

    public static PatientLiveFragment newInstance(String patientId) {
        PatientLiveFragment fragment = new PatientLiveFragment();
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
        View view = inflater.inflate(R.layout.fragment_patient_live, container, false);
        tvHr = view.findViewById(R.id.tv_live_hr_doctor);
        tvSpo2 = view.findViewById(R.id.tv_live_spo2_doctor);
        chartHr = view.findViewById(R.id.chart_hr_doctor);
        chartEcg = view.findViewById(R.id.chart_ecg_doctor);

        setupCharts();
        startListening();
        return view;
    }

    private void setupCharts() {
        // HR Chart
        chartHr.getDescription().setEnabled(false);
        chartHr.getLegend().setEnabled(false);
        chartHr.getXAxis().setDrawGridLines(false);
        chartHr.getAxisRight().setEnabled(false);

        // ECG Chart
        chartEcg.getDescription().setEnabled(false);
        chartEcg.getLegend().setEnabled(false);
        chartEcg.getXAxis().setDrawGridLines(false);
        chartEcg.getAxisRight().setEnabled(false);
        chartEcg.getAxisLeft().setAxisMinimum(0f);
        chartEcg.getAxisLeft().setAxisMaximum(4095f);
    }

    private void startListening() {
        FirebaseManager.getInstance().listenForLiveReadings(patientId, measurement -> {
            if (measurement == null || !isAdded()) return;

            int hr = measurement.getHeartRate();
            int spo2 = measurement.getSpo2();

            if (hr == -1 || spo2 == -1) {
                tvHr.setText("Senzor Deconectat");
                tvSpo2.setText("--");
                tvHr.setTextColor(Color.RED);
            } else {
                tvHr.setText(hr + " bpm");
                tvSpo2.setText(spo2 + " %");
                tvHr.setTextColor(Color.BLACK);
                updateHrChart(hr);
            }

            if (measurement.getEcgSamples() != null) {
                updateEcgChart(measurement.getEcgSamples());
            }
        });
    }

    private void updateHrChart(int hr) {
        hrEntries.add(new Entry(hrEntries.size(), (float) hr));
        if (hrEntries.size() > 20) hrEntries.remove(0);

        LineDataSet dataSet = new LineDataSet(hrEntries, "Puls");
        dataSet.setColor(Color.RED);
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(false);
        dataSet.setLineWidth(2f);

        chartHr.setData(new LineData(dataSet));
        chartHr.invalidate();
    }

    private void updateEcgChart(List<Integer> samples) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < samples.size(); i++) {
            entries.add(new Entry(i, samples.get(i).floatValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "ECG");
        dataSet.setColor(Color.GREEN);
        dataSet.setLineWidth(1.5f);
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        chartEcg.setData(new LineData(dataSet));
        chartEcg.invalidate();
    }
}