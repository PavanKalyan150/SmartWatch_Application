package com.smartwatch.leaderboard.batch.config;

import com.smartwatch.leaderboard.batch.processor.LevelUpProcessor;
import com.smartwatch.leaderboard.batch.writer.LevelUpWriter;
import com.smartwatch.leaderboard.model.User;
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

@Configuration
@RequiredArgsConstructor
public class GamificationJobConfig {

    private static final int CHUNK_SIZE = 100;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final LevelUpProcessor levelUpProcessor;
    private final LevelUpWriter levelUpWriter;

    @Bean
    public Job gamificationJob() {
        return new JobBuilder("gamificationJob", jobRepository)
                .start(levelUpStep())
                .build();
    }

    @Bean
    public Step levelUpStep() {
        return new StepBuilder("levelUpStep", jobRepository)
                .<User, User>chunk(CHUNK_SIZE, transactionManager)
                .reader(userLevelReader())
                .processor(levelUpProcessor)
                .writer(levelUpWriter)
                .build();
    }

    @Bean
    public JpaPagingItemReader<User> userLevelReader() {
        return new JpaPagingItemReaderBuilder<User>()
                .name("userLevelReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT u FROM User u LEFT JOIN FETCH u.level ORDER BY u.id")
                .pageSize(CHUNK_SIZE)
                .build();
    }
}