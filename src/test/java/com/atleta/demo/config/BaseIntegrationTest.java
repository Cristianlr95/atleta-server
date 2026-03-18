package com.atleta.demo.config;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for integration tests that use H2 in-memory database.
 * Provides common configuration and utilities for database integration tests.
 * 
 * Features:
 * - Automatic H2 database setup via application.properties
 * - Data cleanup between tests while preserving schema
 * - Common test utilities and configuration
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestConfig.class})
@Transactional
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /**
     * Cleans up test data after each test while preserving the schema.
     * This ensures test isolation without the overhead of recreating the schema.
     */
    @AfterEach
    void cleanupTestData() {
        try {
            // Clean data from all tables in reverse dependency order
            cleanupDataTables();
        } catch (Exception e) {
            // Ignore cleanup errors in tests
        }
    }

    /**
     * Cleans up data from all tables in the correct order to respect foreign key constraints.
     * Tables are cleaned in reverse dependency order.
     */
    private void cleanupDataTables() {
        // Clean in reverse dependency order - only clean tables that exist
        String[] tablesToClean = {
            "trust_logs", "player_history", "match_events", "match_players", 
            "match_teams", "matches", "team_members", "team_stats", "teams", 
            "player_positions", "player_profiles", "athletes"
        };
        
        for (String table : tablesToClean) {
            try {
                if (tableExists(table)) {
                    jdbcTemplate.execute("DELETE FROM " + table);
                }
            } catch (Exception e) {
                // Ignore individual table cleanup errors
            }
        }
    }

    /**
     * Utility method to count rows in a table.
     * Useful for assertions in tests.
     * 
     * @param tableName the name of the table to count
     * @return the number of rows in the table
     */
    protected long countRowsInTable(String tableName) {
        try {
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName, 
                Long.class
            );
            return count != null ? count : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Utility method to check if a table exists.
     * Useful for schema validation tests.
     * 
     * @param tableName the name of the table to check
     * @return true if the table exists, false otherwise
     */
    protected boolean tableExists(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = UPPER(?)",
                Integer.class,
                tableName
            );
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Utility method to execute SQL scripts for test data setup.
     * 
     * @param sqlScript the SQL script to execute
     */
    protected void executeSqlScript(String sqlScript) {
        if (sqlScript != null && !sqlScript.trim().isEmpty()) {
            try {
                jdbcTemplate.execute(sqlScript);
            } catch (Exception e) {
                // Ignore script execution errors in tests
            }
        }
    }

    /**
     * Gets the base URL for REST API calls.
     * 
     * @return the base URL for the test server
     */
    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }
}
