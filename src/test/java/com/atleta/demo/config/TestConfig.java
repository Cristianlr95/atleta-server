package com.atleta.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Instant;

/**
 * Test configuration that provides beans needed for integration tests.
 * Disables security for easier testing.
 */
@TestConfiguration
@EnableWebSecurity
public class TestConfig {

    /**
     * Provides an ObjectMapper bean for JSON serialization/deserialization in tests.
     * Configured with JavaTimeModule to handle LocalDateTime and other Java 8 time types.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    @Profile("!test")
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder testJwtDecoder() {
        return token -> {
            Instant now = Instant.now();
            return Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject("00000000-0000-0000-0000-000000000000")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();
        };
    }

    /**
     * Disables security for integration tests to allow unrestricted access to endpoints.
     */
    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));
        
        return http.build();
    }
}
