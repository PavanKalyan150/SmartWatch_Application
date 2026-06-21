package com.leaderboard.config;

import com.leaderboard.model.*;
import com.leaderboard.repository.*;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final UserRepository userRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final BadgeRepository badgeRepository;

    public BatchConfig(JobRepository jobRepository,
                       PlatformTransactionManager transactionManager,
                       ChallengeRepository challengeRepository,
                       UserChallengeRepository userChallengeRepository,
                       UserRepository userRepository,
                       LeaderboardRepository leaderboardRepository,
                       BadgeRepository badgeRepository) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.challengeRepository = challengeRepository;
        this.userChallengeRepository = userChallengeRepository;
        this.userRepository = userRepository;
        this.leaderboardRepository = leaderboardRepository;
        this.badgeRepository = badgeRepository;
    }

    // ==========================================
    // 1. AUTOMATED RANKING BATCH JOB
    // ==========================================

    @Bean
    public Job rankingJob() {
        return new JobBuilder("rankingJob", jobRepository)
                .start(rankingStep())
                .build();
    }

    @Bean
    public Step rankingStep() {
        return new StepBuilder("rankingStep", jobRepository)
                .<Challenge, List<Leaderboard>>chunk(5, transactionManager)
                .reader(challengeReader())
                .processor(challengeProcessor())
                .writer(challengeWriter())
                .build();
    }

    @Bean
    public ItemReader<Challenge> challengeReader() {
        return new ItemReader<>() {
            private List<Challenge> expiredChallenges;
            private int index = 0;

            @Override
            public Challenge read() {
                if (expiredChallenges == null) {
                    expiredChallenges = challengeRepository.findByExpiryDateBeforeAndIsProcessed(LocalDateTime.now(), false);
                }
                if (index < expiredChallenges.size()) {
                    return expiredChallenges.get(index++);
                }
                return null; // end of dataset
            }
        };
    }

    @Bean
    public ItemProcessor<Challenge, List<Leaderboard>> challengeProcessor() {
        return challenge -> {
            List<UserChallenge> participants = userChallengeRepository.findByChallengeId(challenge.getId());
            // Sort participants by score descending
            participants.sort((uc1, uc2) -> uc2.getScore().compareTo(uc1.getScore()));

            List<Leaderboard> leaderboards = new ArrayList<>();
            int rank = 1;
            for (UserChallenge uc : participants) {
                uc.setRank(rank);

                // Award points only to completed participants
                if (Boolean.TRUE.equals(uc.getCompleted())) {
                    User user = uc.getUser();
                    int pointsToAward = challenge.getPointsReward();

                    // Bonuses for top positions
                    if (rank == 1) {
                        pointsToAward += 100;
                    } else if (rank == 2) {
                        pointsToAward += 50;
                    }

                    uc.setPointsAwarded(pointsToAward);
                    user.setPoints(user.getPoints() + pointsToAward);
                    userRepository.save(user);
                }

                userChallengeRepository.save(uc);

                Leaderboard entry = new Leaderboard(challenge, uc.getUser(), rank, uc.getScore());
                leaderboards.add(entry);
                rank++;
            }

            challenge.setIsProcessed(true);
            challengeRepository.save(challenge);
            return leaderboards;
        };
    }

    @Bean
    public ItemWriter<List<Leaderboard>> challengeWriter() {
        return chunk -> {
            for (List<Leaderboard> list : chunk.getItems()) {
                for (Leaderboard entry : list) {
                    leaderboardRepository.save(entry);
                }
            }
        };
    }

    // ==========================================
    // 2. GAMIFICATION REWARD BATCH JOB
    // ==========================================

    @Bean
    public Job gamificationJob() {
        return new JobBuilder("gamificationJob", jobRepository)
                .start(gamificationStep())
                .build();
    }

    @Bean
    public Step gamificationStep() {
        return new StepBuilder("gamificationStep", jobRepository)
                .<User, List<Badge>>chunk(10, transactionManager)
                .reader(userReader())
                .processor(userProcessor())
                .writer(badgeWriter())
                .build();
    }

    @Bean
    public ItemReader<User> userReader() {
        return new ItemReader<>() {
            private List<User> users;
            private int index = 0;

            @Override
            public User read() {
                if (users == null) {
                    users = userRepository.findAll();
                }
                if (index < users.size()) {
                    return users.get(index++);
                }
                return null;
            }
        };
    }

    @Bean
    public ItemProcessor<User, List<Badge>> userProcessor() {
        return user -> {
            List<Badge> newBadges = new ArrayList<>();
            int points = user.getPoints();

            // Novice Badge (threshold >= 100)
            if (points >= 100 && !badgeRepository.existsByUserIdAndName(user.getId(), "Novice")) {
                Badge badge = new Badge(user, "Novice", LocalDateTime.now());
                newBadges.add(badge);
                user.setLevel("Novice");
            }
            // Athlete Badge (threshold >= 500)
            if (points >= 500 && !badgeRepository.existsByUserIdAndName(user.getId(), "Athlete")) {
                Badge badge = new Badge(user, "Athlete", LocalDateTime.now());
                newBadges.add(badge);
                user.setLevel("Athlete");
            }
            // Champion Badge (threshold >= 1000)
            if (points >= 1000 && !badgeRepository.existsByUserIdAndName(user.getId(), "Champion")) {
                Badge badge = new Badge(user, "Champion", LocalDateTime.now());
                newBadges.add(badge);
                user.setLevel("Champion");
            }

            userRepository.save(user);
            return newBadges.isEmpty() ? null : newBadges;
        };
    }

    @Bean
    public ItemWriter<List<Badge>> badgeWriter() {
        return chunk -> {
            for (List<Badge> list : chunk.getItems()) {
                if (list != null) {
                    for (Badge b : list) {
                        badgeRepository.save(b);
                    }
                }
            }
        };
    }
}
