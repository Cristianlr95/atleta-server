package com.atleta.demo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration specifically for Flyway integration tests.
 * Uses PostgreSQL with Testcontainers for testing when Docker is available,
 * falls back to H2 when Docker is not available.
 */
@TestConfiguration
public class FlywayTestConfig {

    /**
     * Creates and configures a PostgreSQL container for testing.
     * This provides a real PostgreSQL database for testing migrations.
     * Only active when Docker is available.
     * 
     * @return PostgreSQL container configured for testing
     */
    @Bean
    @ServiceConnection
    @Profile("testcontainers")
    @ConditionalOnProperty(name = "testcontainers.enabled", havingValue = "true")
    static PostgreSQLContainer<?> postgreSQLContainer() {
        return new PostgreSQLContainer<>("postgres:16")
                .withDatabaseName("atleta_test")
                .withUsername("test")
                .withPassword("test");
    }

    /**
     * Proporciona un PasswordEncoder para tests.
     */
    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
