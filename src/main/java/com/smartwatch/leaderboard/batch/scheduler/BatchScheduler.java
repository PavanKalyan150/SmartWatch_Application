package com.smartwatch.leaderboard.batch.scheduler;

import com.smartwatch.leaderboard.service.BatchTriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final BatchTriggerService batchTriggerService;

    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    public void scheduledRankingJob() {
        log.info("Scheduled trigger — ranking job [{}]", java.time.LocalDateTime.now());
        batchTriggerService.triggerRankingJob();
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    public void scheduledGamificationJob() {
        log.info("Scheduled trigger — gamification job [{}]", java.time.LocalDateTime.now());
        batchTriggerService.triggerGamificationJob();
    }
}

