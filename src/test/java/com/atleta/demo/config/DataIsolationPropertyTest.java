package com.atleta.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestDatabaseConfig.class, TestConfig.class})
class DataIsolationPropertyTest extends BaseIntegrationTest {

    @Autowired(required = false)
    private DataSource dataSource;

    @Test
    void shouldPreserveSchemaWhileCleaningInsertedData() {
        assertThat(dataSource).isNotNull();
        assertThat(jdbcTemplate).isNotNull();
        assertThat(tableExists("athletes")).isTrue();
        assertThat(tableExists("positions")).isTrue();
        assertThat(tableExists("flyway_schema_history")).isTrue();

        String testEmail = "cleanup-" + UUID.randomUUID() + "@example.com";
        UUID athleteUuid = UUID.randomUUID();

        jdbcTemplate.update(
            "INSERT INTO athletes (atleta_uuid, email, password_hash, nombre) VALUES (?, ?, ?, ?)",
            athleteUuid, testEmail, "hash", "Cleanup Test"
        );

        Long beforeCleanup = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM athletes WHERE email = ?",
            Long.class,
            testEmail
        );
        assertThat(beforeCleanup).isEqualTo(1L);

        cleanupTestData();

        Long afterCleanup = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM athletes WHERE email = ?",
            Long.class,
            testEmail
        );
        assertThat(afterCleanup).isZero();
        assertThat(tableExists("athletes")).isTrue();
        assertThat(countRowsInTable("positions")).isGreaterThan(0);
    }
}
