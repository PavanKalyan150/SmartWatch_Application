package com.smartwatch.leaderboard.service;

import com.smartwatch.leaderboard.dto.response.BatchTriggerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchTriggerService {

    private final JobLauncher jobLauncher;
    private final Job rankingJob;
    private final Job gamificationJob;

    public BatchTriggerResponse triggerRankingJob() {
        return launchJob(rankingJob, "rankingJob");
    }

    public BatchTriggerResponse triggerGamificationJob() {
        return launchJob(gamificationJob, "gamificationJob");
    }

    private BatchTriggerResponse launchJob(Job job, String jobName) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLocalDateTime("triggeredAt", LocalDateTime.now())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(job, params);
            log.info("Job '{}' launched — executionId={}, status={}",
                    jobName, execution.getId(), execution.getStatus());

            return BatchTriggerResponse.builder()
                    .jobId(execution.getId())
                    .status(execution.getStatus().name())
                    .triggeredAt(execution.getStartTime())
                    .build();

        } catch (Exception e) {
            log.error("Failed to launch job '{}'", jobName, e);
            return BatchTriggerResponse.builder()
                    .jobId(null)
                    .status("FAILED")
                    .triggeredAt(LocalDateTime.now())
                    .build();
        }
    }
}