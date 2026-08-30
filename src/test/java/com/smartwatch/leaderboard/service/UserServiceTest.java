package com.smartwatch.leaderboard.service;

import com.smartwatch.leaderboard.dto.kafka.ActivityEventMessage;
import com.smartwatch.leaderboard.dto.request.ActivityEventRequest;
import com.smartwatch.leaderboard.dto.response.ActivityIngestionResponse;
import com.smartwatch.leaderboard.dto.response.UserActivityEventResponse;
import com.smartwatch.leaderboard.dto.response.UserProfileResponse;
import com.smartwatch.leaderboard.model.Device;
import com.smartwatch.leaderboard.model.Level;
import com.smartwatch.leaderboard.model.User;
import com.smartwatch.leaderboard.model.UserActivityEvent;
import com.smartwatch.leaderboard.model.enums.ProcessedStatus;
import com.smartwatch.leaderboard.repository.UserActivityEventRepository;
import com.smartwatch.leaderboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserActivityEventRepository activityEventRepository;
    @Mock private KafkaTemplate<String, ActivityEventMessage> kafkaTemplate;

    @InjectMocks private UserService userService;

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "user@example.com";
    private static final String EVENT_ID = "evt-abc-123";

    private User user;
    private Level level;
    private Device device;

    @BeforeEach
    void setUp() {
        level = Level.builder().id(10L).levelName("BRONZE").pointThreshold(100).build();
        device = Device.builder().id(200L).deviceName("Galaxy Watch").build();
        user = User.builder()
                .id(USER_ID)
                .email(EMAIL)
                .fullName("Jane Doe")
                .level(level)
                .device(device)
                .pointsBalance(250)
                .build();
    }

    // ---------- getUserProfile ----------

    @Nested
    class GetUserProfile {

        @Test
        void shouldReturnProfileWhenUserExists() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            UserProfileResponse response = userService.getUserProfile(USER_ID);

            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getEmail()).isEqualTo(EMAIL);
            assertThat(response.getFullName()).isEqualTo("Jane Doe");
            assertThat(response.getLevelName()).isEqualTo("BRONZE");
            assertThat(response.getPointsBalance()).isEqualTo(250);
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserProfile(USER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
        }
    }

    // ---------- ingestActivity ----------

    @Nested
    class IngestActivity {

        private ActivityEventRequest request;

        @BeforeEach
        void buildRequest() {
            request = new ActivityEventRequest();
            request.setEventId(EVENT_ID);
            request.setMetricType("STEPS");
            request.setMetricValue(7500.0);
            request.setEventTime(LocalDateTime.now().minusMinutes(5));
        }

        @Test
        void shouldPublishToKafkaAndReturnPendingResponse() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            ActivityIngestionResponse response = userService.ingestActivity(USER_ID, request);

            // verify Kafka publication
            ArgumentCaptor<ActivityEventMessage> msgCaptor =
                    ArgumentCaptor.forClass(ActivityEventMessage.class);
            verify(kafkaTemplate).send(eq("events"), eq(EVENT_ID), msgCaptor.capture());

            ActivityEventMessage published = msgCaptor.getValue();
            assertThat(published.getUserId()).isEqualTo(USER_ID);
            assertThat(published.getEventId()).isEqualTo(EVENT_ID);
            assertThat(published.getMetricType()).isEqualTo("STEPS");
            assertThat(published.getMetricValue()).isEqualTo(7500.0);
            assertThat(published.getEventTime()).isEqualTo(request.getEventTime());

            // verify response shape
            assertThat(response.getEventId()).isEqualTo(EVENT_ID);
            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getStatus()).isEqualTo("PENDING");
            assertThat(response.getMessage()).contains("accepted");
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.ingestActivity(USER_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");

            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void shouldThrowWhenUserHasNoDevice() {
            user.setDevice(null);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.ingestActivity(USER_ID, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no device assigned");

            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void shouldUseEventIdAsKafkaPartitionKey() {
            // The Kafka key matters for partitioning/ordering. Lock it down.
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            userService.ingestActivity(USER_ID, request);

            verify(kafkaTemplate).send(eq("events"), eq(EVENT_ID), any(ActivityEventMessage.class));
        }
    }

    // ---------- getActivityEvents ----------

    @Nested
    class GetActivityEvents {

        @Test
        void shouldReturnMappedEventsForUser() {
            UserActivityEvent event1 = UserActivityEvent.builder()
                    .eventId("evt-1").metricType("STEPS").metricValue(5000.0)
                    .eventTime(LocalDateTime.now().minusHours(2))
                    .processedStatus(ProcessedStatus.PROCESSED)
                    .processedAt(LocalDateTime.now().minusHours(1))
                    .build();
            UserActivityEvent event2 = UserActivityEvent.builder()
                    .eventId("evt-2").metricType("HEART_RATE").metricValue(72.0)
                    .eventTime(LocalDateTime.now().minusHours(1))
                    .processedStatus(ProcessedStatus.PENDING)
                    .build();

            when(activityEventRepository.findByUserIdOrderByEventTimeDesc(USER_ID))
                    .thenReturn(List.of(event1, event2));

            List<UserActivityEventResponse> responses = userService.getActivityEvents(USER_ID);

            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(UserActivityEventResponse::getEventId)
                    .containsExactly("evt-1", "evt-2");
            assertThat(responses).extracting(UserActivityEventResponse::getMetricType)
                    .containsExactly("STEPS", "HEART_RATE");
            assertThat(responses).extracting(UserActivityEventResponse::getProcessedStatus)
                    .containsExactly(ProcessedStatus.PROCESSED, ProcessedStatus.PENDING);
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoEvents() {
            when(activityEventRepository.findByUserIdOrderByEventTimeDesc(USER_ID))
                    .thenReturn(Collections.emptyList());

            assertThat(userService.getActivityEvents(USER_ID)).isEmpty();
        }
    }
}