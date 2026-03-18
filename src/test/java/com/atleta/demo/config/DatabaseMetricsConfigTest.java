package com.atleta.demo.config;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DatabaseMetricsConfig.
 * Tests the metrics registration functionality without requiring full Spring context.
 * **Validates: Requirements 7.1, 7.2, 7.4**
 */
class DatabaseMetricsConfigTest {

    private DatabaseMetricsConfig databaseMetricsConfig;
    private MeterRegistry meterRegistry;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        jdbcTemplate = mock(JdbcTemplate.class);
        databaseMetricsConfig = new DatabaseMetricsConfig(jdbcTemplate, meterRegistry);
    }

    @Test
    void shouldRegisterHikariMetricsWhenDataSourceIsHikari() {
        // Given
        HikariDataSource hikariDataSource = mock(HikariDataSource.class);
        when(hikariDataSource.getMaximumPoolSize()).thenReturn(10);
        
        // When
        var meterBinder = databaseMetricsConfig.hikariMetrics(hikariDataSource);
        meterBinder.bindTo(meterRegistry);
        
        // Then
        assertThat(meterRegistry.getMeters())
            .isNotEmpty()
            .anyMatch(meter -> meter.getId().getName().startsWith("hikari"));
    }

    @Test
    void shouldRegisterCustomDatabaseMetrics() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
            .thenReturn(5);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
            .thenReturn(3);
        
        // When
        databaseMetricsConfig.handleApplicationReady(mock(ApplicationReadyEvent.class));
        
        // Then
        assertThat(meterRegistry.find("atleta.athletes.active").gauge()).isNotNull();
        assertThat(meterRegistry.find("atleta.teams.total").gauge()).isNotNull();
        assertThat(meterRegistry.find("atleta.matches.created").gauge()).isNotNull();
        assertThat(meterRegistry.find("atleta.matches.started").gauge()).isNotNull();
        assertThat(meterRegistry.find("atleta.matches.finished").gauge()).isNotNull();
        assertThat(meterRegistry.find("atleta.players.with_positions").gauge()).isNotNull();
    }

    @Test
    void shouldReturnValidMetricValues() {
        // Given
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM player_profiles WHERE trust_score > 0", Integer.class))
            .thenReturn(10);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM teams", Integer.class))
            .thenReturn(5);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM matches WHERE estado = ?", Integer.class, "CREADO"))
            .thenReturn(3);
        
        // When
        databaseMetricsConfig.handleApplicationReady(mock(ApplicationReadyEvent.class));
        
        // Then
        assertThat(meterRegistry.find("atleta.athletes.active").gauge().value()).isEqualTo(10.0);
        assertThat(meterRegistry.find("atleta.teams.total").gauge().value()).isEqualTo(5.0);
        assertThat(meterRegistry.find("atleta.matches.created").gauge().value()).isEqualTo(3.0);
    }

    @Test
    void shouldHandleDatabaseExceptionsGracefully() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
            .thenThrow(new RuntimeException("Database error"));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
            .thenThrow(new RuntimeException("Database error"));
        
        // When
        databaseMetricsConfig.handleApplicationReady(mock(ApplicationReadyEvent.class));
        
        // Then - metrics should still be registered with 0.0 values
        assertThat(meterRegistry.find("atleta.athletes.active").gauge().value()).isEqualTo(0.0);
        assertThat(meterRegistry.find("atleta.teams.total").gauge().value()).isEqualTo(0.0);
        assertThat(meterRegistry.find("atleta.matches.created").gauge().value()).isEqualTo(0.0);
    }

    @Test
    void shouldConfigureMeterRegistryCustomizer() {
        // When
        var customizer = databaseMetricsConfig.metricsCommonTags();
        
        // Then
        assertThat(customizer).isNotNull();
        
        // Apply customizer and verify it doesn't throw exceptions
        customizer.customize(meterRegistry);
    }
}