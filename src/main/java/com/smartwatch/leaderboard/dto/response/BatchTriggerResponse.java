package com.smartwatch.leaderboard.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class BatchTriggerResponse {

    private Long jobId;
    private String status;              // e.g. STARTED, FAILED, COMPLETED
    private LocalDateTime triggeredAt;
}