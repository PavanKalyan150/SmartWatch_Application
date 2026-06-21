package com.leaderboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WatchLeaderboardApplication {
    public static void main(String[] args) {
        SpringApplication.run(WatchLeaderboardApplication.class, args);
    }
}
