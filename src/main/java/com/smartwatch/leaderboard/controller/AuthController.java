package com.smartwatch.leaderboard.controller;

import com.smartwatch.leaderboard.dto.request.LoginRequest;
import com.smartwatch.leaderboard.dto.request.RegisterRequest;
import com.smartwatch.leaderboard.dto.response.AuthResponse;
import com.smartwatch.leaderboard.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.debug("Register attempt for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        log.info("User registered successfully: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("Login attempt for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        log.info("Login successful for email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        log.info("Logout called — stateless acknowledgement");
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
