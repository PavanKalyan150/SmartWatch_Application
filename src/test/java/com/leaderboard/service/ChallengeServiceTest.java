package com.leaderboard.service;

import com.leaderboard.model.Challenge;
import com.leaderboard.model.Device;
import com.leaderboard.model.User;
import com.leaderboard.repository.ChallengeRepository;
import com.leaderboard.repository.UserChallengeRepository;
import com.leaderboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ChallengeServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private UserChallengeRepository userChallengeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChallengeService challengeService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testIsCompatible_NoRequiredFeatures_ReturnsTrue() {
        Challenge challenge = new Challenge();
        challenge.setRequiredFeatures(new HashSet<>());

        User user = new User();
        user.setDevice(null);

        when(challengeRepository.findAll()).thenReturn(Collections.singletonList(challenge));

        List<Challenge> result = challengeService.discoverChallenges(null, null, null, null);
        assertEquals(1, result.size());
    }

    @Test
    public void testIsCompatible_UserHasCompatibleDevice_ReturnsTrue() {
        Challenge challenge = new Challenge();
        challenge.setRequiredFeatures(new HashSet<>(Arrays.asList("GPS", "HRM")));

        Device device = new Device();
        device.setFeatures(new HashSet<>(Arrays.asList("GPS", "HRM", "ACCELEROMETER")));

        User user = new User();
        user.setId(1L);
        user.setDevice(device);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(challengeRepository.findAll()).thenReturn(Collections.singletonList(challenge));

        List<Challenge> result = challengeService.discoverChallenges(1L, null, null, null);
        assertEquals(1, result.size());
    }

    @Test
    public void testIsCompatible_UserLacksFeatures_ReturnsFalse() {
        Challenge challenge = new Challenge();
        challenge.setRequiredFeatures(new HashSet<>(Arrays.asList("GPS", "HRM")));

        Device device = new Device();
        device.setFeatures(new HashSet<>(Collections.singletonList("GPS"))); // lacks HRM

        User user = new User();
        user.setId(1L);
        user.setDevice(device);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(challengeRepository.findAll()).thenReturn(Collections.singletonList(challenge));

        List<Challenge> result = challengeService.discoverChallenges(1L, null, null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIsLocationMatching_GlobalChallenge_ReturnsTrue() {
        Challenge challenge = new Challenge();
        challenge.setIsGlobal(true);

        when(challengeRepository.findAll()).thenReturn(Collections.singletonList(challenge));

        List<Challenge> result = challengeService.discoverChallenges(null, null, null, null);
        assertEquals(1, result.size());
    }

    @Test
    public void testIsLocationMatching_CityMatches_ReturnsTrue() {
        Challenge challenge = new Challenge();
        challenge.setIsGlobal(false);
        challenge.setCity("Mumbai");

        when(challengeRepository.findAll()).thenReturn(Collections.singletonList(challenge));

        List<Challenge> result = challengeService.discoverChallenges(null, null, null, "Mumbai");
        assertEquals(1, result.size());
    }

    @Test
    public void testIsLocationMatching_RadiusMatches_ReturnsTrue() {
        Challenge challenge = new Challenge();
        challenge.setIsGlobal(false);
        challenge.setLatitude(19.0760); // Mumbai center lat
        challenge.setLongitude(72.8777); // Mumbai center lon
        challenge.setRadiusKm(10.0);

        when(challengeRepository.findAll()).thenReturn(Collections.singletonList(challenge));

        // Location 5km away
        List<Challenge> result = challengeService.discoverChallenges(null, 19.0700, 72.8700, null);
        assertEquals(1, result.size());
    }

    @Test
    public void testIsLocationMatching_RadiusOut_ReturnsFalse() {
        Challenge challenge = new Challenge();
        challenge.setIsGlobal(false);
        challenge.setLatitude(19.0760);
        challenge.setLongitude(72.8777);
        challenge.setRadiusKm(5.0);

        when(challengeRepository.findAll()).thenReturn(Collections.singletonList(challenge));

        // Location 50km away
        List<Challenge> result = challengeService.discoverChallenges(null, 19.4000, 72.8000, null);
        assertTrue(result.isEmpty());
    }
}
