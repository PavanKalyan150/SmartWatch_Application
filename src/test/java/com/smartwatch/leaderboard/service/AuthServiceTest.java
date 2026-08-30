package com.smartwatch.leaderboard.service;

import com.smartwatch.leaderboard.dto.request.LoginRequest;
import com.smartwatch.leaderboard.dto.request.RegisterRequest;
import com.smartwatch.leaderboard.dto.response.AuthResponse;
import com.smartwatch.leaderboard.model.Device;
import com.smartwatch.leaderboard.model.Level;
import com.smartwatch.leaderboard.model.User;
import com.smartwatch.leaderboard.model.enums.Role;
import com.smartwatch.leaderboard.repository.DeviceRepository;
import com.smartwatch.leaderboard.repository.LevelRepository;
import com.smartwatch.leaderboard.repository.UserRepository;
import com.smartwatch.leaderboard.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private LevelRepository levelRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private JwtUtil jwtUtils;
    @Mock private UserDetails userDetails;

    @InjectMocks private AuthService authService;

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "Password1!";
    private static final String HASHED_PASSWORD = "hashed-password";
    private static final String PHONE = "+15555550123";
    private static final String FULL_NAME = "Test User";
    private static final String GENERATED_TOKEN = "generated.jwt.token";
    private static final Long DEVICE_ID = 1L;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private Device device;
    private Level entryLevel;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail(EMAIL);
        registerRequest.setPassword(PASSWORD);
        registerRequest.setPhone(PHONE);
        registerRequest.setFullName(FULL_NAME);
        registerRequest.setDeviceId(DEVICE_ID);

        loginRequest = new LoginRequest();
        loginRequest.setEmail(EMAIL);
        loginRequest.setPassword(PASSWORD);

        device = new Device();
        entryLevel = new Level();
    }

    // ---------- register() ----------

    @Test
    void shouldRegisterUserSuccessfullyWhenAllInputsValid() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.existsByPhone(PHONE)).thenReturn(false);
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));
        when(levelRepository.findTopByOrderByPointThresholdAsc()).thenReturn(Optional.of(entryLevel));
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
        when(jwtUtils.generateToken(userDetails)).thenReturn(GENERATED_TOKEN);

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(GENERATED_TOKEN);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getPasswordHash()).isEqualTo(HASHED_PASSWORD);
        assertThat(saved.getPhone()).isEqualTo(PHONE);
        assertThat(saved.getFullName()).isEqualTo(FULL_NAME);
        assertThat(saved.getDevice()).isSameAs(device);
        assertThat(saved.getLevel()).isSameAs(entryLevel);
        assertThat(saved.getPointsBalance()).isZero();
        assertThat(saved.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void shouldThrowWhenEmailAlreadyInUse() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already in use");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, jwtUtils);
    }

    @Test
    void shouldThrowWhenPhoneAlreadyInUse() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.existsByPhone(PHONE)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Phone number already in use");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, deviceRepository, levelRepository, jwtUtils);
    }

    @Test
    void shouldThrowWhenDeviceNotFound() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.existsByPhone(PHONE)).thenReturn(false);
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Device not found")
                .hasMessageContaining(DEVICE_ID.toString());

        verify(userRepository, never()).save(any());
        verifyNoInteractions(jwtUtils);
    }

    @Test
    void shouldThrowWhenNoEntryLevelConfigured() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.existsByPhone(PHONE)).thenReturn(false);
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));
        when(levelRepository.findTopByOrderByPointThresholdAsc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No levels configured in the system");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, jwtUtils);
    }

    @Test
    void shouldEncodePasswordBeforeSavingUser() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.existsByPhone(PHONE)).thenReturn(false);
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));
        when(levelRepository.findTopByOrderByPointThresholdAsc()).thenReturn(Optional.of(entryLevel));
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
        when(jwtUtils.generateToken(userDetails)).thenReturn(GENERATED_TOKEN);

        authService.register(registerRequest);

        verify(passwordEncoder).encode(PASSWORD);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        // ensures the raw password never reaches persistence
        assertThat(userCaptor.getValue().getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo(HASHED_PASSWORD);
    }

    // ---------- login() ----------

    @Test
    void shouldLoginSuccessfullyAndReturnToken() {
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
        when(jwtUtils.generateToken(userDetails)).thenReturn(GENERATED_TOKEN);

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(GENERATED_TOKEN);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(tokenCaptor.capture());

        UsernamePasswordAuthenticationToken passed = tokenCaptor.getValue();
        assertThat(passed.getPrincipal()).isEqualTo(EMAIL);
        assertThat(passed.getCredentials()).isEqualTo(PASSWORD);
    }

    @Test
    void shouldPropagateExceptionWhenAuthenticationFails() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Bad credentials");

        verifyNoInteractions(userDetailsService, jwtUtils);
    }

    @Test
    void shouldPropagateWhenUserDetailsNotFoundAfterAuthenticationSucceeds() {
        // Edge case: auth succeeds but user lookup fails (race condition / inconsistent state)
        when(userDetailsService.loadUserByUsername(EMAIL))
                .thenThrow(new UsernameNotFoundException("User not found"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(authenticationManager).authenticate(any());
        verifyNoInteractions(jwtUtils);
    }

    @Test
    void shouldAuthenticateUsingExactCredentialsFromRequest() {
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
        when(jwtUtils.generateToken(userDetails)).thenReturn(GENERATED_TOKEN);

        authService.login(loginRequest);

        verify(authenticationManager).authenticate(
                eq(new UsernamePasswordAuthenticationToken(EMAIL, PASSWORD)));
    }
}
