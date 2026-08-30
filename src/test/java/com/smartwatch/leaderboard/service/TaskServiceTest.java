package com.smartwatch.leaderboard.service;

import com.smartwatch.leaderboard.dto.request.TaskRequest;
import com.smartwatch.leaderboard.dto.response.TaskResponse;
import com.smartwatch.leaderboard.model.*;
import com.smartwatch.leaderboard.model.enums.TaskStatus;
import com.smartwatch.leaderboard.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private LevelRepository levelRepository;
    @Mock private DeviceCapabilityRepository deviceCapabilityRepository;

    @InjectMocks private TaskService taskService;

    private static final Long USER_ID = 1L;
    private static final Long TASK_ID = 50L;
    private static final Long LEVEL_ID = 10L;
    private static final Long DEVICE_ID = 100L;
    private static final int LEVEL_THRESHOLD = 1000;
    private static final int REWARD_POINTS = 50;

    private Level level;
    private Device device;
    private User user;
    private Task task;
    private TaskRequest taskRequest;

    @BeforeEach
    void setUp() {
        level = Level.builder().id(LEVEL_ID).pointThreshold(LEVEL_THRESHOLD).build();
        device = Device.builder().id(DEVICE_ID).build();
        user = User.builder().id(USER_ID).level(level).device(device).build();

        task = Task.builder()
                .id(TASK_ID)
                .name("10k Steps")
                .description("Walk 10,000 steps")
                .requiredMetric("STEPS")
                .requiredLevel(level)
                .rewardPoints(REWARD_POINTS)
                .status(TaskStatus.ACTIVE)
                .build();

        taskRequest = new TaskRequest();
        taskRequest.setName("10k Steps");
        taskRequest.setDescription("Walk 10,000 steps");
        taskRequest.setRequiredMetric("STEPS");
        taskRequest.setRequiredLevelId(LEVEL_ID);
        taskRequest.setRewardPoints(REWARD_POINTS);
    }

    // ---------- createTask ----------

    @Nested
    class CreateTask {

        @Test
        void shouldCreateTaskWhenLevelExists() {
            when(levelRepository.findById(LEVEL_ID)).thenReturn(Optional.of(level));

            TaskResponse response = taskService.createTask(taskRequest);

            ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository).save(captor.capture());
            Task saved = captor.getValue();

            assertThat(saved.getName()).isEqualTo("10k Steps");
            assertThat(saved.getRequiredMetric()).isEqualTo("STEPS");
            assertThat(saved.getRequiredLevel()).isSameAs(level);
            assertThat(saved.getRewardPoints()).isEqualTo(REWARD_POINTS);
            assertThat(saved.getStatus()).isEqualTo(TaskStatus.ACTIVE);

            assertThat(response.getName()).isEqualTo("10k Steps");
            assertThat(response.getRequiredLevelId()).isEqualTo(LEVEL_ID);
            assertThat(response.getStatus()).isEqualTo(TaskStatus.ACTIVE);
        }

        @Test
        void shouldThrowWhenRequiredLevelNotFound() {
            when(levelRepository.findById(LEVEL_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.createTask(taskRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Level not found")
                    .hasMessageContaining(String.valueOf(LEVEL_ID));

            verify(taskRepository, never()).save(any());
        }
    }

    // ---------- getAllTasks ----------

    @Nested
    class GetAllTasks {

        @Test
        void shouldReturnMappedListOfTasks() {
            Task second = Task.builder()
                    .id(51L).name("Sleep 8h").requiredMetric("SLEEP")
                    .requiredLevel(level).rewardPoints(20)
                    .status(TaskStatus.ACTIVE).build();
            when(taskRepository.findAll()).thenReturn(List.of(task, second));

            List<TaskResponse> responses = taskService.getAllTasks();

            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(TaskResponse::getName)
                    .containsExactly("10k Steps", "Sleep 8h");
            assertThat(responses).extracting(TaskResponse::getRequiredMetric)
                    .containsExactly("STEPS", "SLEEP");
        }

        @Test
        void shouldReturnEmptyListWhenNoTasksExist() {
            when(taskRepository.findAll()).thenReturn(Collections.emptyList());

            assertThat(taskService.getAllTasks()).isEmpty();
        }
    }

    // ---------- getTasksForUser ----------

    @Nested
    class GetTasksForUser {

        @Test
        void shouldReturnPagedEligibleTasksForUser() {
            DeviceCapability cap1 = DeviceCapability.builder()
                    .capabilityCode("STEPS").build();
            DeviceCapability cap2 = DeviceCapability.builder()
                    .capabilityCode("HEART_RATE").build();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Task> taskPage = new PageImpl<>(List.of(task), pageable, 1);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(deviceCapabilityRepository.findByDeviceId(DEVICE_ID))
                    .thenReturn(List.of(cap1, cap2));
            when(taskRepository.findEligibleTasksForUser(
                    eq(TaskStatus.ACTIVE),
                    eq(LEVEL_THRESHOLD),
                    eq(Set.of("STEPS", "HEART_RATE")),
                    eq(pageable)
            )).thenReturn(taskPage);

            Page<TaskResponse> result = taskService.getTasksForUser(USER_ID, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("10k Steps");
        }

        @Test
        void shouldPassEmptyCapabilitySetWhenDeviceHasNoCapabilities() {
            Pageable pageable = PageRequest.of(0, 5);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(deviceCapabilityRepository.findByDeviceId(DEVICE_ID))
                    .thenReturn(Collections.emptyList());
            when(taskRepository.findEligibleTasksForUser(
                    any(TaskStatus.class), any(Integer.class), any(Set.class), any(Pageable.class)
            )).thenReturn(Page.empty(pageable));

            Page<TaskResponse> result = taskService.getTasksForUser(USER_ID, pageable);

            assertThat(result.getContent()).isEmpty();

            // Capture the actual Set passed in to confirm it was empty
            ArgumentCaptor<Set<String>> capCaptor = ArgumentCaptor.forClass(Set.class);
            verify(taskRepository).findEligibleTasksForUser(
                    eq(TaskStatus.ACTIVE),
                    eq(LEVEL_THRESHOLD),
                    capCaptor.capture(),
                    eq(pageable));
            assertThat(capCaptor.getValue()).isEmpty();
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    taskService.getTasksForUser(USER_ID, PageRequest.of(0, 10)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");

            verifyNoInteractions(deviceCapabilityRepository);
            verify(taskRepository, never()).findEligibleTasksForUser(
                    any(), any(Integer.class), any(), any());
        }
    }

    // ---------- getTaskById ----------

    @Nested
    class GetTaskById {

        @Test
        void shouldReturnTaskWhenFound() {
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));

            TaskResponse response = taskService.getTaskById(TASK_ID);

            assertThat(response.getId()).isEqualTo(TASK_ID);
            assertThat(response.getName()).isEqualTo("10k Steps");
        }

        @Test
        void shouldThrowWhenTaskNotFound() {
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getTaskById(TASK_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Task not found");
        }
    }

    // ---------- updateTask ----------

    @Nested
    class UpdateTask {

        @Test
        void shouldUpdateAllFieldsWhenTaskAndLevelExist() {
            Level newLevel = Level.builder().id(20L).pointThreshold(2000).build();
            taskRequest.setName("20k Steps");
            taskRequest.setDescription("Walk 20,000 steps");
            taskRequest.setRequiredLevelId(20L);
            taskRequest.setRewardPoints(100);

            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            when(levelRepository.findById(20L)).thenReturn(Optional.of(newLevel));

            TaskResponse response = taskService.updateTask(TASK_ID, taskRequest);

            verify(taskRepository).save(task);
            assertThat(task.getName()).isEqualTo("20k Steps");
            assertThat(task.getDescription()).isEqualTo("Walk 20,000 steps");
            assertThat(task.getRequiredLevel()).isSameAs(newLevel);
            assertThat(task.getRewardPoints()).isEqualTo(100);
            assertThat(response.getName()).isEqualTo("20k Steps");
            assertThat(response.getRequiredLevelId()).isEqualTo(20L);
        }

        @Test
        void shouldThrowWhenTaskNotFound() {
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateTask(TASK_ID, taskRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Task not found");

            verifyNoInteractions(levelRepository);
            verify(taskRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenLevelNotFoundDuringUpdate() {
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            when(levelRepository.findById(LEVEL_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateTask(TASK_ID, taskRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Level not found");

            verify(taskRepository, never()).save(any());
        }
    }
}