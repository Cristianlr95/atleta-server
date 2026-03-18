package com.atleta.demo.repository;

import com.atleta.demo.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para PlayerPositionRepository.
 * Verifica operaciones CRUD y consultas personalizadas.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlayerPositionRepositoryTest {

    @Autowired
    private PlayerPositionRepository playerPositionRepository;

    @Autowired
    private AthleteRepository athleteRepository;

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PlayerProfile testPlayer1;
    private PlayerProfile testPlayer2;
    private Position testPosition1;
    private Position testPosition2;
    private Position testPosition3;
    private PlayerPosition testPlayerPosition1;
    private PlayerPosition testPlayerPosition2;
    private PlayerPosition testPlayerPosition3;

    @BeforeEach
    void setUp() {
        cleanupData();

        // Crear atletas y perfiles
        Athlete athlete1 = new Athlete("player1@example.com", "hash1", "Player One");
        Athlete athlete2 = new Athlete("player2@example.com", "hash2", "Player Two");
        
        athlete1 = athleteRepository.save(athlete1);
        athlete2 = athleteRepository.save(athlete2);

        testPlayer1 = new PlayerProfile(athlete1, "Player1");
        testPlayer2 = new PlayerProfile(athlete2, "Player2");
        
        testPlayer1 = playerProfileRepository.save(testPlayer1);
        testPlayer2 = playerProfileRepository.save(testPlayer2);

        // Crear posiciones
        testPosition1 = findOrCreatePosition("Delantero");
        testPosition2 = findOrCreatePosition("Mediocampista");
        testPosition3 = findOrCreatePosition("Defensa");

        // Crear asignaciones de posiciones
        testPlayerPosition1 = new PlayerPosition();
        testPlayerPosition1.setPlayer(testPlayer1);
        testPlayerPosition1.setPosition(testPosition1);
        testPlayerPosition1.setPrioridad(1);
        testPlayerPosition1.setXp(100);

        testPlayerPosition2 = new PlayerPosition();
        testPlayerPosition2.setPlayer(testPlayer1);
        testPlayerPosition2.setPosition(testPosition2);
        testPlayerPosition2.setPrioridad(2);
        testPlayerPosition2.setXp(50);

        testPlayerPosition3 = new PlayerPosition();
        testPlayerPosition3.setPlayer(testPlayer2);
        testPlayerPosition3.setPosition(testPosition1);
        testPlayerPosition3.setPrioridad(1);
        testPlayerPosition3.setXp(80);

        testPlayerPosition1 = playerPositionRepository.save(testPlayerPosition1);
        testPlayerPosition2 = playerPositionRepository.save(testPlayerPosition2);
        testPlayerPosition3 = playerPositionRepository.save(testPlayerPosition3);
    }

    @Test
    void testFindByPlayerOrderByPrioridad_ValidPlayer_ReturnsPositionsOrderedByPriority() {
        // When
        List<PlayerPosition> results = playerPositionRepository.findByPlayerOrderByPrioridad(testPlayer1);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getPrioridad()).isEqualTo(1);
        assertThat(results.get(1).getPrioridad()).isEqualTo(2);
        assertThat(results.get(0).getPosition().getNombre()).isEqualTo("Delantero");
        assertThat(results.get(1).getPosition().getNombre()).isEqualTo("Mediocampista");
    }

    @Test
    void testFindByPlayerAtletaUuidOrderByPrioridad_ValidUuid_ReturnsPositionsOrderedByPriority() {
        // When
        List<PlayerPosition> results = playerPositionRepository.findByPlayerAtletaUuidOrderByPrioridad(testPlayer1.getAtletaUuid());

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getPrioridad()).isEqualTo(1);
        assertThat(results.get(1).getPrioridad()).isEqualTo(2);
    }

    @Test
    void testFindByPosition_ValidPosition_ReturnsPlayersWithPosition() {
        // When
        List<PlayerPosition> results = playerPositionRepository.findByPosition(testPosition1);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(pp -> pp.getPlayer().getAlias())
                .containsExactlyInAnyOrder("Player1", "Player2");
    }

    @Test
    void testFindByPlayerAndPrioridad_ValidPlayerAndPriority_ReturnsSpecificPosition() {
        // When
        Optional<PlayerPosition> result = playerPositionRepository.findByPlayerAndPrioridad(testPlayer1, 1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getPosition().getNombre()).isEqualTo("Delantero");
        assertThat(result.get().getXp()).isEqualTo(100);
    }

    @Test
    void testFindByPlayerAndPrioridad_NonExistingPriority_ReturnsEmpty() {
        // When
        Optional<PlayerPosition> result = playerPositionRepository.findByPlayerAndPrioridad(testPlayer1, 3);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testFindByPlayerAndPosition_ValidPlayerAndPosition_ReturnsAssignment() {
        // When
        Optional<PlayerPosition> result = playerPositionRepository.findByPlayerAndPosition(testPlayer1, testPosition1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getPrioridad()).isEqualTo(1);
        assertThat(result.get().getXp()).isEqualTo(100);
    }

    @Test
    void testExistsByPlayerAndPrioridad_ExistingAssignment_ReturnsTrue() {
        // When
        boolean exists = playerPositionRepository.existsByPlayerAndPrioridad(testPlayer1, 1);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByPlayerAndPrioridad_NonExistingAssignment_ReturnsFalse() {
        // When
        boolean exists = playerPositionRepository.existsByPlayerAndPrioridad(testPlayer2, 2);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void testFindByPositionOrderByXpDesc_ValidPosition_ReturnsPlayersOrderedByXp() {
        // When
        List<PlayerPosition> results = playerPositionRepository.findByPositionOrderByXpDesc(testPosition1);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getXp()).isEqualTo(100); // testPlayer1
        assertThat(results.get(1).getXp()).isEqualTo(80);  // testPlayer2
    }

    @Test
    void testFindByPositionAndXpGreaterThanEqual_ValidCriteria_ReturnsMatchingPlayers() {
        // When
        List<PlayerPosition> results = playerPositionRepository.findByPositionAndXpGreaterThanEqual(testPosition1, 90);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPlayer()).isEqualTo(testPlayer1);
        assertThat(results.get(0).getXp()).isEqualTo(100);
    }

    @Test
    void testFindPrimaryPositionByPlayer_ValidPlayer_ReturnsPrimaryPosition() {
        // When
        Optional<PlayerPosition> result = playerPositionRepository.findPrimaryPositionByPlayer(testPlayer1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getPrioridad()).isEqualTo(1);
        assertThat(result.get().getPosition().getNombre()).isEqualTo("Delantero");
    }

    @Test
    void testFindPlayersByPrimaryPosition_ValidPosition_ReturnsPlayersWithPrimaryPosition() {
        // When
        List<PlayerPosition> results = playerPositionRepository.findPlayersByPrimaryPosition(testPosition1);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(pp -> pp.getPrioridad() == 1);
        assertThat(results).extracting(pp -> pp.getPlayer().getAlias())
                .containsExactlyInAnyOrder("Player1", "Player2");
    }

    @Test
    void testCountByPosition_ValidPosition_ReturnsCorrectCount() {
        // When
        long count = playerPositionRepository.countByPosition(testPosition1);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testGetXpStatisticsByPosition_ValidPosition_ReturnsCorrectStats() {
        // When
        Object[] stats = playerPositionRepository.getXpStatisticsByPosition(testPosition1);
        Object[] values = stats.length == 1 && stats[0] instanceof Object[] nestedStats
                ? nestedStats
                : stats;

        // Then
        assertThat(values).hasSize(3);
        assertThat(values[0]).isEqualTo(80);   // MIN
        assertThat(values[1]).isEqualTo(100);  // MAX
        assertThat(values[2]).isEqualTo(90.0); // AVG
    }

    @Test
    void testFindPlayersWithMultiplePositions_ReturnsPlayersWithMultiplePositions() {
        // When
        List<PlayerProfile> results = playerPositionRepository.findPlayersWithMultiplePositions();

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(testPlayer1);
    }

    @Test
    void testFindByXpBetween_ValidRange_ReturnsAssignmentsInRange() {
        // When
        List<PlayerPosition> results = playerPositionRepository.findByXpBetween(60, 90);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getXp()).isEqualTo(80);
        assertThat(results.get(0).getPlayer()).isEqualTo(testPlayer2);
    }

    @Test
    void testSaveAndFindById_ValidPlayerPosition_SavesAndRetrievesCorrectly() {
        // Given
        PlayerPosition newPlayerPosition = new PlayerPosition();
        newPlayerPosition.setPlayer(testPlayer2);
        newPlayerPosition.setPosition(testPosition2);
        newPlayerPosition.setPrioridad(2);
        newPlayerPosition.setXp(60);

        // When
        PlayerPosition saved = playerPositionRepository.save(newPlayerPosition);
        Optional<PlayerPosition> found = playerPositionRepository.findById(saved.getId());

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getPrioridad()).isEqualTo(2);
        assertThat(found.get().getXp()).isEqualTo(60);
        assertThat(found.get().getPosition().getNombre()).isEqualTo("Mediocampista");
    }

    @Test
    void testDeleteById_ExistingPlayerPosition_DeletesSuccessfully() {
        // Given
        Long playerPositionId = testPlayerPosition1.getId();

        // When
        playerPositionRepository.deleteById(playerPositionId);

        // Then
        Optional<PlayerPosition> found = playerPositionRepository.findById(playerPositionId);
        assertThat(found).isEmpty();
        assertThat(playerPositionRepository.count()).isEqualTo(2);
    }

    @Test
    void testDeleteByPlayer_ValidPlayer_DeletesAllPlayerPositions() {
        // When
        playerPositionRepository.deleteByPlayer(testPlayer1);

        // Then
        List<PlayerPosition> remaining = playerPositionRepository.findByPlayerOrderByPrioridad(testPlayer1);
        assertThat(remaining).isEmpty();
        assertThat(playerPositionRepository.count()).isEqualTo(1); // Only testPlayer2's position remains
    }

    @Test
    void testFindAll_ReturnsAllPlayerPositions() {
        // When
        List<PlayerPosition> results = playerPositionRepository.findAll();

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void testUpdate_ExistingPlayerPosition_UpdatesSuccessfully() {
        // Given
        testPlayerPosition1.setXp(150);

        // When
        PlayerPosition updated = playerPositionRepository.save(testPlayerPosition1);

        // Then
        assertThat(updated.getXp()).isEqualTo(150);
        
        Optional<PlayerPosition> found = playerPositionRepository.findById(testPlayerPosition1.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getXp()).isEqualTo(150);
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
                "DELETE FROM trust_logs",
                "DELETE FROM player_history",
                "DELETE FROM match_events",
                "DELETE FROM match_players",
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
