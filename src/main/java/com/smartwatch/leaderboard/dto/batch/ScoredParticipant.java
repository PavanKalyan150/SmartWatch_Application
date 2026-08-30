package com.smartwatch.leaderboard.dto.batch;

import com.smartwatch.leaderboard.model.UserChallenge;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ScoredParticipant {
    private UserChallenge userChallenge;
    private Double finalScore;
    private LocalDateTime lastCompletionAt;
}