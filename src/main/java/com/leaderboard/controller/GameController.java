package com.leaderboard.controller;

import com.leaderboard.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/game")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> triggerGamification() {
        Map<String, String> response = new HashMap<>();
        try {
            gameService.runGamificationJob();
            response.put("status", "SUCCESS");
            response.put("message", "Gamification batch job triggered successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "FAILED");
            response.put("message", "Error starting gamification batch job: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
