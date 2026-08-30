package com.smartwatch.leaderboard.dto.response;
import com.smartwatch.leaderboard.model.enums.TaskStatus;
import com.smartwatch.leaderboard.model.enums.UserTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class UserTaskProgressResponse {

    private Long taskId;
    private Long userId;
    private String taskName;
    private String requiredMetric;
    private Double targetValue;
    private Double progressValue;
    private UserTaskStatus status;              // IN_PROGRESS, COMPLETED
    private Integer rewardPoints;
    private LocalDateTime completedAt;  // null if still in progress
}