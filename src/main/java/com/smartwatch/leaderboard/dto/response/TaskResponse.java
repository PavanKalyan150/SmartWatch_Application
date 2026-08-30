package com.smartwatch.leaderboard.dto.response;

import com.smartwatch.leaderboard.model.enums.TaskStatus;
import com.smartwatch.leaderboard.model.enums.UserTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TaskResponse {

    private Long id;
    private String name;
    private String description;
    private Long requiredLevelId;
    private String requiredLevelName;
    private String requiredMetric;
    private Double targetValue;
    private Integer rewardPoints;
    private TaskStatus status;
}