package com.smartwatch.leaderboard.service;

import com.smartwatch.leaderboard.dto.request.TaskRequest;
import com.smartwatch.leaderboard.dto.response.TaskResponse;
import com.smartwatch.leaderboard.dto.response.UserTaskProgressResponse;
import com.smartwatch.leaderboard.model.*;
import com.smartwatch.leaderboard.model.enums.TaskStatus;
import com.smartwatch.leaderboard.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final LevelRepository levelRepository;
    private final DeviceCapabilityRepository deviceCapabilityRepository;

    @Transactional
    public TaskResponse createTask(TaskRequest request) {
        Level requiredLevel = findLevelById(request.getRequiredLevelId());

        Task task = Task.builder()
                .name(request.getName())
                .description(request.getDescription())
                .requiredMetric(request.getRequiredMetric())
                .requiredLevel(requiredLevel)
                .targetValue(request.getTargetValue())
                .rewardPoints(request.getRewardPoints())
                .status(TaskStatus.ACTIVE)
                .build();

        taskRepository.save(task);
        return mapToTaskResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks() {
        return taskRepository.findAll()
                .stream()
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksForUser(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Set<String> userCapabilities = deviceCapabilityRepository
                .findByDeviceId(user.getDevice().getId())
                .stream()
                .map(DeviceCapability::getCapabilityCode)
                .collect(Collectors.toSet());

        return taskRepository.findEligibleTasksForUser(
                        TaskStatus.ACTIVE,
                        user.getLevel().getPointThreshold(),
                        userCapabilities,
                        pageable
                )
                .map(this::mapToTaskResponse);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        return mapToTaskResponse(findTaskById(id));
    }

    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task = findTaskById(id);

        Level requiredLevel = findLevelById(request.getRequiredLevelId());

        task.setName(request.getName());
        task.setDescription(request.getDescription());
        task.setRequiredMetric(request.getRequiredMetric());
        task.setRequiredLevel(requiredLevel);
        task.setTargetValue(request.getTargetValue());
        task.setRewardPoints(request.getRewardPoints());

        taskRepository.save(task);
        return mapToTaskResponse(task);
    }

    // --- private helpers ---

    private boolean userMeetsLevelRequirement(User user, Task task) {
        return user.getLevel().getPointThreshold() >= task.getRequiredLevel().getPointThreshold();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    private Task findTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
    }

    private Level findLevelById(Long id) {
        return levelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Level not found: " + id));
    }

    private TaskResponse mapToTaskResponse(Task task) {
        Level lvl = task.getRequiredLevel();
        return TaskResponse.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .requiredMetric(task.getRequiredMetric())
                .requiredLevelId(lvl != null ? lvl.getId() : null)
                .requiredLevelName(lvl != null ? lvl.getLevelName() : null)
                .targetValue(task.getTargetValue())
                .rewardPoints(task.getRewardPoints())
                .status(task.getStatus())
                .build();
    }

    private UserTaskProgressResponse mapToUserTaskProgressResponse(UserTask userTask) {
        return UserTaskProgressResponse.builder()
                .userId(userTask.getUser().getId())
                .taskId(userTask.getTask().getId())
                .status(userTask.getStatus())
                .completedAt(userTask.getCompletedAt())
                .build();
    }
}