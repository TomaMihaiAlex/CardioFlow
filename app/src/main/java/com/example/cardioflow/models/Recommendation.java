package com.example.cardioflow.models;

public class Recommendation {
    private String recommendationId;
    private String patientId;
    private String doctorId;
    private String type; // e.g., "bicicletă"
    private int dailyDurationMin;
    private String instructions;
    private String severity;

    public Recommendation() {}

    public Recommendation(String recommendationId, String patientId, String doctorId, String type, int dailyDurationMin, String instructions, String severity) {
        this.recommendationId = recommendationId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.type = type;
        this.dailyDurationMin = dailyDurationMin;
        this.instructions = instructions;
        this.severity = severity;
    }

    // Getteri și setteri
    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getDailyDurationMin() { return dailyDurationMin; }
    public void setDailyDurationMin(int dailyDurationMin) { this.dailyDurationMin = dailyDurationMin; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
}