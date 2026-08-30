package com.smartwatch.leaderboard.repository;

import com.smartwatch.leaderboard.model.UserActivityEvent;
import com.smartwatch.leaderboard.model.enums.ProcessedStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserActivityEventRepository extends JpaRepository<UserActivityEvent, Long> {

    // Idempotency check — consumer skips if event_id already exists
    boolean existsByEventId(String eventId);

    Optional<UserActivityEvent> findByEventId(String eventId);
    List<UserActivityEvent> findByUserIdOrderByEventTimeDesc(Long userId);
    // Dead-letter queue retry or monitoring queries
    List<UserActivityEvent> findByProcessedStatus(ProcessedStatus status);
}