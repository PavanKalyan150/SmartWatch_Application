package com.smartwatch.leaderboard.repository;

import com.smartwatch.leaderboard.model.ChallengeTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChallengeTaskRepository extends JpaRepository<ChallengeTask, Long> {

    List<ChallengeTask> findByChallengeId(Long challengeId);

    boolean existsByChallengeIdAndTaskId(Long challengeId, Long taskId);

    void deleteByChallengeIdAndTaskId(Long challengeId, Long taskId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ChallengeTask ct where ct.challenge.id = :challengeId")
    void deleteByChallengeId(@Param("challengeId") Long challengeId);
    @Query("SELECT ct.task.id FROM ChallengeTask ct WHERE ct.challenge.id = :challengeId")
    List<Long> findTaskIdsByChallengeId(@Param("challengeId") Long challengeId);
}