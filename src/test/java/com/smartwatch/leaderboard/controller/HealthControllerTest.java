package com.smartwatch.leaderboard.controller;

import com.smartwatch.leaderboard.dto.response.HealthResponse;
import com.smartwatch.leaderboard.dto.response.HealthResponse.ComponentHealth;
import com.smartwatch.leaderboard.service.HealthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private HealthService healthService;

    @InjectMocks
    private HealthController healthController;

    // ---------- Happy path ----------

    @Test
    void health_shouldReturn200_whenAllComponentsUp() {
        HealthResponse healthy = new HealthResponse(
                "UP",
                Instant.parse("2026-05-21T00:00:00Z"),
                Map.of(
                        "database", new ComponentHealth("UP", null),
                        "kafka",    new ComponentHealth("UP", "clusterId=abc, nodes=3")
                )
        );
        when(healthService.checkHealth()).thenReturn(healthy);

        ResponseEntity<HealthResponse> response = healthController.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("UP");
        assertThat(response.getBody().getComponents())
                .containsKeys("database", "kafka")
                .hasSize(2);
        assertThat(response.getBody().getComponents().get("database").getStatus()).isEqualTo("UP");
        assertThat(response.getBody().getComponents().get("kafka").getStatus()).isEqualTo("UP");

        verify(healthService).checkHealth();
        verifyNoMoreInteractions(healthService);
    }

    // ---------- Degraded scenarios → 503 ----------

    @Test
    void health_shouldReturn503_whenDatabaseDown() {
        HealthResponse degraded = new HealthResponse(
                "DOWN",
                Instant.now(),
                Map.of(
                        "database", new ComponentHealth("DOWN", "Connection refused"),
                        "kafka",    new ComponentHealth("UP", null)
                )
        );
        when(healthService.checkHealth()).thenReturn(degraded);

        ResponseEntity<HealthResponse> response = healthController.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("DOWN");
        assertThat(response.getBody().getComponents().get("database").getStatus()).isEqualTo("DOWN");
        assertThat(response.getBody().getComponents().get("database").getDetails())
                .isEqualTo("Connection refused");
        assertThat(response.getBody().getComponents().get("kafka").getStatus()).isEqualTo("UP");
    }

    @Test
    void health_shouldReturn503_whenKafkaDown() {
        HealthResponse degraded = new HealthResponse(
                "DOWN",
                Instant.now(),
                Map.of(
                        "database", new ComponentHealth("UP", null),
                        "kafka",    new ComponentHealth("DOWN", "Timed out waiting for node assignment")
                )
        );
        when(healthService.checkHealth()).thenReturn(degraded);

        ResponseEntity<HealthResponse> response = healthController.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getStatus()).isEqualTo("DOWN");
        assertThat(response.getBody().getComponents().get("kafka").getStatus()).isEqualTo("DOWN");
        assertThat(response.getBody().getComponents().get("kafka").getDetails())
                .contains("Timed out");
    }

    @Test
    void health_shouldReturn503_whenAllComponentsDown() {
        HealthResponse allDown = new HealthResponse(
                "DOWN",
                Instant.now(),
                Map.of(
                        "database", new ComponentHealth("DOWN", "Connection refused"),
                        "kafka",    new ComponentHealth("DOWN", "Broker unreachable")
                )
        );
        when(healthService.checkHealth()).thenReturn(allDown);

        ResponseEntity<HealthResponse> response = healthController.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getComponents().values())
                .allMatch(c -> "DOWN".equals(c.getStatus()));
    }

    // ---------- Edge cases ----------

    @Test
    void health_shouldPropagateTimestampFromService() {
        Instant fixedTime = Instant.parse("2026-05-21T12:34:56Z");
        HealthResponse healthy = new HealthResponse(
                "UP",
                fixedTime,
                Map.of("database", new ComponentHealth("UP", null),
                        "kafka",    new ComponentHealth("UP", null))
        );
        when(healthService.checkHealth()).thenReturn(healthy);

        ResponseEntity<HealthResponse> response = healthController.health();

        assertThat(response.getBody().getTimestamp()).isEqualTo(fixedTime);
    }

    @Test
    void health_shouldCallServiceExactlyOncePerRequest() {
        HealthResponse healthy = new HealthResponse(
                "UP",
                Instant.now(),
                Map.of("database", new ComponentHealth("UP", null),
                        "kafka",    new ComponentHealth("UP", null))
        );
        when(healthService.checkHealth()).thenReturn(healthy);

        healthController.health();

        verify(healthService).checkHealth();
        verifyNoMoreInteractions(healthService);
    }
}
