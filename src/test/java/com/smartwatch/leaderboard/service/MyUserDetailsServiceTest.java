package com.smartwatch.leaderboard.service;

import com.smartwatch.leaderboard.model.User;
import com.smartwatch.leaderboard.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyUserDetailsServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private MyUserDetailsService userDetailsService;

    private static final String EMAIL = "user@example.com";

    @Test
    void shouldReturnUserDetailsWhenUserExists() {
        User user = User.builder()
                .id(1L)
                .email(EMAIL)
                .passwordHash("hashed")
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername(EMAIL);

        assertThat(result).isNotNull();
        assertThat(result).isSameAs(user); // User implements UserDetails directly
        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    void shouldThrowUsernameNotFoundWhenUserMissing() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(EMAIL))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found")
                .hasMessageContaining(EMAIL);
    }

    @Test
    void shouldLookupByEmailExactlyAsProvided() {
        // Spring Security passes whatever was used as principal — no normalization here.
        // This test pins down that we don't accidentally lowercase/trim, which would
        // be a security-relevant change.
        String mixedCase = "User@Example.COM";
        when(userRepository.findByEmail(mixedCase)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(mixedCase))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(userRepository).findByEmail(mixedCase);
    }
}