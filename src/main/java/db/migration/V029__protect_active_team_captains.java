package db.migration;

import java.sql.SQLException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V029__protect_active_team_captains extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws SQLException {
        var connection = context.getConnection();
        String vendor = connection.getMetaData().getDatabaseProductName();
        try (var sql = connection.createStatement()) {
            if ("PostgreSQL".equals(vendor)) {
                // Keep the preflight and index creation on the same set of memberships.
                sql.execute("LOCK TABLE teams, team_members IN SHARE ROW EXCLUSIVE MODE");
            } else if (!"H2".equals(vendor)) {
                throw new SQLException("V029: motor no soportado: " + vendor);
            }
            try (var counts = sql.executeQuery("""
                    SELECT count(*) FILTER (WHERE NOT archived AND captains = 0),
                           count(*) FILTER (WHERE NOT archived AND captains > 1),
                           count(*) FILTER (WHERE archived AND captains > 1)
                    FROM (
                        SELECT t.id, t.archived, count(m.id) AS captains
                        FROM teams t LEFT JOIN team_members m
                          ON m.team_id = t.id AND m.activo AND m.rol = 'CAPITAN'
                        GROUP BY t.id, t.archived
                    ) AS inventory
                    """)) {
                counts.next();
                long missing = counts.getLong(1);
                long activeDuplicates = counts.getLong(2);
                long archivedDuplicates = counts.getLong(3);
                if (missing + activeDuplicates + archivedDuplicates > 0) {
                    throw new SQLException("V029: activos sin capitan=" + missing
                            + ", activos con duplicados=" + activeDuplicates + ", archivados con duplicados=" + archivedDuplicates
                            + ". Revisar scripts/inventory-team-captains.sql y resolver las asignaciones antes de reintentar.");
                }
            }
            if ("PostgreSQL".equals(vendor)) {
                sql.execute("CREATE UNIQUE INDEX uq_team_active_captain ON team_members (team_id) WHERE activo AND rol = 'CAPITAN'");
            } else {
                // H2 has no partial indexes. Non-captains get NULL and do not compete for a team.
                sql.execute("ALTER TABLE team_members ADD active_captain_team BIGINT GENERATED ALWAYS AS (CASE WHEN activo AND rol = 'CAPITAN' THEN team_id END)");
                sql.execute("CREATE UNIQUE INDEX uq_team_active_captain ON team_members (active_captain_team)");
            }
        }
    }
}
