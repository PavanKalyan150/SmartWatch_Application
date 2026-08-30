package com.smartwatch.leaderboard.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "levels")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Level {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String levelName;

    @Column(nullable = false)
    private Integer pointThreshold;
}