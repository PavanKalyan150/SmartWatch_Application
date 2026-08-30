package com.smartwatch.leaderboard.batch.tasklet;

import com.smartwatch.leaderboard.model.Challenge;
import com.smartwatch.leaderboard.model.Leaderboard;
import com.smartwatch.leaderboard.model.User;
import com.smartwatch.leaderboard.model.UserChallenge;
import com.smartwatch.leaderboard.model.enums.ChallengeStatus;
import com.smartwatch.leaderboard.model.enums.RewardScheme;
import com.smartwatch.leaderboard.model.enums.UserChallengeStatus;
import com.smartwatch.leaderboard.repository.ChallengeRepository;
import com.smartwatch.leaderboard.repository.LeaderboardRepository;
import com.smartwatch.leaderboard.repository.UserChallengeRepository;
import com.smartwatch.leaderboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.repeat.RepeatStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankPublishTaskletTest {

    @Mock private ChallengeRepository challengeRepository;
    @Mock private UserChallengeRepository userChallengeRepository;
    @Mock private LeaderboardRepository leaderboardRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private RankPublishTasklet tasklet;

    private Challenge challenge;

    @BeforeEach
    void setUp() {
        challenge = Challenge.builder()
                .id(100L)
                .status(ChallengeStatus.EXPIRED)
                .rewardScheme(RewardScheme.RANK_BASED)
                .build();
    }

    private UserChallenge participant(Long userId, double score, LocalDateTime rankedAt) {
        User user = User.builder().id(userId).build();
        return UserChallenge.builder()
                .user(user)
                .challenge(challenge)
                .finalScore(score)
                .rankedAt(rankedAt)
                .status(UserChallengeStatus.RANKED)
                .build();
    }

    // ---------- discovery ----------

    @Nested
    class ExpiredChallengeDiscovery {

        @Test
        void shouldDoNothingWhenNoExpiredChallengesExist() throws Exception {
            when(challengeRepository.findByStatus(ChallengeStatus.EXPIRED))
                    .thenReturn(Collections.emptyList());

            RepeatStatus result = tasklet.execute(null, null);

            assertThat(result).isEqualTo(RepeatStatus.FINISHED);
            verifyNoInteractions(userChallengeRepository, leaderboardRepository, userRepository);
        }

        @Test
        void shouldProcessEachExpiredChallenge() throws Exception {
            Challenge c1 = Challenge.builder().id(100L)
                    .status(ChallengeStatus.EXPIRED).rewardScheme(RewardScheme.RANK_BASED).build();
            Challenge c2 = Challenge.builder().id(200L)
                    .status(ChallengeStatus.EXPIRED).rewardScheme(RewardScheme.RANK_BASED).build();

            when(challengeRepository.findByStatus(ChallengeStatus.EXPIRED))
                    .thenReturn(List.of(c1, c2));
            when(userChallengeRepository.findByChallengeIdAndStatus(any(), eq(UserChallengeStatus.RANKED)))
                    .thenReturn(Collections.emptyList());

            tasklet.execute(null, null);

            verify(userChallengeRepository).findByChallengeIdAndStatus(100L, UserChallengeStatus.RANKED);
            verify(userChallengeRepository).findByChallengeIdAndStatus(200L, UserChallengeStatus.RANKED);
            verify(challengeRepository).save(c1);
            verify(challengeRepository).save(c2);
        }
    }

    // ---------- sorting ----------

    @Nested
    class SortingAndRanking {

        @Test
        void shouldRankParticipantsByScoreDescending() throws Exception {
            // user 2 has highest score -> rank 1
            UserChallenge u1 = participant(1L, 100.0, LocalDateTime.now());
            UserChallenge u2 = participant(2L, 300.0, LocalDateTime.now());
            UserChallenge u3 = participant(3L, 200.0, LocalDateTime.now());

            when(challengeRepository.findByStatus(ChallengeStatus.EXPIRED))
                    .thenReturn(List.of(challenge));
            when(userChallengeRepository.findByChallengeIdAndStatus(100L, UserChallengeStatus.RANKED))
                    .thenReturn(List.of(u1, u2, u3));

            tasklet.execute(null, null);

            assertThat(u2.getRank()).isEqualTo(1);
            assertThat(u3.getRank()).isEqualTo(2);
            assertThat(u1.getRank()).isEqualTo(3);
        }

        @Test
        void shouldBreakScoreTiesUsingEarlierRankedAt() throws Exception {
            // Two users tied at 200.0 — the one ranked earlier wins the tiebreak.
            LocalDateTime earlier = LocalDateTime.of(2026, 5, 20, 10, 0);
            LocalDateTime later   = LocalDateTime.of(2026, 5, 20, 12, 0);

            UserChallenge laterUser   = participant(1L, 200.0, later);
            UserChallenge earlierUser = participant(2L, 200.0, earlier);

            when(challengeRepository.findByStatus(ChallengeStatus.EXPIRED))
                    .thenReturn(List.of(challenge));
            when(userChallengeRepository.findByChallengeIdAndStatus(100L, UserChallengeStatus.RANKED))
                    .thenReturn(List.of(laterUser, earlierUser));

            tasklet.execute(null, null);

            assertThat(earlierUser.getRank()).isEqualTo(1);
            assertThat(laterUser.getRank()).isEqualTo(2);
        }

        @Test
        void shouldTreatNullRankedAtAsLatestForTiebreaker() throws Exception {
            // Null rankedAt is mapped to LocalDateTime.MAX -> they lose the tiebreaker.
            UserChallenge nullTime    = participant(1L, 200.0, null);
            UserChallenge earlierUser = participant(2L, 200.0,
                    LocalDateTime.of(2026, 5, 20, 10, 0));

            when(challengeRepository.findByStatus(ChallengeStatus.EXPIRED))
                    .thenReturn(List.of(challenge));
            when(userChallengeRepository.findByChallengeIdAndStatus(100L, UserChallengeStatus.RANKED))
                    .thenReturn(List.of(nullTime, earlierUser));

            tasklet.execute(null, null);

            assertThat(earlierUser.getRank()).isEqualTo(1);
            assertThat(nullTime.getRank()).isEqualTo(2);
        }
    }

    // ---------- reward scheme ----------

    @Nested
    class RewardSchemeAwarding {

        @Test
        void shouldAwardRankBasedPointsToTopThree() throws Exception {
            UserChallenge first  = participant(1L, 500.0, LocalDateTime.now());
            UserChallenge second = participant(2L, 400.0, LocalDateTime.now());
            UserChallenge third  = participant(3L, 300.0, LocalDateTime.now());

            when(challengeRepository.findByStatus(ChallengeStatus.EXPIRED))
                    .thenReturn(List.of(challenge));
            when(userChallengeRepository.findByChallengeIdAndStatus(100L, UserChallengeStatus.RANKED))
                    .thenReturn(List.of(first, second, third));

            tasklet.execute(null, null);

            assertThat(first.getPointsAwarded()).isEqualTo(100);
            assertThat(second.getPointsAwarded()).isEqualTo(50);
            assertThat(third.getPointsAwarded()).isEqualTo(25);

            verify(userRepository).addPoints(1L, 100);
            verify(userRepository).addPoints(2L, 50);
            verify(userRepository).addPoints(3L, 25);
        }

        @Test
        void shouldAwardZeroPointsToFourthAndBelow() throws Exception {
            UserChallenge u1 = participant(1L, 500.0, LocalDateTime.now());
            UserChallenge u2 = participant(2L, 400.0, LocalDateTime.now());
            UserChallenge u3 = participant(3L, 300.0, LocalDateTime.now());
            UserChallenge u4 = participant(4L, 200.0, LocalDateTime.now());
            UserChallenge u5 = participant(5L, 100.0, LocalDateTime.now());

            when(challengeRepository.findByStatus(ChallengeStatus.EXPIRED))
                    .thenReturn(List.of(challenge));
            when(userChallengeRepository.findByChallengeIdAndStatus(100L, UserChallengeStatus.RANKED))
                    .thenReturn(List.of(u1, u2, u3, u4, u5));

            tasklet.execute(null, null);

            assertThat(u4.getPointsAwarded()).isZero();
            assertThat(u5.getPointsAwarded()).isZero();
            // addPoints should NOT be called for zero-point users (saves DB roundtrips)
            verify(userRepository, never()).addPoints(eq(4L), anyInt());
            verify(userRepository, never()).addPoints(eq(5L), anyInt());
        }

        @Test
        void shouldAwardZeroPointsForNonRankBasedReward() throws Exception {
            // PARTICIPATION (or any non-RANK_BASED) scheme awards 0 to everyone.
            challenge.setRewardScheme(RewardScheme.NONE);

            UserChallenge u1 = participant(1L, 500.0, LocalDateTime.now());
            UserChallenge u2 = participant(2L, 400.0, LocalDateTime.now());

            when(challengeRepository.findByStatus(ChallengeStatus.EXPIRED))
                    .thenReturn(List.of(challenge));
            when(userChallengeRepository.findByChallengeIdAndStatus(100L, UserChallengeStatus.RANKED))
                    .thenReturn(List.of(u1, u2));

            tasklet.execute(null, null);

            assertThat(u1.getPointsAwarded()).isZero();
            assertThat(u2.getPointsAwarded()).isZero();
            verify(userRepository, never()).addPoints(anyLong(), anyInt());
        }
    }

    // ---------- persistence ----------

    @Nested
    class Persistence {

        @Test
        void shouldSaveLeaderboardEntriesWithCorrectFields() throws Exception {
            UserChallenge u1 = participant(1L, 500.0, LocalDateTime.now());
            UserChallenge u2 = participant(2L, 400.0, LocalDateTime.now());

            when(challengeRepository.findByStatus(ChallengeStatus.EXPIRED))
                    .thenReturn(List.of(challenge));
            when(userChallengeRepository.findByChallengeIdAndStatus(100L, UserChallengeStatus.RANKED))
                    .thenReturn(List.of(u1, u2));

            tasklet.execute(null, null);

            ArgumentCaptor<List<Leaderboard>> captor = ArgumentCaptor.forClass(List.class);
            verify(leaderboardRepository).saveAll(captor.capture());

            List<Leaderboard> entries = captor.getValue();
            assertThat(entries).hasSize(2);

            Leaderboard rank1 = entries.get(0);
            assertThat(rank1.getUser().getId()).isEqualTo(1L);
            assertThat(rank1.getChallenge()).isSameAs(challenge);
            assertThat(rank1.getRank()).isEqualTo(1);
            assertThat(rank1.getFinalScore()).isEqualTo(500.0);
            assertThat(rank1.getAwardedPoints()).isEqualTo(100);
            assertThat(rank1.getGeneratedAt()).isNotNull();

            Leaderboard rank2 = entries.get(1);
            assertThat(rank2.getRank()).isEqualTo(2);
            assertThat(rank2.getAwardedPoints()).isEqualTo(50);
        }

        @Test
        void shouldSetUserChallengeStatusToRankedOnAllParticipants() throws Exception {
            UserChallenge u1 = participant(1L, 500.0, LocalDateTime.now());
            u1.setStatus(UserChallengeStatus.RANKED); // already RANKED — should remain
            UserChallenge u2 = participant(2L, 400.0, LocalDateTime.now());

            when(challengeRepository.findByStatus(ChallengeStatus.EXPIRED))
                    .thenReturn(List.of(challenge));
            when(userChallengeRepository.findByChallengeIdAndStatus(100L, UserChallengeStatus.RANKED))
                    .thenReturn(List.of(u1, u2));

            tasklet.execute(null, null);

            assertThat(u1.getStatus()).isEqualTo(UserChallengeStatus.RANKED);
            assertThat(u2.getStatus()).isEqualTo(UserChallengeStatus.RANKED);
            verify(userChallengeRepository).saveAll(List.of(u1, u2)); // sorted order
        }

        @Test
        void shouldFinalizeChallengeStatusToRanked() throws Exception {
            when(challengeRepository.findByStatus(ChallengeStatus.EXPIRED))
                    .thenReturn(List.of(challenge));
            when(userChallengeRepository.findByChallengeIdAndStatus(100L, UserChallengeStatus.RANKED))
                    .thenReturn(List.of(participant(1L, 100.0, LocalDateTime.now())));

            tasklet.execute(null, null);

            assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.RANKED);
            verify(challengeRepository).save(challenge);
        }

        @Test
        void shouldUseSameTimestampForRankedAtAndGeneratedAt() throws Exception {
            // Lock down: the LocalDateTime.now() is computed ONCE per challenge
            // and reused for all participants and their leaderboard entries.
            UserChallenge u1 = participant(1L, 500.0, LocalDateTime.now().minusDays(1));
            UserChallenge u2 = participant(2L, 400.0, LocalDateTime.now().minusDays(1));

            when(challengeRepository.findByStatus(ChallengeStatus.EXPIRED))
                    .thenReturn(List.of(challenge));
            when(userChallengeRepository.findByChallengeIdAndStatus(100L, UserChallengeStatus.RANKED))
                    .thenReturn(List.of(u1, u2));

            tasklet.execute(null, null);

            assertThat(u1.getRankedAt()).isEqualTo(u2.getRankedAt());

            ArgumentCaptor<List<Leaderboard>> captor = ArgumentCaptor.forClass(List.class);
            verify(leaderboardRepository).saveAll(captor.capture());
            List<Leaderboard> entries = captor.getValue();
            assertThat(entries.get(0).getGeneratedAt())
                    .isEqualTo(entries.get(1).getGeneratedAt())
                    .isEqualTo(u1.getRankedAt());
        }
    }

    // ---------- edge cases ----------

    @Nested
    class EdgeCases {

        @Test
        void shouldFinalizeChallengeEvenWhenNoParticipants() throws Exception {
            when(challengeRepository.findByStatus(ChallengeStatus.EXPIRED))
                    .thenReturn(List.of(challenge));
            when(userChallengeRepository.findByChallengeIdAndStatus(100L, UserChallengeStatus.RANKED))
                    .thenReturn(Collections.emptyList());

            tasklet.execute(null, null);

            assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.RANKED);
            verify(challengeRepository).save(challenge);
            verify(leaderboardRepository, never()).saveAll(any());
            verify(userChallengeRepository, never()).saveAll(any());
            verify(userRepository, never()).addPoints(anyLong(), anyInt());
        }

        @Test
        void shouldReturnFinishedFromExecute() throws Exception {
            when(challengeRepository.findByStatus(ChallengeStatus.EXPIRED))
                    .thenReturn(Collections.emptyList());

            assertThat(tasklet.execute(null, null)).isEqualTo(RepeatStatus.FINISHED);
        }
    }
}