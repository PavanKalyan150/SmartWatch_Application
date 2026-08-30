package com.smartwatch.leaderboard.controller;

import com.smartwatch.leaderboard.service.HealthService;
import com.smartwatch.leaderboard.dto.response.HealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        log.debug("Health check called");
        HealthResponse response = healthService.checkHealth();

        HttpStatus status = response.isHealthy()
                ? HttpStatus.OK
                : HttpStatus.SERVICE_UNAVAILABLE;   // 503 when degraded

        return ResponseEntity.status(status).body(response);
    }
}