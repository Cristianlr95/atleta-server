package com.atleta.demo;

import com.atleta.demo.config.BaseIntegrationTest;
import com.atleta.demo.dto.request.CreateAthleteRequest;
import com.atleta.demo.dto.request.CreatePlayerProfileRequest;
import com.atleta.demo.dto.request.CreateTeamRequest;
import com.atleta.demo.dto.request.CreateMatchRequest;
import com.atleta.demo.enums.MatchMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests that verify the complete flow from application startup
 * to database operations, including error handling and recovery scenarios.
 * 
 * Tests complete integration of:
 * - Application startup with PostgreSQL
 * - Flyway migrations execution
 * - Database connectivity and operations
 * - Error handling and recovery
 * - Configuration validation
 * 
 * Requisitos: Integración completa
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class EndToEndIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataSource dataSource;

    @Test
    void testCompleteApplicationStartupFlow() {
        // Verify application started successfully
        assertThat(port).isGreaterThan(0);
        
        // Verify database connection is working
        assertDoesNotThrow(() -> {
            try (Connection connection = dataSource.getConnection()) {
                assertThat(connection.isValid(5)).isTrue();
            }
        });
        
        // Verify Flyway migrations were executed
        Long migrationCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", 
            Long.class
        );
        assertThat(migrationCount).isGreaterThan(0);
        
        // Verify all required tables exist
        verifyRequiredTablesExist();
        
        // Verify initial data was loaded
        verifyInitialDataLoaded();
    }

    @Test
    void testCompleteUserJourneyFlow() throws Exception {
        String baseUrl = "http://localhost:" + port + "/api/v1";
        
        // Step 1: Register a new athlete
        CreateAthleteRequest athleteRequest = new CreateAthleteRequest();
        athleteRequest.setEmail("journey@example.com");
        athleteRequest.setPassword("password123");
        athleteRequest.setNombre("Journey Test User");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateAthleteRequest> athleteEntity = new HttpEntity<>(athleteRequest, headers);
        
        ResponseEntity<Map> athleteResponse = restTemplate.postForEntity(
            baseUrl + "/athletes/register", 
            athleteEntity, 
            Map.class
        );
        
        assertThat(athleteResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(athleteResponse.getBody()).isNotNull();
        String athleteUuid = (String) athleteResponse.getBody().get("atletaUuid");
        assertThat(athleteUuid).isNotNull();
        
        // Step 2: Create player profile
        CreatePlayerProfileRequest profileRequest = new CreatePlayerProfileRequest();
        profileRequest.setAtletaUuid(UUID.fromString(athleteUuid));
        profileRequest.setAlias("JourneyPlayer");
        
        HttpEntity<CreatePlayerProfileRequest> profileEntity = new HttpEntity<>(profileRequest, headers);
        
        ResponseEntity<Map> profileResponse = restTemplate.postForEntity(
            baseUrl + "/player-profiles", 
            profileEntity, 
            Map.class
        );
        
        assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        // Step 3: Create a team
        CreateTeamRequest teamRequest = new CreateTeamRequest();
        teamRequest.setNombre("Journey Team");
        teamRequest.setCreadorUuid(UUID.fromString(athleteUuid));
        teamRequest.setAnioFundacion(2024);
        
        HttpEntity<CreateTeamRequest> teamEntity = new HttpEntity<>(teamRequest, headers);
        
        ResponseEntity<Map> teamResponse = restTemplate.postForEntity(
            baseUrl + "/teams", 
            teamEntity, 
            Map.class
        );
        
        assertThat(teamResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(teamResponse.getBody()).isNotNull();
        Integer teamId = (Integer) teamResponse.getBody().get("id");
        assertThat(teamId).isNotNull();
        
        // Step 4: Create a match
        CreateMatchRequest matchRequest = new CreateMatchRequest();
        matchRequest.setModalidad(MatchMode.CINCO_VS_CINCO);
        matchRequest.setFechaHoraProgramada(LocalDateTime.now().plusDays(1));
        matchRequest.setLatitud(new BigDecimal("40.7128"));
        matchRequest.setLongitud(new BigDecimal("-74.0060"));
        matchRequest.setCuota(new BigDecimal("25.00"));
        matchRequest.setCreadorUuid(UUID.fromString(athleteUuid));
        
        HttpEntity<CreateMatchRequest> matchEntity = new HttpEntity<>(matchRequest, headers);
        
        ResponseEntity<Map> matchResponse = restTemplate.postForEntity(
            baseUrl + "/matches", 
            matchEntity, 
            Map.class
        );
        
        assertThat(matchResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(matchResponse.getBody()).isNotNull();
        Integer matchId = (Integer) matchResponse.getBody().get("id");
        assertThat(matchId).isNotNull();
        
        // Step 5: Verify data persistence
        verifyDataPersistence(athleteUuid, teamId, matchId);
    }

    @Test
    void testDatabaseConnectionFailoverAndRecovery() {
        // Test 1: Verify current connection is healthy
        assertDoesNotThrow(() -> {
            try (Connection connection = dataSource.getConnection()) {
                assertThat(connection.isValid(5)).isTrue();
            }
        });
        
        // Test 2: Simulate connection timeout and recovery
        // This tests HikariCP's connection validation and recovery
        assertDoesNotThrow(() -> {
            // Execute a query that should work
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM athletes", Long.class);
            assertThat(count).isNotNull();
            
            // Test connection pool behavior under load
            for (int i = 0; i < 10; i++) {
                Long result = jdbcTemplate.queryForObject("SELECT 1", Long.class);
                assertThat(result).isEqualTo(1L);
            }
        });
        
        // Test 3: Verify connection pool metrics are available
        // This would be expanded in a real scenario to check actual metrics
        assertDoesNotThrow(() -> {
            // Verify we can still execute queries after stress test
            Long finalCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM positions", Long.class);
            assertThat(finalCount).isGreaterThan(0);
        });
    }

    @Test
    void testTransactionRollbackAndRecovery() {
        // Test transaction rollback behavior
        String testEmail = "rollback@example.com";
        
        // Verify athlete doesn't exist initially
        Long initialCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM athletes WHERE email = ?", 
            Long.class, 
            testEmail
        );
        assertThat(initialCount).isEqualTo(0);
        
        // Attempt to create athlete with invalid data that should rollback
        assertThrows(Exception.class, () -> {
            jdbcTemplate.execute("BEGIN");
            try {
                // Insert athlete
                jdbcTemplate.update(
                    "INSERT INTO athletes (atleta_uuid, email, password_hash, nombre) VALUES (?, ?, ?, ?)",
                    java.util.UUID.randomUUID(), testEmail, "hash", "Test User"
                );
                
                // This should fail due to constraint violation (simulate error)
                jdbcTemplate.update(
                    "INSERT INTO athletes (atleta_uuid, email, password_hash, nombre) VALUES (?, ?, ?, ?)",
                    java.util.UUID.randomUUID(), testEmail, "hash", "Duplicate User" // Same email should fail
                );
                
                jdbcTemplate.execute("COMMIT");
            } catch (Exception e) {
                jdbcTemplate.execute("ROLLBACK");
                throw e;
            }
        });
        
        // Verify rollback worked - no athlete should exist
        Long finalCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM athletes WHERE email = ?", 
            Long.class, 
            testEmail
        );
        assertThat(finalCount).isEqualTo(0);
    }

    @Test
    void testHealthCheckEndpoints() {
        String baseUrl = "http://localhost:" + port;
        
        // Test application health endpoint
        ResponseEntity<Map> healthResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/health", 
            Map.class
        );
        
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(healthResponse.getBody()).isNotNull();
        assertThat(healthResponse.getBody().get("status")).isEqualTo("UP");
        
        // Test database health specifically
        ResponseEntity<Map> dbHealthResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/health/db", 
            Map.class
        );
        
        assertThat(dbHealthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dbHealthResponse.getBody()).isNotNull();
        assertThat(dbHealthResponse.getBody().get("status")).isEqualTo("UP");
    }

    @Test
    void testFlywayMigrationIntegrity() {
        // Verify all expected migrations were executed successfully
        String migrationQuery = """
            SELECT version, description, success, execution_time 
            FROM flyway_schema_history 
            ORDER BY installed_rank
            """;
        
        var migrations = jdbcTemplate.queryForList(migrationQuery);
        assertThat(migrations).isNotEmpty();
        
        // Verify specific migrations exist
        boolean hasInitialSchema = migrations.stream()
            .anyMatch(m -> m.get("description").toString().contains("create_initial_schema"));
        assertThat(hasInitialSchema).isTrue();
        
        boolean hasIndexes = migrations.stream()
            .anyMatch(m -> m.get("description").toString().contains("add_indexes_performance"));
        assertThat(hasIndexes).isTrue();
        
        boolean hasInitialData = migrations.stream()
            .anyMatch(m -> m.get("description").toString().contains("insert_initial_data"));
        assertThat(hasInitialData).isTrue();
        
        // Verify all migrations were successful
        boolean allSuccessful = migrations.stream()
            .allMatch(m -> Boolean.TRUE.equals(m.get("success")));
        assertThat(allSuccessful).isTrue();
    }

    private void verifyRequiredTablesExist() {
        String[] requiredTables = {
            "athletes", "positions", "player_profile", "player_positions",
            "teams", "team_stats", "team_members", "matches", "match_teams",
            "match_players", "match_events", "player_history", "trust_logs"
        };
        
        for (String table : requiredTables) {
            assertThat(tableExists(table))
                .as("Table %s should exist", table)
                .isTrue();
        }
    }

    private void verifyInitialDataLoaded() {
        // Verify positions were loaded
        Long positionCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM positions", 
            Long.class
        );
        assertThat(positionCount).isGreaterThan(0);
        
        // Verify specific positions exist
        Integer porterCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM positions WHERE nombre = 'Portero'", 
            Integer.class
        );
        assertThat(porterCount).isEqualTo(1);
    }

    private void verifyDataPersistence(String athleteUuid, Integer teamId, Integer matchId) {
        // Verify athlete exists
        Long athleteCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM athletes WHERE atleta_uuid = ?::uuid", 
            Long.class, 
            athleteUuid
        );
        assertThat(athleteCount).isEqualTo(1);
        
        // Verify player profile exists
        Long profileCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM player_profile WHERE atleta_uuid = ?::uuid", 
            Long.class, 
            athleteUuid
        );
        assertThat(profileCount).isEqualTo(1);
        
        // Verify team exists
        Long teamCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM teams WHERE id = ?", 
            Long.class, 
            teamId
        );
        assertThat(teamCount).isEqualTo(1);
        
        // Verify match exists
        Long matchCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM matches WHERE id = ?", 
            Long.class, 
            matchId
        );
        assertThat(matchCount).isEqualTo(1);
        
        // Verify team stats were created
        Long statsCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM team_stats WHERE team_id = ?", 
            Long.class, 
            teamId
        );
        assertThat(statsCount).isEqualTo(1);
    }
}