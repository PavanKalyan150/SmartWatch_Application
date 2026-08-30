package com.smartwatch.leaderboard.model;

import com.smartwatch.leaderboard.model.enums.UserChallengeStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_challenge",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "challenge_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserChallengeStatus status;

    private Double progressValue;

    private Double finalScore;
    // computed by batch — sum of completed task points within window
    @Column(name = "user_rank")
    private Integer rank;              // null until batch assigns it

    @Builder.Default
    @Column(nullable = false)
    private Integer pointsAwarded = 0;  // zero for NONE reward scheme

    private LocalDateTime joinedAt;

    private LocalDateTime rankedAt;     // null until batch runs
}