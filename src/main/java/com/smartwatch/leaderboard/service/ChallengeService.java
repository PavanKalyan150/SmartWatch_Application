package com.smartwatch.leaderboard.service;

import com.smartwatch.leaderboard.dto.request.ChallengeRequest;
import com.smartwatch.leaderboard.dto.response.ChallengeResponse;
import com.smartwatch.leaderboard.dto.response.TaskResponse;
import com.smartwatch.leaderboard.dto.response.UserChallengeRankResponse;
import com.smartwatch.leaderboard.dto.response.UserChallengeResponse;
import com.smartwatch.leaderboard.model.*;
import com.smartwatch.leaderboard.model.enums.ChallengeStatus;
import com.smartwatch.leaderboard.model.enums.RewardScheme;
import com.smartwatch.leaderboard.model.enums.UserChallengeStatus;
import com.smartwatch.leaderboard.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeTaskRepository challengeTaskRepository;
    private final UserRepository userRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final DeviceCapabilityRepository deviceCapabilityRepository;
    private final LevelRepository levelRepository;          // NEW
    private final TaskRepository taskRepository;            // NEW


    @Transactional
    public ChallengeResponse createChallenge(ChallengeRequest request, String email) {
        User creator = (email == null || email.isBlank()) ? null : findUserByEmail(email);

        Level requiredLevel = findLevelById(request.getRequiredLevelId());
        RewardScheme rewardScheme = parseRewardScheme(request.getRewardScheme());

        Challenge challenge = Challenge.builder()
                .name(request.getName())
                .description(request.getDescription())
                .requiredLevel(requiredLevel)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(ChallengeStatus.ACTIVE)            // always start as ACTIVE
                .rewardScheme(rewardScheme)
                .createdByUser(creator)
                .build();

        challengeRepository.save(challenge);

        // Persist the challenge_task links
        replaceChallengeTasks(challenge, request.getTaskIds());

        return mapToChallengeResponse(challenge);
    }

    // ---------------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ChallengeResponse> getChallengesForUser(String email) {
        User user = findUserByEmail(email);

        return challengeRepository.findByStatus(ChallengeStatus.ACTIVE)
                .stream()
                .filter(challenge -> isEligible(user, challenge))
                .map(this::mapToChallengeResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChallengeResponse getChallengeById(Long id) {
        Challenge challenge = findChallengeById(id);
        return mapToChallengeResponse(challenge);
    }

    // ---------------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------------

    @Transactional
    public ChallengeResponse updateChallenge(Long id, ChallengeRequest request) {
        Challenge challenge = findChallengeById(id);

        challenge.setName(request.getName());
        challenge.setDescription(request.getDescription());
        challenge.setStartTime(request.getStartTime());
        challenge.setEndTime(request.getEndTime());

        if (request.getRequiredLevelId() != null) {
            challenge.setRequiredLevel(findLevelById(request.getRequiredLevelId()));
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            challenge.setStatus(parseChallengeStatus(request.getStatus()));
        }
        if (request.getRewardScheme() != null && !request.getRewardScheme().isBlank()) {
            challenge.setRewardScheme(parseRewardScheme(request.getRewardScheme()));
        }

        challengeRepository.save(challenge);

        // Replace tasks if the client sent the list (null = leave alone)
        if (request.getTaskIds() != null) {
            replaceChallengeTasks(challenge, request.getTaskIds());
        }

        return mapToChallengeResponse(challenge);
    }

    // ---------------------------------------------------------------------
    // Enrollment & ranking (unchanged behavior)
    // ---------------------------------------------------------------------

    @Transactional
    public UserChallengeResponse enrollUser(Long challengeId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Challenge challenge = findChallengeById(challengeId);

        if (userChallengeRepository.existsByUserIdAndChallengeId(userId, challengeId)) {
            throw new IllegalStateException("User is already enrolled in this challenge");
        }

        if (!isEligible(user, challenge)) {
            throw new IllegalStateException(
                    "User does not meet level or device requirements for this challenge");
        }

        UserChallenge enrollment = UserChallenge.builder()
                .user(user)
                .challenge(challenge)
                .status(UserChallengeStatus.JOINED)
                .build();

        userChallengeRepository.save(enrollment);

        return UserChallengeResponse.builder()
                .userId(userId)
                .challengeId(challengeId)
                .status(UserChallengeStatus.JOINED)
                .build();
    }

    @Transactional(readOnly = true)
    public UserChallengeRankResponse getUserRankInChallenge(Long challengeId, String email) {
        User user = findUserByEmail(email);

        UserChallenge uc = userChallengeRepository
                .findByUserIdAndChallengeId(user.getId(), challengeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User has not joined challenge: " + challengeId));

        return toRankResponse(uc);
    }

    private UserChallengeRankResponse toRankResponse(UserChallenge uc) {
        return UserChallengeRankResponse.builder()
                .userId(uc.getUser().getId())
                .challengeId(uc.getChallenge().getId())
                .status(uc.getStatus() != null ? uc.getStatus().name() : null)
                .finalScore(uc.getFinalScore())
                .rank(uc.getRank())
                .pointsAwarded(uc.getPointsAwarded())
                .joinedAt(uc.getJoinedAt())
                .rankedAt(uc.getRankedAt())
                .build();
    }

    // ---------------------------------------------------------------------
    // Eligibility (unchanged)
    // ---------------------------------------------------------------------

    private boolean isEligible(User user, Challenge challenge) {
        List<ChallengeTask> challengeTasks =
                challengeTaskRepository.findByChallengeId(challenge.getId());

        if (challengeTasks.isEmpty()) {
            return false;
        }

        Set<Long> requiredLevelIds = challengeTasks.stream()
                .map(ct -> ct.getTask().getRequiredLevel().getId())
                .collect(Collectors.toSet());

        Set<String> requiredMetrics = challengeTasks.stream()
                .map(ct -> ct.getTask().getRequiredMetric())
                .collect(Collectors.toSet());

        boolean levelQualifies = requiredLevelIds.stream()
                .allMatch(reqLevelId -> user.getLevel().getPointThreshold() >=
                        getThresholdForLevel(reqLevelId, challengeTasks));

        if (!levelQualifies) {
            return false;
        }

        Set<String> userCapabilities = deviceCapabilityRepository
                .findByDeviceId(user.getDevice().getId())
                .stream()
                .map(DeviceCapability::getCapabilityCode)
                .collect(Collectors.toSet());

        return userCapabilities.containsAll(requiredMetrics);
    }

    private int getThresholdForLevel(Long levelId, List<ChallengeTask> tasks) {
        return tasks.stream()
                .filter(ct -> ct.getTask().getRequiredLevel().getId().equals(levelId))
                .map(ct -> ct.getTask().getRequiredLevel().getPointThreshold())
                .findFirst()
                .orElse(0);
    }

    // ---------------------------------------------------------------------
    // Lookup helpers
    // ---------------------------------------------------------------------

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    private Challenge findChallengeById(Long id) {
        return challengeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found: " + id));
    }

    private Level findLevelById(Long id) {
        return levelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Level not found: " + id));
    }

    private Task findTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
    }

    private ChallengeStatus parseChallengeStatus(String raw) {
        try {
            return ChallengeStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid challenge status: " + raw);
        }
    }

    private RewardScheme parseRewardScheme(String raw) {
        try {
            return RewardScheme.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid reward scheme: " + raw);
        }
    }

    /**
     * Wipes existing challenge_task rows for the given challenge and inserts
     * fresh links for every taskId provided. Deduplicates input.
     */
    private void replaceChallengeTasks(Challenge challenge, List<Long> taskIds) {
        challengeTaskRepository.deleteByChallengeId(challenge.getId());

        if (taskIds == null || taskIds.isEmpty()) {
            return;
        }

        List<ChallengeTask> links = taskIds.stream()
                .distinct()
                .map(taskId -> ChallengeTask.builder()
                        .challenge(challenge)
                        .task(findTaskById(taskId))
                        .build())
                .collect(Collectors.toList());

        challengeTaskRepository.saveAll(links);
    }

    // ---------------------------------------------------------------------
    // Response mappers
    // ---------------------------------------------------------------------

    private ChallengeResponse mapToChallengeResponse(Challenge challenge) {
        Level reqLevel = challenge.getRequiredLevel();
        User creator = challenge.getCreatedByUser();

        List<TaskResponse> taskResponses = challengeTaskRepository
                .findByChallengeId(challenge.getId())
                .stream()
                .map(ct -> mapToTaskResponse(ct.getTask()))
                .collect(Collectors.toList());

        return ChallengeResponse.builder()
                .id(challenge.getId())
                .name(challenge.getName())
                .description(challenge.getDescription())
                .requiredLevelId(reqLevel != null ? reqLevel.getId() : null)
                .requiredLevelName(reqLevel != null ? reqLevel.getLevelName() : null)
                .startTime(challenge.getStartTime())
                .endTime(challenge.getEndTime())
                .status(challenge.getStatus())
                .rewardScheme(challenge.getRewardScheme() != null
                        ? challenge.getRewardScheme().name() : null)
                .createdByUserId(creator != null ? creator.getId() : null)
                .tasks(taskResponses != null ? taskResponses : new ArrayList<>())
                .build();
    }

    /**
     * NOTE: tweak field names below to match your actual TaskResponse DTO.
     */
    private TaskResponse mapToTaskResponse(Task task) {
        Level lvl = task.getRequiredLevel();
        return TaskResponse.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .requiredLevelId(lvl != null ? lvl.getId() : null)
                .requiredLevelName(lvl != null ? lvl.getLevelName() : null)
                .requiredMetric(task.getRequiredMetric())
                .targetValue(task.getTargetValue())
                .rewardPoints(task.getRewardPoints())
                .status(task.getStatus())
                .build();
    }
}