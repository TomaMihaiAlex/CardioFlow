package com.example.cardioflow.models;

public class Thresholds {
    private String patientId;
    private int hrMin;
    private int hrMax;
    private int spo2Min;
    private double tempMin;
    private double tempMax;
    private double humMin;
    private double humMax;
    private int persistSeconds;
    private int activityIntervalMinutes;

    public Thresholds() {}

    public Thresholds(String patientId, int hrMin, int hrMax, int spo2Min, double tempMin, double tempMax, double humMin, double humMax, int persistSeconds, int activityIntervalMinutes) {
        this.patientId = patientId;
        this.hrMin = hrMin;
        this.hrMax = hrMax;
        this.spo2Min = spo2Min;
        this.tempMin = tempMin;
        this.tempMax = tempMax;
        this.humMin = humMin;
        this.humMax = humMax;
        this.persistSeconds = persistSeconds;
        this.activityIntervalMinutes = activityIntervalMinutes;
    }

    // Getteri și setteri
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public int getHrMin() { return hrMin; }
    public void setHrMin(int hrMin) { this.hrMin = hrMin; }
    public int getHrMax() { return hrMax; }
    public void setHrMax(int hrMax) { this.hrMax = hrMax; }
    public int getSpo2Min() { return spo2Min; }
    public void setSpo2Min(int spo2Min) { this.spo2Min = spo2Min; }
    public double getTempMin() { return tempMin; }
    public void setTempMin(double tempMin) { this.tempMin = tempMin; }
    public double getTempMax() { return tempMax; }
    public void setTempMax(double tempMax) { this.tempMax = tempMax; }
    public double getHumMin() { return humMin; }
    public void setHumMin(double humMin) { this.humMin = humMin; }
    public double getHumMax() { return humMax; }
    public void setHumMax(double humMax) { this.humMax = humMax; }
    public int getPersistSeconds() { return persistSeconds; }
    public void setPersistSeconds(int persistSeconds) { this.persistSeconds = persistSeconds; }
    public int getActivityIntervalMinutes() { return activityIntervalMinutes; }
    public void setActivityIntervalMinutes(int activityIntervalMinutes) { this.activityIntervalMinutes = activityIntervalMinutes; }
}