package com.atleta.demo.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

/**
 * H2 asigna nombres internos a las restricciones UNIQUE sin conservar el
 * nombre usado en la migracion V007. Esta compatibilidad solo se ejecuta en
 * el perfil de pruebas, donde elimina la antigua restriccion por rol que no
 * existe en PostgreSQL desde V007.
 */
@Component
@Profile("test")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class H2MatchPlayerConstraintCompatibility implements ApplicationRunner {

    private static final List<String> LEGACY_COLUMNS = List.of("MATCH_ID", "TEAM_ID", "ROL");

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public H2MatchPlayerConstraintCompatibility(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!isH2()) {
            return;
        }

        List<String> constraints = jdbcTemplate.queryForList("""
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE UPPER(table_schema) = 'PUBLIC'
                  AND UPPER(table_name) = 'MATCH_PLAYERS'
                  AND constraint_type = 'UNIQUE'
                """, String.class);

        for (String constraint : constraints) {
            List<String> columns = jdbcTemplate.queryForList("""
                    SELECT column_name
                    FROM information_schema.key_column_usage
                    WHERE UPPER(table_schema) = 'PUBLIC'
                      AND UPPER(table_name) = 'MATCH_PLAYERS'
                      AND constraint_name = ?
                    ORDER BY ordinal_position
                    """, String.class, constraint);

            if (LEGACY_COLUMNS.equals(columns.stream().map(String::toUpperCase).toList())) {
                jdbcTemplate.execute("ALTER TABLE match_players DROP CONSTRAINT \""
                        + constraint.replace("\"", "\"\"") + "\"");
            }
        }
    }

    private boolean isH2() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            return "H2".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        }
    }
}
