package com.smartwatch.leaderboard.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "challenge_task",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"challenge_id", "task_id"})
        },
        indexes = {
                @Index(name = "idx_challenge_task", columnList = "challenge_id, task_id")
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChallengeTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;
}