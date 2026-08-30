package com.smartwatch.leaderboard.dto.response;

import com.smartwatch.leaderboard.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;
}