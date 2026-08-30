package com.smartwatch.leaderboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartwatch.leaderboard.dto.request.TaskRequest;
import com.smartwatch.leaderboard.dto.response.TaskResponse;
import com.smartwatch.leaderboard.model.enums.TaskStatus;
import com.smartwatch.leaderboard.service.TaskService;
import com.smartwatch.leaderboard.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

@WebMvcTest(TaskController.class)
@Import(TaskControllerTest.TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private TaskService taskService;
    @MockBean private JwtUtil jwtUtil;

    @EnableMethodSecurity
    static class TestSecurityConfig { }

    private TaskRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new TaskRequest();
        validRequest.setName("Walk 10K Steps");
        validRequest.setDescription("Daily step goal");
        validRequest.setRequiredLevelId(1L);
        validRequest.setRequiredMetric("STEPS");
        validRequest.setTargetValue(10000.0);
        validRequest.setRewardPoints(50);
        validRequest.setStatus("ACTIVE");
    }

    private TaskResponse buildTaskResponse(Long id, String name) {
        return TaskResponse.builder()
                .id(id)
                .name(name)
                .description("desc")
                .requiredLevelId(1L)
                .requiredLevelName("Beginner")
                .requiredMetric("STEPS")
                .targetValue(10000.0)
                .rewardPoints(50)
                .status(TaskStatus.ACTIVE)
                .build();
    }

    // ========== POST /tasks — ADMIN ONLY ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void createTask_shouldReturn201ForAdmin() throws Exception {
        TaskResponse mockResponse = buildTaskResponse(1L, "Walk 10K Steps");

        when(taskService.createTask(any(TaskRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Walk 10K Steps"))
                .andExpect(jsonPath("$.requiredMetric").value("STEPS"))
                .andExpect(jsonPath("$.targetValue").value(10000.0))
                .andExpect(jsonPath("$.rewardPoints").value(50))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(taskService).createTask(any(TaskRequest.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createTask_shouldReturn403ForUser() throws Exception {
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());

        verify(taskService, never()).createTask(any());
    }

    // ========== GET /tasks ==========

    @Test
    @WithMockUser(roles = "USER")
    void getTasks_shouldReturnListForUser() throws Exception {
        TaskResponse t1 = buildTaskResponse(1L, "Task 1");
        TaskResponse t2 = buildTaskResponse(2L, "Task 2");

        when(taskService.getAllTasks()).thenReturn(List.of(t1, t2));

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(taskService).getAllTasks();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getTasks_shouldReturnListForAdmin() throws Exception {
        when(taskService.getAllTasks()).thenReturn(List.of(buildTaskResponse(1L, "X")));

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getTasks_shouldReturnEmptyList() throws Exception {
        when(taskService.getAllTasks()).thenReturn(List.of());

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ========== GET /tasks/{id} ==========

    @Test
    @WithMockUser(roles = "USER")
    void getTaskById_shouldReturn200() throws Exception {
        TaskResponse mockResponse = buildTaskResponse(42L, "Found Task");

        when(taskService.getTaskById(42L)).thenReturn(mockResponse);

        mockMvc.perform(get("/tasks/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.name").value("Found Task"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(taskService).getTaskById(42L);
    }

    // ========== PUT /tasks/{id} — ADMIN ONLY ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateTask_shouldReturn200ForAdmin() throws Exception {
        TaskResponse mockResponse = buildTaskResponse(7L, "Updated Task");

        when(taskService.updateTask(eq(7L), any(TaskRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(put("/tasks/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Updated Task"));

        verify(taskService).updateTask(eq(7L), any(TaskRequest.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateTask_shouldReturn403ForUser() throws Exception {
        mockMvc.perform(put("/tasks/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());

        verify(taskService, never()).updateTask(any(), any());
    }

    // ========== GET /tasks/user/{userId} — paginated ==========
    // NOTE: Spring Boot 3.3+ logs a warning about direct Page<T> serialization.
    // The JSON shape (content, totalElements, etc.) is stable for now but may
    // change in future versions — consider PagedModel<T> or a custom DTO.

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void getTasksForUser_shouldReturnPagedResponseWithDefaultPageable() throws Exception {
        TaskResponse t1 = buildTaskResponse(1L, "User Task 1");
        TaskResponse t2 = buildTaskResponse(2L, "User Task 2");

        Pageable defaultPageable = PageRequest.of(0, 10, Sort.by("name"));
        Page<TaskResponse> page = new PageImpl<>(List.of(t1, t2), defaultPageable, 2);

        when(taskService.getTasksForUser(eq(99L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/tasks/user/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("User Task 1"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10));

        verify(taskService).getTasksForUser(eq(99L), any(Pageable.class));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void getTasksForUser_shouldRespectCustomPageableParams() throws Exception {
        TaskResponse t1 = buildTaskResponse(3L, "Page 2 Task");

        Pageable customPageable = PageRequest.of(1, 5, Sort.by("name").descending());
        Page<TaskResponse> page = new PageImpl<>(List.of(t1), customPageable, 6);

        when(taskService.getTasksForUser(eq(99L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/tasks/user/99")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "name,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(3))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void getTasksForUser_shouldReturnEmptyPage() throws Exception {
        Page<TaskResponse> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(taskService.getTasksForUser(eq(99L), any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/tasks/user/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // TODO: Authorization gap — currently any USER can call /tasks/user/{anyOtherUserId}.
    // Consider @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    // or a service-layer ownership check.
}
