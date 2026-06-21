package com.leaderboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;

public class TelemetryEvent {

    @JsonProperty("Step Count Value")
    private Integer stepCountValue;

    private String date; // YYYY-MM-DD format

    private Set<String> tags; // Hardware feature tags active during the activity

    public TelemetryEvent() {}

    public TelemetryEvent(Integer stepCountValue, String date, Set<String> tags) {
        this.stepCountValue = stepCountValue;
        this.date = date;
        this.tags = tags;
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
