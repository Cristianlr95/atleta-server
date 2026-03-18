package com.atleta.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestDatabaseConfig.class, TestConfig.class})
class StartupValidationPropertyTest extends BaseIntegrationTest {

    @Autowired(required = false)
    private Environment environment;

    @Autowired(required = false)
    private DataSource dataSource;

    @Test
    void shouldValidateStartupConfigurationAndDatabaseConnectivity() {
        assertThat(environment).isNotNull();
        assertThat(dataSource).isNotNull();
        assertThat(jdbcTemplate).isNotNull();

        assertThat(environment.getActiveProfiles()).contains("test");
        assertThat(environment.getProperty("spring.datasource.url")).isNotBlank();
        assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isTrue();

        assertThatCode(() -> {
            try (Connection connection = dataSource.getConnection()) {
                assertThat(connection.isValid(5)).isTrue();
            }
        }).doesNotThrowAnyException();

        Long successfulMigrations = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true",
            Long.class
        );

        assertThat(successfulMigrations).isNotNull().isGreaterThan(0);
        assertThat(tableExists("athletes")).isTrue();
        assertThat(tableExists("positions")).isTrue();
        assertThat(countRowsInTable("positions")).isGreaterThan(0);
    }
}
