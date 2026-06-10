package com.example.cardioflow.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.cardioflow.database.DatabaseHelper;
import com.example.cardioflow.database.FirebaseManager;
import com.example.cardioflow.models.Alert;
import com.example.cardioflow.models.Measurement;
import com.example.cardioflow.sync.CloudSyncWorker;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.CollectionReference;
import com.google.gson.Gson;

public class CloudSync {
    private static final String TAG = "CloudSync";

    public static void sendRealTimeData(Context context, Measurement m) {
        FirebaseManager fm = FirebaseManager.getInstance();
        
        // According to provided rules, readings go to RTDB: device_data/$deviceId/readings
        // Using patientId as deviceId for now
        String devId = m.getPatientId();
        DatabaseReference rtdbRef = fm.getReadingsReference(devId);
        
        if (rtdbRef != null) {
            Log.d(TAG, "### CLOUD ### Sending RTDB reading for device: " + devId);
            rtdbRef.push().setValue(m)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "### CLOUD ### RTDB sync success!"))
                .addOnFailureListener(e -> Log.e(TAG, "### CLOUD ### RTDB sync fail: " + e.getMessage()));
        }

        // Keep Firestore sync as backup if collection exists
        CollectionReference coll = fm.getSensorDataCollection();
        if (coll != null) {
            Log.d(TAG, "### CLOUD ### Attempting Firestore backup for patient: " + m.getPatientId());
            coll.add(m)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "### CLOUD ### Firestore sync success! ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "### CLOUD ### Firestore sync failed", e));
        }
    }

    public static void sendAggregatedData(Context context, Measurement m) {
        FirebaseManager fm = FirebaseManager.getInstance();
        if (fm.getSensorDataCollection() != null) {
            fm.getSensorDataCollection().add(m)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Data synced to Firestore"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore sync failed, saving to offline requests", e);
                    saveToOfflineRequests(context, "sensorData", new Gson().toJson(m));
                    scheduleSyncWorker(context);
                });
        } else {
            saveToOfflineRequests(context, "sensorData", new Gson().toJson(m));
            scheduleSyncWorker(context);
        }
    }

    public static void sendAlarm(Context context, Alert alert) {
        FirebaseManager fm = FirebaseManager.getInstance();
        if (fm.getAlertsCollection() != null) {
            fm.getAlertsCollection().add(alert)
                .addOnSuccessListener(doc -> Log.d(TAG, "Alert synced to Firestore"))
                .addOnFailureListener(e -> {
                    saveToOfflineRequests(context, "alerts", new Gson().toJson(alert));
                    scheduleSyncWorker(context);
                });
        } else {
            saveToOfflineRequests(context, "alerts", new Gson().toJson(alert));
            scheduleSyncWorker(context);
        }
    }

    private static void saveToOfflineRequests(Context context, String type, String payload) {
        try {
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("url", type); // Using URL field as collection type identifier
            values.put("method", "POST");
            values.put("payload", payload);
            db.insert("offline_requests", null, values);
            db.close();
        } catch (Exception e) {
            Log.e(TAG, "Error saving offline request", e);
        }
    }

    public static void scheduleSyncWorker(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(CloudSyncWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueue(syncRequest);
    }
}
