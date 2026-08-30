package com.smartwatch.leaderboard.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "leaderboard",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"challenge_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_leaderboard_challenge_rank", columnList = "challenge_id, leaderboard_rank")
        }
)
@Getter
@Setter
public class Leaderboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Double finalScore;

    @Column(nullable = false, name = "leaderboard_rank")
    private Integer rank;

    @Column(nullable = false)
    private Integer awardedPoints;

    @Column(nullable = false)
    private LocalDateTime generatedAt;
}