package com.smartwatch.leaderboard.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256-ok";
    private static final long EXPIRATION_MS = 60_000L; // 1 minute
    private static final String EMAIL = "user@example.com";

    private JwtUtil jwtUtil;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", EXPIRATION_MS);

        userDetails = new User(EMAIL, "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void shouldGenerateNonNullTokenWithThreeParts() {
        String token = jwtUtil.generateToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    void shouldExtractUsernameFromGeneratedToken() {
        String token = jwtUtil.generateToken(userDetails);

        String username = jwtUtil.extractUsername(token);

        assertThat(username).isEqualTo(EMAIL);
    }

    @Test
    void shouldReturnTrueWhenTokenIsValidForMatchingUser() {
        String token = jwtUtil.generateToken(userDetails);

        boolean valid = jwtUtil.isTokenValid(token, userDetails);

        assertThat(valid).isTrue();
    }

    @Test
    void shouldReturnFalseWhenTokenSubjectDoesNotMatchUser() {
        String token = jwtUtil.generateToken(userDetails);
        UserDetails otherUser = new User("other@example.com", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

        boolean valid = jwtUtil.isTokenValid(token, otherUser);

        assertThat(valid).isFalse();
    }

    @Test
    void shouldReturnFalseWhenTokenIsExpired() {
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", -1000L); // already expired
        String expiredToken = jwtUtil.generateToken(userDetails);

        boolean valid = jwtUtil.isTokenValid(expiredToken, userDetails);

        assertThat(valid).isFalse();
    }

    @Test
    void shouldReturnFalseWhenTokenIsMalformed() {
        boolean valid = jwtUtil.isTokenValid("this.is.not-a-valid-jwt", userDetails);

        assertThat(valid).isFalse();
    }

    @Test
    void shouldReturnFalseWhenTokenIsSignedWithDifferentKey() {
        String otherSecret = "another-secret-key-also-256-bits-long-for-hmac-sha256-padding!";
        SecretKey differentKey = Keys.hmacShaKeyFor(otherSecret.getBytes(StandardCharsets.UTF_8));
        String foreignToken = Jwts.builder()
                .subject(EMAIL)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(differentKey)
                .compact();

        boolean valid = jwtUtil.isTokenValid(foreignToken, userDetails);

        assertThat(valid).isFalse();
    }

    @Test
    void shouldReturnFalseWhenTokenIsBlank() {
        boolean valid = jwtUtil.isTokenValid("", userDetails);

        assertThat(valid).isFalse();
    }

    @Test
    void shouldGenerateUniqueTokensWhenCalledAtDifferentTimes() throws InterruptedException {
        String token1 = jwtUtil.generateToken(userDetails);
        Thread.sleep(1100); // JWT timestamps are second-precision; need >1s gap
        String token2 = jwtUtil.generateToken(userDetails);

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void shouldThrowWhenExtractingUsernameFromInvalidToken() {
        // extractUsername doesn't catch — caller (JwtAuthFilter) handles the exception.
        // This test locks in that contract so we know not to swallow exceptions here.
        org.junit.jupiter.api.Assertions.assertThrows(
                io.jsonwebtoken.JwtException.class,
                () -> jwtUtil.extractUsername("garbage.token.here"));
    }
}