package com.leaderboard.batch;

import com.leaderboard.model.*;
import com.leaderboard.repository.*;
import com.leaderboard.service.GameService;
import com.leaderboard.service.RankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@org.springframework.kafka.test.context.EmbeddedKafka(partitions = 1, topics = { "smartwatch-telemetry" })
public class BatchTest {

    @Autowired
    private RankService rankService;

    @Autowired
    private GameService gameService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private UserChallengeRepository userChallengeRepository;

    @Autowired
    private LeaderboardRepository leaderboardRepository;

    @Autowired
    private BadgeRepository badgeRepository;

    private Challenge expiredChallenge;
    private User user1;
    private User user2;

    @BeforeEach
    public void setup() {
        leaderboardRepository.deleteAll();
        userChallengeRepository.deleteAll();
        challengeRepository.deleteAll();
        badgeRepository.deleteAll();
        userRepository.deleteAll();

        // Create Users
        user1 = new User("1111111111", "user1@example.com", "User One", "pwd", Role.ROLE_USER);
        user1.setPoints(0);
        user1 = userRepository.save(user1);

        user2 = new User("2222222222", "user2@example.com", "User Two", "pwd", Role.ROLE_USER);
        user2.setPoints(0);
        user2 = userRepository.save(user2);

        // Create Expired Challenge
        expiredChallenge = new Challenge();
        expiredChallenge.setTitle("Expired Challenge");
        expiredChallenge.setDescription("Should be ranked");
        expiredChallenge.setRequiredSteps(5000);
        expiredChallenge.setPointsReward(100);
        expiredChallenge.setExpiryDate(LocalDateTime.now().minusHours(1)); // Expired!
        expiredChallenge.setIsGlobal(true);
        expiredChallenge = challengeRepository.save(expiredChallenge);

        // Create User Challenges registrations
        UserChallenge uc1 = new UserChallenge(user1, expiredChallenge);
        uc1.setScore(8000); // completed
        uc1.setCompleted(true);
        userChallengeRepository.save(uc1);

        UserChallenge uc2 = new UserChallenge(user2, expiredChallenge);
        uc2.setScore(6000); // completed but lower score
        uc2.setCompleted(true);
        userChallengeRepository.save(uc2);
    }

    @Test
    public void testRankingAndGamificationBatchJobs() throws Exception {
        // 1. Run Ranking Job
        rankService.runRankingJob();

        // 2. Verify challenge is marked processed
        Challenge updatedChallenge = challengeRepository.findById(expiredChallenge.getId()).orElseThrow();
        assertTrue(updatedChallenge.getIsProcessed());

        // 3. Verify user challenge registrations got ranks and points
        UserChallenge uc1Updated = userChallengeRepository.findByUserIdAndChallengeId(user1.getId(), expiredChallenge.getId()).orElseThrow();
        UserChallenge uc2Updated = userChallengeRepository.findByUserIdAndChallengeId(user2.getId(), expiredChallenge.getId()).orElseThrow();

        assertEquals(1, uc1Updated.getRank()); // Winner (higher score)
        assertEquals(2, uc2Updated.getRank());

        // Points check:
        // Winner gets base 100 + 100 winner bonus = 200 points
        // Runner-up gets base 100 + 50 runner bonus = 150 points
        assertEquals(200, uc1Updated.getPointsAwarded());
        assertEquals(150, uc2Updated.getPointsAwarded());

        User user1Updated = userRepository.findById(user1.getId()).orElseThrow();
        User user2Updated = userRepository.findById(user2.getId()).orElseThrow();

        assertEquals(200, user1Updated.getPoints());
        assertEquals(150, user2Updated.getPoints());

        // 4. Run Gamification Job (checks points thresholds)
        // User1 has 200 points (should get Novice badge)
        // User2 has 150 points (should get Novice badge)
        gameService.runGamificationJob();

        List<Badge> u1Badges = badgeRepository.findByUserId(user1.getId());
        List<Badge> u2Badges = badgeRepository.findByUserId(user2.getId());

        assertEquals(1, u1Badges.size());
        assertEquals("Novice", u1Badges.get(0).getName());
        assertEquals("Novice", userRepository.findById(user1.getId()).orElseThrow().getLevel());

        assertEquals(1, u2Badges.size());
        assertEquals("Novice", u2Badges.get(0).getName());
        assertEquals("Novice", userRepository.findById(user2.getId()).orElseThrow().getLevel());
    }
}
