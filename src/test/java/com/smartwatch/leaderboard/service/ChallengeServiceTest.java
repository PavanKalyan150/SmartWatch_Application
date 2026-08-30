package com.smartwatch.leaderboard.service;

import com.smartwatch.leaderboard.dto.request.ChallengeRequest;
import com.smartwatch.leaderboard.dto.response.ChallengeResponse;
import com.smartwatch.leaderboard.dto.response.UserChallengeRankResponse;
import com.smartwatch.leaderboard.dto.response.UserChallengeResponse;
import com.smartwatch.leaderboard.model.*;
import com.smartwatch.leaderboard.model.enums.ChallengeStatus;
import com.smartwatch.leaderboard.model.enums.UserChallengeStatus;
import com.smartwatch.leaderboard.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceTest {

    @Mock private ChallengeRepository challengeRepository;
    @Mock private ChallengeTaskRepository challengeTaskRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserChallengeRepository userChallengeRepository;
    @Mock private LeaderboardRepository leaderboardRepository;
    @Mock private DeviceCapabilityRepository deviceCapabilityRepository;

    @InjectMocks private ChallengeService challengeService;

    private static final String EMAIL = "user@example.com";
    private static final Long USER_ID = 1L;
    private static final Long CHALLENGE_ID = 100L;
    private static final Long DEVICE_ID = 200L;
    private static final Long LEVEL_ID = 10L;

    private User user;
    private Level userLevel;
    private Device device;
    private Challenge challenge;
    private ChallengeRequest challengeRequest;

    @BeforeEach
    void setUp() {
        userLevel = Level.builder().id(LEVEL_ID).pointThreshold(1000).build();
        device = Device.builder().id(DEVICE_ID).build();
        user = User.builder()
                .id(USER_ID)
                .email(EMAIL)
                .level(userLevel)
                .device(device)
                .build();

        challenge = Challenge.builder()
                .id(CHALLENGE_ID)
                .name("Step Challenge")
                .description("Walk 10k steps")
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(7))
                .status(ChallengeStatus.ACTIVE)
                .build();

        challengeRequest = new ChallengeRequest();
        challengeRequest.setName("Step Challenge");
        challengeRequest.setDescription("Walk 10k steps");
        challengeRequest.setStartTime(LocalDateTime.now());
        challengeRequest.setEndTime(LocalDateTime.now().plusDays(7));
    }

    // ---------- createChallenge ----------

    @Nested
    class CreateChallenge {

        @Test
        void shouldCreateChallengeWithCreatorWhenEmailProvided() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            ChallengeResponse response = challengeService.createChallenge(challengeRequest, EMAIL);

            ArgumentCaptor<Challenge> captor = ArgumentCaptor.forClass(Challenge.class);
            verify(challengeRepository).save(captor.capture());
            Challenge saved = captor.getValue();

            assertThat(saved.getName()).isEqualTo("Step Challenge");
            assertThat(saved.getDescription()).isEqualTo("Walk 10k steps");
            assertThat(saved.getStatus()).isEqualTo(ChallengeStatus.ACTIVE);
            assertThat(saved.getCreatedByUser()).isSameAs(user);
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("Step Challenge");
        }

        @Test
        void shouldCreateChallengeWithoutCreatorWhenEmailIsNull() {
            challengeService.createChallenge(challengeRequest, null);

            ArgumentCaptor<Challenge> captor = ArgumentCaptor.forClass(Challenge.class);
            verify(challengeRepository).save(captor.capture());
            assertThat(captor.getValue().getCreatedByUser()).isNull();
            verifyNoInteractions(userRepository);
        }

        @Test
        void shouldCreateChallengeWithoutCreatorWhenEmailIsBlank() {
            challengeService.createChallenge(challengeRequest, "   ");

            verify(challengeRepository).save(any());
            verifyNoInteractions(userRepository);
        }

        @Test
        void shouldThrowWhenCreatorEmailNotFound() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> challengeService.createChallenge(challengeRequest, EMAIL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");

            verify(challengeRepository, never()).save(any());
        }
    }

    // ---------- getChallengeById ----------

    @Nested
    class GetChallengeById {

        @Test
        void shouldReturnChallengeWhenFound() {
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));

            ChallengeResponse response = challengeService.getChallengeById(CHALLENGE_ID);

            assertThat(response.getId()).isEqualTo(CHALLENGE_ID);
            assertThat(response.getName()).isEqualTo("Step Challenge");
        }

        @Test
        void shouldThrowWhenChallengeNotFound() {
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> challengeService.getChallengeById(CHALLENGE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Challenge not found");
        }
    }

    // ---------- updateChallenge ----------

    @Nested
    class UpdateChallenge {

        @Test
        void shouldUpdateExistingChallengeFields() {
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
            challengeRequest.setName("Updated Name");
            challengeRequest.setDescription("Updated description");

            ChallengeResponse response = challengeService.updateChallenge(CHALLENGE_ID, challengeRequest);

            verify(challengeRepository).save(challenge);
            assertThat(challenge.getName()).isEqualTo("Updated Name");
            assertThat(challenge.getDescription()).isEqualTo("Updated description");
            assertThat(response.getName()).isEqualTo("Updated Name");
        }

        @Test
        void shouldThrowWhenUpdatingMissingChallenge() {
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> challengeService.updateChallenge(CHALLENGE_ID, challengeRequest))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(challengeRepository, never()).save(any());
        }
    }

    // ---------- enrollUser ----------

    @Nested
    class EnrollUser {

        @Test
        void shouldEnrollUserWhenEligibleAndNotAlreadyEnrolled() {
            stubEligibleScenario(800, 2000, "STEPS", List.of("STEPS"));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
            when(userChallengeRepository.existsByUserIdAndChallengeId(USER_ID, CHALLENGE_ID))
                    .thenReturn(false);

            UserChallengeResponse response = challengeService.enrollUser(CHALLENGE_ID, USER_ID);

            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getChallengeId()).isEqualTo(CHALLENGE_ID);
            assertThat(response.getStatus()).isEqualTo(UserChallengeStatus.JOINED);

            ArgumentCaptor<UserChallenge> captor = ArgumentCaptor.forClass(UserChallenge.class);
            verify(userChallengeRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(UserChallengeStatus.JOINED);
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> challengeService.enrollUser(CHALLENGE_ID, USER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");

            verify(userChallengeRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenChallengeNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> challengeService.enrollUser(CHALLENGE_ID, USER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Challenge not found");

            verify(userChallengeRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenUserAlreadyEnrolled() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
            when(userChallengeRepository.existsByUserIdAndChallengeId(USER_ID, CHALLENGE_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> challengeService.enrollUser(CHALLENGE_ID, USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already enrolled");

            verify(userChallengeRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenUserLevelTooLow() {
            // user level threshold = 1000, challenge requires 5000
            stubEligibleScenario(5000, 1000, "STEPS", List.of("STEPS"));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
            when(userChallengeRepository.existsByUserIdAndChallengeId(USER_ID, CHALLENGE_ID))
                    .thenReturn(false);

            assertThatThrownBy(() -> challengeService.enrollUser(CHALLENGE_ID, USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("level or device requirements");

            verify(userChallengeRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenDeviceMissingCapability() {
            // user has STEPS only, challenge requires HEART_RATE
            stubEligibleScenario(500, 1000, "HEART_RATE", List.of("STEPS"));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
            when(userChallengeRepository.existsByUserIdAndChallengeId(USER_ID, CHALLENGE_ID))
                    .thenReturn(false);

            assertThatThrownBy(() -> challengeService.enrollUser(CHALLENGE_ID, USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("level or device requirements");
        }

        @Test
        void shouldThrowWhenChallengeHasNoTasks() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
            when(userChallengeRepository.existsByUserIdAndChallengeId(USER_ID, CHALLENGE_ID))
                    .thenReturn(false);
            when(challengeTaskRepository.findByChallengeId(CHALLENGE_ID))
                    .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> challengeService.enrollUser(CHALLENGE_ID, USER_ID))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ---------- getChallengesForUser ----------

    @Nested
    class GetChallengesForUser {

        @Test
        void shouldReturnOnlyEligibleActiveChallenges() {
            Challenge eligibleChallenge = challenge;
            Challenge ineligibleChallenge = Challenge.builder()
                    .id(999L).name("Hard Challenge")
                    .status(ChallengeStatus.ACTIVE).build();

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(challengeRepository.findByStatus(ChallengeStatus.ACTIVE))
                    .thenReturn(List.of(eligibleChallenge, ineligibleChallenge));

            // eligible challenge: tasks exist, user qualifies
            when(challengeTaskRepository.findByChallengeId(CHALLENGE_ID))
                    .thenReturn(List.of(buildChallengeTask(500, "STEPS")));
            when(deviceCapabilityRepository.findByDeviceId(DEVICE_ID))
                    .thenReturn(List.of(buildDeviceCapability("STEPS")));

            // ineligible challenge: no tasks → not eligible
            when(challengeTaskRepository.findByChallengeId(999L))
                    .thenReturn(Collections.emptyList());

            List<ChallengeResponse> responses = challengeService.getChallengesForUser(EMAIL);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getId()).isEqualTo(CHALLENGE_ID);
        }

        @Test
        void shouldReturnEmptyListWhenNoActiveChallenges() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(challengeRepository.findByStatus(ChallengeStatus.ACTIVE))
                    .thenReturn(Collections.emptyList());

            List<ChallengeResponse> responses = challengeService.getChallengesForUser(EMAIL);

            assertThat(responses).isEmpty();
            verifyNoInteractions(challengeTaskRepository, deviceCapabilityRepository);
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> challengeService.getChallengesForUser(EMAIL))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ---------- getUserRankInChallenge ----------

    @Nested
    class GetUserRankInChallenge {

        @Test
        void shouldReturnRankResponseWhenUserHasJoined() {
            UserChallenge uc = UserChallenge.builder()
                    .user(user)
                    .challenge(challenge)
                    .status(UserChallengeStatus.JOINED)
                    .finalScore(85.0)
                    .rank(3)
                    .pointsAwarded(50)
                    .joinedAt(LocalDateTime.now().minusDays(1))
                    .rankedAt(LocalDateTime.now())
                    .build();

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(userChallengeRepository.findByUserIdAndChallengeId(USER_ID, CHALLENGE_ID))
                    .thenReturn(Optional.of(uc));

            UserChallengeRankResponse response =
                    challengeService.getUserRankInChallenge(CHALLENGE_ID, EMAIL);

            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getChallengeId()).isEqualTo(CHALLENGE_ID);
            assertThat(response.getStatus()).isEqualTo("JOINED");
            assertThat(response.getFinalScore()).isEqualTo(85.0);
            assertThat(response.getRank()).isEqualTo(3);
            assertThat(response.getPointsAwarded()).isEqualTo(50);
        }

        @Test
        void shouldReturnNullStatusWhenUserChallengeStatusIsNull() {
            UserChallenge uc = UserChallenge.builder()
                    .user(user).challenge(challenge).status(null).build();

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(userChallengeRepository.findByUserIdAndChallengeId(USER_ID, CHALLENGE_ID))
                    .thenReturn(Optional.of(uc));

            UserChallengeRankResponse response =
                    challengeService.getUserRankInChallenge(CHALLENGE_ID, EMAIL);

            assertThat(response.getStatus()).isNull();
        }

        @Test
        void shouldThrowWhenUserNotJoinedInChallenge() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(userChallengeRepository.findByUserIdAndChallengeId(USER_ID, CHALLENGE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> challengeService.getUserRankInChallenge(CHALLENGE_ID, EMAIL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("has not joined");
        }
    }

    // ---------- helpers ----------

    /**
     * Sets up an isEligible() scenario.
     * @param requiredThreshold  level threshold the challenge task requires
     * @param userThreshold      user's level threshold (overrides @BeforeEach default)
     * @param requiredMetric     metric the task requires
     * @param userCapabilities   capability codes the user's device supports
     */
    private void stubEligibleScenario(int requiredThreshold, int userThreshold,
                                      String requiredMetric, List<String> userCapabilities) {
        userLevel.setPointThreshold(userThreshold);

        when(challengeTaskRepository.findByChallengeId(CHALLENGE_ID))
                .thenReturn(List.of(buildChallengeTask(requiredThreshold, requiredMetric)));

        // only stub capabilities lookup when level check would actually pass
        if (userThreshold >= requiredThreshold) {
            when(deviceCapabilityRepository.findByDeviceId(DEVICE_ID))
                    .thenReturn(userCapabilities.stream()
                            .map(this::buildDeviceCapability).toList());
        }
    }

    private ChallengeTask buildChallengeTask(int levelThreshold, String metric) {
        Level requiredLevel = Level.builder()
                .id(LEVEL_ID).pointThreshold(levelThreshold).build();
        Task task = Task.builder()
                .requiredLevel(requiredLevel)
                .requiredMetric(metric)
                .build();
        return ChallengeTask.builder().task(task).build();
    }

    private DeviceCapability buildDeviceCapability(String code) {
        return DeviceCapability.builder().capabilityCode(code).build();
    }
}