package com.example.cardioflow.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.cardioflow.utils.AppConstants;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = AppConstants.DATABASE_NAME;
    private static final int DATABASE_VERSION = AppConstants.DATABASE_VERSION;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Table sensor_data
        db.execSQL("CREATE TABLE sensor_data (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "heart_rate INTEGER, " +
                "spo2 INTEGER, " +
                "temperature REAL, " +
                "humidity REAL, " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");

        // Table offline_requests
        db.execSQL("CREATE TABLE offline_requests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "url TEXT, " +
                "method TEXT, " +
                "payload TEXT, " +
                "retry_count INTEGER DEFAULT 0)");

        // Table alerts_local
        db.execSQL("CREATE TABLE alerts_local (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "type TEXT, " +
                "value REAL, " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "severity TEXT, " +
                "patient_id TEXT, " +
                "user_text TEXT)");

        // Table recommendations_status
        db.execSQL("CREATE TABLE recommendations_status (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "recommendation_id TEXT, " +
                "date TEXT, " +
                "is_done INTEGER DEFAULT 0)");

        // Table recommendations
        db.execSQL("CREATE TABLE recommendations (" +
                "id TEXT PRIMARY KEY, " +
                "patient_id TEXT, " +
                "doctor_id TEXT, " +
                "type TEXT, " +
                "duration INTEGER, " +
                "instructions TEXT, " +
                "severity TEXT)");

        // Table thresholds
        db.execSQL("CREATE TABLE thresholds (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "patient_id TEXT UNIQUE, " +
                "hr_min INTEGER, " +
                "hr_max INTEGER, " +
                "spo2_min INTEGER, " +
                "temp_min REAL, " +
                "temp_max REAL, " +
                "hum_min REAL, " +
                "hum_max REAL, " +
                "persistence INTEGER, " +
                "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Version 2 added recommendations
            db.execSQL("CREATE TABLE IF NOT EXISTS recommendations (" +
                    "id TEXT PRIMARY KEY, " +
                    "patient_id TEXT, " +
                    "doctor_id TEXT, " +
                    "type TEXT, " +
                    "duration INTEGER, " +
                    "instructions TEXT, " +
                    "severity TEXT)");
        }
        if (oldVersion < 3) {
            // Version 3 added thresholds
            db.execSQL("CREATE TABLE IF NOT EXISTS thresholds (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "patient_id TEXT UNIQUE, " +
                    "hr_min INTEGER, " +
                    "hr_max INTEGER, " +
                    "spo2_min INTEGER, " +
                    "temp_min REAL, " +
                    "temp_max REAL, " +
                    "hum_min REAL, " +
                    "hum_max REAL, " +
                    "persistence INTEGER, " +
                    "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        }
    }
}