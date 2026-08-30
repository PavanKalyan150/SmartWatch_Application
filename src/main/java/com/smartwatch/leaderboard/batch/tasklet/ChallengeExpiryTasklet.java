package com.smartwatch.leaderboard.batch.tasklet;

import com.smartwatch.leaderboard.model.enums.ChallengeStatus;
import com.smartwatch.leaderboard.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeExpiryTasklet implements Tasklet {

    private final ChallengeRepository challengeRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        int expired = challengeRepository.markExpiredChallenges(
                ChallengeStatus.ACTIVE,
                ChallengeStatus.EXPIRED,
                LocalDateTime.now()
        );
        log.info("Marked {} challenge(s) as EXPIRED", expired);
        return RepeatStatus.FINISHED;
    }
}