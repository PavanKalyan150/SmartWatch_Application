package com.smartwatch.leaderboard.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ActivityEventRequest {
    @NotBlank
    private String eventId;

    @NotBlank
    private String metricType;      // e.g. STEPS, HRM, SLEEP

    @NotNull
    @Min(0)
    private Double metricValue;

    @NotNull
    private LocalDateTime eventTime;
}