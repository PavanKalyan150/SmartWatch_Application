package com.smartwatch.leaderboard.service;

import com.smartwatch.leaderboard.dto.kafka.ActivityEventMessage;
import com.smartwatch.leaderboard.dto.request.ActivityEventRequest;
import com.smartwatch.leaderboard.dto.response.ActivityIngestionResponse;
import com.smartwatch.leaderboard.dto.response.UserActivityEventResponse;
import com.smartwatch.leaderboard.dto.response.UserProfileResponse;
import com.smartwatch.leaderboard.model.User;
import com.smartwatch.leaderboard.repository.UserActivityEventRepository;
import com.smartwatch.leaderboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserActivityEventRepository activityEventRepository;
    private final KafkaTemplate<String, ActivityEventMessage> kafkaTemplate; // fixed type

    private static final String ACTIVITY_TOPIC = "events";

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = findUserById(userId);
        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .levelName(user.getLevel().getLevelName())
                .pointsBalance(user.getPointsBalance())
                .build();
    }

    public ActivityIngestionResponse ingestActivity(Long userId, ActivityEventRequest request) {
        User user = findUserById(userId);

        if (user.getDevice() == null) {
            throw new IllegalStateException("User has no device assigned, cannot ingest activity");
        }

        ActivityEventMessage message = toMessage(userId, request);
        kafkaTemplate.send(ACTIVITY_TOPIC, message.getEventId(), message);

        return ActivityIngestionResponse.builder()
                .eventId(request.getEventId())
                .userId(userId)
                .message("Activity event accepted and published")
                .status("PENDING")
                .build();
    }

    private ActivityEventMessage toMessage(Long userId, ActivityEventRequest request) {
        ActivityEventMessage message = new ActivityEventMessage();
        message.setUserId(userId);
        message.setEventId(request.getEventId());
        message.setMetricType(request.getMetricType());
        message.setMetricValue(request.getMetricValue());
        message.setEventTime(request.getEventTime());
        return message;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    public List<UserActivityEventResponse> getActivityEvents(Long userId) {
        return activityEventRepository.findByUserIdOrderByEventTimeDesc(userId)
                .stream()
                .map(e -> UserActivityEventResponse.builder()
                        .eventId(e.getEventId())
                        .metricType(e.getMetricType())
                        .metricValue(e.getMetricValue())
                        .eventTime(e.getEventTime())
                        .processedStatus(e.getProcessedStatus())
                        .processedAt(e.getProcessedAt())
                        .build())
                .toList();
    }
}