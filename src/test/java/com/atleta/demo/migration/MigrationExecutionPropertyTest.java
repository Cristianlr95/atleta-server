package com.atleta.demo.migration;

import com.atleta.demo.config.BaseIntegrationTest;
import net.jqwik.api.*;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for automatic migration execution.
 * 
 * **Validates: Requirements 2.1, 2.2**
 * 
 * **Property 4: Ejecución automática de migraciones**
 * For any application startup, Flyway should automatically execute pending migrations
 * and maintain the history in flyway_schema_history.
 */
class MigrationExecutionPropertyTest extends BaseIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Property(tries = 100)
    @Label("Feature: database-migration, Property 4: For any application startup, Flyway should automatically execute pending migrations and maintain history")
    void shouldAutomaticallyExecuteMigrationsOnStartup(@ForAll("migrationScenarios") MigrationScenario scenario) {
        // Given: Application has started (this happens automatically in test setup)
        assertNotNull(flyway, "Flyway should be configured and available");
        
        // When: We check the migration info
        MigrationInfo[] migrations = flyway.info().all();
        
        // Then: All migrations should be applied successfully
        assertTrue(migrations.length > 0, 
            "Should have at least one migration available");
        
        // And: All versioned migrations should be in SUCCESS state
        for (MigrationInfo migration : migrations) {
            if (migration.getVersion() != null) { // Skip repeatable migrations
                assertEquals("SUCCESS", migration.getState().getDisplayName(),
                    "Migration " + migration.getVersion() + " should be in SUCCESS state");
                assertNotNull(migration.getInstalledOn(),
                    "Migration " + migration.getVersion() + " should have installation timestamp");
            }
        }
        
        // And: Schema history table should exist and contain records
        long historyCount = countRowsInTable("flyway_schema_history");
        assertTrue(historyCount > 0, 
            "flyway_schema_history should contain migration records");
        
        // And: The number of successful migrations should match history records
        long successfulMigrations = java.util.Arrays.stream(migrations)
            .filter(m -> m.getVersion() != null)
            .filter(m -> "SUCCESS".equals(m.getState().getDisplayName()))
            .count();
        
        assertTrue(successfulMigrations > 0, 
            "Should have at least one successful migration");
        
        // Verify that each successful migration has proper metadata
        for (MigrationInfo migration : migrations) {
            if (migration.getVersion() != null && "SUCCESS".equals(migration.getState().getDisplayName())) {
                assertNotNull(migration.getDescription(), 
                    "Migration " + migration.getVersion() + " should have description");
                assertNotNull(migration.getScript(), 
                    "Migration " + migration.getVersion() + " should have script name");
                assertTrue(migration.getExecutionTime() >= 0, 
                    "Migration " + migration.getVersion() + " should have non-negative execution time");
            }
        }
    }

    @Property(tries = 100)
    @Label("Feature: database-migration, Property 4: Migration history should be maintained correctly")
    void shouldMaintainMigrationHistoryCorrectly(@ForAll("migrationScenarios") MigrationScenario scenario) {
        // Given: Migrations have been executed
        MigrationInfo[] migrations = flyway.info().all();
        
        // When: We query the schema history directly
        var historyRecords = jdbcTemplate.queryForList(
            """
            SELECT version, description, type, script, checksum, installed_by, 
                   installed_on, execution_time, success 
            FROM flyway_schema_history 
            WHERE version IS NOT NULL 
            ORDER BY installed_rank
            """
        );
        
        // Then: History should contain all successful migrations
        assertTrue(historyRecords.size() > 0, 
            "Schema history should contain migration records");
        
        // And: Each history record should have required fields
        for (var record : historyRecords) {
            assertNotNull(record.get("version"), 
                "History record should have version");
            assertNotNull(record.get("description"), 
                "History record should have description");
            assertNotNull(record.get("script"), 
                "History record should have script name");
            assertNotNull(record.get("installed_on"), 
                "History record should have installation timestamp");
            assertTrue((Boolean) record.get("success"), 
                "History record should be marked as successful");
        }
        
        // And: History should be consistent with migration info
        long successfulMigrations = java.util.Arrays.stream(migrations)
            .filter(m -> m.getVersion() != null)
            .filter(m -> "SUCCESS".equals(m.getState().getDisplayName()))
            .count();
        
        assertEquals(successfulMigrations, historyRecords.size(),
            "Number of successful migrations should match history records");
    }

    @Property(tries = 100)
    @Label("Feature: database-migration, Property 4: Migration execution should be idempotent")
    void shouldBeIdempotentOnMultipleStartups(@ForAll("migrationScenarios") MigrationScenario scenario) {
        // Given: Initial migration state
        MigrationInfo[] initialMigrations = flyway.info().all();
        long initialHistoryCount = countRowsInTable("flyway_schema_history");
        
        // When: We run migrations again (simulating another startup)
        flyway.migrate();
        
        // Then: Migration state should remain the same
        MigrationInfo[] afterMigrations = flyway.info().all();
        long afterHistoryCount = countRowsInTable("flyway_schema_history");
        
        assertEquals(initialMigrations.length, afterMigrations.length,
            "Number of migrations should remain the same");
        assertEquals(initialHistoryCount, afterHistoryCount,
            "History count should remain the same");
        
        // And: All migrations should still be in SUCCESS state
        for (int i = 0; i < initialMigrations.length; i++) {
            if (initialMigrations[i].getVersion() != null) {
                assertEquals(initialMigrations[i].getState(), afterMigrations[i].getState(),
                    "Migration state should remain unchanged");
                assertEquals(initialMigrations[i].getInstalledOn(), afterMigrations[i].getInstalledOn(),
                    "Installation timestamp should remain unchanged");
            }
        }
    }

    @Provide
    Arbitrary<MigrationScenario> migrationScenarios() {
        return Arbitraries.of(
            new MigrationScenario("normal_startup", "Normal application startup"),
            new MigrationScenario("restart_scenario", "Application restart scenario"),
            new MigrationScenario("clean_startup", "Clean database startup"),
            new MigrationScenario("existing_data", "Startup with existing data")
        );
    }

    /**
     * Represents different migration execution scenarios for property testing.
     */
    static class MigrationScenario {
        private final String name;
        private final String description;

        public MigrationScenario(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return name + ": " + description;
        }
    }
}