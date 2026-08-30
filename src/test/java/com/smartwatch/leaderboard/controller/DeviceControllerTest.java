package com.smartwatch.leaderboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartwatch.leaderboard.dto.request.DeviceRequest;
import com.smartwatch.leaderboard.dto.response.DeviceResponse;
import com.smartwatch.leaderboard.service.DeviceService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeviceController.class)
@Import(DeviceControllerTest.TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class DeviceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private DeviceService deviceService;
    @MockBean private JwtUtil jwtUtil;

    @EnableMethodSecurity
    static class TestSecurityConfig { }

    private DeviceRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new DeviceRequest();
        validRequest.setDeviceName("Garmin Forerunner 965");
        validRequest.setManufacturer("Garmin");
        validRequest.setModel("Forerunner 965");
        validRequest.setCapabilities(List.of("HEART_RATE", "GPS", "STEP_COUNT"));
    }

    private DeviceResponse buildDeviceResponse(Long id, String name) {
        return DeviceResponse.builder()
                .id(id)
                .deviceName(name)
                .manufacturer("Garmin")
                .model("Forerunner 965")
                .capabilities(List.of("HEART_RATE", "GPS", "STEP_COUNT"))
                .build();
    }

    // ========== POST /devices — ADMIN ONLY ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void createDevice_shouldReturn201ForAdmin() throws Exception {
        DeviceResponse mockResponse = buildDeviceResponse(1L, "Garmin Forerunner 965");

        when(deviceService.createDevice(any(DeviceRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.deviceName").value("Garmin Forerunner 965"))
                .andExpect(jsonPath("$.manufacturer").value("Garmin"))
                .andExpect(jsonPath("$.model").value("Forerunner 965"))
                .andExpect(jsonPath("$.capabilities.length()").value(3))
                .andExpect(jsonPath("$.capabilities[0]").value("HEART_RATE"));

        verify(deviceService).createDevice(any(DeviceRequest.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createDevice_shouldReturn403ForUser() throws Exception {
        mockMvc.perform(post("/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());

        verify(deviceService, never()).createDevice(any());
    }

    // ========== GET /devices/ — USER or ADMIN ==========
    // Note: trailing slash is required because controller maps @GetMapping("/")

    @Test
    @WithMockUser(roles = "USER")
    void getAllDevices_shouldReturnListForUser() throws Exception {
        DeviceResponse d1 = buildDeviceResponse(1L, "Garmin Forerunner 965");
        DeviceResponse d2 = buildDeviceResponse(2L, "Apple Watch Series 9");

        when(deviceService.getAllDevices()).thenReturn(List.of(d1, d2));

        mockMvc.perform(get("/devices/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].deviceName").value("Garmin Forerunner 965"))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(deviceService).getAllDevices();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllDevices_shouldReturnListForAdmin() throws Exception {
        when(deviceService.getAllDevices()).thenReturn(List.of(buildDeviceResponse(1L, "X")));

        mockMvc.perform(get("/devices/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllDevices_shouldReturnEmptyList() throws Exception {
        when(deviceService.getAllDevices()).thenReturn(List.of());

        mockMvc.perform(get("/devices/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ========== PUT /devices/{id} — ADMIN ONLY ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateDevice_shouldReturn200ForAdmin() throws Exception {
        DeviceResponse mockResponse = buildDeviceResponse(7L, "Updated Device");

        when(deviceService.updateDevice(eq(7L), any(DeviceRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(put("/devices/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.deviceName").value("Updated Device"));

        verify(deviceService).updateDevice(eq(7L), any(DeviceRequest.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateDevice_shouldReturn403ForUser() throws Exception {
        mockMvc.perform(put("/devices/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());

        verify(deviceService, never()).updateDevice(any(), any());
    }
}
