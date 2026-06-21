package com.leaderboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leaderboard.dto.LoginRequest;
import com.leaderboard.dto.RegisterRequest;
import com.leaderboard.model.Role;
import com.leaderboard.model.User;
import com.leaderboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@org.springframework.kafka.test.context.EmbeddedKafka(partitions = 1, topics = { "smartwatch-telemetry" })
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void cleanup() {
        userRepository.deleteAll();
    }

    @Test
    public void testRegisterUser_Success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setPhone("1234567890");
        request.setEmail("test@example.com");
        request.setFullName("Test User");
        request.setPassword("password123");
        request.setRole(Role.ROLE_USER);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.phone", equalTo("******7890"))) // Serialized masking check!
                .andExpect(jsonPath("$.email", equalTo("t***t@example.com")));
    }

    @Test
    public void testRegisterUser_DuplicatePhone_ThrowsBadRequest() throws Exception {
        User user = new User("1234567890", "existing@example.com", "Name", passwordEncoder.encode("pwd"), Role.ROLE_USER);
        userRepository.save(user);

        RegisterRequest request = new RegisterRequest();
        request.setPhone("1234567890");
        request.setEmail("new@example.com");
        request.setFullName("New User");
        request.setPassword("password");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Phone number already registered")));
    }

    @Test
    public void testLogin_Success() throws Exception {
        User user = new User("1234567890", "test@example.com", "Test User", passwordEncoder.encode("password123"), Role.ROLE_USER);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhone("1234567890");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", nullValue()));
    }

    @Test
    public void testLogin_BadCredentials_ThrowsUnauthorized() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhone("1234567890");
        loginRequest.setPassword("wrongpwd");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
    }
}
