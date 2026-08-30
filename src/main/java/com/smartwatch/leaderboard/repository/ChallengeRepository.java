package com.smartwatch.leaderboard.repository;

import com.smartwatch.leaderboard.model.Challenge;
import com.smartwatch.leaderboard.model.enums.ChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    List<Challenge> findByStatus(ChallengeStatus status);

    // Used by batch job — find all ACTIVE challenges whose window has closed
    List<Challenge> findByStatusAndEndTimeBefore(ChallengeStatus status, LocalDateTime now);

    @Modifying
    @Query("UPDATE Challenge c SET c.status = :to WHERE c.status = :from AND c.endTime < :now")
    int markExpiredChallenges(@Param("from") ChallengeStatus from,
                              @Param("to") ChallengeStatus to,
                              @Param("now") LocalDateTime now);

    @Query("SELECT ct.task.id FROM ChallengeTask ct WHERE ct.challenge.id = :challengeId")
    List<Long> findTaskIdsByChallengeId(@Param("challengeId") Long challengeId);
}