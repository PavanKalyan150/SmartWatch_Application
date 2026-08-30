package com.smartwatch.leaderboard.controller;

import com.smartwatch.leaderboard.dto.request.TaskRequest;
import com.smartwatch.leaderboard.dto.response.TaskResponse;
import com.smartwatch.leaderboard.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    private final TaskService taskService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request) {
        log.debug("Admin creating task: {}", request.getName());
        TaskResponse response = taskService.createTask(request);
        log.info("Task created with id: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<TaskResponse>> getTasks() {
        log.debug("Fetching all tasks");
        List<TaskResponse> tasks = taskService.getAllTasks();
        log.info("Returned {} tasks", tasks.size());
        return ResponseEntity.status(HttpStatus.OK).body(tasks);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        log.debug("Fetching task id: {}", id);
        TaskResponse response = taskService.getTaskById(id);
        log.info("Task found with id: {}", id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long id,
                                                   @Valid @RequestBody TaskRequest request) {
        log.debug("Admin updating task id: {}", id);
        TaskResponse response = taskService.updateTask(id, request);
        log.info("Task updated with id: {}", id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<TaskResponse>> getTasks(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        log.debug("Fetching tasks for userId: {} requested by: {}", userId, userDetails.getUsername());
        Page<TaskResponse> tasks = taskService.getTasksForUser(userId, pageable);
        log.info("Returned {} tasks (page {} of {}) for userId: {}",
                tasks.getNumberOfElements(), tasks.getNumber(),
                tasks.getTotalPages(), userId);
        return ResponseEntity.ok(tasks);
    }
}
