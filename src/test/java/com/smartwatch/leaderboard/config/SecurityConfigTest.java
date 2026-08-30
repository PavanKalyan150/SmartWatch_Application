package com.smartwatch.leaderboard.config;

import com.smartwatch.leaderboard.dto.response.HealthResponse;
import com.smartwatch.leaderboard.dto.response.HealthResponse.ComponentHealth;
import com.smartwatch.leaderboard.service.HealthService;
import com.smartwatch.leaderboard.utils.JwtAuthFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired private WebApplicationContext context;
    @Autowired private SecurityFilterChain securityFilterChain;
    @Autowired private DaoAuthenticationProvider authenticationProvider;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockBean private HealthService healthService;
    @MockBean private JwtAuthFilter jwtAuthFilter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // /health responds with a real body
        when(healthService.checkHealth()).thenReturn(new HealthResponse(
                "UP",
                Instant.now(),
                Map.of(
                        "database", new ComponentHealth("UP", null),
                        "kafka",    new ComponentHealth("UP", null)
                )
        ));

        // CRITICAL: mocked JwtAuthFilter must delegate to the next filter,
        // otherwise Spring Security's auth check is never reached
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    // ================== Bean wiring ==================

    @Test
    void securityFilterChain_shouldBeRegistered() {
        assertThat(securityFilterChain).isNotNull();
    }

    @Test
    void authenticationProvider_shouldBeRegistered() {
        assertThat(authenticationProvider).isNotNull();
    }

    @Test
    void authenticationManager_shouldBeRegistered() {
        assertThat(authenticationManager).isNotNull();
    }

    @Test
    void passwordEncoder_shouldBeBCrypt() {
        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void passwordEncoder_shouldEncodeAndMatchPasswords() {
        String raw = "MySecret123!";
        String encoded = passwordEncoder.encode(raw);

        assertThat(encoded).isNotEqualTo(raw);
        assertThat(passwordEncoder.matches(raw, encoded)).isTrue();
        assertThat(passwordEncoder.matches("WrongPassword", encoded)).isFalse();
    }

    @Test
    void jwtAuthFilter_shouldBeRegisteredInFilterChain() {
        boolean hasJwtFilter = securityFilterChain.getFilters().stream()
                .map(Filter::getClass)
                .map(Class::getSimpleName)
                .anyMatch(name -> name.contains("JwtAuthFilter")
                        || name.contains("Mockito"));

        assertThat(hasJwtFilter).isTrue();
    }

    // ================== Public endpoints (permitAll) ==================

    @Test
    void healthEndpoint_shouldBePublic() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }

    @Test
    void authEndpoints_shouldBePublic() throws Exception {
        int status = mockMvc.perform(post("/auth/login"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isNotIn(401, 403);
    }

    @Test
    void devicesEndpoint_shouldBePublic() throws Exception {
        int status = mockMvc.perform(get("/devices"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isNotIn(401, 403);
    }

    @Test
    void swaggerUi_shouldBePublic() throws Exception {
        int status = mockMvc.perform(get("/swagger-ui.html"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isNotIn(401, 403);
    }

    @Test
    void apiDocs_shouldBePublic() throws Exception {
        int status = mockMvc.perform(get("/v3/api-docs"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isNotIn(401, 403);
    }

    @Test
    void errorEndpoint_shouldBePublic() throws Exception {
        int status = mockMvc.perform(get("/error"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isNotIn(401, 403);
    }

    // ================== Protected endpoints ==================
    @Test
    void anyArbitraryEndpoint_shouldRequireAuthentication() throws Exception {
        int status = mockMvc.perform(get("/some/random/protected/path"))
                .andReturn().getResponse().getStatus();

        // Either is acceptable — point is the request was blocked
        assertThat(status).isIn(401, 403);
    }

    // ================== CSRF & session ==================

    @Test
    void csrf_shouldBeDisabled_postWithoutCsrfTokenSucceeds() throws Exception {
        int status = mockMvc.perform(post("/auth/login"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isNotEqualTo(403);
    }

    @Test
    void session_shouldBeStateless_noJsessionidCookieSet() throws Exception {
        String setCookie = mockMvc.perform(get("/health"))
                .andReturn().getResponse().getHeader("Set-Cookie");

        if (setCookie != null) {
            assertThat(setCookie).doesNotContain("JSESSIONID");
        }
    }
}