package com.example.cardioflow.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SensorReading {
    @SerializedName("device_id")
    public String deviceId;
    
    @SerializedName("uptime_ms")
    public long uptimeMs;
    
    public Sensors sensors;
    
    @SerializedName("hardware_status")
    public HardwareStatus hardwareStatus;

    public static class Sensors {
        @SerializedName("heart_rate_bpm")
        public int heartRate;
        
        @SerializedName("spo2_percent")
        public int spo2;
        
        @SerializedName("temperature_c")
        public double temperature;
        
        @SerializedName("humidity_percent")
        public double humidity;
        
        @SerializedName("ecg_data")
        public List<Integer> ecgData;
    }

    public static class HardwareStatus {
        @SerializedName("ecg_leads_off")
        public boolean ecgLeadsOff;
    }
}
