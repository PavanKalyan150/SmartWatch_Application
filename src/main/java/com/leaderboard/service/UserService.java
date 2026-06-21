package com.leaderboard.service;

import com.leaderboard.model.*;
import com.leaderboard.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final UserActivityRepository userActivityRepository;
    private final TaskRepository taskRepository;
    private final UserTaskRepository userTaskRepository;
    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;

    public UserService(UserRepository userRepository, DeviceRepository deviceRepository,
                       UserActivityRepository userActivityRepository, TaskRepository taskRepository,
                       UserTaskRepository userTaskRepository, ChallengeRepository challengeRepository,
                       UserChallengeRepository userChallengeRepository) {
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.userActivityRepository = userActivityRepository;
        this.taskRepository = taskRepository;
        this.userTaskRepository = userTaskRepository;
        this.challengeRepository = challengeRepository;
        this.userChallengeRepository = userChallengeRepository;
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
    }

    public List<UserActivity> getUserActivities(Long userId) {
        return userActivityRepository.findByUserIdAndActivityDateBetween(
                userId,
                java.time.LocalDate.now().minusDays(30),
                java.time.LocalDate.now()
        );
    }

    @Transactional
    public User associateDevice(Long userId, Long deviceId) {
        User user = getUserById(userId);
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found with ID: " + deviceId));
        user.setDevice(device);
        return userRepository.save(user);
    }

    @Transactional
    public UserTask registerForTask(Long userId, Long taskId) {
        User user = getUserById(userId);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));

        if (userTaskRepository.findByUserIdAndTaskId(userId, taskId).isPresent()) {
            throw new IllegalArgumentException("User already registered for this task");
        }

        UserTask userTask = new UserTask(user, task);
        return userTaskRepository.save(userTask);
    }

    @Transactional
    public UserChallenge registerForChallenge(Long userId, Long challengeId) {
        User user = getUserById(userId);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found with ID: " + challengeId));

        if (userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId).isPresent()) {
            throw new IllegalArgumentException("User already registered for this challenge");
        }

        // Verify device compatibility before registering
        if (challenge.getRequiredFeatures() != null && !challenge.getRequiredFeatures().isEmpty()) {
            if (user.getDevice() == null) {
                throw new IllegalArgumentException("Device association required for this challenge");
            }
            if (!user.getDevice().getFeatures().containsAll(challenge.getRequiredFeatures())) {
                throw new IllegalArgumentException("Associated device does not support required features: " + challenge.getRequiredFeatures());
            }
        }

        UserChallenge userChallenge = new UserChallenge(user, challenge);
        return userChallengeRepository.save(userChallenge);
    }
}
