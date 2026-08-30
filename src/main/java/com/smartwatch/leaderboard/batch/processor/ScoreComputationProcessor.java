package com.smartwatch.leaderboard.batch.processor;

import com.smartwatch.leaderboard.dto.batch.ScoredParticipant;
import com.smartwatch.leaderboard.model.UserChallenge;
import com.smartwatch.leaderboard.model.enums.UserTaskStatus;
import com.smartwatch.leaderboard.repository.ChallengeTaskRepository;
import com.smartwatch.leaderboard.repository.UserTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScoreComputationProcessor implements ItemProcessor<UserChallenge, ScoredParticipant> {

    private final ChallengeTaskRepository challengeTaskRepository;
    private final UserTaskRepository userTaskRepository;

    @Override
    public ScoredParticipant process(UserChallenge uc) {
        Long challengeId = uc.getChallenge().getId();
        Long userId = uc.getUser().getId();
        LocalDateTime start = uc.getChallenge().getStartTime();
        LocalDateTime end = uc.getChallenge().getEndTime();

        List<Long> taskIds = challengeTaskRepository.findTaskIdsByChallengeId(challengeId);

        if (taskIds.isEmpty()) {
            log.warn("Challenge {} has no tasks — scoring zero for user {}", challengeId, userId);
            return ScoredParticipant.builder()
                    .userChallenge(uc)
                    .finalScore(0.0)
                    .lastCompletionAt(null)
                    .build();
        }

        Double score = userTaskRepository.sumPointsForUserInScope(
                userId, taskIds, UserTaskStatus.COMPLETED, start, end);

        LocalDateTime lastCompletion = userTaskRepository.findLastCompletionInScope(
                userId, taskIds, UserTaskStatus.COMPLETED, start, end);

        log.debug("User {} | Challenge {} | Score={} | LastCompletion={}",
                userId, challengeId, score, lastCompletion);

        return ScoredParticipant.builder()
                .userChallenge(uc)
                .finalScore(score != null ? score : 0.0)
                .lastCompletionAt(lastCompletion)
                .build();
    }
}