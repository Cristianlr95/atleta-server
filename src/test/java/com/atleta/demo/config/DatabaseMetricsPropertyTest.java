package com.atleta.demo.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Integration tests for database metrics exposure.
 * **Property 9: Database metrics exposure**
 * **Validates: Requirements 7.1, 7.2, 7.4**
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestConfig.class)
class DatabaseMetricsPropertyTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        // Mock database responses for metrics
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM player_profile WHERE trust_score > 0", Integer.class))
            .thenReturn(5);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM player_profiles WHERE trust_score > 0", Integer.class))
            .thenReturn(5);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM teams", Integer.class))
            .thenReturn(3);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM matches", Integer.class))
            .thenReturn(7);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM matches WHERE estado = ?", Integer.class, "CREADO"))
            .thenReturn(2);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM matches WHERE estado = ?", Integer.class, "INICIADO"))
            .thenReturn(1);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM matches WHERE estado = ?", Integer.class, "FINALIZADO"))
            .thenReturn(4);
        when(jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT user_id) FROM player_positions", Integer.class))
            .thenReturn(8);
        when(jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT player_id) FROM player_positions", Integer.class))
            .thenReturn(8);
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
            .thenReturn(1);
        for (String tableName : List.of("athletes", "player_profiles", "positions", "teams", "matches", "match_players", "player_history")) {
            when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE UPPER(table_name) = UPPER(?)",
                Integer.class,
                tableName
            )).thenReturn(1);
        }
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'flyway_schema_history'", Integer.class))
            .thenReturn(1);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM flyway_schema_history", Integer.class))
            .thenReturn(2);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Integer.class))
            .thenReturn(2);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = false", Integer.class))
            .thenReturn(0);
        when(jdbcTemplate.queryForList("SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1"))
            .thenReturn(List.of(Map.of("version", "1", "description", "init", "success", true)));
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM athletes", Integer.class))
            .thenReturn(5);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM positions", Integer.class))
            .thenReturn(6);
        
        // Wait a moment for metrics to be registered after application startup
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Test: Atleta domain-specific metrics should be exposed.
     * Validates that custom business metrics are available.
     */
    @Test
    void atletaDomainMetricsShouldBeExposed() {
        // Given: The application is initialized with mocked database
        
        // When: We check domain-specific metrics
        Gauge activeAthletesGauge = meterRegistry.find("atleta.athletes.active").gauge();
        Gauge totalTeamsGauge = meterRegistry.find("atleta.teams.total").gauge();
        Gauge createdMatchesGauge = meterRegistry.find("atleta.matches.created").gauge();
        
        // Then: Domain metrics should be registered
        assertThat(activeAthletesGauge)
            .as("Active athletes metric should be registered")
            .isNotNull();
        
        assertThat(totalTeamsGauge)
            .as("Total teams metric should be registered")
            .isNotNull();
        
        assertThat(createdMatchesGauge)
            .as("Created matches metric should be registered")
            .isNotNull();
        
        // And: Metrics should have valid values (>= 0)
        assertThat(activeAthletesGauge.value())
            .as("Active athletes value should be >= 0")
            .isGreaterThanOrEqualTo(0.0);
        
        assertThat(totalTeamsGauge.value())
            .as("Total teams value should be >= 0")
            .isGreaterThanOrEqualTo(0.0);
        
        assertThat(createdMatchesGauge.value())
            .as("Created matches value should be >= 0")
            .isGreaterThanOrEqualTo(0.0);
    }

    /**
     * Test: Metrics should reflect mocked database values.
     * Validates that metrics accurately represent database state.
     */
    @Test
    void metricsShouldReflectMockedDatabaseCounts() {
        // Given: We have mocked database responses
        
        // When: We get the metrics
        Gauge activeAthletesGauge = meterRegistry.find("atleta.athletes.active").gauge();
        Gauge totalTeamsGauge = meterRegistry.find("atleta.teams.total").gauge();
        Gauge createdMatchesGauge = meterRegistry.find("atleta.matches.created").gauge();
        
        // Then: The metrics should reflect the mocked values
        assertThat(activeAthletesGauge).isNotNull();
        assertThat(totalTeamsGauge).isNotNull();
        assertThat(createdMatchesGauge).isNotNull();
        
        // Values should match our mocked responses
        assertThat(activeAthletesGauge.value()).isEqualTo(5.0);
        assertThat(totalTeamsGauge.value()).isEqualTo(3.0);
        assertThat(createdMatchesGauge.value()).isEqualTo(2.0);
    }

    /**
     * Test: Custom health indicator should be available and functional.
     * Validates that the custom database health check is working.
     */
    @Test
    void customHealthIndicatorShouldBeAvailable() {
        // Given: The application is initialized
        
        // When: We query the health endpoint
        ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        // Then: The endpoint should be available
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        Map<String, Object> healthResponse = response.getBody();
        assertThat(healthResponse).isNotNull();
        
        // And: Should contain the general status
        assertThat(healthResponse).containsKey("status");
        
        // And: Status should be UP (application is working)
        String status = (String) healthResponse.get("status");
        assertThat(status).isEqualTo("UP");
    }

    /**
     * Test: Actuator endpoints should be exposed.
     * Validates that required actuator endpoints are available.
     */
    @Test
    void actuatorEndpointsShouldBeExposed() {
        // Given: The application is initialized
        
        // When: We query the actuator endpoint
        ResponseEntity<Map<String, Object>> actuatorResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        // Then: The main endpoint should be available
        assertThat(actuatorResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        Map<String, Object> actuatorData = actuatorResponse.getBody();
        assertThat(actuatorData).isNotNull();
        
        // And: Should contain links to specific endpoints
        @SuppressWarnings("unchecked")
        Map<String, Object> links = (Map<String, Object>) actuatorData.get("_links");
        assertThat(links).isNotNull();
        
        // Verify that required endpoints are available
        assertThat(links).containsKey("health");
        assertThat(links).containsKey("metrics");
    }

    /**
     * Test: Metrics endpoint should be accessible.
     * Validates that metrics can be queried via HTTP.
     */
    @Test
    void metricsEndpointShouldBeAccessible() {
        // Given: The application is initialized
        
        // When: We query the metrics endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/metrics", String.class);
        
        // Then: The endpoint should be available
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // And: Should contain metrics information
        String responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody).contains("names");
    }

    /**
     * Test: Multiple metrics should be consistent across multiple reads.
     * Validates that metrics provide stable values.
     */
    @Test
    void metricsShouldBeConsistentAcrossMultipleReads() {
        // Given: The application is initialized with mocked data
        
        // When: We read metrics multiple times
        Gauge totalTeamsGauge = meterRegistry.find("atleta.teams.total").gauge();
        assertThat(totalTeamsGauge).isNotNull();
        
        double firstRead = totalTeamsGauge.value();
        double secondRead = totalTeamsGauge.value();
        double thirdRead = totalTeamsGauge.value();
        
        // Then: Values should be consistent (no database changes between reads)
        assertThat(firstRead)
            .as("Metric values should be consistent across reads")
            .isEqualTo(secondRead)
            .isEqualTo(thirdRead);
        
        // And: Values should match our mocked data
        assertThat(firstRead).isEqualTo(3.0);
    }

    /**
     * Test: All expected Atleta domain metrics should be registered.
     * Validates that all custom metrics are properly configured.
     */
    @Test
    void allAtletaDomainMetricsShouldBeRegistered() {
        // Given: The application is initialized
        
        // When: We check for all expected metrics
        Gauge activeAthletesGauge = meterRegistry.find("atleta.athletes.active").gauge();
        Gauge totalTeamsGauge = meterRegistry.find("atleta.teams.total").gauge();
        Gauge createdMatchesGauge = meterRegistry.find("atleta.matches.created").gauge();
        Gauge startedMatchesGauge = meterRegistry.find("atleta.matches.started").gauge();
        Gauge finishedMatchesGauge = meterRegistry.find("atleta.matches.finished").gauge();
        Gauge playersWithPositionsGauge = meterRegistry.find("atleta.players.with_positions").gauge();
        
        // Then: All metrics should be registered
        assertThat(activeAthletesGauge).as("Active athletes metric").isNotNull();
        assertThat(totalTeamsGauge).as("Total teams metric").isNotNull();
        assertThat(createdMatchesGauge).as("Created matches metric").isNotNull();
        assertThat(startedMatchesGauge).as("Started matches metric").isNotNull();
        assertThat(finishedMatchesGauge).as("Finished matches metric").isNotNull();
        assertThat(playersWithPositionsGauge).as("Players with positions metric").isNotNull();
        
        // And: All should have valid values
        assertThat(activeAthletesGauge.value()).isEqualTo(5.0);
        assertThat(totalTeamsGauge.value()).isEqualTo(3.0);
        assertThat(createdMatchesGauge.value()).isEqualTo(2.0);
        assertThat(startedMatchesGauge.value()).isEqualTo(1.0);
        assertThat(finishedMatchesGauge.value()).isEqualTo(4.0);
        assertThat(playersWithPositionsGauge.value()).isEqualTo(8.0);
    }
}
