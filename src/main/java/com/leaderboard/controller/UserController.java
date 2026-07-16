package com.leaderboard.controller;

import com.leaderboard.dto.TelemetryEvent;
import com.leaderboard.dto.UserTelemetryMessage;
import com.leaderboard.model.User;
import com.leaderboard.model.UserChallenge;
import com.leaderboard.model.UserTask;
import com.leaderboard.service.KafkaProducerService;
import com.leaderboard.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class UserController {

    private final UserService userService;
    private final KafkaProducerService kafkaProducerService;

    public UserController(UserService userService, KafkaProducerService kafkaProducerService) {
        this.userService = userService;
        this.kafkaProducerService = kafkaProducerService;
    }

    // Telemetry ingestion endpoint (POST /user/{userId})
    @PostMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, String>> ingestTelemetry(
            @PathVariable Long userId,
            @RequestBody TelemetryEvent event) {

        // Validate required fields
        if (event.getStepCountValue() == null) {
            throw new IllegalArgumentException("Step Count Value is missing");
        }
        if (event.getDate() == null || event.getDate().isBlank()) {
            throw new IllegalArgumentException("Date is missing");
        }
        if (event.getTags() == null || event.getTags().isEmpty()) {
            throw new IllegalArgumentException("Tags are missing");
        }

        // Validate user existence first
        userService.getUserById(userId);

        // Publish to Kafka queue
        UserTelemetryMessage message = new UserTelemetryMessage(
                userId,
                event.getStepCountValue(),
                event.getDate(),
                event.getTags()
        );
        kafkaProducerService.sendTelemetry(message);

        Map<String, String> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Telemetry telemetry event published to Kafka");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // GET /user/me - retrieve current user profile details
    @GetMapping("/user/me")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getCurrentUserData(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userService.getUserByPhone(authentication.getName());
        return getUserData(user.getId());
    }

    // GET /user/{userId} - retrieve profile details and active stats
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserData(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("fullName", user.getFullName());
        data.put("email", user.getEmail());
        data.put("phone", user.getPhone());
        data.put("points", user.getPoints());
        data.put("level", user.getLevel());
        data.put("device", user.getDevice());
        data.put("activities", userService.getUserActivities(userId));
        return ResponseEntity.ok(data);
    }

    // PUT /user/{userId}/device - associate a device with a user
    @PutMapping("/user/{userId}/device")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<User> associateDevice(@PathVariable Long userId, @RequestParam Long deviceId) {
        return ResponseEntity.ok(userService.associateDevice(userId, deviceId));
    }

    // POST /task/{taskId}/register - Register user to task
    @PostMapping("/task/{taskId}/register")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<UserTask> registerForTask(@PathVariable Long taskId, @RequestParam Long userId) {
        return new ResponseEntity<>(userService.registerForTask(userId, taskId), HttpStatus.CREATED);
    }

    // POST /challenge/{challengeId}/register - Register user to challenge
    @PostMapping("/challenge/{challengeId}/register")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<UserChallenge> registerForChallenge(@PathVariable Long challengeId, @RequestParam Long userId) {
        return new ResponseEntity<>(userService.registerForChallenge(userId, challengeId), HttpStatus.CREATED);
    }

    // GET /user/{userId}/challenges - get challenges joined by user
    @GetMapping("/user/{userId}/challenges")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<java.util.List<UserChallenge>> getUserChallenges(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserChallenges(userId));
    }

    // GET /user/{userId}/tasks - get tasks joined by user
    @GetMapping("/user/{userId}/tasks")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<java.util.List<UserTask>> getUserTasks(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserTasks(userId));
    }
}
