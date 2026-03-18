package com.atleta.demo.repository;

import com.atleta.demo.entity.*;
import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchResult;
import com.atleta.demo.enums.MatchStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para PlayerHistoryRepository.
 * Verifica operaciones CRUD y consultas personalizadas.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlayerHistoryRepositoryTest {

    @Autowired
    private PlayerHistoryRepository playerHistoryRepository;

    @Autowired
    private AthleteRepository athleteRepository;

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PositionRepository positionRepository;

    private PlayerProfile testPlayer1;
    private PlayerProfile testPlayer2;
    private Team testTeam1;
    private Team testTeam2;
    private Match testMatch1;
    private Match testMatch2;
    private Position testPosition;
    private PlayerHistory testHistory1;
    private PlayerHistory testHistory2;
    private PlayerHistory testHistory3;

    @BeforeEach
    void setUp() {
        // Limpiar datos existentes
        playerHistoryRepository.deleteAll();
        matchRepository.deleteAll();
        teamRepository.deleteAll();
        playerProfileRepository.deleteAll();
        athleteRepository.deleteAll();

        // Crear atletas y perfiles
        Athlete athlete1 = new Athlete("player1@example.com", "hash1", "Player One");
        Athlete athlete2 = new Athlete("player2@example.com", "hash2", "Player Two");
        
        athlete1 = athleteRepository.save(athlete1);
        athlete2 = athleteRepository.save(athlete2);

        testPlayer1 = new PlayerProfile(athlete1, "Player1");
        testPlayer2 = new PlayerProfile(athlete2, "Player2");
        
        testPlayer1 = playerProfileRepository.save(testPlayer1);
        testPlayer2 = playerProfileRepository.save(testPlayer2);

        // Crear equipos
        testTeam1 = new Team("Team Alpha", testPlayer1);
        testTeam2 = new Team("Team Beta", testPlayer2);
        
        testTeam1 = teamRepository.save(testTeam1);
        testTeam2 = teamRepository.save(testTeam2);

        // Crear posición
        testPosition = new Position();
        testPosition.setNombre("Delantero");
        testPosition = positionRepository.save(testPosition);

        // Crear partidos
        testMatch1 = new Match(MatchMode.CINCO_VS_CINCO, LocalDateTime.now().minusDays(1), testPlayer1);
        testMatch1.setEstado(MatchStatus.FINALIZADO);
        
        testMatch2 = new Match(MatchMode.SEIS_VS_SEIS, LocalDateTime.now().minusDays(2), testPlayer2);
        testMatch2.setEstado(MatchStatus.FINALIZADO);
        
        testMatch1 = matchRepository.save(testMatch1);
        testMatch2 = matchRepository.save(testMatch2);

        // Crear historial de jugadores
        testHistory1 = new PlayerHistory(testMatch1, testPlayer1, testTeam1, testPosition, 2, 1, MatchResult.VICTORIA, 50);
        testHistory2 = new PlayerHistory(testMatch1, testPlayer2, testTeam2, testPosition, 1, 0, MatchResult.DERROTA, 20);
        testHistory3 = new PlayerHistory(testMatch2, testPlayer1, testTeam1, testPosition, 0, 2, MatchResult.EMPATE, 30);

        testHistory1 = playerHistoryRepository.save(testHistory1);
        testHistory2 = playerHistoryRepository.save(testHistory2);
        testHistory3 = playerHistoryRepository.save(testHistory3);
    }

    @Test
    void testFindByPlayerOrderByCreatedAtDesc_ValidPlayer_ReturnsPlayerHistory() {
        // When
        List<PlayerHistory> results = playerHistoryRepository.findByPlayerOrderByCreatedAtDesc(testPlayer1);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getCreatedAt()).isAfter(results.get(1).getCreatedAt());
    }

    @Test
    void testFindByMatch_ValidMatch_ReturnsMatchHistory() {
        // When
        List<PlayerHistory> results = playerHistoryRepository.findByMatch(testMatch1);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(PlayerHistory::getPlayer)
                .containsExactlyInAnyOrder(testPlayer1, testPlayer2);
    }

    @Test
    void testFindByMatchAndPlayer_ValidMatchAndPlayer_ReturnsSpecificHistory() {
        // When
        Optional<PlayerHistory> result = playerHistoryRepository.findByMatchAndPlayer(testMatch1, testPlayer1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getGoles()).isEqualTo(2);
        assertThat(result.get().getAsistencias()).isEqualTo(1);
    }

    @Test
    void testFindByPlayerAndTeamOrderByCreatedAtDesc_ValidPlayerAndTeam_ReturnsHistory() {
        // When
        List<PlayerHistory> results = playerHistoryRepository.findByPlayerAndTeamOrderByCreatedAtDesc(testPlayer1, testTeam1);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(h -> h.getPlayer().equals(testPlayer1));
        assertThat(results).allMatch(h -> h.getTeam().equals(testTeam1));
    }

    @Test
    void testFindByPlayerAndResultado_ValidPlayerAndResult_ReturnsMatchingHistory() {
        // When
        List<PlayerHistory> results = playerHistoryRepository.findByPlayerAndResultado(testPlayer1, MatchResult.VICTORIA);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getResultado()).isEqualTo(MatchResult.VICTORIA);
        assertThat(results.get(0).getGoles()).isEqualTo(2);
    }

    @Test
    void testFindByPlayerAndPosition_ValidPlayerAndPosition_ReturnsHistory() {
        // When
        List<PlayerHistory> results = playerHistoryRepository.findByPlayerAndPosition(testPlayer1, testPosition);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(h -> h.getPosition().equals(testPosition));
    }

    @Test
    void testCountVictoriesByPlayer_ValidPlayer_ReturnsCorrectCount() {
        // When
        long victories = playerHistoryRepository.countVictoriesByPlayer(testPlayer1);

        // Then
        assertThat(victories).isEqualTo(1);
    }

    @Test
    void testCountDefeatsByPlayer_ValidPlayer_ReturnsCorrectCount() {
        // When
        long defeats = playerHistoryRepository.countDefeatsByPlayer(testPlayer2);

        // Then
        assertThat(defeats).isEqualTo(1);
    }

    @Test
    void testCountDrawsByPlayer_ValidPlayer_ReturnsCorrectCount() {
        // When
        long draws = playerHistoryRepository.countDrawsByPlayer(testPlayer1);

        // Then
        assertThat(draws).isEqualTo(1);
    }

    @Test
    void testGetTotalGoalsByPlayer_ValidPlayer_ReturnsCorrectTotal() {
        // When
        Long totalGoals = playerHistoryRepository.getTotalGoalsByPlayer(testPlayer1);

        // Then
        assertThat(totalGoals).isEqualTo(2); // 2 + 0 = 2
    }

    @Test
    void testGetTotalAssistsByPlayer_ValidPlayer_ReturnsCorrectTotal() {
        // When
        Long totalAssists = playerHistoryRepository.getTotalAssistsByPlayer(testPlayer1);

        // Then
        assertThat(totalAssists).isEqualTo(3); // 1 + 2 = 3
    }

    @Test
    void testGetTotalXpByPlayer_ValidPlayer_ReturnsCorrectTotal() {
        // When
        Long totalXp = playerHistoryRepository.getTotalXpByPlayer(testPlayer1);

        // Then
        assertThat(totalXp).isEqualTo(80); // 50 + 30 = 80
    }

    @Test
    void testFindTopScorersByTotalGoals_ReturnsPlayersOrderedByGoals() {
        // When
        List<PlayerProfile> topScorers = playerHistoryRepository.findTopScorersByTotalGoals();

        // Then
        assertThat(topScorers).hasSize(2);
        // testPlayer1 should be first (2 goals total) vs testPlayer2 (1 goal total)
        assertThat(topScorers.get(0)).isEqualTo(testPlayer1);
    }

    @Test
    void testFindTopAssistersByTotalAssists_ReturnsPlayersOrderedByAssists() {
        // When
        List<PlayerProfile> topAssisters = playerHistoryRepository.findTopAssistersByTotalAssists();

        // Then
        assertThat(topAssisters).hasSize(2);
        // testPlayer1 should be first (3 assists total) vs testPlayer2 (0 assists total)
        assertThat(topAssisters.get(0)).isEqualTo(testPlayer1);
    }

    @Test
    void testFindMostActivePlayersByMatchCount_ReturnsPlayersOrderedByMatches() {
        // When
        List<PlayerProfile> mostActive = playerHistoryRepository.findMostActivePlayersByMatchCount();

        // Then
        assertThat(mostActive).hasSize(2);
        // testPlayer1 should be first (2 matches) vs testPlayer2 (1 match)
        assertThat(mostActive.get(0)).isEqualTo(testPlayer1);
    }

    @Test
    void testFindByCreatedAtBetween_ValidRange_ReturnsHistoryInRange() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(3);
        LocalDateTime end = LocalDateTime.now();

        // When
        List<PlayerHistory> results = playerHistoryRepository.findByCreatedAtBetween(start, end);

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void testFindByPlayerAtletaUuidOrderByCreatedAtDesc_ValidUuid_ReturnsHistory() {
        // When
        List<PlayerHistory> results = playerHistoryRepository.findByPlayerAtletaUuidOrderByCreatedAtDesc(testPlayer1.getAtletaUuid());

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(h -> h.getPlayer().getAtletaUuid().equals(testPlayer1.getAtletaUuid()));
    }

    @Test
    void testGetCompleteStatsByPlayer_ValidPlayer_ReturnsCompleteStats() {
        // When
        Object[] stats = playerHistoryRepository.getCompleteStatsByPlayer(testPlayer1);

        // Then
        assertThat(stats).hasSize(7);
        assertThat(stats[0]).isEqualTo(2L); // partidos
        assertThat(stats[1]).isEqualTo(1L); // victorias
        assertThat(stats[2]).isEqualTo(0L); // derrotas
        assertThat(stats[3]).isEqualTo(1L); // empates
        assertThat(stats[4]).isEqualTo(2L); // goles
        assertThat(stats[5]).isEqualTo(3L); // asistencias
        assertThat(stats[6]).isEqualTo(80L); // xp total
    }

    @Test
    void testFindMatchesWithGoalsByPlayer_ValidPlayer_ReturnsMatchesWithGoals() {
        // When
        List<PlayerHistory> results = playerHistoryRepository.findMatchesWithGoalsByPlayer(testPlayer1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getGoles()).isGreaterThan(0);
    }

    @Test
    void testFindMatchesWithAssistsByPlayer_ValidPlayer_ReturnsMatchesWithAssists() {
        // When
        List<PlayerHistory> results = playerHistoryRepository.findMatchesWithAssistsByPlayer(testPlayer1);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(h -> h.getAsistencias() > 0);
    }

    @Test
    void testFindBestMatchesByPlayer_ValidPlayer_ReturnsMatchesOrderedByPerformance() {
        // When
        List<PlayerHistory> results = playerHistoryRepository.findBestMatchesByPlayer(testPlayer1);

        // Then
        assertThat(results).hasSize(2);
        // First match should have higher goals + assists (2+1=3 vs 0+2=2)
        assertThat(results.get(0).getGoles() + results.get(0).getAsistencias())
                .isGreaterThanOrEqualTo(results.get(1).getGoles() + results.get(1).getAsistencias());
    }

    @Test
    void testGetWinPercentageByPlayer_ValidPlayer_ReturnsCorrectPercentage() {
        // When
        Double winPercentage = playerHistoryRepository.getWinPercentageByPlayer(testPlayer1);

        // Then
        assertThat(winPercentage).isEqualTo(0.5); // 1 victory out of 2 matches = 50%
    }

    @Test
    void testSaveAndFindById_ValidHistory_SavesAndRetrievesCorrectly() {
        // Given
        PlayerHistory newHistory = new PlayerHistory(testMatch2, testPlayer2, testTeam2, testPosition, 3, 1, MatchResult.VICTORIA, 60);

        // When
        PlayerHistory saved = playerHistoryRepository.save(newHistory);
        Optional<PlayerHistory> found = playerHistoryRepository.findById(saved.getId());

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getGoles()).isEqualTo(3);
        assertThat(found.get().getAsistencias()).isEqualTo(1);
        assertThat(found.get().getResultado()).isEqualTo(MatchResult.VICTORIA);
    }

    @Test
    void testFindAll_ReturnsAllHistory() {
        // When
        List<PlayerHistory> results = playerHistoryRepository.findAll();

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void testDeleteById_ExistingHistory_DeletesSuccessfully() {
        // Given
        Long historyId = testHistory1.getId();

        // When
        playerHistoryRepository.deleteById(historyId);

        // Then
        Optional<PlayerHistory> found = playerHistoryRepository.findById(historyId);
        assertThat(found).isEmpty();
        assertThat(playerHistoryRepository.count()).isEqualTo(2);
    }
}