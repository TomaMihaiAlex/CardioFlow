package com.example.cardioflow.database;

public class RecommendationStatus {
    private String recommendationId;
    private String date;
    private boolean isDone;

    public RecommendationStatus(String recommendationId, String date, boolean isDone) {
        this.recommendationId = recommendationId;
        this.date = date;
        this.isDone = isDone;
    }

    public String getRecommendationId() { return recommendationId; }
    public String getDate() { return date; }
    public boolean isDone() { return isDone; }
}