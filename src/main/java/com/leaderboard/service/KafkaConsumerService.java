package com.leaderboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leaderboard.dto.UserTelemetryMessage;
import com.leaderboard.model.*;
import com.leaderboard.repository.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class KafkaConsumerService {

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final UserActivityRepository userActivityRepository;
    private final UserTaskRepository userTaskRepository;
    private final UserChallengeRepository userChallengeRepository;

    public KafkaConsumerService(ObjectMapper objectMapper, UserRepository userRepository,
                                UserActivityRepository userActivityRepository,
                                UserTaskRepository userTaskRepository,
                                UserChallengeRepository userChallengeRepository) {
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.userActivityRepository = userActivityRepository;
        this.userTaskRepository = userTaskRepository;
        this.userChallengeRepository = userChallengeRepository;
    }

    @KafkaListener(topics = "smartwatch-telemetry", groupId = "leaderboard-group")
    @Transactional
    public void consumeTelemetry(String messageJson) {
        try {
            UserTelemetryMessage msg = objectMapper.readValue(messageJson, UserTelemetryMessage.class);
            Long userId = msg.getUserId();
            Integer steps = msg.getStepCountValue();
            LocalDate activityDate = LocalDate.parse(msg.getDate());

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            // 1. Update Daily UserActivity
            Optional<UserActivity> activityOpt = userActivityRepository.findByUserIdAndActivityDate(userId, activityDate);
            UserActivity activity;
            if (activityOpt.isPresent()) {
                activity = activityOpt.get();
                activity.setStepCount(activity.getStepCount() + steps);
                if (msg.getTags() != null) {
                    activity.getFeatures().addAll(msg.getTags());
                }
            } else {
                activity = new UserActivity(user, activityDate, steps, msg.getTags());
            }
            userActivityRepository.save(activity);

            // 2. Update Uncompleted Tasks
            List<UserTask> userTasks = userTaskRepository.findByUserId(userId);
            for (UserTask ut : userTasks) {
                if (Boolean.FALSE.equals(ut.getCompleted())) {
                    ut.setScore(ut.getScore() + steps);
                    if (ut.getScore() >= ut.getTask().getRequiredSteps()) {
                        ut.setCompleted(true);
                        ut.setCompletionDate(LocalDateTime.now());
                        user.setPoints(user.getPoints() + ut.getTask().getPointsReward());
                    }
                    userTaskRepository.save(ut);
                }
            }

            // 3. Update Uncompleted/Active Challenges
            List<UserChallenge> userChallenges = userChallengeRepository.findByUserId(userId);
            LocalDateTime now = LocalDateTime.now();
            for (UserChallenge uc : userChallenges) {
                Challenge challenge = uc.getChallenge();
                if (Boolean.FALSE.equals(uc.getCompleted()) && challenge.getExpiryDate().isAfter(now)) {
                    uc.setScore(uc.getScore() + steps);
                    if (uc.getScore() >= challenge.getRequiredSteps()) {
                        uc.setCompleted(true);
                        uc.setCompletionDate(LocalDateTime.now());
                        // Challenge points will be finalized/awarded by the Spring Batch ranking job
                    }
                    userChallengeRepository.save(uc);
                }
            }

            userRepository.save(user);

        } catch (Exception e) {
            // In a production system, write to a dead letter queue or log
            System.err.println("Error processing telemetry: " + e.getMessage());
        }
    }
}
