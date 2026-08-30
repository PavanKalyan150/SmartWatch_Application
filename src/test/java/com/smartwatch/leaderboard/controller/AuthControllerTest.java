package com.smartwatch.leaderboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartwatch.leaderboard.dto.request.LoginRequest;
import com.smartwatch.leaderboard.dto.request.RegisterRequest;
import com.smartwatch.leaderboard.dto.response.AuthResponse;
import com.smartwatch.leaderboard.service.AuthService;
import com.smartwatch.leaderboard.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(AuthControllerWebMvcTest.TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerWebMvcTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private JwtUtil jwtUtil;

    @EnableMethodSecurity
    static class TestSecurityConfig { }

    private RegisterRequest validRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("user@example.com");
        req.setPhone("9876543210");
        req.setFullName("Jane Doe");
        req.setPassword("Secret@123");
        req.setDeviceId(101L);
        return req;
    }

    // ---------- /auth/register ----------

    @Test
    void register_shouldReturn201WithToken() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse("jwt-token-xyz"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token-xyz"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void register_shouldReturn400WhenEmailMissing() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setEmail(null);   // invalidate just the email

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any());
    }

    @Test
    void register_shouldReturn400WhenPasswordTooShort() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setPassword("short");   // < 8 chars

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any());
    }

    @Test
    void register_shouldReturn400WhenDeviceIdMissing() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setDeviceId(null);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any());
    }

    @Test
    void register_shouldReturn400WhenBodyMalformed() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not valid json"))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any());
    }

    // ---------- /auth/login ----------

    @Test
    void login_shouldReturn200WithToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@example.com");
        req.setPassword("Secret@123");

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("jwt-token-xyz"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-xyz"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void login_shouldReturn400WhenEmailMissing() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setPassword("Secret@123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any());
    }

    // ---------- /auth/logout ----------

    @Test
    void logout_shouldReturn204() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent());

        verify(authService, never()).register(any());
        verify(authService, never()).login(any());
    }
}