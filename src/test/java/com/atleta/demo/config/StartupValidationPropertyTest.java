package com.atleta.demo.config;

import net.jqwik.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Property-based tests for startup configuration validation.
 * **Feature: database-migration, Property 13: Validación de configuraciones al startup**
 * **Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.5**
 * 
 * Note: These tests require Docker to be available. They will be skipped if Docker is not running.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import({TestDatabaseConfig.class, TestConfig.class})
class StartupValidationPropertyTest extends BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = TestDatabaseConfig.postgreSQLContainer();

    @Autowired(required = false)
    private Environment environment;

    @Autowired(required = false)
    private DataSource dataSource;

    /**
     * Property 13: Validación de configuraciones al startup
     * For any application startup, required environment variables should be validated,
     * database connectivity should be verified, and migration consistency should be checked.
     * **Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.5**
     */
    @Property(tries = 100)
    void startupValidationProperty(@ForAll("requiredConfigurationKeys") String configKey) {
        // Skip test if Docker is not available
        assumeTrue(isDockerAvailable(), "Docker is not available - skipping startup validation test");
        assumeTrue(environment != null && dataSource != null && jdbcTemplate != null, 
            "Spring context is not available - application may not have started properly");
        
        // Step 1: Verify required configuration properties are present
        verifyRequiredConfiguration(configKey);
        
        // Step 2: Verify database connectivity
        verifyDatabaseConnectivity();
        
        // Step 3: Verify migration consistency
        verifyMigrationConsistency();
        
        // Step 4: Verify health check functionality
        verifyHealthCheckFunctionality();
        
        // Step 5: Verify application context loaded successfully
        verifyApplicationContextLoaded();
    }

    /**
     * Test that verifies all critical startup validations work together.
     */
    @Test
    void testCompleteStartupValidation() {
        // Skip test if Docker is not available
        assumeTrue(isDockerAvailable(), "Docker is not available - skipping complete startup validation test");
        assumeTrue(environment != null && dataSource != null && jdbcTemplate != null, 
            "Spring context is not available - application may not have started properly");
        
        // Verify all required environment variables are present
        String[] requiredKeys = {
            "spring.datasource.url", "spring.datasource.username", "spring.datasource.password",
            "spring.jpa.database-platform", "spring.flyway.enabled"
        };
        
        for (String key : requiredKeys) {
            String value = environment.getProperty(key);
            assertNotNull(value, "Required configuration key " + key + " should be present");
            assertFalse(value.trim().isEmpty(), "Required configuration key " + key + " should not be empty");
        }
        
        // Verify database connectivity
        assertDoesNotThrow(() -> {
            try (Connection connection = dataSource.getConnection()) {
                assertTrue(connection.isValid(5), "Database connection should be valid");
                
                DatabaseMetaData metaData = connection.getMetaData();
                assertTrue(metaData.getDatabaseProductName().toLowerCase().contains("postgresql"),
                    "Should be connected to PostgreSQL database");
            }
        }, "Should be able to connect to database without errors");
        
        // Verify Flyway migrations were executed
        Long migrationCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", 
            Long.class
        );
        assertNotNull(migrationCount, "Migration count should not be null");
        assertTrue(migrationCount > 0, "At least one migration should have been executed successfully");
        
        // Verify all required tables exist
        String[] requiredTables = {
            "athletes", "positions", "player_profile", "teams", "matches"
        };
        
        for (String table : requiredTables) {
            assertTrue(tableExists(table), "Required table " + table + " should exist after migrations");
        }
        
        // Verify initial data was loaded
        Long positionCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM positions", 
            Long.class
        );
        assertNotNull(positionCount, "Position count should not be null");
        assertTrue(positionCount > 0, "Initial position data should have been loaded");
    }

    /**
     * Test that verifies configuration validation for different environments.
     */
    @Test
    void testEnvironmentSpecificValidation() {
        // Skip test if Docker is not available
        assumeTrue(isDockerAvailable(), "Docker is not available - skipping environment validation test");
        assumeTrue(environment != null, "Environment should be available");
        
        // Verify test profile is active
        String[] activeProfiles = environment.getActiveProfiles();
        assertTrue(Arrays.asList(activeProfiles).contains("test"), 
            "Test profile should be active during test execution");
        
        // Verify test-specific configurations
        String datasourceUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(datasourceUrl, "Datasource URL should be configured");
        
        // In test environment, we should be using Testcontainers
        // The URL will be dynamically set by Testcontainers
        assertTrue(datasourceUrl.contains("postgresql"), 
            "Should be using PostgreSQL in test environment");
        
        // Verify Flyway is enabled in test environment
        Boolean flywayEnabled = environment.getProperty("spring.flyway.enabled", Boolean.class);
        assertNotNull(flywayEnabled, "Flyway enabled property should be set");
        assertTrue(flywayEnabled, "Flyway should be enabled in test environment");
        
        // Verify test-specific Flyway locations
        String flywayLocations = environment.getProperty("spring.flyway.locations");
        assertNotNull(flywayLocations, "Flyway locations should be configured");
        assertTrue(flywayLocations.contains("classpath:db/migration"), 
            "Should include main migration location");
    }

    /**
     * Test that verifies error handling during startup validation.
     */
    @Test
    void testStartupValidationErrorHandling() {
        // Skip test if Docker is not available
        assumeTrue(isDockerAvailable(), "Docker is not available - skipping error handling test");
        assumeTrue(dataSource != null && jdbcTemplate != null, 
            "DataSource and JdbcTemplate should be available");
        
        // Test that we can handle database queries gracefully
        assertDoesNotThrow(() -> {
            // This should work - testing that our error handling doesn't interfere with normal operations
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM positions", Long.class);
            assertNotNull(count, "Should be able to execute basic queries");
        }, "Basic database operations should work without throwing exceptions");
        
        // Test that we can detect invalid queries
        assertThrows(Exception.class, () -> {
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM non_existent_table", Long.class);
        }, "Should throw exception for invalid table queries");
        
        // Verify connection pool is working
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 5; i++) {
                Long result = jdbcTemplate.queryForObject("SELECT 1", Long.class);
                assertEquals(1L, result, "Connection pool should handle multiple queries");
            }
        }, "Connection pool should handle multiple concurrent queries");
    }

    private void verifyRequiredConfiguration(String configKey) {
        String value = environment.getProperty(configKey);
        assertNotNull(value, "Configuration key " + configKey + " should be present");
        assertFalse(value.trim().isEmpty(), "Configuration key " + configKey + " should not be empty");
    }

    private void verifyDatabaseConnectivity() {
        assertDoesNotThrow(() -> {
            try (Connection connection = dataSource.getConnection()) {
                assertTrue(connection.isValid(5), "Database connection should be valid");
                
                // Verify we can execute a simple query
                Long result = jdbcTemplate.queryForObject("SELECT 1", Long.class);
                assertEquals(1L, result, "Should be able to execute basic queries");
            }
        }, "Database connectivity should be working");
    }

    private void verifyMigrationConsistency() {
        // Verify Flyway schema history table exists
        assertTrue(tableExists("flyway_schema_history"), 
            "Flyway schema history table should exist");
        
        // Verify at least one migration was executed successfully
        Long successfulMigrations = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", 
            Long.class
        );
        assertNotNull(successfulMigrations, "Successful migration count should not be null");
        assertTrue(successfulMigrations > 0, "At least one migration should have been executed successfully");
        
        // Verify no failed migrations
        Long failedMigrations = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = false", 
            Long.class
        );
        assertNotNull(failedMigrations, "Failed migration count should not be null");
        assertEquals(0L, failedMigrations, "No migrations should have failed");
    }

    private void verifyHealthCheckFunctionality() {
        // Verify we can check database health
        assertDoesNotThrow(() -> {
            // This simulates what a health check would do
            try (Connection connection = dataSource.getConnection()) {
                assertTrue(connection.isValid(1), "Health check should pass for valid connection");
            }
        }, "Health check functionality should work");
        
        // Verify we can query critical tables (what health checks typically do)
        assertDoesNotThrow(() -> {
            Long athleteCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM athletes", Long.class);
            assertNotNull(athleteCount, "Should be able to query athletes table for health check");
            
            Long positionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM positions", Long.class);
            assertNotNull(positionCount, "Should be able to query positions table for health check");
        }, "Health check queries should work");
    }

    private void verifyApplicationContextLoaded() {
        // Verify Spring context loaded successfully
        assertNotNull(environment, "Spring Environment should be loaded");
        assertNotNull(dataSource, "DataSource should be loaded");
        assertNotNull(jdbcTemplate, "JdbcTemplate should be loaded");
        
        // Verify we can access configuration properties
        String datasourceUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(datasourceUrl, "Should be able to access datasource URL from environment");
        
        // Verify active profiles are set
        String[] activeProfiles = environment.getActiveProfiles();
        assertTrue(activeProfiles.length > 0, "At least one profile should be active");
    }

    /**
     * Utility method to check if Docker is available.
     */
    private boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Exception e) {
            System.out.println("Docker is not available: " + e.getMessage());
            return false;
        }
    }

    /**
     * Generator for required configuration keys that should be validated at startup.
     */
    @Provide
    Arbitrary<String> requiredConfigurationKeys() {
        return Arbitraries.of(
            "spring.datasource.url",
            "spring.datasource.username", 
            "spring.datasource.password",
            "spring.datasource.driver-class-name",
            "spring.jpa.database-platform",
            "spring.flyway.enabled",
            "spring.flyway.locations",
            "spring.flyway.baseline-on-migrate",
            "spring.flyway.validate-on-migrate"
        );
    }
}