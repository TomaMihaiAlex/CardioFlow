package com.example.cardioflow.database;

import android.content.Context;
import android.util.Log;

import com.example.cardioflow.models.Recommendation;
import com.example.cardioflow.models.Thresholds;
import com.example.cardioflow.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private static FirebaseManager instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private FirebaseManager() {
        FirebaseFirestore dbInstance = null;
        FirebaseAuth authInstance = null;
        try {
            dbInstance = FirebaseFirestore.getInstance();
            authInstance = FirebaseAuth.getInstance();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Firebase not initialized: " + e.getMessage());
        }
        this.db = dbInstance;
        this.auth = authInstance;
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    // Collections
    public CollectionReference getUsersCollection() { 
        return db != null ? db.collection("users") : null; 
    }
    public CollectionReference getSensorDataCollection() { 
        return db != null ? db.collection("sensorData") : null; 
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
