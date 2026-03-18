package com.atleta.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Health indicator personalizado para la base de datos Atleta.
 * Verifica conectividad, existencia de tablas críticas y estado de migraciones.
 */
@Component
public class AtletaDatabaseHealthIndicator implements HealthIndicator {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String[] CRITICAL_TABLES = {
        "athletes", "player_profiles", "positions", "teams", 
        "matches", "match_players", "player_history"
    };

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();
            
            // Verificar conectividad básica
            if (!checkDatabaseConnectivity()) {
                return Health.down()
                    .withDetail("error", "No se puede conectar a la base de datos")
                    .build();
            }
            details.put("connectivity", "OK");

            // Verificar existencia de tablas críticas
            Map<String, Boolean> tableStatus = checkCriticalTables();
            details.put("critical_tables", tableStatus);
            
            boolean allTablesExist = tableStatus.values().stream().allMatch(exists -> exists);
            if (!allTablesExist) {
                return Health.down()
                    .withDetails(details)
                    .withDetail("error", "Faltan tablas críticas")
                    .build();
            }

            // Verificar estado de migraciones Flyway
            Map<String, Object> migrationStatus = checkFlywayMigrations();
            details.put("flyway_migrations", migrationStatus);
            
            if (!(Boolean) migrationStatus.get("all_applied")) {
                return Health.down()
                    .withDetails(details)
                    .withDetail("error", "Migraciones pendientes o fallidas")
                    .build();
            }

            // Verificar datos básicos
            Map<String, Integer> basicCounts = getBasicCounts();
            details.put("basic_counts", basicCounts);

            return Health.up()
                .withDetails(details)
                .build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", "Error durante health check: " + e.getMessage())
                .withException(e)
                .build();
        }
    }

    /**
     * Verifica la conectividad básica con la base de datos.
     */
    private boolean checkDatabaseConnectivity() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica la existencia de todas las tablas críticas.
     */
    private Map<String, Boolean> checkCriticalTables() {
        Map<String, Boolean> tableStatus = new HashMap<>();
        
        for (String tableName : CRITICAL_TABLES) {
            try {
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE UPPER(table_name) = UPPER(?)",
                    Integer.class, tableName);
                tableStatus.put(tableName, count != null && count > 0);
            } catch (Exception e) {
                tableStatus.put(tableName, false);
            }
        }
        
        return tableStatus;
    }

    /**
     * Verifica el estado de las migraciones Flyway.
     */
    private Map<String, Object> checkFlywayMigrations() {
        Map<String, Object> migrationStatus = new HashMap<>();
        
        try {
            // Verificar si existe la tabla de historial de Flyway
            Integer flywayTableExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'flyway_schema_history'",
                Integer.class);
            
            if (flywayTableExists == null || flywayTableExists == 0) {
                migrationStatus.put("flyway_table_exists", false);
                migrationStatus.put("all_applied", false);
                migrationStatus.put("error", "Tabla flyway_schema_history no existe");
                return migrationStatus;
            }
            
            migrationStatus.put("flyway_table_exists", true);

            // Obtener información de migraciones
            Integer totalMigrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history", Integer.class);
            migrationStatus.put("total_migrations", totalMigrations);

            Integer successfulMigrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Integer.class);
            migrationStatus.put("successful_migrations", successfulMigrations);

            Integer failedMigrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = false", Integer.class);
            migrationStatus.put("failed_migrations", failedMigrations);

            // Obtener la última migración aplicada
            List<Map<String, Object>> lastMigration = jdbcTemplate.queryForList(
                "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1");
            
            if (!lastMigration.isEmpty()) {
                migrationStatus.put("last_migration", lastMigration.get(0));
            }

            // Determinar si todas las migraciones están aplicadas correctamente
            migrationStatus.put("all_applied", failedMigrations == 0);

        } catch (Exception e) {
            migrationStatus.put("error", "Error verificando migraciones: " + e.getMessage());
            migrationStatus.put("all_applied", false);
        }
        
        return migrationStatus;
    }

    /**
     * Obtiene conteos básicos de entidades principales.
     */
    private Map<String, Integer> getBasicCounts() {
        Map<String, Integer> counts = new HashMap<>();
        
        try {
            counts.put("athletes", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM athletes", Integer.class));
            counts.put("positions", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM positions", Integer.class));
            counts.put("teams", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM teams", Integer.class));
            counts.put("matches", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM matches", Integer.class));
        } catch (Exception e) {
            // Si hay error, devolver conteos en 0
            counts.put("error", -1);
        }
        
        return counts;
    }
}
