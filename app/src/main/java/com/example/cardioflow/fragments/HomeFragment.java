package com.example.cardioflow.fragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cardioflow.R;
import com.example.cardioflow.auth.AuthManager;
import com.example.cardioflow.data.DataManager;
import com.example.cardioflow.database.DatabaseManager;
import com.example.cardioflow.models.Alert;
import com.example.cardioflow.models.Measurement;
import com.example.cardioflow.models.Thresholds;
import com.example.cardioflow.models.User;
import com.example.cardioflow.services.NotificationHelper;
import com.example.cardioflow.utils.AppConstants;
import com.example.cardioflow.utils.BodyPartDialog;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HomeFragment extends Fragment {

    private TextView tvLastHr, tvLastSpo2, tvLastTemp, tvLastHum, tvActivityCountdown, tvAlarmStatus;
    private Button btnStartActivity;
    private LineChart chartHr;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Random random = new Random();
    private DatabaseManager dbManager;
    private Thresholds userThresholds;
    private User currentUser;

    private static final int SIMULATION_INTERVAL_MS = AppConstants.SIMULATION_INTERVAL_MS;
    private static final int SYNC_INTERVAL_MS = AppConstants.SYNC_INTERVAL_MS;
    private int readingsCount = 0;
    private double sumHr = 0, sumSpo2 = 0, sumTemp = 0, sumHum = 0;

    private int alarmPersistCounter = 0;
    private long activityEndTime = 0;

    private Runnable simulationRunnable = new Runnable() {
        @Override
        public void run() {
            simulateData();
            updateActivityStatus();
            handler.postDelayed(this, SIMULATION_INTERVAL_MS);
        }
    };

    private Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            syncAggregatedData();
            handler.postDelayed(this, SYNC_INTERVAL_MS);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvLastHr = view.findViewById(R.id.tv_last_hr);
        tvLastSpo2 = view.findViewById(R.id.tv_last_spo2);
        tvLastTemp = view.findViewById(R.id.tv_last_temp);
        tvLastHum = view.findViewById(R.id.tv_last_hum);
        tvActivityCountdown = view.findViewById(R.id.tv_activity_countdown);
        tvAlarmStatus = view.findViewById(R.id.tv_alarm_status);
        btnStartActivity = view.findViewById(R.id.btn_start_activity);
        chartHr = view.findViewById(R.id.chart_hr);

        dbManager = DatabaseManager.getInstance(requireContext());
        currentUser = AuthManager.getInstance(requireContext()).getCurrentUser();
        if (currentUser != null) {
            userThresholds = DataManager.getInstance(requireContext()).getThresholdsForPatient(currentUser.getId());
        }

        btnStartActivity.setOnClickListener(v -> startActivitySuppression());

        view.findViewById(R.id.view_heart).setOnClickListener(v -> 
            BodyPartDialog.show(requireContext(), BodyPartDialog.PartType.HEART, userThresholds));
        view.findViewById(R.id.view_lungs).setOnClickListener(v -> 
            BodyPartDialog.show(requireContext(), BodyPartDialog.PartType.LUNGS, userThresholds));
        view.findViewById(R.id.view_skin).setOnClickListener(v -> 
            BodyPartDialog.show(requireContext(), BodyPartDialog.PartType.SKIN, userThresholds));

        setupChart();
        updateChart();
        startSimulation();
    }

    private void setupChart() {
        chartHr.getDescription().setEnabled(false);
        chartHr.getLegend().setEnabled(false);
        chartHr.getXAxis().setDrawGridLines(false);
        chartHr.getAxisRight().setEnabled(false);
    }

    private void startSimulation() {
        handler.post(simulationRunnable);
        handler.post(syncRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(simulationRunnable);
        handler.removeCallbacks(syncRunnable);
    }

    private void simulateData() {
        int hr = 60 + random.nextInt(61); // 60-120
        int spo2 = 90 + random.nextInt(11); // 90-100
        double temp = 36.0 + (random.nextDouble() * 1.5); // 36.0-37.5
        double hum = 30.0 + (random.nextDouble() * 40.0); // 30-70

        dbManager.insertSensorData(hr, spo2, temp, hum);
        updateLastValues(hr, spo2, temp, hum);
        
        // Nu actualizăm graficul la fiecare secundă dacă nu este necesar
        // Putem folosi un contor sau să verificăm dacă fragmentul este vizibil
        if (isResumed()) {
            updateChart();
        }

        checkThresholds(hr, spo2, temp, hum);

        readingsCount++;
        sumHr += hr;
        sumSpo2 += spo2;
        sumTemp += temp;
        sumHum += hum;
    }

    private void updateLastValues(int hr, int spo2, double temp, double hum) {
        tvLastHr.setText(getString(R.string.hr_format, hr));
        tvLastSpo2.setText(getString(R.string.spo2_format, spo2));
        tvLastTemp.setText(getString(R.string.temp_format, temp));
        tvLastHum.setText(getString(R.string.hum_format, hum));
    }

    private void updateChart() {
        List<Measurement> readings = dbManager.getLastReadings(30);
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < readings.size(); i++) {
            entries.add(new Entry(i, (float) readings.get(readings.size() - 1 - i).getHeartRate()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Puls");
        dataSet.setColor(Color.RED);
        dataSet.setCircleColor(Color.RED);
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        chartHr.setData(lineData);
        chartHr.invalidate();
    }

    private void checkThresholds(int hr, int spo2, double temp, double hum) {
        if (userThresholds == null) return;

        // Reload thresholds in case they were updated by the doctor
        userThresholds = DataManager.getInstance(requireContext()).getThresholdsForPatient(currentUser.getId());

        String type = null;
        double value = 0;

        if (hr > userThresholds.getHrMax()) { type = "High Heart Rate"; value = hr; }
        else if (hr < userThresholds.getHrMin()) { type = "Low Heart Rate"; value = hr; }
        else if (spo2 < userThresholds.getSpo2Min()) { type = "Low SpO2"; value = spo2; }
        else if (temp > userThresholds.getTempMax()) { type = "High Temperature"; value = temp; }
        else if (temp < userThresholds.getTempMin()) { type = "Low Temperature"; value = temp; }

        if (type != null) {
            // Check if we are in an activity interval
            if (System.currentTimeMillis() < activityEndTime) {
                Log.d("Alarm", "Abnormal value ignored due to physical activity");
                tvAlarmStatus.setText(R.string.status_activity_detected);
                tvAlarmStatus.setTextColor(Color.BLUE);
                alarmPersistCounter = 0;
                return;
            }

            alarmPersistCounter++;
            Log.d("Alarm", "Abnormal value counter: " + alarmPersistCounter + "/" + userThresholds.getPersistSeconds());
            tvAlarmStatus.setText(getString(R.string.status_abnormal_detect, alarmPersistCounter, userThresholds.getPersistSeconds()));
            tvAlarmStatus.setTextColor(Color.parseColor("#FFA500")); // Orange

            if (alarmPersistCounter >= userThresholds.getPersistSeconds()) {
                triggerAlert(type, value);
                alarmPersistCounter = 0; // Reset after trigger? Or keep at max? Requirement says reset on return to normal.
                // Let's reset so it doesn't trigger every 10s if it persists.
            }
        } else {
            alarmPersistCounter = 0;
            if (System.currentTimeMillis() >= activityEndTime) {
                tvAlarmStatus.setText(R.string.status_normal);
                tvAlarmStatus.setTextColor(Color.parseColor("#006400")); // Dark Green
            }
        }
    }

    private void startActivitySuppression() {
        if (userThresholds == null) return;
        long intervalMinutes = userThresholds.getActivityIntervalMinutes();
        activityEndTime = System.currentTimeMillis() + (intervalMinutes * 60 * 1000);
        btnStartActivity.setEnabled(false);
        Toast.makeText(getContext(), getString(R.string.activity_started_msg, intervalMinutes), Toast.LENGTH_SHORT).show();
    }

    private void updateActivityStatus() {
        long timeLeftMs = activityEndTime - System.currentTimeMillis();
        if (timeLeftMs > 0) {
            long seconds = (timeLeftMs / 1000) % 60;
            long minutes = (timeLeftMs / (1000 * 60)) % 60;
            tvActivityCountdown.setText(getString(R.string.activity_countdown_format, minutes, seconds));
            btnStartActivity.setEnabled(false);
        } else {
            tvActivityCountdown.setText("");
            btnStartActivity.setEnabled(true);
        }
    }

    private void triggerAlert(String type, double value) {
        Alert alert = new Alert();
        alert.setPatientId(currentUser != null ? currentUser.getId() : "unknown");
        alert.setType(type);
        alert.setValue(value);
        alert.setSeverity("High");
        dbManager.insertAlert(alert);

        com.example.cardioflow.services.CloudSync.sendAlarm(requireContext(), alert);

        NotificationHelper.showNotification(requireContext(), getString(R.string.notification_title), getString(R.string.notification_msg, type, value));
        showAlarmDialog(type, value);
    }

    private void showAlarmDialog(String type, double value) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.alarm_dialog_title, type));
        builder.setMessage(getString(R.string.alarm_dialog_msg, value));

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
            String userText = input.getText().toString();
            // Update the last inserted alert (assuming it's the one we just triggered)
            List<Alert> alerts = dbManager.getAllAlerts();
            if (!alerts.isEmpty()) {
                dbManager.updateAlertUserText(alerts.get(0).getAlertId(), userText);
            }
            Toast.makeText(getContext(), R.string.alert_saved_msg, Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    private void syncAggregatedData() {
        if (readingsCount > 0) {
            double avgHr = sumHr / readingsCount;
            double avgSpo2 = sumSpo2 / readingsCount;
            double avgTemp = sumTemp / readingsCount;
            double avgHum = sumHum / readingsCount;

            Log.d("CloudSync", String.format("Simulare sincronizare: AVG HR: %.1f, SpO2: %.1f", avgHr, avgSpo2));
            
            Measurement avgM = new Measurement();
            avgM.setHeartRate((int)avgHr);
            avgM.setSpo2((int)avgSpo2);
            avgM.setTemperature(avgTemp);
            avgM.setHumidity(avgHum);
            com.example.cardioflow.services.CloudSync.sendAggregatedData(requireContext(), avgM);

            // Reset counters
            readingsCount = 0;
            sumHr = 0; sumSpo2 = 0; sumTemp = 0; sumHum = 0;
        }
    }
}