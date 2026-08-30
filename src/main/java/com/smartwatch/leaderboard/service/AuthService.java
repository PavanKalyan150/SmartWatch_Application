package com.smartwatch.leaderboard.service;

import com.smartwatch.leaderboard.model.User;
import com.smartwatch.leaderboard.model.enums.Role;
import com.smartwatch.leaderboard.repository.DeviceRepository;
import com.smartwatch.leaderboard.repository.LevelRepository;
import com.smartwatch.leaderboard.repository.UserRepository;
import com.smartwatch.leaderboard.utils.JwtUtil;
import com.smartwatch.leaderboard.dto.request.LoginRequest;
import com.smartwatch.leaderboard.dto.request.RegisterRequest;
import com.smartwatch.leaderboard.dto.response.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final LevelRepository levelRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtils;

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Phone number already in use");
        }

        var device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + request.getDeviceId()));

        var entryLevel = levelRepository.findTopByOrderByPointThresholdAsc()
                .orElseThrow(() -> new IllegalStateException("No levels configured in the system"));

        var user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .fullName(request.getFullName())
                .device(device)
                .level(entryLevel)
                .pointsBalance(0)
                .role(Role.USER)
                .build();

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtils.generateToken(userDetails);

        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtUtils.generateToken(userDetails);

        return new AuthResponse(token);
    }
}