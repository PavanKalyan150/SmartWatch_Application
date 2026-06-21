package com.leaderboard.controller;

import com.leaderboard.model.Challenge;
import com.leaderboard.model.UserChallenge;
import com.leaderboard.service.ChallengeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/challenge")
public class ChallengeController {

    private final ChallengeService challengeService;

    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Challenge> createChallenge(@RequestBody Challenge challenge) {
        return new ResponseEntity<>(challengeService.createChallenge(challenge), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<Challenge>> discoverChallenges(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(challengeService.discoverChallenges(userId, latitude, longitude, city));
    }

    @GetMapping("/{challengeId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Challenge> getChallengeById(@PathVariable Long challengeId) {
        return ResponseEntity.ok(challengeService.getChallengeById(challengeId));
    }

    @GetMapping("/{challengeId}/user")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<UserChallenge>> getChallengeUsers(@PathVariable Long challengeId, Pageable pageable) {
        return ResponseEntity.ok(challengeService.getChallengeUsers(challengeId, pageable));
    }

    @PutMapping("/{challengeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Challenge> updateChallenge(@PathVariable Long challengeId, @RequestBody Challenge challenge) {
        return ResponseEntity.ok(challengeService.updateChallenge(challengeId, challenge));
    }
}
