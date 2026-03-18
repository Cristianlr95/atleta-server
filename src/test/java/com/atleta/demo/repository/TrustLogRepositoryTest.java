package com.atleta.demo.repository;

import com.atleta.demo.entity.*;
import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para TrustLogRepository.
 * Verifica operaciones CRUD y consultas personalizadas.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TrustLogRepositoryTest {

    @Autowired
    private TrustLogRepository trustLogRepository;

    @Autowired
    private AthleteRepository athleteRepository;

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PlayerProfile testPlayer1;
    private PlayerProfile testPlayer2;
    private Match testMatch1;
    private Match testMatch2;
    private TrustLog testLog1;
    private TrustLog testLog2;
    private TrustLog testLog3;
    private TrustLog testLog4;

    @BeforeEach
    void setUp() {
        cleanupData();

        // Crear atletas y perfiles
        Athlete athlete1 = new Athlete("player1@example.com", "hash1", "Player One");
        Athlete athlete2 = new Athlete("player2@example.com", "hash2", "Player Two");
        
        athlete1 = athleteRepository.save(athlete1);
        athlete2 = athleteRepository.save(athlete2);

        testPlayer1 = new PlayerProfile(athlete1, "Player1");
        testPlayer1.setTrustScore(120);
        testPlayer2 = new PlayerProfile(athlete2, "Player2");
        testPlayer2.setTrustScore(80);
        
        testPlayer1 = playerProfileRepository.save(testPlayer1);
        testPlayer2 = playerProfileRepository.save(testPlayer2);

        // Crear partidos
        testMatch1 = new Match(MatchMode.CINCO_VS_CINCO, LocalDateTime.now().minusDays(1), testPlayer1);
        testMatch1.setEstado(MatchStatus.FINALIZADO);
        
        testMatch2 = new Match(MatchMode.SEIS_VS_SEIS, LocalDateTime.now().minusDays(2), testPlayer2);
        testMatch2.setEstado(MatchStatus.FINALIZADO);
        
        testMatch1 = matchRepository.save(testMatch1);
        testMatch2 = matchRepository.save(testMatch2);

        // Crear logs de confianza
        testLog1 = new TrustLog();
        testLog1.setPlayer(testPlayer1);
        testLog1.setMatch(testMatch1);
        testLog1.setCambio(20);
        testLog1.setTrustScoreAnterior(100);
        testLog1.setTrustScoreNuevo(120);
        testLog1.setMotivo("Excelente desempeño en el partido");

        testLog2 = new TrustLog();
        testLog2.setPlayer(testPlayer1);
        testLog2.setMatch(null); // Cambio no relacionado con partido
        testLog2.setCambio(-10);
        testLog2.setTrustScoreAnterior(120);
        testLog2.setTrustScoreNuevo(110);
        testLog2.setMotivo("Llegada tardía a entrenamiento");

        testLog3 = new TrustLog();
        testLog3.setPlayer(testPlayer2);
        testLog3.setMatch(testMatch2);
        testLog3.setCambio(-20);
        testLog3.setTrustScoreAnterior(100);
        testLog3.setTrustScoreNuevo(80);
        testLog3.setMotivo("Comportamiento antideportivo");

        testLog4 = new TrustLog();
        testLog4.setPlayer(testPlayer2);
        testLog4.setMatch(null);
        testLog4.setCambio(15);
        testLog4.setTrustScoreAnterior(80);
        testLog4.setTrustScoreNuevo(95);
        testLog4.setMotivo("Participación en evento comunitario");

        testLog1 = trustLogRepository.save(testLog1);
        testLog2 = trustLogRepository.save(testLog2);
        testLog3 = trustLogRepository.save(testLog3);
        testLog4 = trustLogRepository.save(testLog4);
    }

    @Test
    void testFindByPlayerOrderByCreatedAtDesc_ValidPlayer_ReturnsPlayerLogsOrderedByDate() {
        // When
        List<TrustLog> results = trustLogRepository.findByPlayerOrderByCreatedAtDesc(testPlayer1);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getCreatedAt()).isAfterOrEqualTo(results.get(1).getCreatedAt());
        assertThat(results).extracting(TrustLog::getCambio)
                .containsExactlyInAnyOrder(-10, 20);
    }

    @Test
    void testFindByMatch_ValidMatch_ReturnsMatchRelatedLogs() {
        // When
        List<TrustLog> results = trustLogRepository.findByMatch(testMatch1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPlayer()).isEqualTo(testPlayer1);
        assertThat(results.get(0).getCambio()).isEqualTo(20);
    }

    @Test
    void testFindByPlayerWithMatchOrderByCreatedAtDesc_ValidPlayer_ReturnsOnlyMatchRelatedLogs() {
        // When
        List<TrustLog> results = trustLogRepository.findByPlayerWithMatchOrderByCreatedAtDesc(testPlayer1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMatch()).isNotNull();
        assertThat(results.get(0).getCambio()).isEqualTo(20);
    }

    @Test
    void testFindByPlayerWithoutMatchOrderByCreatedAtDesc_ValidPlayer_ReturnsOnlyNonMatchRelatedLogs() {
        // When
        List<TrustLog> results = trustLogRepository.findByPlayerWithoutMatchOrderByCreatedAtDesc(testPlayer1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMatch()).isNull();
        assertThat(results.get(0).getCambio()).isEqualTo(-10);
    }

    @Test
    void testFindPositiveChangesByPlayer_ValidPlayer_ReturnsOnlyPositiveChanges() {
        // When
        List<TrustLog> results = trustLogRepository.findPositiveChangesByPlayer(testPlayer1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCambio()).isEqualTo(20);
        assertThat(results.get(0).getCambio()).isGreaterThan(0);
    }

    @Test
    void testFindNegativeChangesByPlayer_ValidPlayer_ReturnsOnlyNegativeChanges() {
        // When
        List<TrustLog> results = trustLogRepository.findNegativeChangesByPlayer(testPlayer1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCambio()).isEqualTo(-10);
        assertThat(results.get(0).getCambio()).isLessThan(0);
    }

    @Test
    void testFindByMotivoContainingIgnoreCase_PartialMotivo_ReturnsMatchingLogs() {
        // When
        List<TrustLog> results = trustLogRepository.findByMotivoContainingIgnoreCase("desempeño");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMotivo()).contains("desempeño");
    }

    @Test
    void testFindByMotivoContainingIgnoreCase_CaseInsensitive_ReturnsMatchingLogs() {
        // When
        List<TrustLog> results = trustLogRepository.findByMotivoContainingIgnoreCase("COMPORTAMIENTO");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMotivo()).containsIgnoringCase("comportamiento");
    }

    @Test
    void testFindByCreatedAtBetween_ValidRange_ReturnsLogsInRange() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(3);
        LocalDateTime end = LocalDateTime.now();

        // When
        List<TrustLog> results = trustLogRepository.findByCreatedAtBetween(start, end);

        // Then
        assertThat(results).hasSize(4);
    }

    @Test
    void testGetTotalChangesByPlayer_ValidPlayer_ReturnsCorrectSum() {
        // When
        Long totalChanges = trustLogRepository.getTotalChangesByPlayer(testPlayer1);

        // Then
        assertThat(totalChanges).isEqualTo(10); // 20 + (-10) = 10
    }

    @Test
    void testCountPositiveChangesByPlayer_ValidPlayer_ReturnsCorrectCount() {
        // When
        long positiveCount = trustLogRepository.countPositiveChangesByPlayer(testPlayer2);

        // Then
        assertThat(positiveCount).isEqualTo(1);
    }

    @Test
    void testCountNegativeChangesByPlayer_ValidPlayer_ReturnsCorrectCount() {
        // When
        long negativeCount = trustLogRepository.countNegativeChangesByPlayer(testPlayer2);

        // Then
        assertThat(negativeCount).isEqualTo(1);
    }

    @Test
    void testFindLatestChangeByPlayer_ValidPlayer_ReturnsLatestChange() {
        // When
        List<TrustLog> results = trustLogRepository.findLatestChangeByPlayer(testPlayer1);

        // Then
        assertThat(results).hasSize(2); // Returns all logs ordered by date
        // The first one should be the most recent
        assertThat(results.get(0).getCreatedAt()).isAfterOrEqualTo(results.get(1).getCreatedAt());
    }

    @Test
    void testFindByPlayerAtletaUuidOrderByCreatedAtDesc_ValidUuid_ReturnsPlayerLogs() {
        // When
        List<TrustLog> results = trustLogRepository.findByPlayerAtletaUuidOrderByCreatedAtDesc(testPlayer1.getAtletaUuid());

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(log -> log.getPlayer().getAtletaUuid().equals(testPlayer1.getAtletaUuid()));
    }

    @Test
    void testFindByChangeGreaterThanEqual_ValidThreshold_ReturnsMatchingLogs() {
        // When
        List<TrustLog> results = trustLogRepository.findByChangeGreaterThanEqual(15);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(TrustLog::getCambio)
                .containsExactlyInAnyOrder(20, 15);
    }

    @Test
    void testFindByChangeLessThanEqual_ValidThreshold_ReturnsMatchingLogs() {
        // When
        List<TrustLog> results = trustLogRepository.findByChangeLessThanEqual(-10);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(TrustLog::getCambio)
                .containsExactlyInAnyOrder(-10, -20);
    }

    @Test
    void testGetTrustChangeStatsByPlayer_ValidPlayer_ReturnsCompleteStats() {
        // When
        Object[] stats = trustLogRepository.getTrustChangeStatsByPlayer(testPlayer1);
        Object[] values = stats.length == 1 && stats[0] instanceof Object[] nestedStats
                ? nestedStats
                : stats;

        // Then
        assertThat(values).hasSize(4);
        assertThat(values[0]).isEqualTo(2L); // total_cambios
        assertThat(values[1]).isEqualTo(1L); // cambios_positivos
        assertThat(values[2]).isEqualTo(1L); // cambios_negativos
        assertThat(values[3]).isEqualTo(10L); // suma_total (20 + (-10) = 10)
    }

    @Test
    void testFindPlayersOrderByTrustLogCount_ReturnsPlayersOrderedByLogCount() {
        // When
        List<PlayerProfile> players = trustLogRepository.findPlayersOrderByTrustLogCount();

        // Then
        assertThat(players).hasSize(2);
        // Both players have 2 logs each, so order might vary
        assertThat(players).containsExactlyInAnyOrder(testPlayer1, testPlayer2);
    }

    @Test
    void testFindByMatchAndPlayer_ValidMatchAndPlayer_ReturnsSpecificLogs() {
        // When
        List<TrustLog> results = trustLogRepository.findByMatchAndPlayer(testMatch1, testPlayer1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCambio()).isEqualTo(20);
        assertThat(results.get(0).getMotivo()).contains("Excelente desempeño");
    }

    @Test
    void testFindMostSignificantChanges_ReturnsLogsOrderedByAbsoluteValue() {
        // When
        List<TrustLog> results = trustLogRepository.findMostSignificantChanges();

        // Then
        assertThat(results).hasSize(4);
        // Should be ordered by absolute value: 20, -20, 15, -10
        assertThat(Math.abs(results.get(0).getCambio())).isGreaterThanOrEqualTo(Math.abs(results.get(1).getCambio()));
    }

    @Test
    void testFindRecentLogs_RecentDate_ReturnsRecentLogs() {
        // Given
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);

        // When
        List<TrustLog> results = trustLogRepository.findRecentLogs(twentyFourHoursAgo);

        // Then
        assertThat(results).hasSize(4); // All logs should be recent
    }

    @Test
    void testSaveAndFindById_ValidLog_SavesAndRetrievesCorrectly() {
        // Given
        TrustLog newLog = new TrustLog();
        newLog.setPlayer(testPlayer1);
        newLog.setMatch(testMatch2);
        newLog.setCambio(25);
        newLog.setTrustScoreAnterior(110);
        newLog.setTrustScoreNuevo(135);
        newLog.setMotivo("Liderazgo excepcional");

        // When
        TrustLog saved = trustLogRepository.save(newLog);
        Optional<TrustLog> found = trustLogRepository.findById(saved.getId());

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getCambio()).isEqualTo(25);
        assertThat(found.get().getMotivo()).isEqualTo("Liderazgo excepcional");
    }

    @Test
    void testDeleteById_ExistingLog_DeletesSuccessfully() {
        // Given
        Long logId = testLog1.getId();

        // When
        trustLogRepository.deleteById(logId);

        // Then
        Optional<TrustLog> found = trustLogRepository.findById(logId);
        assertThat(found).isEmpty();
        assertThat(trustLogRepository.count()).isEqualTo(3);
    }

    @Test
    void testFindAll_ReturnsAllLogs() {
        // When
        List<TrustLog> results = trustLogRepository.findAll();

        // Then
        assertThat(results).hasSize(4);
    }

    private void cleanupData() {
        String[] statements = {
                "DELETE FROM trust_logs",
                "DELETE FROM player_history",
                "DELETE FROM match_players",
                "DELETE FROM match_events",
                "DELETE FROM match_teams",
                "DELETE FROM matches",
                "DELETE FROM team_members",
                "DELETE FROM team_stats",
                "DELETE FROM teams",
                "DELETE FROM player_positions",
                "DELETE FROM player_profiles",
                "DELETE FROM athletes"
        };

        for (String statement : statements) {
            jdbcTemplate.execute(statement);
        }
    }
}
