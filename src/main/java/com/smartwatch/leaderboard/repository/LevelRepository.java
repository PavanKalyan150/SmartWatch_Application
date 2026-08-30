package com.smartwatch.leaderboard.repository;

import com.smartwatch.leaderboard.model.Level;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LevelRepository extends JpaRepository<Level, Long> {

    // Used at registration to assign the entry-level (lowest threshold)
    Optional<Level> findTopByOrderByPointThresholdAsc();

    List<Level> findAllByOrderByPointThresholdDesc();

    // Used when checking level-up — find the highest level the user's points qualify for
    Optional<Level> findTopByPointThresholdLessThanEqualOrderByPointThresholdDesc(int points);
}