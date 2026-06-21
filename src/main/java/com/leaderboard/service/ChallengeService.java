package com.leaderboard.service;

import com.leaderboard.model.Challenge;
import com.leaderboard.model.User;
import com.leaderboard.model.UserChallenge;
import com.leaderboard.repository.ChallengeRepository;
import com.leaderboard.repository.UserChallengeRepository;
import com.leaderboard.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final UserRepository userRepository;

    public ChallengeService(ChallengeRepository challengeRepository,
                            UserChallengeRepository userChallengeRepository,
                            UserRepository userRepository) {
        this.challengeRepository = challengeRepository;
        this.userChallengeRepository = userChallengeRepository;
        this.userRepository = userRepository;
    }

    public Challenge createChallenge(Challenge challenge) {
        // If geospatial or city scope is provided, set isGlobal to false
        if (challenge.getCity() != null || (challenge.getLatitude() != null && challenge.getLongitude() != null)) {
            challenge.setIsGlobal(false);
        } else {
            challenge.setIsGlobal(true);
        }
        challenge.setIsProcessed(false);
        return challengeRepository.save(challenge);
    }

    public Challenge getChallengeById(Long id) {
        return challengeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found with ID: " + id));
    }

    public Page<UserChallenge> getChallengeUsers(Long challengeId, Pageable pageable) {
        return userChallengeRepository.findByChallengeId(challengeId, pageable);
    }

    public Challenge updateChallenge(Long id, Challenge details) {
        Challenge existing = getChallengeById(id);
        existing.setTitle(details.getTitle());
        existing.setDescription(details.getDescription());
        existing.setRequiredSteps(details.getRequiredSteps());
        existing.setPointsReward(details.getPointsReward());
        existing.setExpiryDate(details.getExpiryDate());
        existing.setRequiredFeatures(details.getRequiredFeatures());
        existing.setLatitude(details.getLatitude());
        existing.setLongitude(details.getLongitude());
        existing.setRadiusKm(details.getRadiusKm());
        existing.setCity(details.getCity());
        if (details.getCity() != null || (details.getLatitude() != null && details.getLongitude() != null)) {
            existing.setIsGlobal(false);
        } else {
            existing.setIsGlobal(true);
        }
        return challengeRepository.save(existing);
    }

    public List<Challenge> discoverChallenges(Long userId, Double latitude, Double longitude, String city) {
        List<Challenge> all = challengeRepository.findAll();

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        final User finalUser = user;
        return all.stream()
                .filter(challenge -> isCompatible(finalUser, challenge))
                .filter(challenge -> isLocationMatching(challenge, latitude, longitude, city))
                .collect(Collectors.toList());
    }

    private boolean isCompatible(User user, Challenge challenge) {
        if (challenge.getRequiredFeatures() == null || challenge.getRequiredFeatures().isEmpty()) {
            return true;
        }
        if (user == null || user.getDevice() == null) {
            return false;
        }
        return user.getDevice().getFeatures().containsAll(challenge.getRequiredFeatures());
    }

    private boolean isLocationMatching(Challenge challenge, Double lat, Double lon, String city) {
        if (Boolean.TRUE.equals(challenge.getIsGlobal())) {
            return true;
        }

        // Check city matching
        if (challenge.getCity() != null) {
            if (city != null && challenge.getCity().equalsIgnoreCase(city.trim())) {
                return true;
            }
        }

        // Check coordinate radius matching
        if (challenge.getLatitude() != null && challenge.getLongitude() != null && challenge.getRadiusKm() != null) {
            if (lat != null && lon != null) {
                double distance = calculateDistance(lat, lon, challenge.getLatitude(), challenge.getLongitude());
                return distance <= challenge.getRadiusKm();
            }
        }

        return false;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371.0; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}
