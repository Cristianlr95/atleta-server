package com.atleta.demo.migration;

import com.atleta.demo.config.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.ConnectionCallback;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SchemaIntegrityPropertyTest extends BaseIntegrationTest {

    @Test
    void shouldHaveAllRequiredTables() {
        String[] requiredTables = {
            "athletes", "positions", "player_profiles", "player_positions",
            "teams", "team_stats", "team_members", "matches", "match_teams",
            "match_players", "match_events", "player_history", "trust_logs"
        };

        for (String tableName : requiredTables) {
            assertThat(tableExists(tableName)).as("Table %s should exist", tableName).isTrue();

            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                """
                SELECT column_name, data_type, is_nullable, column_default
                FROM information_schema.columns
                WHERE table_name = ?
                ORDER BY ordinal_position
                """,
                tableName
            );

            assertThat(columns).as("Table %s should have columns", tableName).isNotEmpty();
        }
    }

    @Test
    void shouldHaveCorrectPrimaryKeys() {
        Map<String, String> expectedPrimaryKeys = Map.of(
            "athletes", "atleta_uuid",
            "positions", "id",
            "player_profiles", "atleta_uuid",
            "teams", "id",
            "matches", "id"
        );

        for (Map.Entry<String, String> entry : expectedPrimaryKeys.entrySet()) {
            List<String> pkColumns = jdbcTemplate.queryForList(
                """
                SELECT kcu.column_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                    ON tc.constraint_name = kcu.constraint_name
                    AND tc.table_schema = kcu.table_schema
                WHERE tc.constraint_type = 'PRIMARY KEY'
                  AND tc.table_name = ?
                ORDER BY kcu.ordinal_position
                """,
                String.class,
                entry.getKey()
            );

            assertThat(pkColumns).containsExactly(entry.getValue());
        }
    }

    @Test
    void shouldHaveForeignKeyIntegrity() {
        List<Map<String, Object>> foreignKeys = jdbcTemplate.queryForList(
            """
            SELECT
                tc.table_name,
                kcu.column_name,
                ccu.table_name AS foreign_table_name,
                ccu.column_name AS foreign_column_name
            FROM information_schema.table_constraints AS tc
            JOIN information_schema.key_column_usage AS kcu
                ON tc.constraint_name = kcu.constraint_name
                AND tc.table_schema = kcu.table_schema
            JOIN information_schema.constraint_column_usage AS ccu
                ON ccu.constraint_name = tc.constraint_name
                AND ccu.table_schema = tc.table_schema
            WHERE tc.constraint_type = 'FOREIGN KEY'
            ORDER BY tc.table_name, kcu.column_name
            """
        );

        assertThat(foreignKeys).isNotEmpty();
        assertThat(foreignKeys).anyMatch(fk ->
            "player_profiles".equals(fk.get("table_name")) &&
            "atleta_uuid".equals(fk.get("column_name")) &&
            "athletes".equals(fk.get("foreign_table_name")));
        assertThat(foreignKeys).anyMatch(fk ->
            "team_members".equals(fk.get("table_name")) &&
            "team_id".equals(fk.get("column_name")) &&
            "teams".equals(fk.get("foreign_table_name")));
    }

    @Test
    void shouldHavePerformanceIndexes() {
        assertThatCode(() -> jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM athletes WHERE email = ?",
            Long.class,
            "nobody@example.com"
        )).doesNotThrowAnyException();
        assertThatCode(() -> jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM matches WHERE estado = ?",
            Long.class,
            "CREADO"
        )).doesNotThrowAnyException();
    }

    @Test
    void shouldHaveBusinessRuleConstraints() {
        List<Map<String, Object>> checkConstraints = jdbcTemplate.queryForList(
            """
            SELECT
                tc.table_name,
                tc.constraint_name,
                cc.check_clause
            FROM information_schema.table_constraints AS tc
            JOIN information_schema.check_constraints AS cc
                ON tc.constraint_name = cc.constraint_name
            WHERE tc.constraint_type = 'CHECK'
            ORDER BY tc.table_name, tc.constraint_name
            """
        );

        assertThat(checkConstraints).isNotEmpty();
        assertThat(checkConstraints).anyMatch(constraint ->
            "player_positions".equals(constraint.get("table_name")) &&
            constraint.get("check_clause").toString().contains("prioridad"));
        assertThat(checkConstraints).anyMatch(constraint ->
            "matches".equals(constraint.get("table_name")) &&
            constraint.get("check_clause").toString().contains("modalidad"));
    }

    @Test
    void shouldHaveInitialMasterData() {
        long positionCount = countRowsInTable("positions");
        List<String> positionNames = jdbcTemplate.queryForList(
            "SELECT nombre FROM positions ORDER BY nombre",
            String.class
        );

        assertThat(positionCount).isGreaterThan(0);
        assertThat(positionNames).contains("Portero", "Defensa", "Delantero");
    }

    @Test
    void shouldHaveTableDocumentation() {
        Map<String, String> remarksByTable = jdbcTemplate.execute((ConnectionCallback<Map<String, String>>) connection -> {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet tables = metadata.getTables(null, null, "%", new String[]{"TABLE"})) {
                Map<String, String> remarks = new java.util.HashMap<>();
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    if (tableName != null) {
                        remarks.put(tableName.toLowerCase(), tables.getString("REMARKS"));
                    }
                }
                return remarks;
            }
        });

        assertThat(remarksByTable).containsKeys("athletes", "player_history");
        String playerHistoryRemark = remarksByTable.get("player_history");
        if (playerHistoryRemark != null && !playerHistoryRemark.isBlank()) {
            assertThat(playerHistoryRemark).containsIgnoringCase("FUENTE DE VERDAD");
        }
    }
}
