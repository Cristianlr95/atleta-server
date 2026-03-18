package com.atleta.demo.repository;

import com.atleta.demo.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para TeamStatsRepository.
 * Verifica operaciones CRUD y consultas personalizadas.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TeamStatsRepositoryTest {

    @Autowired
    private TeamStatsRepository teamStatsRepository;

    @Autowired
    private AthleteRepository athleteRepository;

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private TeamRepository teamRepository;

    private PlayerProfile testCreator1;
    private PlayerProfile testCreator2;
    private PlayerProfile testCreator3;
    private Team testTeam1;
    private Team testTeam2;
    private Team testTeam3;
    private TeamStats testStats1;
    private TeamStats testStats2;
    private TeamStats testStats3;

    @BeforeEach
    void setUp() {
        // Limpiar datos existentes
        teamStatsRepository.deleteAll();
        teamRepository.deleteAll();
        playerProfileRepository.deleteAll();
        athleteRepository.deleteAll();

        // Crear atletas y perfiles
        Athlete athlete1 = new Athlete("creator1@example.com", "hash1", "Creator One");
        Athlete athlete2 = new Athlete("creator2@example.com", "hash2", "Creator Two");
        Athlete athlete3 = new Athlete("creator3@example.com", "hash3", "Creator Three");
        
        athlete1 = athleteRepository.save(athlete1);
        athlete2 = athleteRepository.save(athlete2);
        athlete3 = athleteRepository.save(athlete3);

        testCreator1 = new PlayerProfile(athlete1, "Creator1");
        testCreator2 = new PlayerProfile(athlete2, "Creator2");
        testCreator3 = new PlayerProfile(athlete3, "Creator3");
        
        testCreator1 = playerProfileRepository.save(testCreator1);
        testCreator2 = playerProfileRepository.save(testCreator2);
        testCreator3 = playerProfileRepository.save(testCreator3);

        // Crear equipos
        testTeam1 = new Team("Team Alpha", testCreator1);
        testTeam2 = new Team("Team Beta", testCreator2);
        testTeam3 = new Team("Team Gamma", testCreator3);
        
        testTeam1 = teamRepository.save(testTeam1);
        testTeam2 = teamRepository.save(testTeam2);
        testTeam3 = teamRepository.save(testTeam3);

        // Crear estadísticas de equipos
        testStats1 = new TeamStats();
        testStats1.setTeam(testTeam1);
        testStats1.setPartidosJugados(10);
        testStats1.setPartidosGanados(7);
        testStats1.setPartidosPerdidos(2);
        testStats1.setPartidosEmpatados(1);
        testStats1.setGolesFavor(25);
        testStats1.setGolesContra(8);

        testStats2 = new TeamStats();
        testStats2.setTeam(testTeam2);
        testStats2.setPartidosJugados(8);
        testStats2.setPartidosGanados(3);
        testStats2.setPartidosPerdidos(4);
        testStats2.setPartidosEmpatados(1);
        testStats2.setGolesFavor(12);
        testStats2.setGolesContra(15);

        testStats3 = new TeamStats();
        testStats3.setTeam(testTeam3);
        testStats3.setPartidosJugados(0);
        testStats3.setPartidosGanados(0);
        testStats3.setPartidosPerdidos(0);
        testStats3.setPartidosEmpatados(0);
        testStats3.setGolesFavor(0);
        testStats3.setGolesContra(0);

        testStats1 = teamStatsRepository.save(testStats1);
        testStats2 = teamStatsRepository.save(testStats2);
        testStats3 = teamStatsRepository.save(testStats3);
    }

    @Test
    void testFindByTeam_ValidTeam_ReturnsTeamStats() {
        // When
        Optional<TeamStats> result = teamStatsRepository.findByTeam(testTeam1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getPartidosJugados()).isEqualTo(10);
        assertThat(result.get().getPartidosGanados()).isEqualTo(7);
    }

    @Test
    void testFindByTeam_NonExistingTeam_ReturnsEmpty() {
        // Given
        Team newTeam = new Team("New Team", testCreator1);
        newTeam = teamRepository.save(newTeam);

        // When
        Optional<TeamStats> result = teamStatsRepository.findByTeam(newTeam);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testFindAllOrderByPartidosJugadosDesc_ReturnsTeamsOrderedByMatchesPlayed() {
        // When
        List<TeamStats> results = teamStatsRepository.findAllOrderByPartidosJugadosDesc();

        // Then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getPartidosJugados()).isEqualTo(10); // testStats1
        assertThat(results.get(1).getPartidosJugados()).isEqualTo(8);  // testStats2
        assertThat(results.get(2).getPartidosJugados()).isEqualTo(0);  // testStats3
    }

    @Test
    void testFindAllOrderByVictoriasDesc_ReturnsTeamsOrderedByWins() {
        // When
        List<TeamStats> results = teamStatsRepository.findAllOrderByVictoriasDesc();

        // Then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getPartidosGanados()).isEqualTo(7); // testStats1
        assertThat(results.get(1).getPartidosGanados()).isEqualTo(3); // testStats2
        assertThat(results.get(2).getPartidosGanados()).isEqualTo(0); // testStats3
    }

    @Test
    void testFindAllOrderByGolesAFavorDesc_ReturnsTeamsOrderedByGoalsFor() {
        // When
        List<TeamStats> results = teamStatsRepository.findAllOrderByGolesAFavorDesc();

        // Then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getGolesFavor()).isEqualTo(25); // testStats1
        assertThat(results.get(1).getGolesFavor()).isEqualTo(12); // testStats2
        assertThat(results.get(2).getGolesFavor()).isEqualTo(0);  // testStats3
    }

    @Test
    void testFindAllOrderByGoalDifferenceDesc_ReturnsTeamsOrderedByGoalDifference() {
        // When
        List<TeamStats> results = teamStatsRepository.findAllOrderByGoalDifferenceDesc();

        // Then
        assertThat(results).hasSize(3);
        // testStats1: 25-8 = +17
        // testStats2: 12-15 = -3
        // testStats3: 0-0 = 0
        assertThat(results.get(0)).isEqualTo(testStats1);
        assertThat(results.get(1)).isEqualTo(testStats3);
        assertThat(results.get(2)).isEqualTo(testStats2);
    }

    @Test
    void testFindByPartidosJugadosGreaterThanEqual_ValidThreshold_ReturnsMatchingTeams() {
        // When
        List<TeamStats> results = teamStatsRepository.findByPartidosJugadosGreaterThanEqual(5);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(ts -> ts.getTeam().getNombre())
                .containsExactlyInAnyOrder("Team Alpha", "Team Beta");
    }

    @Test
    void testFindByVictoriasGreaterThanEqual_ValidThreshold_ReturnsMatchingTeams() {
        // When
        List<TeamStats> results = teamStatsRepository.findByVictoriasGreaterThanEqual(5);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTeam().getNombre()).isEqualTo("Team Alpha");
    }

    @Test
    void testFindByWinPercentageGreaterThanEqual_ValidPercentage_ReturnsMatchingTeams() {
        // When
        List<TeamStats> results = teamStatsRepository.findByWinPercentageGreaterThanEqual(0.5);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTeam().getNombre()).isEqualTo("Team Alpha");
        // testStats1: 7/10 = 0.7 (70%)
    }

    @Test
    void testFindUndefeatedTeams_ReturnsTeamsWithoutDefeats() {
        // Given - Create an undefeated team
        Team undefeatedTeam = new Team("Undefeated Team", testCreator1);
        undefeatedTeam = teamRepository.save(undefeatedTeam);
        
        TeamStats undefeatedStats = new TeamStats();
        undefeatedStats.setTeam(undefeatedTeam);
        undefeatedStats.setPartidosJugados(5);
        undefeatedStats.setPartidosGanados(4);
        undefeatedStats.setPartidosPerdidos(0);
        undefeatedStats.setPartidosEmpatados(1);
        teamStatsRepository.save(undefeatedStats);

        // When
        List<TeamStats> results = teamStatsRepository.findUndefeatedTeams();

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTeam().getNombre()).isEqualTo("Undefeated Team");
    }

    @Test
    void testFindTeamsWithoutWins_ReturnsTeamsWithoutVictories() {
        // Given - testStats3 already has 0 wins but 0 matches, create one with matches but no wins
        Team winlessTeam = new Team("Winless Team", testCreator2);
        winlessTeam = teamRepository.save(winlessTeam);
        
        TeamStats winlessStats = new TeamStats();
        winlessStats.setTeam(winlessTeam);
        winlessStats.setPartidosJugados(3);
        winlessStats.setPartidosGanados(0);
        winlessStats.setPartidosPerdidos(2);
        winlessStats.setPartidosEmpatados(1);
        teamStatsRepository.save(winlessStats);

        // When
        List<TeamStats> results = teamStatsRepository.findTeamsWithoutWins();

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTeam().getNombre()).isEqualTo("Winless Team");
    }

    @Test
    void testFindTopScoringTeams_ReturnsTeamsOrderedByGoalsFor() {
        // When
        List<TeamStats> results = teamStatsRepository.findTopScoringTeams();

        // Then
        assertThat(results).hasSize(2); // Only teams with matches > 0
        assertThat(results.get(0).getGolesFavor()).isEqualTo(25); // testStats1
        assertThat(results.get(1).getGolesFavor()).isEqualTo(12); // testStats2
    }

    @Test
    void testFindBestDefensiveTeams_ReturnsTeamsOrderedByGoalsAgainst() {
        // When
        List<TeamStats> results = teamStatsRepository.findBestDefensiveTeams();

        // Then
        assertThat(results).hasSize(2); // Only teams with matches > 0
        assertThat(results.get(0).getGolesContra()).isEqualTo(8);  // testStats1
        assertThat(results.get(1).getGolesContra()).isEqualTo(15); // testStats2
    }

    @Test
    void testGetGeneralStatistics_ReturnsCorrectAggregatedStats() {
        // When
        Object[] stats = teamStatsRepository.getGeneralStatistics();

        // Then
        assertThat(stats).hasSize(3);
        assertThat(stats[0]).isEqualTo(18L); // total_partidos (10 + 8 + 0)
        assertThat(stats[1]).isEqualTo(37L); // total_goles (25 + 12 + 0)
        assertThat((Double) stats[2]).isEqualTo(12.333333333333334); // promedio_goles_por_partido
    }

    @Test
    void testFindTeamsWithoutActivity_ReturnsInactiveTeams() {
        // When
        List<TeamStats> results = teamStatsRepository.findTeamsWithoutActivity();

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTeam().getNombre()).isEqualTo("Team Gamma");
    }

    @Test
    void testFindMostActiveTeams_ReturnsTeamsOrderedByActivity() {
        // When
        List<TeamStats> results = teamStatsRepository.findMostActiveTeams();

        // Then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getPartidosJugados()).isEqualTo(10); // testStats1
        assertThat(results.get(1).getPartidosJugados()).isEqualTo(8);  // testStats2
        assertThat(results.get(2).getPartidosJugados()).isEqualTo(0);  // testStats3
    }

    @Test
    void testFindByPartidosJugadosBetween_ValidRange_ReturnsTeamsInRange() {
        // When
        List<TeamStats> results = teamStatsRepository.findByPartidosJugadosBetween(5, 10);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(ts -> ts.getTeam().getNombre())
                .containsExactlyInAnyOrder("Team Alpha", "Team Beta");
    }

    @Test
    void testSaveAndFindById_ValidTeamStats_SavesAndRetrievesCorrectly() {
        // Given
        Team newTeam = new Team("New Team", testCreator1);
        newTeam = teamRepository.save(newTeam);
        
        TeamStats newStats = new TeamStats();
        newStats.setTeam(newTeam);
        newStats.setPartidosJugados(5);
        newStats.setPartidosGanados(3);
        newStats.setPartidosPerdidos(1);
        newStats.setPartidosEmpatados(1);
        newStats.setGolesFavor(10);
        newStats.setGolesContra(5);

        // When
        TeamStats saved = teamStatsRepository.save(newStats);
        Optional<TeamStats> found = teamStatsRepository.findById(saved.getId());

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getPartidosJugados()).isEqualTo(5);
        assertThat(found.get().getPartidosGanados()).isEqualTo(3);
        assertThat(found.get().getGolesFavor()).isEqualTo(10);
    }

    @Test
    void testDeleteById_ExistingTeamStats_DeletesSuccessfully() {
        // Given
        Long statsId = testStats1.getId();

        // When
        teamStatsRepository.deleteById(statsId);

        // Then
        Optional<TeamStats> found = teamStatsRepository.findById(statsId);
        assertThat(found).isEmpty();
        assertThat(teamStatsRepository.count()).isEqualTo(2);
    }

    @Test
    void testFindAll_ReturnsAllTeamStats() {
        // When
        List<TeamStats> results = teamStatsRepository.findAll();

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void testUpdate_ExistingTeamStats_UpdatesSuccessfully() {
        // Given
        testStats1.setPartidosGanados(8);
        testStats1.setPartidosJugados(11);

        // When
        TeamStats updated = teamStatsRepository.save(testStats1);

        // Then
        assertThat(updated.getPartidosGanados()).isEqualTo(8);
        assertThat(updated.getPartidosJugados()).isEqualTo(11);
        
        Optional<TeamStats> found = teamStatsRepository.findById(testStats1.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getPartidosGanados()).isEqualTo(8);
        assertThat(found.get().getPartidosJugados()).isEqualTo(11);
    }

    @Test
    void testGoalDifferenceCalculation_CorrectlyCalculatesPositiveAndNegativeDifferences() {
        // When
        List<TeamStats> results = teamStatsRepository.findAllOrderByGoalDifferenceDesc();

        // Then
        assertThat(results).hasSize(3);
        
        // Calculate expected differences
        int diff1 = testStats1.getGolesFavor() - testStats1.getGolesContra(); // 25 - 8 = +17
        int diff2 = testStats2.getGolesFavor() - testStats2.getGolesContra(); // 12 - 15 = -3
        int diff3 = testStats3.getGolesFavor() - testStats3.getGolesContra(); // 0 - 0 = 0
        
        assertThat(diff1).isEqualTo(17);
        assertThat(diff2).isEqualTo(-3);
        assertThat(diff3).isEqualTo(0);
        
        // Verify order: +17, 0, -3
        assertThat(results.get(0)).isEqualTo(testStats1);
        assertThat(results.get(1)).isEqualTo(testStats3);
        assertThat(results.get(2)).isEqualTo(testStats2);
    }
}