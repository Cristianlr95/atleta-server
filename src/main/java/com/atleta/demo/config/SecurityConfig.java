package com.atleta.demo.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.atleta.demo.security.ActiveSessionTokenValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, AppCorsProperties.class})
public class SecurityConfig {

    private final Environment environment;
    private final JwtProperties jwtProperties;
    private final AppCorsProperties corsProperties;
    private final ActiveSessionTokenValidator activeSessionTokenValidator;

    public SecurityConfig(
        Environment environment,
        JwtProperties jwtProperties,
        AppCorsProperties corsProperties,
        ActiveSessionTokenValidator activeSessionTokenValidator
    ) {
        this.environment = environment;
        this.jwtProperties = jwtProperties;
        this.corsProperties = corsProperties;
        this.activeSessionTokenValidator = activeSessionTokenValidator;
    }

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean isDev = environment.acceptsProfiles(Profiles.of("dev"));
        boolean isTest = environment.acceptsProfiles(Profiles.of("test"));

        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                if (isDev || isTest) {
                    auth
                        .requestMatchers(
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/swagger-ui.html",
                            "/actuator/**"
                        ).permitAll();
                }

                auth
                    .requestMatchers("/error").permitAll()
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/v1/athletes/register").permitAll()
                    .requestMatchers("/api/v1/athletes/login").permitAll()
                    .requestMatchers("/api/v1/athletes/auth/google").permitAll()
                    .requestMatchers("/api/v1/athletes/auth/refresh").permitAll()
                    .requestMatchers("/api/v1/athletes/auth/logout").permitAll()
                    .requestMatchers("/api/v1/athletes/password-reset/**").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/actuator/health/**").permitAll()
                    .anyRequest().authenticated();
            })
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    public SecretKey jwtSigningKey() {
        String configuredSecret = jwtProperties.getSecret();
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            byte[] keyBytes = decodeSecret(configuredSecret.trim());
            validateKeyLength(keyBytes);
            return new SecretKeySpec(keyBytes, "HmacSHA256");
        }

        if (environment.acceptsProfiles(Profiles.of("dev", "test"))) {
            byte[] generatedKey = new byte[32];
            new SecureRandom().nextBytes(generatedKey);
            return new SecretKeySpec(generatedKey, "HmacSHA256");
        }

        throw new IllegalStateException("JWT secret is required outside dev/test profiles");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).build();
        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefaultWithIssuer(jwtProperties.getIssuer());
        OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator(Duration.ofSeconds(30));
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            defaultValidator, timestampValidator, activeSessionTokenValidator));
        return jwtDecoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(corsProperties.getAllowedOriginPatterns());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setExposedHeaders(corsProperties.getExposedHeaders());
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        configuration.setMaxAge(corsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private byte[] decodeSecret(String secret) {
        try {
            return Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ignored) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    private void validateKeyLength(byte[] keyBytes) {
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256");
        }
    }
}
