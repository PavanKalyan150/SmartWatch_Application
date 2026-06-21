package com.leaderboard.repository;

import com.leaderboard.model.UserTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTaskRepository extends JpaRepository<UserTask, Long> {
    Page<UserTask> findByTaskId(Long taskId, Pageable pageable);
    Optional<UserTask> findByUserIdAndTaskId(Long userId, Long taskId);
    List<UserTask> findByUserId(Long userId);
}
