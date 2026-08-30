package com.smartwatch.leaderboard.model;

import com.smartwatch.leaderboard.model.enums.UserTaskStatus;
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
        name = "user_task",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "task_id"})
        },
        indexes = {
                @Index(name = "idx_user_task", columnList = "user_id, task_id")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Builder.Default
    @Column(nullable = false)
    private Double progressValue = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserTaskStatus status;

    private LocalDateTime completedAt;

    // Points awarded when this task was completed — 0 if not yet completed
    @Builder.Default
    @Column(nullable = false)
    private Integer pointsAwarded = 0;
}