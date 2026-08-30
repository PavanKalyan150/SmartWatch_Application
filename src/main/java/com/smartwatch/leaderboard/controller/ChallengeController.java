package com.smartwatch.leaderboard.controller;

import com.smartwatch.leaderboard.dto.request.ChallengeRequest;
import com.smartwatch.leaderboard.dto.response.ChallengeResponse;
import com.smartwatch.leaderboard.dto.response.UserChallengeRankResponse;
import com.smartwatch.leaderboard.dto.response.UserChallengeResponse;
import com.smartwatch.leaderboard.service.ChallengeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private static final Logger log = LoggerFactory.getLogger(ChallengeController.class);

    private final ChallengeService challengeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ChallengeResponse> createChallenge(
            @Valid @RequestBody ChallengeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("User {} creating challenge: {}", userDetails.getUsername(), request.getName());
        ChallengeResponse response = challengeService.createChallenge(request, userDetails.getUsername());
        log.info("Challenge created with id: {} by: {}", response.getId(), userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<ChallengeResponse>> getChallenges(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("Fetching challenges for user: {}", userDetails.getUsername());
        List<ChallengeResponse> challenges = challengeService.getChallengesForUser(userDetails.getUsername());
        log.info("Returned {} challenges for user: {}", challenges.size(), userDetails.getUsername());
        return ResponseEntity.ok(challenges);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ChallengeResponse> getChallengeById(@PathVariable Long id) {
        log.debug("Fetching challenge id: {}", id);
        ChallengeResponse response = challengeService.getChallengeById(id);
        log.info("Challenge found with id: {}", id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChallengeResponse> updateChallenge(
            @PathVariable Long id,
            @Valid @RequestBody ChallengeRequest request) {
        log.debug("Admin updating challenge id: {}", id);
        ChallengeResponse response = challengeService.updateChallenge(id, request);
        log.info("Challenge updated with id: {}", id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{challengeId}/enroll/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<UserChallengeResponse> enrollUser(
            @PathVariable Long challengeId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("User {} enrolling userId: {} into challenge: {}", userDetails.getUsername(), userId, challengeId);
        UserChallengeResponse response = challengeService.enrollUser(challengeId, userId);
        log.info("UserId: {} successfully enrolled in challenge: {}", userId, challengeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{challengeId}/user")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<UserChallengeRankResponse> getUserRankInChallenge(
            @PathVariable Long challengeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("User {} requesting rank for challenge: {}", userDetails.getUsername(), challengeId);
        UserChallengeRankResponse response = challengeService.getUserRankInChallenge(challengeId, userDetails.getUsername());
        if (response.getRank() == null) {
            log.warn("Rank not yet assigned for user: {} in challenge: {} — batch job may not have run",
                    userDetails.getUsername(), challengeId);
        } else {
            log.info("Rank {} returned for user: {} in challenge: {}",
                    response.getRank(), userDetails.getUsername(), challengeId);
        }
        return ResponseEntity.ok(response);
    }
}