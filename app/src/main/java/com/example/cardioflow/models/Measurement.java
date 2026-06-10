package com.example.cardioflow.models;

import com.google.gson.annotations.SerializedName;

public class Measurement {
    private String patientId;
    private String timestamp; // ISO 8601
    private int heartRate;
    private int spo2;
    private double temperature;
    private double humidity;
    private java.util.List<Integer> ecgSamples;

    public Measurement() {}

    public Measurement(String patientId, String timestamp, int heartRate, int spo2, double temperature, double humidity) {
        this.patientId = patientId;
        this.timestamp = timestamp;
        this.heartRate = heartRate;
        this.spo2 = spo2;
        this.temperature = temperature;
        this.humidity = humidity;
    }

    public Measurement(String patientId, String timestamp, int heartRate, int spo2, double temperature, double humidity, java.util.List<Integer> ecgSamples) {
        this(patientId, timestamp, heartRate, spo2, temperature, humidity);
        this.ecgSamples = ecgSamples;
    }

    // Getteri și setteri
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public int getHeartRate() { return heartRate; }
    public void setHeartRate(int heartRate) { this.heartRate = heartRate; }
    public int getSpo2() { return spo2; }
    public void setSpo2(int spo2) { this.spo2 = spo2; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }
    public java.util.List<Integer> getEcgSamples() { return ecgSamples; }
    public void setEcgSamples(java.util.List<Integer> ecgSamples) { this.ecgSamples = ecgSamples; }
}