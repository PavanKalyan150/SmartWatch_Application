package com.smartwatch.leaderboard.repository;

import com.smartwatch.leaderboard.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Modifying
    @Query("UPDATE User u SET u.pointsBalance = u.pointsBalance + :points WHERE u.id = :userId")
    void addPoints(@Param("userId") Long userId, @Param("points") int points);
}