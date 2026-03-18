package com.atleta.demo.migration;

import com.atleta.demo.config.BaseIntegrationTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationExecutionPropertyTest extends BaseIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Test
    void shouldAutomaticallyExecuteMigrationsOnStartup() {
        MigrationInfo[] migrations = flyway.info().all();

        assertThat(flyway).isNotNull();
        assertThat(migrations).isNotEmpty();
        assertThat(countRowsInTable("flyway_schema_history")).isGreaterThan(0);

        for (MigrationInfo migration : migrations) {
            if (migration.getVersion() != null) {
                assertThat(migration.getState().getDisplayName()).isEqualToIgnoringCase("SUCCESS");
                assertThat(migration.getInstalledOn()).isNotNull();
                assertThat(migration.getDescription()).isNotBlank();
                assertThat(migration.getScript()).isNotBlank();
                assertThat(migration.getExecutionTime()).isGreaterThanOrEqualTo(0);
            }
        }
    }

    @Test
    void shouldMaintainMigrationHistoryCorrectly() {
        MigrationInfo[] migrations = flyway.info().all();
        var historyRecords = jdbcTemplate.queryForList(
            """
            SELECT version, description, script, installed_on, success
            FROM flyway_schema_history
            WHERE version IS NOT NULL
            ORDER BY installed_rank
            """
        );

        long successfulMigrations = java.util.Arrays.stream(migrations)
            .filter(migration -> migration.getVersion() != null)
            .filter(migration -> "SUCCESS".equalsIgnoreCase(migration.getState().getDisplayName()))
            .count();

        assertThat(historyRecords).isNotEmpty();
        assertThat(historyRecords).hasSize((int) successfulMigrations);
        historyRecords.forEach(record -> {
            assertThat(record.get("version")).isNotNull();
            assertThat(record.get("description")).isNotNull();
            assertThat(record.get("script")).isNotNull();
            assertThat(record.get("installed_on")).isNotNull();
            assertThat(record.get("success")).isEqualTo(Boolean.TRUE);
        });
    }

    @Test
    void shouldBeIdempotentOnMultipleStartups() {
        MigrationInfo[] initialMigrations = flyway.info().all();
        long initialHistoryCount = countRowsInTable("flyway_schema_history");

        flyway.migrate();

        MigrationInfo[] afterMigrations = flyway.info().all();
        long afterHistoryCount = countRowsInTable("flyway_schema_history");

        assertThat(afterMigrations).hasSameSizeAs(initialMigrations);
        assertThat(afterHistoryCount).isEqualTo(initialHistoryCount);
        for (int i = 0; i < initialMigrations.length; i++) {
            if (initialMigrations[i].getVersion() != null) {
                assertThat(afterMigrations[i].getState()).isEqualTo(initialMigrations[i].getState());
                assertThat(afterMigrations[i].getInstalledOn()).isEqualTo(initialMigrations[i].getInstalledOn());
            }
        }
    }
}
