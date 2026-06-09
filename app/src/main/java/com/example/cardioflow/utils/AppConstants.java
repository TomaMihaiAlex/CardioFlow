package com.example.cardioflow.utils;

public class AppConstants {
    // SharedPreferences
    public static final String PREFS_NAME = "CardioFlowPrefs";
    public static final String KEY_BLE_INTERVAL = "ble_interval";
    public static final String KEY_THEME = "ui_theme";
    
    // Theme values
    public static final int THEME_STANDARD = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;
    
    // Intent Extras
    public static final String EXTRA_PATIENT_ID = "patientId";
    
    // Threshold Defaults
    public static final int DEFAULT_HR_MIN = 50;
    public static final int DEFAULT_HR_MAX = 100;
    public static final int DEFAULT_SPO2_MIN = 90;
    public static final double DEFAULT_TEMP_MIN = 35.5;
    public static final double DEFAULT_TEMP_MAX = 37.5;
    public static final int DEFAULT_HUM_MIN = 30;
    public static final int DEFAULT_HUM_MAX = 70;
    public static final int DEFAULT_PERSIST_SECONDS = 10;
    public static final int DEFAULT_ACTIVITY_INTERVAL_MIN = 5;

    // Database
    public static final String DATABASE_NAME = "cardioflow.db";
    public static final int DATABASE_VERSION = 3;
    
    // Roles
    public static final String ROLE_DOCTOR = "Doctor";
    public static final String ROLE_PATIENT = "Patient";
    
    // Simulation Intervals
    public static final int SIMULATION_INTERVAL_MS = 10000;
    public static final int SYNC_INTERVAL_MS = 30000;
}