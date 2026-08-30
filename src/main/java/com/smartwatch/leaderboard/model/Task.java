package com.smartwatch.leaderboard.model;

import com.smartwatch.leaderboard.model.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "tasks",
        indexes = {
                @Index(name = "idx_tasks_level_metric_status", columnList = "required_level, required_metric, status")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class Task {

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
    private String requiredMetric;      // validated against device capability_code in service layer

    @Column(nullable = false)
    private Double targetValue;

    @Column(nullable = false)
    private Integer rewardPoints;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;
}