package com.leaderboard.dto;

import java.util.Set;

public class UserTelemetryMessage {
    private Long userId;
    private Integer stepCountValue;
    private String date;
    private Set<String> tags;

    public UserTelemetryMessage() {}

    public UserTelemetryMessage(Long userId, Integer stepCountValue, String date, Set<String> tags) {
        this.userId = userId;
        this.stepCountValue = stepCountValue;
        this.date = date;
        this.tags = tags;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getStepCountValue() {
        return stepCountValue;
    }

    public void setStepCountValue(Integer stepCountValue) {
        this.stepCountValue = stepCountValue;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
}
