package com.example.cardioflow.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.cardioflow.models.Measurement;
import com.example.cardioflow.models.Alert;
import com.example.cardioflow.models.Recommendation;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static DatabaseManager instance;
    private DatabaseHelper dbHelper;

    private DatabaseManager(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public static synchronized DatabaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseManager(context.getApplicationContext());
        }
        return instance;
    }

    // Sensor Data
    public void insertSensorData(int hr, int spo2, double temp, double hum) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("heart_rate", hr);
        values.put("spo2", spo2);
        values.put("temperature", temp);
        values.put("humidity", hum);
        db.insert("sensor_data", null, values);
    }

    public List<Measurement> getLastReadings(int limit) {
        List<Measurement> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("sensor_data", null, null, null, null, null, "timestamp DESC", String.valueOf(limit));
        if (cursor.moveToFirst()) {
            do {
                Measurement m = new Measurement();
                m.setHeartRate(cursor.getInt(cursor.getColumnIndexOrThrow("heart_rate")));
                m.setSpo2(cursor.getInt(cursor.getColumnIndexOrThrow("spo2")));
                m.setTemperature(cursor.getDouble(cursor.getColumnIndexOrThrow("temperature")));
                m.setHumidity(cursor.getDouble(cursor.getColumnIndexOrThrow("humidity")));
                m.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow("timestamp")));
                list.add(m);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // Alerts
    public void insertAlert(Alert alert) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("type", alert.getType());
        values.put("value", alert.getValue());
        values.put("severity", alert.getSeverity());
        values.put("patient_id", alert.getPatientId());
        values.put("user_text", alert.getUserText());
        db.insert("alerts_local", null, values);
    }

    public List<Alert> getAllAlerts() {
        List<Alert> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("alerts_local", null, null, null, null, null, "timestamp DESC");
        if (cursor.moveToFirst()) {
            do {
                Alert a = new Alert();
                a.setAlertId(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("id"))));
                a.setType(cursor.getString(cursor.getColumnIndexOrThrow("type")));
                a.setValue(cursor.getDouble(cursor.getColumnIndexOrThrow("value")));
                a.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow("timestamp")));
                a.setSeverity(cursor.getString(cursor.getColumnIndexOrThrow("severity")));
                a.setPatientId(cursor.getString(cursor.getColumnIndexOrThrow("patient_id")));
                a.setUserText(cursor.getString(cursor.getColumnIndexOrThrow("user_text")));
                list.add(a);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public void updateAlertUserText(String alertId, String text) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_text", text);
        db.update("alerts_local", values, "id = ?", new String[]{alertId});
    }

    // Recommendations Status
    public void setRecommendationDone(String recId, String date, boolean done) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("recommendation_id", recId);
        values.put("date", date);
        values.put("is_done", done ? 1 : 0);
        db.insertWithOnConflict("recommendations_status", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public boolean isRecommendationDone(String recId, String date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("recommendations_status", new String[]{"is_done"},
                "recommendation_id = ? AND date = ?", new String[]{recId, date}, null, null, null);
        boolean done = false;
        if (cursor.moveToFirst()) {
            done = cursor.getInt(0) == 1;
        }
        cursor.close();
        return done;
    }

    // Recommendations
    public void insertRecommendation(Recommendation r) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", r.getRecommendationId());
        values.put("patient_id", r.getPatientId());
        values.put("doctor_id", r.getDoctorId());
        values.put("type", r.getType());
        values.put("duration", r.getDailyDurationMin());
        values.put("instructions", r.getInstructions());
        values.put("severity", r.getSeverity());
        db.insertWithOnConflict("recommendations", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<Recommendation> getRecommendationsForPatient(String patientId) {
        List<Recommendation> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("recommendations", null, "patient_id = ?", new String[]{patientId}, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                Recommendation r = new Recommendation();
                r.setRecommendationId(cursor.getString(cursor.getColumnIndexOrThrow("id")));
                r.setPatientId(cursor.getString(cursor.getColumnIndexOrThrow("patient_id")));
                r.setDoctorId(cursor.getString(cursor.getColumnIndexOrThrow("doctor_id")));
                r.setType(cursor.getString(cursor.getColumnIndexOrThrow("type")));
                r.setDailyDurationMin(cursor.getInt(cursor.getColumnIndexOrThrow("duration")));
                r.setInstructions(cursor.getString(cursor.getColumnIndexOrThrow("instructions")));
                r.setSeverity(cursor.getString(cursor.getColumnIndexOrThrow("severity")));
                list.add(r);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public void deleteRecommendation(String id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("recommendations", "id = ?", new String[]{id});
    }

    public void clearAllData() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("DELETE FROM sensor_data");
        db.execSQL("DELETE FROM offline_requests");
        db.execSQL("DELETE FROM alerts_local");
        db.execSQL("DELETE FROM recommendations_status");
    }
}