package com.smartwatch.leaderboard.model;

import com.smartwatch.leaderboard.model.enums.ChallengeStatus;
import com.smartwatch.leaderboard.model.enums.RewardScheme;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "challenges")
@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "required_level", nullable = false)
    private Level requiredLevel;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RewardScheme rewardScheme;

    // null means curated by admin
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = true)
    private User createdByUser;
}