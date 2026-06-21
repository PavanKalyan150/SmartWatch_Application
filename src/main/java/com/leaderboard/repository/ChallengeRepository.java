package com.leaderboard.repository;

import com.leaderboard.model.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    List<Challenge> findByExpiryDateBeforeAndIsProcessed(LocalDateTime now, Boolean isProcessed);
}
