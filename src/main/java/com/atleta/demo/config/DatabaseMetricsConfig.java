package com.atleta.demo.config;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Configuración de métricas personalizadas para la base de datos Atleta.
 * Registra métricas de HikariCP y métricas específicas del dominio.
 */
@Configuration
public class DatabaseMetricsConfig {

    private final JdbcTemplate jdbcTemplate;
    private final MeterRegistry meterRegistry;

    public DatabaseMetricsConfig(@Lazy JdbcTemplate jdbcTemplate, @Lazy MeterRegistry meterRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Configura métricas de HikariCP cuando Prometheus está habilitado.
     */
    @Bean
    @ConditionalOnProperty(name = "management.prometheus.metrics.export.enabled", havingValue = "true")
    public MeterBinder hikariMetrics(DataSource dataSource) {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            return registry -> {
                // Registrar métricas básicas de HikariCP manualmente
                Gauge.builder("hikari.connections.active", hikariDataSource, 
                    ds -> (double) ds.getHikariPoolMXBean().getActiveConnections())
                    .description("Active connections")
                    .register(registry);
                
                Gauge.builder("hikari.connections.idle", hikariDataSource,
                    ds -> (double) ds.getHikariPoolMXBean().getIdleConnections())
                    .description("Idle connections")
                    .register(registry);
                
                Gauge.builder("hikari.connections.total", hikariDataSource,
                    ds -> (double) ds.getHikariPoolMXBean().getTotalConnections())
                    .description("Total connections")
                    .register(registry);
                
                Gauge.builder("hikari.connections.max", hikariDataSource,
                    ds -> (double) ds.getMaximumPoolSize())
                    .description("Max connections")
                    .register(registry);
            };
        }
        return registry -> {};
    }

    /**
     * Registra métricas personalizadas del dominio Atleta cuando la aplicación está lista.
     */
    @EventListener
    public void handleApplicationReady(ApplicationReadyEvent event) {
        registerCustomDatabaseMetrics();
    }

    /**
     * Registra métricas específicas del dominio Atleta.
     */
    private void registerCustomDatabaseMetrics() {
        // Gauge para atletas activos (con trust_score > 0)
        Gauge.builder("atleta.athletes.active", this, DatabaseMetricsConfig::getActiveAthletesCount)
            .description("Número de atletas activos en el sistema")
            .register(meterRegistry);

        // Gauge para partidos por estado
        Gauge.builder("atleta.matches.created", this, config -> config.getMatchesByStatus("CREADO"))
            .description("Número de partidos en estado CREADO")
            .register(meterRegistry);

        Gauge.builder("atleta.matches.started", this, config -> config.getMatchesByStatus("INICIADO"))
            .description("Número de partidos en estado INICIADO")
            .register(meterRegistry);

        Gauge.builder("atleta.matches.finished", this, config -> config.getMatchesByStatus("FINALIZADO"))
            .description("Número de partidos en estado FINALIZADO")
            .register(meterRegistry);

        // Gauge para equipos activos
        Gauge.builder("atleta.teams.total", this, DatabaseMetricsConfig::getTotalTeamsCount)
            .description("Número total de equipos registrados")
            .register(meterRegistry);

        // Gauge para jugadores con perfiles completos
        Gauge.builder("atleta.players.with_positions", this, DatabaseMetricsConfig::getPlayersWithPositionsCount)
            .description("Número de jugadores con al menos una posición asignada")
            .register(meterRegistry);
    }

    /**
     * Obtiene el número de atletas activos (trust_score > 0).
     */
    private Double getActiveAthletesCount() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM player_profiles WHERE trust_score > 0", 
                Integer.class);
            return count != null ? count.doubleValue() : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Obtiene el número de partidos por estado.
     */
    private Double getMatchesByStatus(String status) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM matches WHERE estado = ?", 
                Integer.class, status);
            return count != null ? count.doubleValue() : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Obtiene el número total de equipos.
     */
    private Double getTotalTeamsCount() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM teams", 
                Integer.class);
            return count != null ? count.doubleValue() : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Obtiene el número de jugadores con al menos una posición asignada.
     * Nota: Esta métrica se implementará cuando se agregue la tabla player_positions.
     */
    private Double getPlayersWithPositionsCount() {
        try {
            // Por ahora retornamos el número de perfiles de jugador
            // TODO: Implementar cuando se agregue la tabla player_positions
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM player_profiles", 
                Integer.class);
            return count != null ? count.doubleValue() : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Customizer para configurar el registro de métricas.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("application", "atleta-server");
    }
}