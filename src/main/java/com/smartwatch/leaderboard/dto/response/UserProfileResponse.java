package com.smartwatch.leaderboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class UserProfileResponse {

    private Long userId;
    private String fullName;
    private String email;
    private String countryCode;
    private Integer pointsBalance;
    private Long levelId;
    private String levelName;
    private List<UserTaskProgressResponse> activeTasks;
    private List<UserChallengeRankResponse> activeChallenges;
}