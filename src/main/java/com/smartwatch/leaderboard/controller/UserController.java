package com.smartwatch.leaderboard.controller;

import com.smartwatch.leaderboard.dto.request.ActivityEventRequest;
import com.smartwatch.leaderboard.dto.response.ActivityIngestionResponse;
import com.smartwatch.leaderboard.dto.response.UserActivityEventResponse;
import com.smartwatch.leaderboard.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    @PostMapping("/{userId}/activity")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ActivityIngestionResponse> ingestActivity(
            @PathVariable Long userId,
            @Valid @RequestBody ActivityEventRequest request) {
        log.debug("Activity ingestion for userId: {}, eventId: {}, metric: {}",
                userId, request.getEventId(), request.getMetricType());
        ActivityIngestionResponse response = userService.ingestActivity(userId, request);
        log.info("Event {} accepted and published to Kafka for userId: {}", request.getEventId(), userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{userId}/activity")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<UserActivityEventResponse>> getUserActivityEvents(
            @PathVariable Long userId) {
        log.debug("Fetching activity events for userId: {}", userId);
        List<UserActivityEventResponse> response = userService.getActivityEvents(userId);
        log.info("Returned {} activity events for userId: {}", response.size(), userId);
        return ResponseEntity.ok(response);
    }
}