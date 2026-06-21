package com.leaderboard.controller;

import com.leaderboard.service.RankService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class RankController {

    private final RankService rankService;

    public RankController(RankService rankService) {
        this.rankService = rankService;
    }

    @GetMapping("/rank")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> triggerRanking() {
        Map<String, String> response = new HashMap<>();
        try {
            rankService.runRankingJob();
            response.put("status", "SUCCESS");
            response.put("message", "Ranking batch job triggered successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "FAILED");
            response.put("message", "Error starting ranking batch job: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
