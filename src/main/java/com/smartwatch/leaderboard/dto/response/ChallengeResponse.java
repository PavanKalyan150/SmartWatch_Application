package com.smartwatch.leaderboard.dto.response;

import com.smartwatch.leaderboard.model.enums.ChallengeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChallengeResponse {

    private Long id;
    private String name;
    private String description;
    private Long requiredLevelId;
    private String requiredLevelName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ChallengeStatus status;
    private String rewardScheme;
    private Long createdByUserId;       // null means admin-curated
    private List<TaskResponse> tasks;   // tasks associated via challenge_task
}