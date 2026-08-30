package com.smartwatch.leaderboard.repository;

import com.smartwatch.leaderboard.model.Leaderboard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaderboardRepository extends JpaRepository<Leaderboard, Long> {

    // Ranked results for a challenge — primary read path for leaderboard display
    List<Leaderboard> findByChallengeIdOrderByRankAsc(Long challengeId);

    boolean existsByChallengeIdAndUserId(Long challengeId, Long userId);
}