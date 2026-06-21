package com.leaderboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leaderboard.model.*;
import com.leaderboard.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.kafka.test.context.EmbeddedKafka(partitions = 1, topics = { "smartwatch-telemetry" })
public class MiscellaneousControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private UserChallengeRepository userChallengeRepository;

    @Autowired
    private UserTaskRepository userTaskRepository;

    @Autowired
    private LeaderboardRepository leaderboardRepository;

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private UserActivityRepository userActivityRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User savedUser;
    private Device savedDevice;
    private Task savedTask;
    private Challenge savedChallenge;

    @BeforeEach
    public void setup() {
        leaderboardRepository.deleteAll();
        userChallengeRepository.deleteAll();
        userTaskRepository.deleteAll();
        badgeRepository.deleteAll();
        userActivityRepository.deleteAll();
        userRepository.deleteAll();
        deviceRepository.deleteAll();
        taskRepository.deleteAll();
        challengeRepository.deleteAll();

        // 1. Create a Device
        Device device = new Device("Garmin Active", new HashSet<>(Arrays.asList("GPS", "HRM", "ACCELEROMETER")));
        savedDevice = deviceRepository.save(device);

        // 2. Create a User
        User user = new User("8888888888", "misc@example.com", "Misc User", "password", Role.ROLE_USER);
        savedUser = userRepository.save(user);

        // 3. Create a Task
        Task task = new Task("Daily Walk", "Walk 5k steps", 5000, 50);
        savedTask = taskRepository.save(task);

        // 4. Create a Challenge
        Challenge challenge = new Challenge();
        challenge.setTitle("Marathon");
        challenge.setDescription("Run 42k steps");
        challenge.setRequiredSteps(42000);
        challenge.setPointsReward(500);
        challenge.setExpiryDate(LocalDateTime.now().plusDays(2));
        challenge.setRequiredFeatures(new HashSet<>(Arrays.asList("GPS", "HRM")));
        savedChallenge = challengeRepository.save(challenge);
    }

    @Test
    public void testGetHealth_Public() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("UP")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @WithMockUser(username = "8888888888", roles = "USER")
    public void testGetAllDevices() throws Exception {
        mockMvc.perform(get("/device"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testCreateAndUpdateDevice_Admin() throws Exception {
        Device device = new Device("Fitbit Sense", new HashSet<>(Arrays.asList("HRM", "SPO2")));

        mockMvc.perform(post("/device")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(device)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", equalTo("Fitbit Sense")));

        device.setFeatures(new HashSet<>(Arrays.asList("HRM", "SPO2", "ECG")));
        mockMvc.perform(put("/device")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(device)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features", hasItem("ECG")));
    }

    @Test
    @WithMockUser(username = "8888888888", roles = "USER")
    public void testCreateDevice_User_Forbidden() throws Exception {
        Device device = new Device("Fitbit Sense", new HashSet<>(Arrays.asList("HRM")));
        mockMvc.perform(post("/device")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(device)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testTaskCrud_Admin() throws Exception {
        // Create Task
        Task task = new Task("Gym Time", "Lift weights for 30m", 3000, 30);
        mockMvc.perform(post("/task")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", equalTo("Gym Time")));

        // Update Task
        task.setPointsReward(40);
        mockMvc.perform(put("/task/" + savedTask.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsReward", equalTo(40)));
    }

    @Test
    @WithMockUser(username = "8888888888", roles = "USER")
    public void testTaskQuery_User() throws Exception {
        mockMvc.perform(get("/task"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/task/" + savedTask.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", equalTo("Daily Walk")));

        mockMvc.perform(get("/task/" + savedTask.getId() + "/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "8888888888", roles = "USER")
    public void testChallengeCrud_User() throws Exception {
        // Create Challenge
        Challenge challenge = new Challenge();
        challenge.setTitle("User Challenge");
        challenge.setDescription("Created by user");
        challenge.setRequiredSteps(10000);
        challenge.setPointsReward(100);
        challenge.setExpiryDate(LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/challenge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(challenge)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", equalTo("User Challenge")));

        // Get challenge list (discovery)
        mockMvc.perform(get("/challenge")
                .param("userId", savedUser.getId().toString())
                .param("city", "Mumbai")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Get challenge detail
        mockMvc.perform(get("/challenge/" + savedChallenge.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", equalTo("Marathon")));

        // Get challenge users
        mockMvc.perform(get("/challenge/" + savedChallenge.getId() + "/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testChallengeUpdate_Admin() throws Exception {
        savedChallenge.setPointsReward(600);
        mockMvc.perform(put("/challenge/" + savedChallenge.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(savedChallenge)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsReward", equalTo(600)));
    }

    @Test
    @WithMockUser(username = "8888888888", roles = "USER")
    public void testDeviceAssociationAndRegistration() throws Exception {
        // 1. Associate Device
        mockMvc.perform(put("/user/" + savedUser.getId() + "/device")
                .param("deviceId", savedDevice.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.device.name", equalTo("Garmin Active")));

        // 2. Register for Task
        mockMvc.perform(post("/task/" + savedTask.getId() + "/register")
                .param("userId", savedUser.getId().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.task.title", equalTo("Daily Walk")));

        // 3. Register for Challenge
        mockMvc.perform(post("/challenge/" + savedChallenge.getId() + "/register")
                .param("userId", savedUser.getId().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.challenge.title", equalTo("Marathon")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testBatchJobsEndpoints_Admin() throws Exception {
        mockMvc.perform(get("/rank"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("SUCCESS")));

        mockMvc.perform(get("/game"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("SUCCESS")));
    }
}
