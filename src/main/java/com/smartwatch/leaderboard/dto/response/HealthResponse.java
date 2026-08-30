package com.smartwatch.leaderboard.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HealthResponse {
    private String status;
    private Instant timestamp;
    private Map<String, ComponentHealth> components;

    @JsonIgnore
    public boolean isHealthy() {
        return "UP".equals(status);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentHealth {
        private String status;
        private String details;
    }
}