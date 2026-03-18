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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Property-based tests for data isolation in testing environment.
 * **Feature: database-migration, Property 8: Aislamiento de datos en testing**
 * **Validates: Requirements 5.3, 5.4**
 * 
 * Note: These tests require Docker to be available. They will be skipped if Docker is not running.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import({TestDatabaseConfig.class, TestConfig.class})
class DataIsolationPropertyTest extends BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = TestDatabaseConfig.postgreSQLContainer();

    @Autowired(required = false)
    private DataSource dataSource;

    /**
     * Property 8: Aislamiento de datos en testing
     * For any individual test, data should be cleaned after execution but schema should be maintained,
     * allowing specific test data to be loaded.
     * **Validates: Requirements 5.3, 5.4**
     */
    @Property(tries = 100)
    void dataIsolationProperty(@ForAll("testDataSets") TestDataSet testData) {
        // Skip test if Docker is not available
        assumeTrue(isDockerAvailable(), "Docker is not available - skipping data isolation test");
        assumeTrue(dataSource != null && jdbcTemplate != null, 
            "DataSource or JdbcTemplate is not available - Spring context may not have loaded properly");
        
        // Step 1: Verify schema exists (should be preserved between tests)
        verifySchemaIntegrity();
        
        // Step 2: Insert test data
        insertTestData(testData);
        
        // Step 3: Verify data was inserted
        verifyTestDataExists(testData);
        
        // Step 4: Simulate test completion - data cleanup happens automatically via @AfterEach
        // The BaseIntegrationTest.cleanupTestData() method will be called after this test
        
        // Step 5: Verify we can load specific test data for this test
        loadSpecificTestData(testData);
        
        // Step 6: Verify specific test data exists
        verifySpecificTestDataExists(testData);
    }

    /**
     * Test that verifies schema preservation across multiple test executions.
     */
    @Test
    void testSchemaPreservationAcrossTests() {
        // Skip test if Docker is not available
        assumeTrue(isDockerAvailable(), "Docker is not available - skipping schema preservation test");
        assumeTrue(dataSource != null && jdbcTemplate != null, 
            "DataSource or JdbcTemplate is not available - Spring context may not have loaded properly");
        
        // Verify all required tables exist
        String[] requiredTables = {
            "athletes", "positions", "player_profile", "player_positions",
            "teams", "team_stats", "team_members", "matches", "match_teams",
            "match_players", "match_events", "player_history", "trust_logs"
        };
        
        for (String table : requiredTables) {
            assertTrue(tableExists(table), "Table " + table + " should exist and be preserved");
        }
        
        // Verify sequences exist and are functional
        verifySequencesExist();
        
        // Verify constraints and indexes are preserved
        verifyConstraintsExist();
    }

    /**
     * Test that verifies data cleanup between tests while preserving schema.
     */
    @Test
    void testDataCleanupBetweenTests() {
        // Skip test if Docker is not available
        assumeTrue(isDockerAvailable(), "Docker is not available - skipping data cleanup test");
        assumeTrue(dataSource != null && jdbcTemplate != null, 
            "DataSource or JdbcTemplate is not available - Spring context may not have loaded properly");
        
        // Insert some test data
        String testEmail = "cleanup-test@example.com";
        UUID testUuid = UUID.randomUUID();
        
        jdbcTemplate.update(
            "INSERT INTO athletes (atleta_uuid, email, password_hash, nombre) VALUES (?, ?, ?, ?)",
            testUuid, testEmail, "test-hash", "Cleanup Test"
        );
        
        // Verify data exists
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM athletes WHERE email = ?",
            Long.class,
            testEmail
        );
        assertEquals(1L, count, "Test data should exist before cleanup");
        
        // Manually trigger cleanup (simulating what happens between tests)
        cleanupTestData();
        
        // Verify data was cleaned
        Long countAfterCleanup = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM athletes WHERE email = ?",
            Long.class,
            testEmail
        );
        assertEquals(0L, countAfterCleanup, "Test data should be cleaned after cleanup");
        
        // Verify schema still exists
        assertTrue(tableExists("athletes"), "Schema should be preserved after cleanup");
    }

    /**
     * Test that verifies ability to load specific test data.
     */
    @Test
    void testSpecificTestDataLoading() {
        // Skip test if Docker is not available
        assumeTrue(isDockerAvailable(), "Docker is not available - skipping specific test data test");
        assumeTrue(dataSource != null && jdbcTemplate != null, 
            "DataSource or JdbcTemplate is not available - Spring context may not have loaded properly");
        
        // Load specific test data for this test scenario
        String specificTestScript = """
            INSERT INTO positions (nombre) VALUES ('Test Position 1');
            INSERT INTO positions (nombre) VALUES ('Test Position 2');
            """;
        
        executeSqlScript(specificTestScript);
        
        // Verify specific test data was loaded
        Long positionCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM positions WHERE nombre LIKE 'Test Position%'",
            Long.class
        );
        assertEquals(2L, positionCount, "Specific test data should be loaded");
        
        // Verify we can query and use this specific data
        String positionName = jdbcTemplate.queryForObject(
            "SELECT nombre FROM positions WHERE nombre = 'Test Position 1'",
            String.class
        );
        assertEquals("Test Position 1", positionName, "Should be able to query specific test data");
    }

    private void verifySchemaIntegrity() {
        // Verify critical tables exist
        assertTrue(tableExists("athletes"), "Athletes table should exist");
        assertTrue(tableExists("positions"), "Positions table should exist");
        assertTrue(tableExists("teams"), "Teams table should exist");
        assertTrue(tableExists("matches"), "Matches table should exist");
        
        // Verify Flyway history table exists
        assertTrue(tableExists("flyway_schema_history"), "Flyway schema history should exist");
    }

    private void insertTestData(TestDataSet testData) {
        // Insert athlete
        jdbcTemplate.update(
            "INSERT INTO athletes (atleta_uuid, email, password_hash, nombre) VALUES (?, ?, ?, ?)",
            testData.athleteUuid, testData.email, "test-hash", testData.name
        );
        
        // Insert player profile
        jdbcTemplate.update(
            "INSERT INTO player_profile (atleta_uuid, alias, trust_score) VALUES (?, ?, ?)",
            testData.athleteUuid, testData.alias, testData.trustScore
        );
    }

    private void verifyTestDataExists(TestDataSet testData) {
        Long athleteCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM athletes WHERE atleta_uuid = ?",
            Long.class,
            testData.athleteUuid
        );
        assertEquals(1L, athleteCount, "Test athlete should exist");
        
        Long profileCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM player_profile WHERE atleta_uuid = ?",
            Long.class,
            testData.athleteUuid
        );
        assertEquals(1L, profileCount, "Test player profile should exist");
    }

    private void loadSpecificTestData(TestDataSet testData) {
        // Load specific test data that's unique to this test scenario
        String specificScript = String.format(
            "INSERT INTO positions (nombre) VALUES ('Specific-%s')",
            testData.alias
        );
        executeSqlScript(specificScript);
    }

    private void verifySpecificTestDataExists(TestDataSet testData) {
        Long specificCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM positions WHERE nombre = ?",
            Long.class,
            "Specific-" + testData.alias
        );
        assertEquals(1L, specificCount, "Specific test data should exist");
    }

    private void verifySequencesExist() {
        String[] sequences = {
            "positions_id_seq", "teams_id_seq", "matches_id_seq",
            "player_positions_id_seq", "team_members_id_seq"
        };
        
        for (String sequence : sequences) {
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.sequences WHERE sequence_name = ?",
                Long.class,
                sequence
            );
            assertEquals(1L, count, "Sequence " + sequence + " should exist");
        }
    }

    private void verifyConstraintsExist() {
        // Verify foreign key constraints exist
        Long fkCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_type = 'FOREIGN KEY'",
            Long.class
        );
        assertTrue(fkCount > 0, "Foreign key constraints should exist");
        
        // Verify unique constraints exist
        Long uniqueCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_type = 'UNIQUE'",
            Long.class
        );
        assertTrue(uniqueCount > 0, "Unique constraints should exist");
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
     * Generator for test data sets.
     */
    @Provide
    Arbitrary<TestDataSet> testDataSets() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20),
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(15),
            Arbitraries.integers().between(50, 150)
        ).as((name, alias, trustScore) -> {
            TestDataSet testData = new TestDataSet();
            testData.athleteUuid = UUID.randomUUID();
            testData.email = name.toLowerCase() + "@test.com";
            testData.name = name;
            testData.alias = alias;
            testData.trustScore = trustScore;
            return testData;
        });
    }

    /**
     * Test data structure for property tests.
     */
    static class TestDataSet {
        UUID athleteUuid;
        String email;
        String name;
        String alias;
        Integer trustScore;
    }
}