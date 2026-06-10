package com.example.cardioflow.fragments;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cardioflow.R;
import com.example.cardioflow.activities.DeviceScanActivity;
import com.example.cardioflow.auth.AuthManager;
import com.example.cardioflow.data.DataManager;
import com.example.cardioflow.database.DatabaseManager;
import com.example.cardioflow.models.Alert;
import com.example.cardioflow.models.Measurement;
import com.example.cardioflow.models.Thresholds;
import com.example.cardioflow.models.User;
import com.example.cardioflow.services.CloudSync;
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

    private TextView tvLastHr, tvLastSpo2, tvLastTemp, tvLastHum, tvActivityCountdown, tvAlarmStatus, tvBleStatus, tvDebugUuids;
    private ImageView ivBleStatusDot;
    private Button btnStartActivity, btnConnectBle;
    private LineChart chartHr, chartEcg;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private DatabaseManager dbManager;
    private Thresholds userThresholds;
    private User currentUser;

    private static final int SYNC_INTERVAL_MS = AppConstants.SYNC_INTERVAL_MS;
    private int readingsCount = 0;
    private double sumHr = 0, sumSpo2 = 0, sumTemp = 0, sumHum = 0;

    private int alarmPersistCounter = 0;
    private long activityEndTime = 0;
    private boolean isSimulationMode = false;

    private final BroadcastReceiver bleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.example.cardioflow.BLE_DATA_RECEIVED".equals(action)) {
                if (isSimulationMode) {
                    isSimulationMode = false;
                    handler.removeCallbacks(simulationRunnable);
                }
                
                List<Integer> ecgSamples = null;
                if (intent.hasExtra("ecgSamples")) {
                    int[] samplesArray = intent.getIntArrayExtra("ecgSamples");
                    if (samplesArray != null) {
                        ecgSamples = new ArrayList<>();
                        for (int s : samplesArray) ecgSamples.add(s);
                    }
                }

                processNewData(
                        intent.getIntExtra("heartRate", 0),
                        intent.getIntExtra("spo2", 0),
                        intent.getDoubleExtra("temp", 0.0),
                        intent.getDoubleExtra("hum", 0.0),
                        intent.getBooleanExtra("leadsOff", false),
                        ecgSamples
                );
            } else if ("com.example.cardioflow.BLE_STATUS_CHANGED".equals(action)) {
                String status = intent.getStringExtra("status");
                String debugInfo = intent.getStringExtra("debug_uuids");
                if (status != null) {
                    updateBleStatusUI(status);
                }
                if (debugInfo != null && tvDebugUuids != null) {
                    tvDebugUuids.setText(debugInfo);
                }
            }
        }
    };

    private void updateBleStatusUI(String status) {
        if (tvBleStatus == null || ivBleStatusDot == null) return;
        
        tvBleStatus.setText(status);
        if (status.contains("Conectat")) {
            ivBleStatusDot.setColorFilter(Color.parseColor("#4CAF50")); // Green
            tvBleStatus.setTextColor(Color.parseColor("#4CAF50"));
        } else if (status.contains("Se conectează") || status.contains("Configurare")) {
            ivBleStatusDot.setColorFilter(Color.parseColor("#FFA500")); // Orange
            tvBleStatus.setTextColor(Color.parseColor("#FFA500"));
        } else {
            ivBleStatusDot.setColorFilter(Color.GRAY);
            tvBleStatus.setTextColor(Color.GRAY);
        }
        
        // Also update the bottom alarm status text if it's a general status
        tvAlarmStatus.setText(status);
    }

    private final Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            syncAggregatedData();
            updateActivityStatus();
            handler.postDelayed(this, SYNC_INTERVAL_MS);
        }
    };

    private final Runnable simulationRunnable = new Runnable() {
        @Override
        public void run() {
            if (isSimulationMode) {
                Random r = new Random();
                int hr = 60 + r.nextInt(40);
                int spo2 = 94 + r.nextInt(6);
                double temp = 36.0 + r.nextDouble() * 1.5;
                double hum = 40.0 + r.nextDouble() * 20.0;
                
                dbManager.insertSensorData(hr, spo2, temp, hum);
                processNewData(hr, spo2, temp, hum, false, null);
                
                handler.postDelayed(this, 10000);
            }
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
        tvDebugUuids = view.findViewById(R.id.tv_debug_uuids);
        tvBleStatus = view.findViewById(R.id.tv_ble_connection_state);
        ivBleStatusDot = view.findViewById(R.id.iv_ble_status_dot);
        btnStartActivity = view.findViewById(R.id.btn_start_activity);

        // Toggle debug view on long press of the status layout
        view.findViewById(R.id.layout_ble_status).setOnLongClickListener(v -> {
            if (tvDebugUuids != null) {
                int vis = tvDebugUuids.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
                tvDebugUuids.setVisibility(vis);
                Toast.makeText(getContext(), "Debug View: " + (vis == View.VISIBLE ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        btnConnectBle = view.findViewById(R.id.btn_connect_ble);
        chartHr = view.findViewById(R.id.chart_hr);
        chartEcg = view.findViewById(R.id.chart_ecg);

        dbManager = DatabaseManager.getInstance(requireContext());
        currentUser = AuthManager.getInstance(requireContext()).getCurrentUser();
        
        SharedPreferences prefs = requireContext().getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
        isSimulationMode = prefs.getBoolean(AppConstants.KEY_SIMULATION_MODE, true);

        if (currentUser != null) {
            userThresholds = DataManager.getInstance(requireContext()).getThresholdsForPatient(currentUser.getId());
        }

        btnStartActivity.setOnClickListener(v -> startActivitySuppression());
        btnConnectBle.setOnClickListener(v -> 
            startActivity(new Intent(requireContext(), DeviceScanActivity.class)));

        view.findViewById(R.id.view_heart).setOnClickListener(v -> 
            BodyPartDialog.show(requireContext(), BodyPartDialog.PartType.HEART, userThresholds));
        view.findViewById(R.id.view_lungs).setOnClickListener(v -> 
            BodyPartDialog.show(requireContext(), BodyPartDialog.PartType.LUNGS, userThresholds));
        view.findViewById(R.id.view_skin).setOnClickListener(v -> 
            BodyPartDialog.show(requireContext(), BodyPartDialog.PartType.SKIN, userThresholds));

        setupChart();
        updateChart();
        handler.post(syncRunnable);
        if (isSimulationMode) {
            handler.post(simulationRunnable);
            tvAlarmStatus.setText("Mod Simulare Activ");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = requireContext().getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
        isSimulationMode = prefs.getBoolean(AppConstants.KEY_SIMULATION_MODE, true);
        
        if (isSimulationMode) {
            if (!handler.hasCallbacks(simulationRunnable)) {
                handler.post(simulationRunnable);
            }
            updateBleStatusUI("Mod Simulare Activ");
        } else {
            handler.removeCallbacks(simulationRunnable);
            updateBleStatusUI("Așteptare conexiune BLE...");
        }

        int flag = (Build.VERSION.SDK_INT >= 33) ? Context.RECEIVER_EXPORTED : 0;
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.cardioflow.BLE_DATA_RECEIVED");
        filter.addAction("com.example.cardioflow.BLE_STATUS_CHANGED");
        requireContext().registerReceiver(bleReceiver, filter, flag);
        updateChart();
    }

    @Override
    public void onPause() {
        super.onPause();
        requireContext().unregisterReceiver(bleReceiver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(syncRunnable);
        handler.removeCallbacks(simulationRunnable);
    }

    private void setupChart() {
        // Setup HR Chart
        chartHr.getDescription().setEnabled(false);
        chartHr.getLegend().setEnabled(false);
        chartHr.getXAxis().setDrawGridLines(false);
        chartHr.getAxisRight().setEnabled(false);

        // Setup ECG Chart
        chartEcg.getDescription().setEnabled(false);
        chartEcg.getLegend().setEnabled(false);
        chartEcg.getXAxis().setDrawGridLines(false);
        chartEcg.getAxisRight().setEnabled(false);
        chartEcg.getAxisLeft().setDrawGridLines(true);
        chartEcg.getAxisLeft().setAxisMinimum(0f);
        chartEcg.getAxisLeft().setAxisMaximum(4095f); // 12-bit ADC range
    }

    private void processNewData(int hr, int spo2, double temp, double hum, boolean leadsOff, List<Integer> ecgSamples) {
        updateLastValues(hr, spo2, temp, hum);
        if (isResumed()) {
            updateChart();
            if (ecgSamples != null && !ecgSamples.isEmpty()) {
                updateEcgChart(ecgSamples);
            }
        }
        checkThresholds(hr, spo2, temp, hum);
        
        if (leadsOff) {
            tvAlarmStatus.setText("Senzor ECG Deconectat!");
            tvAlarmStatus.setTextColor(Color.RED);
        }

        // Send all data to Cloud for verification (even if sensors are disconnected with -1)
        Measurement m = new Measurement();
        m.setPatientId(currentUser != null ? currentUser.getId() : "1");
        m.setHeartRate(hr);
        m.setSpo2(spo2);
        m.setTemperature(temp);
        m.setHumidity(hum);
        m.setEcgSamples(ecgSamples);
        m.setTimestamp(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
        CloudSync.sendRealTimeData(requireContext(), m);

        readingsCount++;
        sumHr += hr;
        sumSpo2 += spo2;
        sumTemp += temp;
        sumHum += hum;
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

        LineData lineData = new LineData(dataSet);
        chartEcg.setData(lineData);
        chartEcg.invalidate();
    }

    private void updateLastValues(int hr, int spo2, double temp, double hum) {
        tvLastHr.setText(getString(R.string.hr_format, hr));
        tvLastSpo2.setText(getString(R.string.spo2_format, spo2));
        tvLastTemp.setText(getString(R.string.temp_format, temp));
        tvLastHum.setText(getString(R.string.hum_format, hum));
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

    private void updateChart() {
        List<Measurement> readings = dbManager.getLastReadings(20);
        if (readings.isEmpty()) return;
        
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < readings.size(); i++) {
            entries.add(new Entry(i, (float) readings.get(readings.size() - 1 - i).getHeartRate()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Puls");
        dataSet.setColor(Color.RED);
        dataSet.setCircleColor(Color.RED);
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(false);

        LineData lineData = new LineData(dataSet);
        chartHr.setData(lineData);
        chartHr.invalidate();
    }

    private void checkThresholds(int hr, int spo2, double temp, double hum) {
        if (userThresholds == null || currentUser == null) return;

        String type = null;
        double value = 0;

        if (hr > userThresholds.getHrMax()) { type = "High Heart Rate"; value = hr; }
        else if (hr < userThresholds.getHrMin()) { type = "Low Heart Rate"; value = hr; }
        else if (spo2 < userThresholds.getSpo2Min()) { type = "Low SpO2"; value = spo2; }
        else if (temp > userThresholds.getTempMax()) { type = "High Temperature"; value = temp; }
        else if (temp < userThresholds.getTempMin()) { type = "Low Temperature"; value = temp; }

        if (type != null) {
            if (System.currentTimeMillis() < activityEndTime) {
                tvAlarmStatus.setText(R.string.status_activity_detected);
                tvAlarmStatus.setTextColor(Color.BLUE);
                alarmPersistCounter = 0;
                return;
            }

            alarmPersistCounter++;
            tvAlarmStatus.setText(getString(R.string.status_abnormal_detect, alarmPersistCounter, userThresholds.getPersistSeconds()));
            tvAlarmStatus.setTextColor(Color.parseColor("#FFA500"));

            if (alarmPersistCounter >= userThresholds.getPersistSeconds()) {
                triggerAlert(type, value);
                alarmPersistCounter = 0;
            }
        } else {
            alarmPersistCounter = 0;
            if (System.currentTimeMillis() >= activityEndTime && !isSimulationMode) {
                tvAlarmStatus.setText(R.string.status_normal);
                tvAlarmStatus.setTextColor(Color.parseColor("#006400"));
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

    private void triggerAlert(String type, double value) {
        Alert alert = new Alert();
        alert.setPatientId(currentUser != null ? currentUser.getId() : "unknown");
        alert.setType(type);
        alert.setValue(value);
        alert.setSeverity("High");
        dbManager.insertAlert(alert);

        CloudSync.sendAlarm(requireContext(), alert);

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

            Measurement avgM = new Measurement();
            avgM.setPatientId(currentUser != null ? currentUser.getId() : "1");
            avgM.setHeartRate((int)avgHr);
            avgM.setSpo2((int)avgSpo2);
            avgM.setTemperature(avgTemp);
            avgM.setHumidity(avgHum);
            CloudSync.sendAggregatedData(requireContext(), avgM);

            readingsCount = 0;
            sumHr = 0; sumSpo2 = 0; sumTemp = 0; sumHum = 0;
        }
    }
}
