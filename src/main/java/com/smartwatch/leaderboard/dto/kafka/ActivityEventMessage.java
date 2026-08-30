package com.smartwatch.leaderboard.dto.kafka;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ActivityEventMessage {
    private Long userId;
    private String eventId;
    private String metricType;
    private Double metricValue;
    private LocalDateTime eventTime;
}
