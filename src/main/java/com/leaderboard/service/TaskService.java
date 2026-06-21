package com.leaderboard.service;

import com.leaderboard.model.Task;
import com.leaderboard.model.UserTask;
import com.leaderboard.repository.TaskRepository;
import com.leaderboard.repository.UserTaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserTaskRepository userTaskRepository;

    public TaskService(TaskRepository taskRepository, UserTaskRepository userTaskRepository) {
        this.taskRepository = taskRepository;
        this.userTaskRepository = userTaskRepository;
    }

    public Task createTask(Task task) {
        return taskRepository.save(task);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + id));
    }

    public Page<UserTask> getTaskUsers(Long taskId, Pageable pageable) {
        return userTaskRepository.findByTaskId(taskId, pageable);
    }

    public Task updateTask(Long id, Task taskDetails) {
        Task task = getTaskById(id);
        task.setTitle(taskDetails.getTitle());
        task.setDescription(taskDetails.getDescription());
        task.setRequiredSteps(taskDetails.getRequiredSteps());
        task.setPointsReward(taskDetails.getPointsReward());
        return taskRepository.save(task);
    }
}
