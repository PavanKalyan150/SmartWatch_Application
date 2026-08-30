package com.smartwatch.leaderboard.model;

import com.smartwatch.leaderboard.model.enums.ProcessedStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_activity_event",
        indexes = {
                @Index(name = "idx_activity_event_id", columnList = "event_id"),
                @Index(name = "idx_activity_user_metric_time", columnList = "user_id, metric_type, event_time")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId;             // business idempotency key

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String metricType;          // e.g. STEPS, HRM, SLEEP

    @Column(nullable = false)
    private Double metricValue;

    @Column(nullable = false)
    private LocalDateTime eventTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessedStatus processedStatus = ProcessedStatus.PENDING;

    private LocalDateTime processedAt;
}