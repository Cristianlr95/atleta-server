package com.atleta.demo.repository;

import com.atleta.demo.entity.*;
import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchStatus;
import com.atleta.demo.enums.PlayerRole;
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
 * Tests unitarios para MatchPlayerRepository.
 * Verifica operaciones CRUD y consultas personalizadas.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MatchPlayerRepositoryTest {

    @Autowired
    private MatchPlayerRepository matchPlayerRepository;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PlayerProfile testPlayer1;
    private PlayerProfile testPlayer2;
    private PlayerProfile testPlayer3;
    private Team testTeam1;
    private Team testTeam2;
    private Match testMatch1;
    private Match testMatch2;
    private Position testPosition1;
    private Position testPosition2;
    private MatchPlayer testMatchPlayer1;
    private MatchPlayer testMatchPlayer2;
    private MatchPlayer testMatchPlayer3;

    @BeforeEach
    void setUp() {
        cleanupData();

        // Crear atletas y perfiles
        Athlete athlete1 = new Athlete("player1@example.com", "hash1", "Player One");
        Athlete athlete2 = new Athlete("player2@example.com", "hash2", "Player Two");
        Athlete athlete3 = new Athlete("player3@example.com", "hash3", "Player Three");
        
        athlete1 = athleteRepository.save(athlete1);
        athlete2 = athleteRepository.save(athlete2);
        athlete3 = athleteRepository.save(athlete3);

        testPlayer1 = new PlayerProfile(athlete1, "Player1");
        testPlayer2 = new PlayerProfile(athlete2, "Player2");
        testPlayer3 = new PlayerProfile(athlete3, "Player3");
        
        testPlayer1 = playerProfileRepository.save(testPlayer1);
        testPlayer2 = playerProfileRepository.save(testPlayer2);
        testPlayer3 = playerProfileRepository.save(testPlayer3);

        // Crear posiciones
        testPosition1 = findOrCreatePosition("Delantero");
        testPosition2 = findOrCreatePosition("Mediocampista");

        // Crear equipos
        testTeam1 = new Team("Team Alpha", testPlayer1);
        testTeam2 = new Team("Team Beta", testPlayer2);
        
        testTeam1 = teamRepository.save(testTeam1);
        testTeam2 = teamRepository.save(testTeam2);

        // Crear partidos
        testMatch1 = new Match(MatchMode.CINCO_VS_CINCO, LocalDateTime.now().plusDays(1), testPlayer1);
        testMatch1.setEstado(MatchStatus.CREADO);
        
        testMatch2 = new Match(MatchMode.SEIS_VS_SEIS, LocalDateTime.now().plusDays(2), testPlayer2);
        testMatch2.setEstado(MatchStatus.INICIADO);
        
        testMatch1 = matchRepository.save(testMatch1);
        testMatch2 = matchRepository.save(testMatch2);

        // Crear participaciones de jugadores en partidos
        testMatchPlayer1 = new MatchPlayer();
        testMatchPlayer1.setMatch(testMatch1);
        testMatchPlayer1.setPlayer(testPlayer1);
        testMatchPlayer1.setTeam(testTeam1);
        testMatchPlayer1.setPosition(testPosition1);
        testMatchPlayer1.setRol(PlayerRole.CAPITAN);
        testMatchPlayer1.setConfirmado(true);

        testMatchPlayer2 = new MatchPlayer();
        testMatchPlayer2.setMatch(testMatch1);
        testMatchPlayer2.setPlayer(testPlayer2);
        testMatchPlayer2.setTeam(testTeam2);
        testMatchPlayer2.setPosition(testPosition2);
        testMatchPlayer2.setRol(PlayerRole.JUGADOR);
        testMatchPlayer2.setConfirmado(false);

        testMatchPlayer3 = new MatchPlayer();
        testMatchPlayer3.setMatch(testMatch2);
        testMatchPlayer3.setPlayer(testPlayer3);
        testMatchPlayer3.setTeam(testTeam1);
        testMatchPlayer3.setPosition(testPosition1);
        testMatchPlayer3.setRol(PlayerRole.DT);
        testMatchPlayer3.setConfirmado(true);

        testMatchPlayer1 = matchPlayerRepository.save(testMatchPlayer1);
        testMatchPlayer2 = matchPlayerRepository.save(testMatchPlayer2);
        testMatchPlayer3 = matchPlayerRepository.save(testMatchPlayer3);
    }

    @Test
    void testFindByMatch_ValidMatch_ReturnsMatchPlayers() {
        // When
        List<MatchPlayer> results = matchPlayerRepository.findByMatch(testMatch1);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(mp -> mp.getPlayer().getAlias())
                .containsExactlyInAnyOrder("Player1", "Player2");
    }

    @Test
    void testFindByPlayer_ValidPlayer_ReturnsPlayerMatches() {
        // When
        List<MatchPlayer> results = matchPlayerRepository.findByPlayer(testPlayer1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMatch()).isEqualTo(testMatch1);
        assertThat(results.get(0).getRol()).isEqualTo(PlayerRole.CAPITAN);
    }

    @Test
    void testFindByMatchAndTeam_ValidMatchAndTeam_ReturnsTeamPlayers() {
        // When
        List<MatchPlayer> results = matchPlayerRepository.findByMatchAndTeam(testMatch1, testTeam1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPlayer()).isEqualTo(testPlayer1);
    }

    @Test
    void testFindByMatchAndPlayer_ValidMatchAndPlayer_ReturnsSpecificParticipation() {
        // When
        Optional<MatchPlayer> result = matchPlayerRepository.findByMatchAndPlayer(testMatch1, testPlayer1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getRol()).isEqualTo(PlayerRole.CAPITAN);
        assertThat(result.get().getPosition()).isEqualTo(testPosition1);
    }

    @Test
    void testFindConfirmedPlayersByMatch_ValidMatch_ReturnsOnlyConfirmedPlayers() {
        // When
        List<MatchPlayer> results = matchPlayerRepository.findConfirmedPlayersByMatch(testMatch1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPlayer()).isEqualTo(testPlayer1);
        assertThat(results.get(0).isConfirmado()).isTrue();
    }

    @Test
    void testFindUnconfirmedPlayersByMatch_ValidMatch_ReturnsOnlyUnconfirmedPlayers() {
        // When
        List<MatchPlayer> results = matchPlayerRepository.findUnconfirmedPlayersByMatch(testMatch1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPlayer()).isEqualTo(testPlayer2);
        assertThat(results.get(0).isConfirmado()).isFalse();
    }

    @Test
    void testFindByMatchAndPosition_ValidMatchAndPosition_ReturnsPlayersInPosition() {
        // When
        List<MatchPlayer> results = matchPlayerRepository.findByMatchAndPosition(testMatch1, testPosition1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPlayer()).isEqualTo(testPlayer1);
    }

    @Test
    void testFindByMatchAndRol_ValidMatchAndRole_ReturnsPlayersWithRole() {
        // When
        List<MatchPlayer> results = matchPlayerRepository.findByMatchAndRol(testMatch1, PlayerRole.JUGADOR);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPlayer()).isEqualTo(testPlayer2);
    }

    @Test
    void testFindCaptainsByMatch_ValidMatch_ReturnsCaptains() {
        // When
        List<MatchPlayer> results = matchPlayerRepository.findCaptainsByMatch(testMatch1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPlayer()).isEqualTo(testPlayer1);
        assertThat(results.get(0).getRol()).isEqualTo(PlayerRole.CAPITAN);
    }

    @Test
    void testFindCoachesByMatch_ValidMatch_ReturnsCoaches() {
        // When
        List<MatchPlayer> results = matchPlayerRepository.findCoachesByMatch(testMatch2);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPlayer()).isEqualTo(testPlayer3);
        assertThat(results.get(0).getRol()).isEqualTo(PlayerRole.DT);
    }

    @Test
    void testExistsByMatchAndPlayer_ExistingParticipation_ReturnsTrue() {
        // When
        boolean exists = matchPlayerRepository.existsByMatchAndPlayer(testMatch1, testPlayer1);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByMatchAndPlayer_NonExistingParticipation_ReturnsFalse() {
        // When
        boolean exists = matchPlayerRepository.existsByMatchAndPlayer(testMatch2, testPlayer1);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void testIsPlayerConfirmedForMatch_ConfirmedPlayer_ReturnsTrue() {
        // When
        boolean isConfirmed = matchPlayerRepository.isPlayerConfirmedForMatch(testMatch1, testPlayer1);

        // Then
        assertThat(isConfirmed).isTrue();
    }

    @Test
    void testIsPlayerConfirmedForMatch_UnconfirmedPlayer_ReturnsFalse() {
        // When
        boolean isConfirmed = matchPlayerRepository.isPlayerConfirmedForMatch(testMatch1, testPlayer2);

        // Then
        assertThat(isConfirmed).isFalse();
    }

    @Test
    void testCountConfirmedPlayersByMatch_ValidMatch_ReturnsCorrectCount() {
        // When
        long count = matchPlayerRepository.countConfirmedPlayersByMatch(testMatch1);

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testCountByMatchAndTeam_ValidMatchAndTeam_ReturnsCorrectCount() {
        // When
        long count = matchPlayerRepository.countByMatchAndTeam(testMatch1, testTeam1);

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testFindByPlayerAndPosition_ValidPlayerAndPosition_ReturnsParticipations() {
        // When
        List<MatchPlayer> results = matchPlayerRepository.findByPlayerAndPosition(testPlayer1, testPosition1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMatch()).isEqualTo(testMatch1);
    }

    @Test
    void testFindMostActivePlayersOrderByMatchCount_ReturnsPlayersOrderedByActivity() {
        // When
        List<PlayerProfile> results = matchPlayerRepository.findMostActivePlayersOrderByMatchCount();

        // Then
        assertThat(results).hasSize(3);
        // All players have 1 match each, so order might vary
        assertThat(results).containsExactlyInAnyOrder(testPlayer1, testPlayer2, testPlayer3);
    }

    @Test
    void testCountByPlayer_ValidPlayer_ReturnsCorrectCount() {
        // When
        long count = matchPlayerRepository.countByPlayer(testPlayer1);

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testFindByMatchAndPlayerAtletaUuid_ValidMatchAndUuid_ReturnsParticipation() {
        // When
        Optional<MatchPlayer> result = matchPlayerRepository.findByMatchAndPlayerAtletaUuid(testMatch1, testPlayer1.getAtletaUuid());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getPlayer()).isEqualTo(testPlayer1);
    }

    @Test
    void testFindMatchesWhereCaptain_ValidPlayer_ReturnsMatchesAsCaptain() {
        // When
        List<MatchPlayer> results = matchPlayerRepository.findMatchesWhereCaptain(testPlayer1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMatch()).isEqualTo(testMatch1);
        assertThat(results.get(0).getRol()).isEqualTo(PlayerRole.CAPITAN);
    }

    @Test
    void testFindPlayersWithMultiplePositions_WithoutMultiplePositions_ReturnsEmpty() {
        // When
        List<PlayerProfile> results = matchPlayerRepository.findPlayersWithMultiplePositions();

        // Then
        assertThat(results).isEmpty(); // Each player has only played in one position
    }

    @Test
    void testGetConfirmationStatsByPlayer_ValidPlayer_ReturnsCorrectStats() {
        // When
        Object[] stats = matchPlayerRepository.getConfirmationStatsByPlayer(testPlayer1);
        Object[] values = stats.length == 1 && stats[0] instanceof Object[] nestedStats
                ? nestedStats
                : stats;

        // Then
        assertThat(values).hasSize(3);
        assertThat(values[0]).isEqualTo(1L); // total_partidos
        assertThat(values[1]).isEqualTo(1L); // partidos_confirmados
        assertThat(values[2]).isEqualTo(1.0); // porcentaje_confirmacion
    }

    @Test
    void testSaveAndFindById_ValidMatchPlayer_SavesAndRetrievesCorrectly() {
        // Given
        MatchPlayer newMatchPlayer = new MatchPlayer();
        newMatchPlayer.setMatch(testMatch2);
        newMatchPlayer.setPlayer(testPlayer2);
        newMatchPlayer.setTeam(testTeam2);
        newMatchPlayer.setPosition(testPosition2);
        newMatchPlayer.setRol(PlayerRole.JUGADOR);
        newMatchPlayer.setConfirmado(true);

        // When
        MatchPlayer saved = matchPlayerRepository.save(newMatchPlayer);
        Optional<MatchPlayer> found = matchPlayerRepository.findById(saved.getId());

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getRol()).isEqualTo(PlayerRole.JUGADOR);
        assertThat(found.get().isConfirmado()).isTrue();
        assertThat(found.get().getPosition().getNombre()).isEqualTo("Mediocampista");
    }

    @Test
    void testDeleteById_ExistingMatchPlayer_DeletesSuccessfully() {
        // Given
        Long matchPlayerId = testMatchPlayer1.getId();

        // When
        matchPlayerRepository.deleteById(matchPlayerId);

        // Then
        Optional<MatchPlayer> found = matchPlayerRepository.findById(matchPlayerId);
        assertThat(found).isEmpty();
        assertThat(matchPlayerRepository.count()).isEqualTo(2);
    }

    @Test
    void testFindAll_ReturnsAllMatchPlayers() {
        // When
        List<MatchPlayer> results = matchPlayerRepository.findAll();

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void testUpdate_ExistingMatchPlayer_UpdatesSuccessfully() {
        // Given
        testMatchPlayer2.setConfirmado(true);

        // When
        MatchPlayer updated = matchPlayerRepository.save(testMatchPlayer2);

        // Then
        assertThat(updated.isConfirmado()).isTrue();
        
        Optional<MatchPlayer> found = matchPlayerRepository.findById(testMatchPlayer2.getId());
        assertThat(found).isPresent();
        assertThat(found.get().isConfirmado()).isTrue();
    }

    private Position findOrCreatePosition(String name) {
        return positionRepository.findByNombre(name)
                .orElseGet(() -> {
                    Position position = new Position();
                    position.setNombre(name);
                    return positionRepository.save(position);
                });
    }

    private void cleanupData() {
        String[] statements = {
                "DELETE FROM match_players",
                "DELETE FROM match_events",
                "DELETE FROM player_history",
                "DELETE FROM trust_logs",
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
