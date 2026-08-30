package com.smartwatch.leaderboard.dto.request;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private Long requiredLevelId;

    @NotBlank
    private String requiredMetric;

    @NotNull
    @Min(1)
    private Double targetValue;

    @NotNull
    @Min(0)
    private Integer rewardPoints;

    @NotBlank
    private String status;
}