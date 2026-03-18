package com.atleta.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestDatabaseConfig.class, TestConfig.class})
class TestcontainersPropertyTest {

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldProvideUsableTestDatabaseConfiguration() {
        assertThat(dataSource).isNotNull();
        assertThat(jdbcTemplate).isNotNull();

        assertThatCode(() -> {
            try (Connection connection = dataSource.getConnection()) {
                assertThat(connection.isValid(5)).isTrue();
                assertThat(connection.getMetaData().getDatabaseProductName()).isNotBlank();
            }
        }).doesNotThrowAnyException();

        Long athletesTable = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE lower(table_name) = 'athletes'",
            Long.class
        );
        Long flywayTable = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE lower(table_name) = 'flyway_schema_history'",
            Long.class
        );

        assertThat(athletesTable).isEqualTo(1L);
        assertThat(flywayTable).isEqualTo(1L);
    }
}
