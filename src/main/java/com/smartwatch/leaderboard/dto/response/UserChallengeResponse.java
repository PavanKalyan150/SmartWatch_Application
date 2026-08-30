package com.smartwatch.leaderboard.dto.response;

import com.smartwatch.leaderboard.model.enums.UserChallengeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserChallengeResponse {

    private Long userId;
    private Long challengeId;
    private UserChallengeStatus status;
}