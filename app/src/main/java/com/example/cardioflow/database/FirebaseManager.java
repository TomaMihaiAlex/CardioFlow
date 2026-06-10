package com.example.cardioflow.database;

import android.content.Context;
import android.util.Log;

import com.example.cardioflow.R;
import com.example.cardioflow.models.Recommendation;
import com.example.cardioflow.models.Thresholds;
import com.example.cardioflow.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private static FirebaseManager instance;
    private final FirebaseFirestore db;
    private final FirebaseDatabase rtdb;
    private final FirebaseAuth auth;

    public static void initializeManual(Context context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setApiKey(context.getString(R.string.firebase_api_key))
                        .setApplicationId(context.getString(R.string.firebase_application_id))
                        .setProjectId(context.getString(R.string.firebase_project_id))
                        .setDatabaseUrl(context.getString(R.string.firebase_database_url))
                        .setStorageBucket(context.getString(R.string.firebase_storage_bucket))
                        .build();

                FirebaseApp.initializeApp(context, options);
                Log.d(TAG, "### CLOUD ### Firebase initialized manually from secrets.xml");
            } else {
                Log.d(TAG, "### CLOUD ### Firebase already initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "### CLOUD ### Manual initialization failed: " + e.getMessage());
        }
    }

    private FirebaseManager() {
        FirebaseFirestore dbInstance = null;
        FirebaseDatabase rtdbInstance = null;
        FirebaseAuth authInstance = null;
        try {
            dbInstance = FirebaseFirestore.getInstance();
            rtdbInstance = FirebaseDatabase.getInstance();
            authInstance = FirebaseAuth.getInstance();
            Log.d(TAG, "### CLOUD ### Firestore/RTDB/Auth instances obtained");
        } catch (IllegalStateException e) {
            Log.e(TAG, "### CLOUD ### Firebase not initialized: " + e.getMessage());
        }
        this.db = dbInstance;
        this.rtdb = rtdbInstance;
        this.auth = authInstance;
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public DatabaseReference getReadingsReference(String deviceId) {
        if (rtdb == null) {
            Log.e(TAG, "### CLOUD ### getReadingsReference: rtdb is NULL");
            return null;
        }
        return rtdb.getReference("device_data").child(deviceId).child("readings");
    }

    public void listenForLiveReadings(String patientId, ReadingsCallback callback) {
        DatabaseReference ref = getReadingsReference(patientId);
        if (ref == null) return;

        ref.orderByKey().limitToLast(1).addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                    com.example.cardioflow.models.Measurement m = child.getValue(com.example.cardioflow.models.Measurement.class);
                    if (m != null) callback.onReadingReceived(m);
                }
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                Log.e(TAG, "Listen failed: " + error.getMessage());
            }
        });
    }

    public interface ReadingsCallback {
        void onReadingReceived(com.example.cardioflow.models.Measurement measurement);
    }

    // Collections
    public CollectionReference getUsersCollection() { 
        return db != null ? db.collection("users") : null; 
    }
    public CollectionReference getSensorDataCollection() { 
        if (db == null) {
            Log.e(TAG, "### CLOUD ### getSensorDataCollection: db is NULL");
            return null;
        }
        Log.d(TAG, "### CLOUD ### Accessing sensorData collection");
        return db.collection("sensorData");
    }
    public CollectionReference getAlertsCollection() { 
        return db != null ? db.collection("alerts") : null; 
    }
    public CollectionReference getRecommendationsCollection() { 
        return db != null ? db.collection("recommendations") : null; 
    }
    public CollectionReference getThresholdsCollection() { 
        return db != null ? db.collection("thresholds") : null; 
    }

    // User Profile
    public void saveUser(User user) {
        if (db == null) return;
        getUsersCollection().document(user.getId()).set(user)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User saved successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving user", e));
    }

    // Thresholds Sync
    public void saveThresholds(Thresholds thresholds) {
        if (db == null) return;
        getThresholdsCollection().document(thresholds.getPatientId()).set(thresholds)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Thresholds saved to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving thresholds", e));
    }

    public void listenForThresholds(String patientId, ThresholdsCallback callback) {
        if (db == null) return;
        getThresholdsCollection().document(patientId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed", e);
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        Thresholds thresholds = snapshot.toObject(Thresholds.class);
                        callback.onThresholdsReceived(thresholds);
                    }
                });
    }

    // Recommendations Sync
    public void saveRecommendation(Recommendation rec) {
        if (db == null) return;
        getRecommendationsCollection().document(rec.getRecommendationId()).set(rec)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Recommendation saved to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving recommendation", e));
    }

    public void deleteRecommendation(String id) {
        if (db == null) return;
        getRecommendationsCollection().document(id).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Recommendation deleted from Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting recommendation", e));
    }

    public void listenForRecommendations(String patientId, RecommendationsCallback callback) {
        if (db == null) return;
        getRecommendationsCollection().whereEqualTo("patientId", patientId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed", e);
                        return;
                    }
                    if (snapshots != null) {
                        List<Recommendation> recommendations = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots) {
                            recommendations.add(doc.toObject(Recommendation.class));
                        }
                        callback.onRecommendationsReceived(recommendations);
                    }
                });
    }

    public interface ThresholdsCallback {
        void onThresholdsReceived(Thresholds thresholds);
    }

    public interface RecommendationsCallback {
        void onRecommendationsReceived(List<Recommendation> recommendations);
    }
}
