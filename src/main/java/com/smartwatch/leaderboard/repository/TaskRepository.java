package com.smartwatch.leaderboard.repository;

import com.smartwatch.leaderboard.model.Task;
import com.smartwatch.leaderboard.model.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(TaskStatus status);
    List<Task> findByRequiredMetricAndStatus(String requiredMetric, TaskStatus status);
    // Used when filtering tasks a user is eligible for by level and device capability
    List<Task> findByRequiredLevelIdAndRequiredMetricInAndStatus(
            Long levelId,
            List<String> capabilityCodes,
            TaskStatus status
    );
    @Query("""
        SELECT t FROM Task t
        WHERE t.status = :status
          AND t.requiredLevel.pointThreshold <= :userPointThreshold
          AND t.requiredMetric IN :capabilityCodes
        """)
    Page<Task> findEligibleTasksForUser(
            @Param("status") TaskStatus status,
            @Param("userPointThreshold") int userPointThreshold,
            @Param("capabilityCodes") Set<String> capabilityCodes,
            Pageable pageable
    );
}