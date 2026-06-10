package com.example.cardioflow.utils;

public class AppConstants {
    // SharedPreferences
    public static final String PREFS_NAME = "CardioFlowPrefs";
    public static final String KEY_BLE_INTERVAL = "ble_interval";
    public static final String KEY_THEME = "ui_theme";
    public static final String KEY_LAST_MAC = "last_ble_mac";
    public static final String KEY_SIMULATION_MODE = "simulation_mode";

    // BLE UUIDs
    public static final String UART_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static final String TX_CHAR_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static final String CCCD_UUID = "00002902-0000-1000-8000-00805f9b34fb";

    // Nordic UART Service (Common for ESP32, Arduino, etc.)
    public static final String NORDIC_UART_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String NORDIC_TX_CHAR = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    
    // Custom Hardware UUIDs (from user's screenshot)
    public static final String CUSTOM_SERVICE_UUID = "12345678-1234-1234-1234-123456789abc";
    public static final String CUSTOM_TX_CHAR_UUID = "12345678-1234-1234-1234-000000000001";
    
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