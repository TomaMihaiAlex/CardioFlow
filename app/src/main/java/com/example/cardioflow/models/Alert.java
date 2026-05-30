package com.example.cardioflow.models;

public class Alert {
    private String alertId;
    private String patientId;
    private String type; // e.g., "high_heart_rate"
    private double value;
    private String timestamp;
    private String userText;
    private String severity; // "low", "medium", "high"

    public Alert() {}

    public Alert(String alertId, String patientId, String type, double value, String timestamp, String userText, String severity) {
        this.alertId = alertId;
        this.patientId = patientId;
        this.type = type;
        this.value = value;
        this.timestamp = timestamp;
        this.userText = userText;
        this.severity = severity;
    }

    // Getteri și setteri
    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getUserText() { return userText; }
    public void setUserText(String userText) { this.userText = userText; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
}