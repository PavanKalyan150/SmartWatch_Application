package com.smartwatch.leaderboard.batch.config;

import com.smartwatch.leaderboard.dto.batch.ScoredParticipant;
import com.smartwatch.leaderboard.batch.processor.ScoreComputationProcessor;
import com.smartwatch.leaderboard.batch.tasklet.ChallengeExpiryTasklet;
import com.smartwatch.leaderboard.batch.tasklet.RankPublishTasklet;
import com.smartwatch.leaderboard.batch.writer.ScoreUpdateWriter;
import com.smartwatch.leaderboard.model.UserChallenge;
import com.smartwatch.leaderboard.model.enums.UserChallengeStatus;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class RankingJobConfig {

    private static final int CHUNK_SIZE = 50;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    private final ChallengeExpiryTasklet challengeExpiryTasklet;
    private final ScoreComputationProcessor scoreComputationProcessor;
    private final ScoreUpdateWriter scoreUpdateWriter;
    private final RankPublishTasklet rankPublishTasklet;

    @Bean
    public Job rankingJob() {
        return new JobBuilder("rankingJob", jobRepository)
                .start(markExpiredStep())
                .next(computeScoresStep())
                .next(rankAndPublishStep())
                .build();
    }

    @Bean
    public Step markExpiredStep() {
        return new StepBuilder("markExpiredStep", jobRepository)
                .tasklet(challengeExpiryTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step computeScoresStep() {
        return new StepBuilder("computeScoresStep", jobRepository)
                .<UserChallenge, ScoredParticipant>chunk(CHUNK_SIZE, transactionManager)
                .reader(userChallengeReader())
                .processor(scoreComputationProcessor)
                .writer(scoreUpdateWriter)
                .build();
    }

    @Bean
    public Step rankAndPublishStep() {
        return new StepBuilder("rankAndPublishStep", jobRepository)
                .tasklet(rankPublishTasklet, transactionManager)
                .build();
    }

    @Bean
    public JpaPagingItemReader<UserChallenge> userChallengeReader() {
        return new JpaPagingItemReaderBuilder<UserChallenge>()
                .name("userChallengeReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("""
                        SELECT uc FROM UserChallenge uc
                        JOIN FETCH uc.user
                        JOIN FETCH uc.challenge
                        WHERE uc.challenge.status = :status
                          AND uc.status = :ucStatus
                        """)
                .parameterValues(Map.of(
                        "status", com.smartwatch.leaderboard.model.enums.ChallengeStatus.EXPIRED,
                        "ucStatus", UserChallengeStatus.JOINED
                ))
                .pageSize(CHUNK_SIZE)
                .build();
    }
}