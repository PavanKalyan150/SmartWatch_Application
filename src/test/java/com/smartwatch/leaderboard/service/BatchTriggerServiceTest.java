package com.smartwatch.leaderboard.service;

import com.smartwatch.leaderboard.dto.response.BatchTriggerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchTriggerServiceTest {

    @Mock private JobLauncher jobLauncher;
    @Mock private Job rankingJob;
    @Mock private Job gamificationJob;
    @Mock private JobExecution jobExecution;

    private BatchTriggerService batchTriggerService;

    private static final long EXECUTION_ID = 42L;
    private static final LocalDateTime START_TIME = LocalDateTime.of(2026, 5, 20, 10, 0);

    @BeforeEach
    void setUp() {
        // Constructor injection — @InjectMocks gets confused with two beans of same type (Job).
        // Manual wiring is cleaner & makes the order obvious.
        batchTriggerService = new BatchTriggerService(jobLauncher, rankingJob, gamificationJob);
    }

    // ---------- triggerRankingJob() ----------

    @Test
    void shouldLaunchRankingJobAndReturnSuccessResponse() throws Exception {
        when(jobLauncher.run(eq(rankingJob), any(JobParameters.class))).thenReturn(jobExecution);
        when(jobExecution.getId()).thenReturn(EXECUTION_ID);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTED);
        when(jobExecution.getStartTime()).thenReturn(START_TIME);

        BatchTriggerResponse response = batchTriggerService.triggerRankingJob();

        assertThat(response).isNotNull();
        assertThat(response.getJobId()).isEqualTo(EXECUTION_ID);
        assertThat(response.getStatus()).isEqualTo("STARTED");
        assertThat(response.getTriggeredAt()).isEqualTo(START_TIME);

        verify(jobLauncher).run(eq(rankingJob), any(JobParameters.class));
    }

    @Test
    void shouldPassTriggeredAtParameterWhenLaunchingRankingJob() throws Exception {
        when(jobLauncher.run(eq(rankingJob), any(JobParameters.class))).thenReturn(jobExecution);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTED);

        LocalDateTime before = LocalDateTime.now();
        batchTriggerService.triggerRankingJob();
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<JobParameters> paramsCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(eq(rankingJob), paramsCaptor.capture());

        LocalDateTime triggeredAt = paramsCaptor.getValue().getLocalDateTime("triggeredAt");
        assertThat(triggeredAt).isNotNull();
        assertThat(triggeredAt).isBetween(before, after);
    }

    // ---------- triggerGamificationJob() ----------

    @Test
    void shouldLaunchGamificationJobAndReturnSuccessResponse() throws Exception {
        when(jobLauncher.run(eq(gamificationJob), any(JobParameters.class))).thenReturn(jobExecution);
        when(jobExecution.getId()).thenReturn(EXECUTION_ID);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getStartTime()).thenReturn(START_TIME);

        BatchTriggerResponse response = batchTriggerService.triggerGamificationJob();

        assertThat(response.getJobId()).isEqualTo(EXECUTION_ID);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getTriggeredAt()).isEqualTo(START_TIME);

        verify(jobLauncher).run(eq(gamificationJob), any(JobParameters.class));
    }

    @Test
    void shouldUseCorrectJobBeanForEachTriggerMethod() throws Exception {
        when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(jobExecution);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTED);

        batchTriggerService.triggerRankingJob();
        batchTriggerService.triggerGamificationJob();

        verify(jobLauncher).run(eq(rankingJob), any(JobParameters.class));
        verify(jobLauncher).run(eq(gamificationJob), any(JobParameters.class));
    }

    // ---------- error handling ----------

    @Test
    void shouldReturnFailedResponseWhenJobLauncherThrowsAlreadyRunningException() throws Exception {
        when(jobLauncher.run(eq(rankingJob), any(JobParameters.class)))
                .thenThrow(new JobExecutionAlreadyRunningException("already running"));

        LocalDateTime before = LocalDateTime.now();
        BatchTriggerResponse response = batchTriggerService.triggerRankingJob();
        LocalDateTime after = LocalDateTime.now();

        assertThat(response).isNotNull();
        assertThat(response.getJobId()).isNull();
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getTriggeredAt()).isBetween(before, after);
    }

    @Test
    void shouldReturnFailedResponseWhenJobLauncherThrowsRuntimeException() throws Exception {
        when(jobLauncher.run(eq(gamificationJob), any(JobParameters.class)))
                .thenThrow(new RuntimeException("unexpected boom"));

        BatchTriggerResponse response = batchTriggerService.triggerGamificationJob();

        assertThat(response.getJobId()).isNull();
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getTriggeredAt()).isNotNull();
    }

    @Test
    void shouldNotPropagateExceptionsToCaller() throws Exception {
        when(jobLauncher.run(any(Job.class), any(JobParameters.class)))
                .thenThrow(new RuntimeException("any failure"));

        // The whole point: caller (controller) gets a clean response, not a stack trace
        assertThat(batchTriggerService.triggerRankingJob()).isNotNull();
        assertThat(batchTriggerService.triggerGamificationJob()).isNotNull();
    }
}