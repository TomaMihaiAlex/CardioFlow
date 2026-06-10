package com.example.cardioflow.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Optimized model for the actual hardware JSON format:
 * {"patientId":"1","valid":false,"heartRate":-1,"spo2":-1,"temperature":29.3,"humidity":47,"ecgSamples":[...]}
 */
public class SensorReading {
    @SerializedName("patientId")
    public String patientId;

    @SerializedName("valid")
    public boolean valid;

    @SerializedName("heartRate")
    public int heartRate;

    @SerializedName("spo2")
    public int spo2;

    @SerializedName("temperature")
    public double temperature;

    @SerializedName("humidity")
    public double humidity;

    @SerializedName("ecgSamples")
    public List<Integer> ecgSamples;

    // Compatibility getters to avoid breaking BLEReceiverService logic
    public Sensors getSensors() {
        Sensors s = new Sensors();
        s.heartRate = this.heartRate;
        s.spo2 = this.spo2;
        s.temperature = this.temperature;
        s.humidity = this.humidity;
        s.ecgData = this.ecgSamples;
        return s;
    }

    public static class Sensors {
        public int heartRate;
        public int spo2;
        public double temperature;
        public double humidity;
        public List<Integer> ecgData;
    }
}
