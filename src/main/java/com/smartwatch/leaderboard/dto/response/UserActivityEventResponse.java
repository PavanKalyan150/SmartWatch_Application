package com.smartwatch.leaderboard.dto.response;

import com.smartwatch.leaderboard.model.enums.ProcessedStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserActivityEventResponse {
    private String eventId;
    private String metricType;
    private Double metricValue;
    private LocalDateTime eventTime;
    private ProcessedStatus processedStatus;
    private LocalDateTime processedAt;
}