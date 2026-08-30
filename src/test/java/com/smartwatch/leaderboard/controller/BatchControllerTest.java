package com.smartwatch.leaderboard.controller;

import com.smartwatch.leaderboard.dto.response.BatchTriggerResponse;
import com.smartwatch.leaderboard.service.BatchTriggerService;
import com.smartwatch.leaderboard.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatchController.class)
@Import(BatchControllerTest.TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class BatchControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private BatchTriggerService batchTriggerService;
    @MockBean private JwtUtil jwtUtil;

    // Minimal config: just enables @PreAuthorize processing
    @EnableMethodSecurity
    static class TestSecurityConfig {
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rank_shouldReturn200ForAdmin() throws Exception {
        when(batchTriggerService.triggerRankingJob())
                .thenReturn(BatchTriggerResponse.builder().jobId(1001L).status("STARTED").build());

        mockMvc.perform(get("/rank"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(1001))
                .andExpect(jsonPath("$.status").value("STARTED"));

        verify(batchTriggerService).triggerRankingJob();
    }

    @Test
    @WithMockUser(roles = "USER")
    void rank_shouldReturn403ForNonAdmin() throws Exception {
        mockMvc.perform(get("/rank"))
                .andExpect(status().isForbidden());

        verify(batchTriggerService, never()).triggerRankingJob();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void game_shouldReturn200ForAdmin() throws Exception {
        when(batchTriggerService.triggerGamificationJob())
                .thenReturn(BatchTriggerResponse.builder().jobId(2002L).status("STARTED").build());

        mockMvc.perform(get("/game"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(2002))
                .andExpect(jsonPath("$.status").value("STARTED"));

        verify(batchTriggerService).triggerGamificationJob();
    }

    @Test
    @WithMockUser(roles = "USER")
    void game_shouldReturn403ForNonAdmin() throws Exception {
        mockMvc.perform(get("/game"))
                .andExpect(status().isForbidden());

        verify(batchTriggerService, never()).triggerGamificationJob();
    }
}