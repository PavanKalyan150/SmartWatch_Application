package com.smartwatch.leaderboard.kafka;

import com.smartwatch.leaderboard.dto.kafka.ActivityEventMessage;
import com.smartwatch.leaderboard.model.*;
import com.smartwatch.leaderboard.model.enums.ProcessedStatus;
import com.smartwatch.leaderboard.model.enums.TaskStatus;
import com.smartwatch.leaderboard.model.enums.UserTaskStatus;
import com.smartwatch.leaderboard.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityEventProcessingService {

    private final UserRepository userRepository;
    private final UserActivityEventRepository activityEventRepository;
    private final DeviceCapabilityRepository deviceCapabilityRepository;
    private final TaskRepository taskRepository;
    private final UserTaskRepository userTaskRepository;
    private final LevelRepository levelRepository;

    @Transactional
    public void process(ActivityEventMessage message) {
        User user = userRepository.findById(message.getUserId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found [userId=" + message.getUserId() + "]"));

        if (!deviceCapabilityRepository.existsByDeviceIdAndCapabilityCode(
                user.getDevice().getId(), message.getMetricType())) {
            log.warn("Device does not support metric [userId={}, metric={}, eventId={}] — DEAD_LETTER",
                    user.getId(), message.getMetricType(), message.getEventId());
            saveEvent(message, user, ProcessedStatus.DEAD_LETTER);
            return;
        }

        UserActivityEvent event = saveEvent(message, user, ProcessedStatus.PENDING);

        List<Task> matchingTasks = taskRepository
                .findByRequiredMetricAndStatus(message.getMetricType(), TaskStatus.ACTIVE);

        // Eligibility filter: only credit progress to tasks the user has unlocked
        // Pre-filter so we know how many were skipped, for visibility.
        List<Task> eligibleTasks = matchingTasks.stream()
                .filter(task -> userMeetsLevelRequirement(user, task))
                .toList();

        int skipped = matchingTasks.size() - eligibleTasks.size();
        if (skipped > 0) {
            log.debug("Skipped {} ineligible task(s) [userId={}, userLevel={}, metric={}]",
                    skipped, user.getId(),
                    user.getLevel() != null ? user.getLevel().getLevelName() : "null",
                    message.getMetricType());
        }

        for (Task task : eligibleTasks) {
            applyProgressToTask(user, task, message.getMetricValue());
        }

        event.setProcessedStatus(ProcessedStatus.PROCESSED);
        event.setProcessedAt(LocalDateTime.now());
        activityEventRepository.save(event);

        log.info("Event processed [eventId={}, userId={}, metric={}, eligibleTasks={}, skipped={}]",
                message.getEventId(), user.getId(), message.getMetricType(),
                eligibleTasks.size(), skipped);
    }

    // REQUIRES_NEW: must commit independently — the outer transaction may have already
    // failed or rolled back by the time this is called from the consumer's catch block.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDeadLetter(ActivityEventMessage message) {
        if (activityEventRepository.existsByEventId(message.getEventId())) return;

        userRepository.findById(message.getUserId()).ifPresentOrElse(
                user -> saveEvent(message, user, ProcessedStatus.DEAD_LETTER),
                () -> log.error("Cannot persist DEAD_LETTER — user not found [userId={}]",
                        message.getUserId())
        );
    }

    private boolean userMeetsLevelRequirement(User user, Task task) {
        return user.getLevel().getPointThreshold() >= task.getRequiredLevel().getPointThreshold();
    }

    private void applyProgressToTask(User user, Task task, Double incomingValue) {
        UserTask userTask = userTaskRepository
                .findByUserIdAndTaskId(user.getId(), task.getId())
                .orElseGet(() -> UserTask.builder()
                        .user(user)
                        .task(task)
                        .progressValue(0.0)
                        .status(UserTaskStatus.IN_PROGRESS)
                        .pointsAwarded(0)
                        .build());

        if (userTask.getStatus() == UserTaskStatus.COMPLETED) return;

        double updated = userTask.getProgressValue() + incomingValue;
        userTask.setProgressValue(updated);

        if (updated >= task.getTargetValue()) {
            userTask.setStatus(UserTaskStatus.COMPLETED);
            userTask.setCompletedAt(LocalDateTime.now());
            userTask.setPointsAwarded(task.getRewardPoints());
            creditPoints(user, task.getRewardPoints());
            log.info("Task completed [userId={}, taskId={}, points={}]",
                    user.getId(), task.getId(), task.getRewardPoints());
        }

        userTaskRepository.save(userTask);
    }

    private void creditPoints(User user, int points) {
        int newBalance = user.getPointsBalance() + points;
        user.setPointsBalance(newBalance);

        levelRepository
                .findTopByPointThresholdLessThanEqualOrderByPointThresholdDesc(newBalance)
                .ifPresent(newLevel -> {
                    if (!newLevel.getId().equals(user.getLevel().getId())) {
                        log.info("Level-up [userId={}, newLevel={}]", user.getId(), newLevel.getLevelName());
                        user.setLevel(newLevel);
                    }
                });

        userRepository.save(user);
    }

    private UserActivityEvent saveEvent(ActivityEventMessage message, User user, ProcessedStatus status) {
        UserActivityEvent event = new UserActivityEvent();
        event.setEventId(message.getEventId());
        event.setUser(user);
        event.setMetricType(message.getMetricType());
        event.setMetricValue(message.getMetricValue());
        event.setEventTime(message.getEventTime());
        event.setProcessedStatus(status);

        if (status != ProcessedStatus.PENDING) {
            event.setProcessedAt(LocalDateTime.now());
        }

        return activityEventRepository.save(event);
    }
}