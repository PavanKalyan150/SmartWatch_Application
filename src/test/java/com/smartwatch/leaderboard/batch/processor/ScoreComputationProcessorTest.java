package com.smartwatch.leaderboard.batch.processor;

import com.smartwatch.leaderboard.dto.batch.ScoredParticipant;
import com.smartwatch.leaderboard.model.Challenge;
import com.smartwatch.leaderboard.model.User;
import com.smartwatch.leaderboard.model.UserChallenge;
import com.smartwatch.leaderboard.model.enums.UserTaskStatus;
import com.smartwatch.leaderboard.repository.ChallengeTaskRepository;
import com.smartwatch.leaderboard.repository.UserTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoreComputationProcessorTest {

    @Mock private ChallengeTaskRepository challengeTaskRepository;
    @Mock private UserTaskRepository userTaskRepository;

    @InjectMocks private ScoreComputationProcessor processor;

    private static final Long USER_ID = 1L;
    private static final Long CHALLENGE_ID = 100L;

    private LocalDateTime start;
    private LocalDateTime end;
    private UserChallenge userChallenge;

    @BeforeEach
    void setUp() {
        start = LocalDateTime.of(2026, 5, 1, 0, 0);
        end   = LocalDateTime.of(2026, 5, 31, 23, 59);

        User user = User.builder().id(USER_ID).build();
        Challenge challenge = Challenge.builder()
                .id(CHALLENGE_ID)
                .startTime(start)
                .endTime(end)
                .build();
        userChallenge = UserChallenge.builder()
                .user(user)
                .challenge(challenge)
                .build();
    }

    // ---------- happy path ----------

    @Nested
    class HappyPath {

        @Test
        void shouldComputeScoreAndLastCompletionFromRepositories() {
            List<Long> taskIds = List.of(10L, 20L, 30L);
            LocalDateTime lastCompletion = LocalDateTime.of(2026, 5, 20, 14, 30);

            when(challengeTaskRepository.findTaskIdsByChallengeId(CHALLENGE_ID))
                    .thenReturn(taskIds);
            when(userTaskRepository.sumPointsForUserInScope(
                    USER_ID, taskIds, UserTaskStatus.COMPLETED, start, end))
                    .thenReturn(450.0);
            when(userTaskRepository.findLastCompletionInScope(
                    USER_ID, taskIds, UserTaskStatus.COMPLETED, start, end))
                    .thenReturn(lastCompletion);

            ScoredParticipant result = processor.process(userChallenge);

            assertThat(result).isNotNull();
            assertThat(result.getUserChallenge()).isSameAs(userChallenge);
            assertThat(result.getFinalScore()).isEqualTo(450.0);
            assertThat(result.getLastCompletionAt()).isEqualTo(lastCompletion);
        }

        @Test
        void shouldPassChallengeTimeWindowToRepositoryQueries() {
            // Lock down that the date scope for the SUM/LAST queries is the
            // challenge's start/end — not "now", not the user's join date.
            List<Long> taskIds = List.of(10L);
            when(challengeTaskRepository.findTaskIdsByChallengeId(CHALLENGE_ID))
                    .thenReturn(taskIds);
            when(userTaskRepository.sumPointsForUserInScope(
                    USER_ID, taskIds, UserTaskStatus.COMPLETED, start, end))
                    .thenReturn(100.0);
            when(userTaskRepository.findLastCompletionInScope(
                    USER_ID, taskIds, UserTaskStatus.COMPLETED, start, end))
                    .thenReturn(null);

            processor.process(userChallenge);

            verify(userTaskRepository).sumPointsForUserInScope(
                    USER_ID, taskIds, UserTaskStatus.COMPLETED, start, end);
            verify(userTaskRepository).findLastCompletionInScope(
                    USER_ID, taskIds, UserTaskStatus.COMPLETED, start, end);
        }
    }

    // ---------- empty challenge tasks ----------

    @Nested
    class NoChallengeTasks {

        @Test
        void shouldReturnZeroScoreShortCircuitWhenChallengeHasNoTasks() {
            when(challengeTaskRepository.findTaskIdsByChallengeId(CHALLENGE_ID))
                    .thenReturn(Collections.emptyList());

            ScoredParticipant result = processor.process(userChallenge);

            assertThat(result.getUserChallenge()).isSameAs(userChallenge);
            assertThat(result.getFinalScore()).isZero();
            assertThat(result.getLastCompletionAt()).isNull();
        }

        @Test
        void shouldNotQueryUserTaskRepositoryWhenChallengeHasNoTasks() {
            // Skipping the user-task queries on empty taskIds avoids
            // running queries with empty IN-clauses (which can be DB-specific quirks).
            when(challengeTaskRepository.findTaskIdsByChallengeId(CHALLENGE_ID))
                    .thenReturn(Collections.emptyList());

            processor.process(userChallenge);

            verify(userTaskRepository, never()).sumPointsForUserInScope(
                    any(), any(), any(), any(), any());
            verify(userTaskRepository, never()).findLastCompletionInScope(
                    any(), any(), any(), any(), any());
        }
    }

    // ---------- null score handling ----------

    @Nested
    class NullScoreFromSum {

        @Test
        void shouldDefaultScoreToZeroWhenSumReturnsNull() {
            // SQL SUM(...) returns NULL when no rows match. The processor must
            // coerce that to 0.0 — otherwise downstream serialization (or
            // ScoredParticipant.finalScore as primitive double) would NPE.
            List<Long> taskIds = List.of(10L, 20L);
            when(challengeTaskRepository.findTaskIdsByChallengeId(CHALLENGE_ID))
                    .thenReturn(taskIds);
            when(userTaskRepository.sumPointsForUserInScope(
                    USER_ID, taskIds, UserTaskStatus.COMPLETED, start, end))
                    .thenReturn(null);
            when(userTaskRepository.findLastCompletionInScope(
                    USER_ID, taskIds, UserTaskStatus.COMPLETED, start, end))
                    .thenReturn(null);

            ScoredParticipant result = processor.process(userChallenge);

            assertThat(result.getFinalScore()).isZero();
            assertThat(result.getLastCompletionAt()).isNull();
        }

        @Test
        void shouldHandleScoreOfZeroAsValidNonNullValue() {
            // Edge case: SUM returns 0.0 (rows existed but summed to zero).
            // Should NOT be confused with null.
            List<Long> taskIds = List.of(10L);
            when(challengeTaskRepository.findTaskIdsByChallengeId(CHALLENGE_ID))
                    .thenReturn(taskIds);
            when(userTaskRepository.sumPointsForUserInScope(
                    USER_ID, taskIds, UserTaskStatus.COMPLETED, start, end))
                    .thenReturn(0.0);
            when(userTaskRepository.findLastCompletionInScope(
                    USER_ID, taskIds, UserTaskStatus.COMPLETED, start, end))
                    .thenReturn(null);

            ScoredParticipant result = processor.process(userChallenge);

            assertThat(result.getFinalScore()).isZero();
        }
    }
}