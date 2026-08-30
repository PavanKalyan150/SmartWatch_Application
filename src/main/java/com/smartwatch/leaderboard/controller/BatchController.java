package com.smartwatch.leaderboard.controller;

import com.smartwatch.leaderboard.dto.response.BatchTriggerResponse;
import com.smartwatch.leaderboard.service.BatchTriggerService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BatchController {

    private static final Logger log = LoggerFactory.getLogger(BatchController.class);

    private final BatchTriggerService batchTriggerService;

    @GetMapping("/rank")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BatchTriggerResponse> triggerRankingJob() {
        log.info("Manual trigger received for ranking batch job");
        BatchTriggerResponse response = batchTriggerService.triggerRankingJob();
        log.info("Ranking job triggered — jobId: {}, status: {}", response.getJobId(), response.getStatus());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/game")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BatchTriggerResponse> triggerGamificationJob() {
        log.info("Manual trigger received for gamification batch job");
        BatchTriggerResponse response = batchTriggerService.triggerGamificationJob();
        log.info("Gamification job triggered — jobId: {}, status: {}", response.getJobId(), response.getStatus());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}