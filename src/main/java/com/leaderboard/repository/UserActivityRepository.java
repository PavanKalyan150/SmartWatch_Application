package com.leaderboard.repository;

import com.leaderboard.model.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    Optional<UserActivity> findByUserIdAndActivityDate(Long userId, LocalDate activityDate);
    List<UserActivity> findByUserIdAndActivityDateBetween(Long userId, LocalDate start, LocalDate end);
}
