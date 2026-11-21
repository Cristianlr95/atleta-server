package com.atleta.demo.migration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfEnvironmentVariable(named = "CAP02_TEST_JDBC_URL", matches = "jdbc:postgresql:.*")
class CaptainMigrationPostgresTest {
    @Test
    void upgradesAnExistingRosterFromV029AndPreservesCoachingAndMemberships() throws Exception {
        String schema = "cap_" + UUID.randomUUID().toString().replace("-", "");
        var config = migrationConfig(schema);
        try (var db = config.getDataSource().getConnection(); var sql = db.createStatement()) {
            try {
                config.target("029").load().migrate();
                db.setSchema(schema);
                sql.executeUpdate("""
                        INSERT INTO athletes(email, password_hash, nombre) VALUES
                        ('a@example.test', 'x', 'A'), ('b@example.test', 'x', 'B'),
                        ('c@example.test', 'x', 'C'), ('d@example.test', 'x', 'D')
                        """);
                sql.executeUpdate("INSERT INTO player_profiles(atleta_uuid) SELECT atleta_uuid FROM athletes");
                sql.executeUpdate("INSERT INTO teams(nombre, creador_user_id) SELECT 'Club', atleta_uuid FROM athletes WHERE nombre = 'A'");
                sql.executeUpdate("""
                        INSERT INTO team_members(team_id, user_id, rol, activo)
                        SELECT t.id, a.atleta_uuid,
                            CASE a.nombre WHEN 'A' THEN 'CAPITAN' WHEN 'B' THEN 'JUGADOR' ELSE 'DT' END,
                            a.nombre <> 'D'
                        FROM teams t CROSS JOIN athletes a
                        """);

                var upgrade = config.target("031").load();
                assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(2);
                upgrade.validate();
                assertThat(upgrade.info().current().getVersion().getVersion()).isEqualTo("031");
                try (var rows = sql.executeQuery("""
                        SELECT m.rol, m.activo, m.es_dt
                        FROM team_members m JOIN athletes a ON a.atleta_uuid = m.user_id ORDER BY a.nombre
                        """)) {
                    var roster = new ArrayList<String>();
                    while (rows.next()) {
                        roster.add("%s:%s:%s".formatted(rows.getString("rol"), rows.getBoolean("activo"), rows.getBoolean("es_dt")));
                    }
                    assertThat(roster).containsExactly("CAPITAN:true:false", "JUGADOR:true:false", "DT:true:true", "DT:false:true");
                }
                try (var history = sql.executeQuery("SELECT id FROM team_captain_transfers")) {
                    assertThat(history.next()).isFalse();
                }
            } finally {
                sql.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"false,0", "false,1", "false,2", "true,0", "true,1", "true,2"})
    void migratesOnlyCompatibleTeams(boolean archived, int captainCount) throws Exception {
        String schema = "cap_" + UUID.randomUUID().toString().replace("-", "");
        var config = migrationConfig(schema);
        try (var db = config.getDataSource().getConnection(); var sql = db.createStatement()) {
            try {
                config.target("028").load().migrate();
                db.setSchema(schema);
                sql.executeUpdate("""
                        INSERT INTO athletes(email,password_hash,nombre)
                        VALUES ('a@example.test','x','A'),('b@example.test','x','B')
                        """);
                sql.executeUpdate("INSERT INTO player_profiles(atleta_uuid) SELECT atleta_uuid FROM athletes");
                sql.executeUpdate("INSERT INTO teams(nombre,creador_user_id,archived) SELECT 'Club', atleta_uuid, "
                        + archived + " FROM athletes WHERE nombre='A'");
                sql.executeUpdate("INSERT INTO team_members(team_id,user_id,rol) SELECT t.id,p.atleta_uuid,'CAPITAN'"
                        + " FROM teams t CROSS JOIN player_profiles p LIMIT " + captainCount);
                var migration = config.target("029").load();
                if (captainCount == 2 || (!archived && captainCount == 0)) {
                    String reason = captainCount == 0 ? "activos sin capitan=1"
                            : archived ? "archivados con duplicados=1" : "activos con duplicados=1";
                    assertThatThrownBy(migration::migrate).isInstanceOf(FlywayException.class)
                            .hasStackTraceContaining(reason);
                    assertThat(migration.info().current().getVersion().getVersion()).isEqualTo("028");
                    return;
                }
                migration.migrate();
                assertThat(migration.info().current().getVersion().getVersion()).isEqualTo("029");
                String insert = "INSERT INTO team_members(team_id,user_id,rol,activo) "
                        + "SELECT t.id,p.atleta_uuid,'CAPITAN',";
                sql.executeUpdate(insert + "FALSE FROM teams t CROSS JOIN player_profiles p");
                if (captainCount == 0) {
                    sql.executeUpdate(insert + "TRUE FROM teams t CROSS JOIN player_profiles p LIMIT 1");
                }
                assertThatThrownBy(() -> sql.executeUpdate(insert + "TRUE FROM teams t CROSS JOIN player_profiles p "
                        + "WHERE NOT EXISTS (SELECT 1 FROM team_members m WHERE m.activo AND m.user_id=p.atleta_uuid)"))
                        .isInstanceOfSatisfying(SQLException.class, e -> assertThat(e.getSQLState()).isEqualTo("23505"));
            } finally {
                sql.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }

    private FluentConfiguration migrationConfig(String schema) {
        return Flyway.configure().dataSource(System.getenv("CAP02_TEST_JDBC_URL"),
                System.getenv("SPRING_DATASOURCE_USERNAME"), System.getenv("SPRING_DATASOURCE_PASSWORD"))
                .schemas(schema).locations("classpath:db/migration");
    }
}
