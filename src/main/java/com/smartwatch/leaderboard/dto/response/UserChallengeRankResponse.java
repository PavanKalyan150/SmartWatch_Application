package com.smartwatch.leaderboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserChallengeRankResponse {

    private Long userId;
    private Long challengeId;
    private String status;              // JOINED, COMPLETED, RANKED
    private Double finalScore;
    private Integer rank;               // null until batch assigns it
    private Integer pointsAwarded;      // 0 for NONE reward scheme
    private LocalDateTime joinedAt;
    private LocalDateTime rankedAt;     // null until batch runs
}