package com.smartwatch.leaderboard.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ChallengeRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private Long requiredLevelId;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    @NotBlank
    private String status;

    @NotBlank
    private String rewardScheme;

    // Task IDs to associate via challenge_task
    @NotEmpty
    private List<Long> taskIds;
}
