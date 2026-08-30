package com.smartwatch.leaderboard.repository;

import com.smartwatch.leaderboard.model.UserChallenge;
import com.smartwatch.leaderboard.model.enums.UserChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserChallengeRepository extends JpaRepository<UserChallenge, Long> {

    Optional<UserChallenge> findByUserIdAndChallengeId(Long userId, Long challengeId);

    boolean existsByUserIdAndChallengeId(Long userId, Long challengeId);

    List<UserChallenge> findByChallengeId(Long challengeId);

    // Used by batch to pull all participants that need scoring
    List<UserChallenge> findByChallengeIdAndStatus(Long challengeId, UserChallengeStatus status);


}