package com.smartwatch.leaderboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartwatch.leaderboard.dto.request.ActivityEventRequest;
import com.smartwatch.leaderboard.dto.response.ActivityIngestionResponse;
import com.smartwatch.leaderboard.dto.response.UserActivityEventResponse;
import com.smartwatch.leaderboard.model.enums.ProcessedStatus;
import com.smartwatch.leaderboard.service.UserService;
import com.smartwatch.leaderboard.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(UserControllerTest.TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;
    @MockBean private JwtUtil jwtUtil;

    @EnableMethodSecurity
    static class TestSecurityConfig { }

    private ActivityEventRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new ActivityEventRequest();
        validRequest.setEventId("evt-12345");
        validRequest.setMetricType("STEPS");
        validRequest.setMetricValue(8500.0);
        validRequest.setEventTime(LocalDateTime.of(2026, 5, 20, 14, 30, 0));
    }

    private ActivityIngestionResponse buildIngestionResponse(String eventId, Long userId) {
        return ActivityIngestionResponse.builder()
                .eventId(eventId)
                .message("Event accepted and queued for processing")
                .status("PENDING")
                .userId(userId)
                .build();
    }

    private UserActivityEventResponse buildActivityEvent(String eventId,
                                                         String metric,
                                                         ProcessedStatus status) {
        return UserActivityEventResponse.builder()
                .eventId(eventId)
                .metricType(metric)
                .metricValue(8500.0)
                .eventTime(LocalDateTime.of(2026, 5, 20, 14, 30, 0))
                .processedStatus(status)
                .processedAt(status == ProcessedStatus.PROCESSED
                        ? LocalDateTime.of(2026, 5, 20, 14, 30, 5)
                        : null)
                .build();
    }

    // ========== POST /user/{userId}/activity ==========

    @Test
    @WithMockUser(roles = "USER")
    void ingestActivity_shouldReturn202ForUser() throws Exception {
        ActivityIngestionResponse mockResponse = buildIngestionResponse("evt-12345", 99L);

        when(userService.ingestActivity(eq(99L), any(ActivityEventRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/user/99/activity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventId").value("evt-12345"))
                .andExpect(jsonPath("$.userId").value(99))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value("Event accepted and queued for processing"));

        verify(userService).ingestActivity(eq(99L), any(ActivityEventRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ingestActivity_shouldReturn202ForAdmin() throws Exception {
        ActivityIngestionResponse mockResponse = buildIngestionResponse("evt-67890", 1L);

        when(userService.ingestActivity(eq(1L), any(ActivityEventRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/user/1/activity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventId").value("evt-67890"))
                .andExpect(jsonPath("$.userId").value(1));

        verify(userService).ingestActivity(eq(1L), any(ActivityEventRequest.class));
    }

    @Test
    void ingestActivity_shouldReturn401WhenUnauthenticated() throws Exception {
        // No @WithMockUser — anonymous request.
        // GlobalExceptionHandler maps AuthenticationCredentialsNotFoundException → 401.
        mockMvc.perform(post("/user/99/activity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"));

        verify(userService, never()).ingestActivity(any(), any());
    }

    // ========== GET /user/{userId}/activity ==========

    @Test
    @WithMockUser(roles = "USER")
    void getUserActivityEvents_shouldReturnListForUser() throws Exception {
        UserActivityEventResponse e1 = buildActivityEvent("evt-1", "STEPS", ProcessedStatus.PROCESSED);
        UserActivityEventResponse e2 = buildActivityEvent("evt-2", "HRM", ProcessedStatus.PENDING);
        UserActivityEventResponse e3 = buildActivityEvent("evt-3", "SLEEP", ProcessedStatus.DEAD_LETTER);

        when(userService.getActivityEvents(99L)).thenReturn(List.of(e1, e2, e3));

        mockMvc.perform(get("/user/99/activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].eventId").value("evt-1"))
                .andExpect(jsonPath("$[0].metricType").value("STEPS"))
                .andExpect(jsonPath("$[0].processedStatus").value("PROCESSED"))
                .andExpect(jsonPath("$[1].processedStatus").value("PENDING"))
                .andExpect(jsonPath("$[1].processedAt").doesNotExist())
                .andExpect(jsonPath("$[2].processedStatus").value("DEAD_LETTER"));

        verify(userService).getActivityEvents(99L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserActivityEvents_shouldReturnListForAdmin() throws Exception {
        UserActivityEventResponse e1 = buildActivityEvent("evt-1", "STEPS", ProcessedStatus.PROCESSED);

        when(userService.getActivityEvents(99L)).thenReturn(List.of(e1));

        mockMvc.perform(get("/user/99/activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventId").value("evt-1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getUserActivityEvents_shouldReturnEmptyList() throws Exception {
        when(userService.getActivityEvents(99L)).thenReturn(List.of());

        mockMvc.perform(get("/user/99/activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(userService).getActivityEvents(99L);
    }

    @Test
    void getUserActivityEvents_shouldReturn401WhenUnauthenticated() throws Exception {
        // No @WithMockUser — anonymous request.
        // GlobalExceptionHandler maps AuthenticationCredentialsNotFoundException → 401.
        mockMvc.perform(get("/user/99/activity"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"));

        verify(userService, never()).getActivityEvents(any());
    }

    // TODO: Authorization gap — currently any authenticated USER can:
    //   1. Ingest activity for any other userId (leaderboard integrity risk)
    //   2. Read activity events for any other userId (privacy risk)
    // Consider @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    // on both endpoints.
}
