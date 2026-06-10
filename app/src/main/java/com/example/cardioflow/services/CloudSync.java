package com.example.cardioflow.services;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.cardioflow.database.DatabaseManager;
import com.example.cardioflow.database.FirebaseManager;
import com.example.cardioflow.models.Alert;
import com.example.cardioflow.models.Measurement;
import com.example.cardioflow.sync.CloudSyncWorker;
import com.google.firebase.firestore.CollectionReference;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Trimite datele agregate (media citirilor) și alarmele în Firebase Firestore.
 *
 * Strategie de sincronizare offline:
 *  - dacă Firestore e disponibil → scrie direct (Firestore are propriul cache offline);
 *  - dacă Firebase NU e configurat încă (lipsește google-services.json → FirebaseManager
 *    întoarce null) sau scrierea eșuează → cererea se pune în coada SQLite `offline_requests`
 *    și se programează {@link CloudSyncWorker} (WorkManager) pentru a o trimite mai târziu.
 *
 * Numele colecțiilor sunt cele expuse de {@link FirebaseManager}: "sensorData" și "alerts".
 */
public class CloudSync {
    private static final String TAG = "CloudSync";

    public static final String COLLECTION_SENSOR_DATA = "sensorData";
    public static final String COLLECTION_ALERTS = "alerts";
    public static final String TYPE_MEASUREMENT = "Measurement";
    public static final String TYPE_ALERT = "Alert";

    private static final String SYNC_WORK_NAME = "cloud_sync_offline_flush";
    private static final Gson gson = new Gson();

    public static void sendAggregatedData(Context context, Measurement m) {
        send(context, COLLECTION_SENSOR_DATA, TYPE_MEASUREMENT, gson.toJson(m));
    }

    public static void sendAlarm(Context context, Alert alert) {
        send(context, COLLECTION_ALERTS, TYPE_ALERT, gson.toJson(alert));
    }

    private static void send(Context context, String collection, String type, String json) {
        CollectionReference ref = collectionFor(collection);

        // Firebase neconfigurat (google-services.json lipsă) → coadă offline.
        if (ref == null) {
            Log.w(TAG, "Firestore indisponibil, pun în coada offline: " + collection);
            queueOffline(context, collection, type, json);
            return;
        }

        Map<String, Object> data = buildDocument(type, json);
        ref.add(data)
                .addOnSuccessListener(docRef ->
                        Log.d(TAG, "Trimis în " + collection + ": " + docRef.getId()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Eșec trimitere " + collection + ", pun în coada offline", e);
                    queueOffline(context, collection, type, json);
                });
    }

    private static void queueOffline(Context context, String collection, String type, String json) {
        DatabaseManager.getInstance(context).insertOfflineRequest(collection, type, json);
        enqueueFlushWorker(context);
    }

    /** Programează un job WorkManager care golește coada offline când există rețea. */
    public static void enqueueFlushWorker(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(CloudSyncWorker.class)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(context)
                .enqueueUniqueWork(SYNC_WORK_NAME, ExistingWorkPolicy.KEEP, request);
    }

    /** Întoarce referința către colecția Firestore sau null dacă Firebase nu e configurat. */
    public static CollectionReference collectionFor(String collection) {
        FirebaseManager fm = FirebaseManager.getInstance();
        if (COLLECTION_ALERTS.equals(collection)) {
            return fm.getAlertsCollection();
        }
        return fm.getSensorDataCollection();
    }

    /**
     * Construiește documentul Firestore cu tipuri corecte (int/double rămân numerice).
     * Folosit atât la trimiterea directă cât și de {@link CloudSyncWorker} la golirea cozii.
     */
    public static Map<String, Object> buildDocument(String type, String json) {
        Map<String, Object> data = new HashMap<>();
        if (TYPE_ALERT.equals(type)) {
            Alert a = gson.fromJson(json, Alert.class);
            data.put("alertId", a.getAlertId());
            data.put("patientId", a.getPatientId());
            data.put("type", a.getType());
            data.put("value", a.getValue());
            data.put("severity", a.getSeverity());
            data.put("userText", a.getUserText());
            data.put("timestamp", a.getTimestamp());
            data.put("createdAt", System.currentTimeMillis());
        } else {
            Measurement m = gson.fromJson(json, Measurement.class);
            data.put("patientId", m.getPatientId());
            data.put("heartRate", m.getHeartRate());
            data.put("spo2", m.getSpo2());
            data.put("temperature", m.getTemperature());
            data.put("humidity", m.getHumidity());
            data.put("timestamp", m.getTimestamp());
            data.put("createdAt", System.currentTimeMillis());
        }
        return data;
    }
}
