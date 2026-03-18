package com.atleta.demo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Test configuration for database using PostgreSQL with Testcontainers.
 * Provides PostgreSQL database configuration for integration tests.
 */
@TestConfiguration
public class TestDatabaseConfig {

    /**
     * Creates and configures a PostgreSQL container for testing.
     * This provides a real PostgreSQL database for integration tests.
     * 
     * @return PostgreSQL container configured for testing
     */
    @Bean
    @ServiceConnection
    @ConditionalOnProperty(name = "testcontainers.enabled", havingValue = "true")
    static PostgreSQLContainer<?> postgreSQLContainer() {
        return new PostgreSQLContainer<>("postgres:16")
                .withDatabaseName("atleta_test")
                .withUsername("test")
                .withPassword("test");
    }
}
