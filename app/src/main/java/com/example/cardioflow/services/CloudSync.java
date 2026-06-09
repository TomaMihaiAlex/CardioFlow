package com.example.cardioflow.services;

import android.content.Context;
import android.util.Log;
import com.example.cardioflow.models.Alert;
import com.example.cardioflow.models.Measurement;
import com.google.gson.Gson;

public class CloudSync {
    private static final String TAG = "CloudSync";
    private static final String BASE_URL = "https://api.cardioflow.com"; // Placeholder

    public static void sendAggregatedData(Context context, Measurement m) {
        String json = new Gson().toJson(m);
        Log.d(TAG, "Simulare POST /api/data: " + json);
        // Aici s-ar adăuga logica de Retrofit și salvare în offline_requests în caz de eșec
    }

    public static void sendAlarm(Context context, Alert alert) {
        String json = new Gson().toJson(alert);
        Log.d(TAG, "Simulare POST /api/alarm: " + json);
        // Aici s-ar adăuga logica de Retrofit
    }
}