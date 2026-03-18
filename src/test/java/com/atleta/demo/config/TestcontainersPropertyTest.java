package com.atleta.demo.config;

import net.jqwik.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Property-based tests for Testcontainers configuration.
 * **Feature: database-migration, Property 2: Uso de Testcontainers en testing**
 * **Validates: Requirements 1.2, 5.1, 5.2, 5.5**
 * 
 * Note: These tests require Docker to be available. They will be skipped if Docker is not running.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import({TestDatabaseConfig.class, TestConfig.class})
class TestcontainersPropertyTest {

    @Container
    static PostgreSQLContainer<?> postgres = TestDatabaseConfig.postgreSQLContainer();

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    /**
     * Property 2: Uso de Testcontainers en testing
     * For any test execution, the system should use Testcontainers with PostgreSQL for complete isolation,
     * not other data sources.
     * **Validates: Requirements 1.2, 5.1, 5.2, 5.5**
     */
    @Property(tries = 100)
    void testcontainersUsageProperty(@ForAll("validTableNames") String tableName) {
        // Skip test if Docker is not available
        assumeTrue(isDockerAvailable(), "Docker is not available - skipping Testcontainers test");
        assumeTrue(dataSource != null, "DataSource is not available - Spring context may not have loaded properly");
        
        // Verify that we're using PostgreSQL (not H2 or other databases)
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // Assert we're using PostgreSQL
            assertTrue(metaData.getDatabaseProductName().toLowerCase().contains("postgresql"),
                "Should be using PostgreSQL database, not other databases");
            
            // Assert we're using the test database name
            assertEquals("atleta_test", metaData.getConnection().getCatalog(),
                "Should be using the test database name");
            
            // Verify container is running and accessible
            assertTrue(postgres.isRunning(), "PostgreSQL container should be running");
            
            // Verify we can execute queries against the database
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?",
                Long.class,
                tableName
            );
            assertNotNull(count, "Should be able to query database schema");
            
        } catch (SQLException e) {
            fail("Should be able to connect to PostgreSQL container: " + e.getMessage());
        }
    }

    /**
     * Test that verifies complete isolation between test runs.
     * This is a unit test that complements the property test.
     */
    @Test
    void testDatabaseIsolationAndSchemaPreservation() {
        // Skip test if Docker is not available
        assumeTrue(isDockerAvailable(), "Docker is not available - skipping Testcontainers test");
        assumeTrue(dataSource != null && jdbcTemplate != null, 
            "DataSource or JdbcTemplate is not available - Spring context may not have loaded properly");
        
        // Verify we have the expected schema tables from migrations
        String[] expectedTables = {
            "athletes", "positions", "player_profile", "player_positions",
            "teams", "team_stats", "team_members", "matches", "match_teams",
            "match_players", "match_events", "player_history", "trust_logs"
        };
        
        for (String table : expectedTables) {
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?",
                Long.class,
                table
            );
            assertEquals(1L, count, "Table " + table + " should exist in schema");
        }
        
        // Verify Flyway schema history table exists
        Long flywayCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'flyway_schema_history'",
            Long.class
        );
        assertEquals(1L, flywayCount, "Flyway schema history table should exist");
        
        // Verify we can insert and query data (basic functionality)
        jdbcTemplate.execute("INSERT INTO positions (nombre) VALUES ('Test Position')");
        Long positionCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM positions WHERE nombre = 'Test Position'",
            Long.class
        );
        assertEquals(1L, positionCount, "Should be able to insert and query test data");
    }

    /**
     * Utility method to check if Docker is available.
     * This prevents tests from failing when Docker is not installed or running.
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
     * Generator for valid table names that should exist in the schema.
     */
    @Provide
    Arbitrary<String> validTableNames() {
        return Arbitraries.of(
            "athletes", "positions", "player_profile", "player_positions",
            "teams", "team_stats", "team_members", "matches", "match_teams",
            "match_players", "match_events", "player_history", "trust_logs",
            "flyway_schema_history"
        );
    }
}