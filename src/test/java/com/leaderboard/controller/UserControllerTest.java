package com.leaderboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leaderboard.dto.TelemetryEvent;
import com.leaderboard.model.Device;
import com.leaderboard.model.User;
import com.leaderboard.repository.DeviceRepository;
import com.leaderboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@org.springframework.kafka.test.context.EmbeddedKafka(partitions = 1, topics = { "smartwatch-telemetry" })
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User savedUser;

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
        deviceRepository.deleteAll();

        Device device = new Device("Garmin Venu", new HashSet<>(Arrays.asList("GPS", "HRM")));
        Device savedDevice = deviceRepository.save(device);

        User user = new User("9876543210", "user@example.com", "John Doe", "hashedPassword", com.leaderboard.model.Role.ROLE_USER);
        user.setDevice(savedDevice);
        savedUser = userRepository.save(user);
    }

    @Test
    @WithMockUser(username = "9876543210", roles = "USER")
    public void testIngestTelemetry_Success() throws Exception {
        TelemetryEvent event = new TelemetryEvent(5000, "2026-06-20", new HashSet<>(Arrays.asList("GPS", "HRM")));

        mockMvc.perform(post("/user/" + savedUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", equalTo("SUCCESS")))
                .andExpect(jsonPath("$.message", containsString("published to Kafka")));
    }

    @Test
    @WithMockUser(username = "9876543210", roles = "USER")
    public void testIngestTelemetry_MissingStepCount_ThrowsBadRequest() throws Exception {
        TelemetryEvent event = new TelemetryEvent(null, "2026-06-20", new HashSet<>(Arrays.asList("GPS", "HRM")));

        mockMvc.perform(post("/user/" + savedUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Step Count Value is missing")));
    }

    @Test
    @WithMockUser(username = "9876543210", roles = "USER")
    public void testIngestTelemetry_MissingTags_ThrowsBadRequest() throws Exception {
        TelemetryEvent event = new TelemetryEvent(1200, "2026-06-20", new HashSet<>());

        mockMvc.perform(post("/user/" + savedUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Tags are missing")));
    }

    @Test
    @WithMockUser(username = "9876543210", roles = "USER")
    public void testGetUserData_Success() throws Exception {
        mockMvc.perform(get("/user/" + savedUser.getId())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(savedUser.getId().intValue())))
                .andExpect(jsonPath("$.fullName", equalTo("John Doe")))
                .andExpect(jsonPath("$.points", equalTo(0)))
                .andExpect(jsonPath("$.level", equalTo("Novice")))
                .andExpect(jsonPath("$.device.name", equalTo("Garmin Venu")));
    }

    @Test
    public void testGetUserData_Unauthorized_Returns401() throws Exception {
        mockMvc.perform(get("/user/" + savedUser.getId())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
    }
}
