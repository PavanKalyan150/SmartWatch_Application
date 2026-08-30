package com.smartwatch.leaderboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ActivityIngestionResponse {

    private String eventId;
    private String message;
    private String status;
    // mirrors processed_status: PENDING
    private Long userId;
}