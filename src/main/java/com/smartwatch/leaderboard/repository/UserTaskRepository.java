package com.smartwatch.leaderboard.repository;

import com.smartwatch.leaderboard.model.UserTask;
import com.smartwatch.leaderboard.model.enums.UserTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserTaskRepository extends JpaRepository<UserTask, Long> {

    Optional<UserTask> findByUserIdAndTaskId(Long userId, Long taskId);

    List<UserTask> findByUserId(Long userId);

    // Used by batch — sum points for completed tasks that belong to a challenge
    // and were completed within the challenge window
    @Query("""
        SELECT ut FROM UserTask ut
        WHERE ut.user.id = :userId
          AND ut.task.id IN (
              SELECT ct.task.id FROM ChallengeTask ct WHERE ct.challenge.id = :challengeId
          )
          AND ut.status = 'COMPLETED'
          AND ut.completedAt BETWEEN :start AND :end
    """)
    List<UserTask> findCompletedTasksWithinChallengeWindow(
            @Param("userId") Long userId,
            @Param("challengeId") Long challengeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
    @Query("""
    SELECT COALESCE(SUM(ut.pointsAwarded), 0)
    FROM UserTask ut
    WHERE ut.user.id = :userId
      AND ut.task.id IN :taskIds
      AND ut.status = :status
      AND ut.completedAt BETWEEN :start AND :end
    """)
    Double sumPointsForUserInScope(@Param("userId") Long userId,
                                   @Param("taskIds") List<Long> taskIds,
                                   @Param("status") UserTaskStatus status,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    @Query("""
    SELECT MAX(ut.completedAt)
    FROM UserTask ut
    WHERE ut.user.id = :userId
      AND ut.task.id IN :taskIds
      AND ut.status = :status
      AND ut.completedAt BETWEEN :start AND :end
    """)
    LocalDateTime findLastCompletionInScope(@Param("userId") Long userId,
                                            @Param("taskIds") List<Long> taskIds,
                                            @Param("status") UserTaskStatus status,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);
}