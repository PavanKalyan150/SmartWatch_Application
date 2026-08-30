package com.smartwatch.leaderboard.utils;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock private JwtUtil jwtUtils;
    @Mock private UserDetailsService userDetailsService;
    @Mock private HttpServletRequest request;
    @Mock private FilterChain filterChain;

    @InjectMocks private JwtAuthFilter jwtAuthFilter;

    private HttpServletResponse response;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String EMAIL = "user@example.com";

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSkipFilterWhenAuthorizationHeaderIsMissing() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtils, userDetailsService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldSkipFilterWhenAuthorizationHeaderDoesNotStartWithBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtils, userDetailsService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldAuthenticateWhenTokenIsValid() throws Exception {
        UserDetails userDetails = buildUserDetails();
        when(request.getHeader("Authorization")).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(jwtUtils.extractUsername(VALID_TOKEN)).thenReturn(EMAIL);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
        when(jwtUtils.isTokenValid(VALID_TOKEN, userDetails)).thenReturn(true);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(userDetails);
        assertThat(auth.getAuthorities()).hasSize(1);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWhenTokenIsInvalidButNotMalformed() throws Exception {
        UserDetails userDetails = buildUserDetails();
        when(request.getHeader("Authorization")).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(jwtUtils.extractUsername(VALID_TOKEN)).thenReturn(EMAIL);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
        when(jwtUtils.isTokenValid(VALID_TOKEN, userDetails)).thenReturn(false);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotReauthenticateWhenContextAlreadyHasAuthentication() throws Exception {
        UserDetails existing = buildUserDetails();
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        existing, null, existing.getAuthorities()));

        when(request.getHeader("Authorization")).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(jwtUtils.extractUsername(VALID_TOKEN)).thenReturn(EMAIL);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtUtils, never()).isTokenValid(anyString(), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWhenExtractedUsernameIsNull() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(jwtUtils.extractUsername(VALID_TOKEN)).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenIsExpired() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(request.getRequestURI()).thenReturn("/api/test");
        when(jwtUtils.extractUsername(VALID_TOKEN))
                .thenThrow(new ExpiredJwtException(null, null, "expired"));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        MockHttpServletResponse mockResponse = (MockHttpServletResponse) response;
        assertThat(mockResponse.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(mockResponse.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(mockResponse.getContentAsString())
                .contains("Your session has expired")
                .contains("Unauthorized");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenIsMalformed() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(request.getRequestURI()).thenReturn("/api/test");
        when(jwtUtils.extractUsername(VALID_TOKEN))
                .thenThrow(new MalformedJwtException("malformed"));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        MockHttpServletResponse mockResponse = (MockHttpServletResponse) response;
        assertThat(mockResponse.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(mockResponse.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(mockResponse.getContentAsString())
                .contains("Invalid or malformed token");
        verify(filterChain, never()).doFilter(request, response);
    }

    private UserDetails buildUserDetails() {
        return new User(EMAIL, "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
