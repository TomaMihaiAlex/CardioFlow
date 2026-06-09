package com.example.cardioflow.data;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.example.cardioflow.models.*;
import com.example.cardioflow.database.DatabaseManager;
import com.example.cardioflow.database.FirebaseManager;
import com.example.cardioflow.utils.AppConstants;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DataManager {
    private static DataManager instance;
    private Context context;
    private List<Measurement> allMeasurements;
    private List<Alert> allAlerts;
    private List<Recommendation> allRecommendations;
    private List<Thresholds> allThresholds;

    private DataManager(Context context) {
        this.context = context.getApplicationContext();
        loadData();
    }

    public static synchronized DataManager getInstance(Context context) {
        if (instance == null) {
            instance = new DataManager(context);
        }
        return instance;
    }

    private void loadData() {
        try {
            InputStream is;
            java.io.File file = new java.io.File(context.getFilesDir(), "patients_data.json");
            if (file.exists()) {
                is = new java.io.FileInputStream(file);
            } else {
                is = context.getAssets().open("patients_data.json");
            }
            InputStreamReader reader = new InputStreamReader(is);
            Gson gson = new Gson();
            Type type = new TypeToken<DataWrapper>(){}.getType();
            DataWrapper wrapper = gson.fromJson(reader, type);
            allMeasurements = wrapper.measurements != null ? wrapper.measurements : new ArrayList<>();
            allAlerts = wrapper.alerts != null ? wrapper.alerts : new ArrayList<>();
            allRecommendations = wrapper.recommendations != null ? wrapper.recommendations : new ArrayList<>();
            allThresholds = wrapper.thresholds != null ? wrapper.thresholds : new ArrayList<>();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
            allMeasurements = new ArrayList<>();
            allAlerts = new ArrayList<>();
            allRecommendations = new ArrayList<>();
            allThresholds = new ArrayList<>();
        }
    }

    // Salvare date în fișier (pentru modificări)
    private void saveData() {
        try {
            DataWrapper wrapper = new DataWrapper();
            wrapper.measurements = allMeasurements;
            wrapper.alerts = allAlerts;
            wrapper.recommendations = allRecommendations;
            wrapper.thresholds = allThresholds;

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(wrapper);
            FileOutputStream fos = context.openFileOutput("patients_data.json", Context.MODE_PRIVATE);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.write(json);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Gettere pentru liste filtrate pe patientId
    public List<Measurement> getMeasurementsForPatient(String patientId) {
        List<Measurement> result = new ArrayList<>();
        for (Measurement m : allMeasurements) {
            if (m.getPatientId().equals(patientId)) {
                result.add(m);
            }
        }
        return result;
    }

    public List<Alert> getAlertsForPatient(String patientId) {
        List<Alert> result = new ArrayList<>();
        for (Alert a : allAlerts) {
            if (a.getPatientId().equals(patientId)) {
                result.add(a);
            }
        }
        return result;
    }

    public List<Recommendation> getRecommendationsForPatient(String patientId) {
        List<Recommendation> result = new ArrayList<>();
        // Recomandări din JSON
        for (Recommendation r : allRecommendations) {
            if (r.getPatientId().equals(patientId)) {
                result.add(r);
            }
        }
        // Recomandări din baza de date
        result.addAll(DatabaseManager.getInstance(context).getRecommendationsForPatient(patientId));
        return result;
    }

    public Thresholds getThresholdsForPatient(String patientId) {
        for (Thresholds t : allThresholds) {
            if (t.getPatientId().equals(patientId)) {
                return t;
            }
        }
        // Dacă nu există, returnăm un obiect cu valori implicite
        Thresholds defaultThresholds = new Thresholds();
        defaultThresholds.setPatientId(patientId);
        defaultThresholds.setHrMin(AppConstants.DEFAULT_HR_MIN);
        defaultThresholds.setHrMax(AppConstants.DEFAULT_HR_MAX);
        defaultThresholds.setSpo2Min(AppConstants.DEFAULT_SPO2_MIN);
        defaultThresholds.setTempMin(AppConstants.DEFAULT_TEMP_MIN);
        defaultThresholds.setTempMax(AppConstants.DEFAULT_TEMP_MAX);
        defaultThresholds.setHumMin(AppConstants.DEFAULT_HUM_MIN);
        defaultThresholds.setHumMax(AppConstants.DEFAULT_HUM_MAX);
        defaultThresholds.setPersistSeconds(AppConstants.DEFAULT_PERSIST_SECONDS);
        defaultThresholds.setActivityIntervalMinutes(AppConstants.DEFAULT_ACTIVITY_INTERVAL_MIN);
        allThresholds.add(defaultThresholds);
        saveData();
        return defaultThresholds;
    }

    // Metodă pentru actualizarea pragurilor unui pacient
    public void updateThresholds(Thresholds thresholds) {
        for (int i = 0; i < allThresholds.size(); i++) {
            if (allThresholds.get(i).getPatientId().equals(thresholds.getPatientId())) {
                allThresholds.set(i, thresholds);
                saveData();
                FirebaseManager.getInstance().saveThresholds(thresholds);
                return;
            }
        }
        allThresholds.add(thresholds);
        saveData();
        FirebaseManager.getInstance().saveThresholds(thresholds);
    }

    // Adăugare recomandare nouă
    public void addRecommendation(Recommendation recommendation) {
        allRecommendations.add(recommendation);
        saveData();
        FirebaseManager.getInstance().saveRecommendation(recommendation);
    }

    // Ștergere recomandare (după id)
    public void deleteRecommendation(String recommendationId) {
        allRecommendations.removeIf(r -> r.getRecommendationId().equals(recommendationId));
        saveData();
    }

    static class DataWrapper {
        List<Measurement> measurements;
        List<Alert> alerts;
        List<Recommendation> recommendations;
        List<Thresholds> thresholds;
    }
}