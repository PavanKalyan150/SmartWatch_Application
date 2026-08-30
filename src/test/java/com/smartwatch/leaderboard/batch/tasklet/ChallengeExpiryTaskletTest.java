package com.smartwatch.leaderboard.batch.tasklet;

import com.smartwatch.leaderboard.model.enums.ChallengeStatus;
import com.smartwatch.leaderboard.repository.ChallengeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.repeat.RepeatStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeExpiryTaskletTest {

    @Mock private ChallengeRepository challengeRepository;

    @InjectMocks private ChallengeExpiryTasklet tasklet;

    @Test
    void shouldCallRepositoryWithActiveAndExpiredStatusesAndCurrentTime() throws Exception {
        when(challengeRepository.markExpiredChallenges(
                any(ChallengeStatus.class), any(ChallengeStatus.class), any(LocalDateTime.class)))
                .thenReturn(3);

        LocalDateTime before = LocalDateTime.now();
        RepeatStatus result = tasklet.execute(null, null);
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(challengeRepository).markExpiredChallenges(
                eq(ChallengeStatus.ACTIVE),
                eq(ChallengeStatus.EXPIRED),
                timeCaptor.capture()
        );

        // Time used should fall within the test execution window
        assertThat(timeCaptor.getValue())
                .isBetween(before, after);

        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
    }

    @Test
    void shouldReturnFinishedEvenWhenNoChallengesAreExpired() throws Exception {
        // Tasklet is one-shot: even with zero updates, it should still report FINISHED.
        when(challengeRepository.markExpiredChallenges(
                any(ChallengeStatus.class), any(ChallengeStatus.class), any(LocalDateTime.class)))
                .thenReturn(0);

        RepeatStatus result = tasklet.execute(null, null);

        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        verify(challengeRepository).markExpiredChallenges(
                eq(ChallengeStatus.ACTIVE),
                eq(ChallengeStatus.EXPIRED),
                any(LocalDateTime.class)
        );
    }

    @Test
    void shouldReturnFinishedWhenManyChallengesAreExpired() throws Exception {
        // Sanity: large counts shouldn't change the return contract.
        when(challengeRepository.markExpiredChallenges(
                any(ChallengeStatus.class), any(ChallengeStatus.class), any(LocalDateTime.class)))
                .thenReturn(10_000);

        RepeatStatus result = tasklet.execute(null, null);

        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
    }

    @Test
    void shouldNotPassExpiredAsTheFromStatusOrActiveAsTheToStatus() throws Exception {
        // Lock down argument *order* — easy to flip when refactoring.
        // If someone swaps ACTIVE/EXPIRED, this test fails immediately.
        when(challengeRepository.markExpiredChallenges(
                any(ChallengeStatus.class), any(ChallengeStatus.class), any(LocalDateTime.class)))
                .thenReturn(1);

        tasklet.execute(null, null);

        ArgumentCaptor<ChallengeStatus> fromCaptor = ArgumentCaptor.forClass(ChallengeStatus.class);
        ArgumentCaptor<ChallengeStatus> toCaptor = ArgumentCaptor.forClass(ChallengeStatus.class);
        verify(challengeRepository).markExpiredChallenges(
                fromCaptor.capture(),
                toCaptor.capture(),
                any(LocalDateTime.class)
        );

        assertThat(fromCaptor.getValue()).isEqualTo(ChallengeStatus.ACTIVE);
        assertThat(toCaptor.getValue()).isEqualTo(ChallengeStatus.EXPIRED);
    }
}