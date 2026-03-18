package com.atleta.demo.repository;

import com.atleta.demo.entity.*;
import com.atleta.demo.enums.MatchMode;
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
 * Tests unitarios para MatchTeamRepository.
 * Verifica operaciones CRUD y consultas personalizadas.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MatchTeamRepositoryTest {

    @Autowired
    private MatchTeamRepository matchTeamRepository;

    @Autowired
    private AthleteRepository athleteRepository;

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MatchRepository matchRepository;

    private PlayerProfile testCreator1;
    private PlayerProfile testCreator2;
    private Team testTeam1;
    private Team testTeam2;
    private Team testTeam3;
    private Match testMatch1;
    private Match testMatch2;
    private MatchTeam testMatchTeam1;
    private MatchTeam testMatchTeam2;
    private MatchTeam testMatchTeam3;

    @BeforeEach
    void setUp() {
        // Limpiar datos existentes
        matchTeamRepository.deleteAll();
        matchRepository.deleteAll();
        teamRepository.deleteAll();
        playerProfileRepository.deleteAll();
        athleteRepository.deleteAll();

        // Crear atletas y perfiles
        Athlete athlete1 = new Athlete("creator1@example.com", "hash1", "Creator One");
        Athlete athlete2 = new Athlete("creator2@example.com", "hash2", "Creator Two");
        
        athlete1 = athleteRepository.save(athlete1);
        athlete2 = athleteRepository.save(athlete2);

        testCreator1 = new PlayerProfile(athlete1, "Creator1");
        testCreator2 = new PlayerProfile(athlete2, "Creator2");
        
        testCreator1 = playerProfileRepository.save(testCreator1);
        testCreator2 = playerProfileRepository.save(testCreator2);

        // Crear equipos
        testTeam1 = new Team("Team Alpha", testCreator1);
        testTeam2 = new Team("Team Beta", testCreator2);
        testTeam3 = new Team("Team Gamma", testCreator1);
        
        testTeam1 = teamRepository.save(testTeam1);
        testTeam2 = teamRepository.save(testTeam2);
        testTeam3 = teamRepository.save(testTeam3);

        // Crear partidos
        testMatch1 = new Match(MatchMode.CINCO_VS_CINCO, LocalDateTime.now().plusDays(1), testCreator1);
        testMatch1.setEstado(MatchStatus.CREADO);
        
        testMatch2 = new Match(MatchMode.SEIS_VS_SEIS, LocalDateTime.now().plusDays(2), testCreator2);
        testMatch2.setEstado(MatchStatus.INICIADO);
        
        testMatch1 = matchRepository.save(testMatch1);
        testMatch2 = matchRepository.save(testMatch2);

        // Crear participaciones de equipos en partidos
        testMatchTeam1 = new MatchTeam();
        testMatchTeam1.setMatch(testMatch1);
        testMatchTeam1.setTeam(testTeam1);
        testMatchTeam1.setEsLocal(true);
        testMatchTeam1.setGoles(3);

        testMatchTeam2 = new MatchTeam();
        testMatchTeam2.setMatch(testMatch1);
        testMatchTeam2.setTeam(testTeam2);
        testMatchTeam2.setEsLocal(false);
        testMatchTeam2.setGoles(1);

        testMatchTeam3 = new MatchTeam();
        testMatchTeam3.setMatch(testMatch2);
        testMatchTeam3.setTeam(testTeam3);
        testMatchTeam3.setEsLocal(true);
        testMatchTeam3.setGoles(2);

        testMatchTeam1 = matchTeamRepository.save(testMatchTeam1);
        testMatchTeam2 = matchTeamRepository.save(testMatchTeam2);
        testMatchTeam3 = matchTeamRepository.save(testMatchTeam3);
    }

    @Test
    void testFindByMatch_ValidMatch_ReturnsMatchTeams() {
        // When
        List<MatchTeam> results = matchTeamRepository.findByMatch(testMatch1);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(mt -> mt.getTeam().getNombre())
                .containsExactlyInAnyOrder("Team Alpha", "Team Beta");
    }

    @Test
    void testFindByTeam_ValidTeam_ReturnsTeamMatches() {
        // When
        List<MatchTeam> results = matchTeamRepository.findByTeam(testTeam1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMatch()).isEqualTo(testMatch1);
        assertThat(results.get(0).getEsLocal()).isTrue();
    }

    @Test
    void testFindLocalTeamByMatch_ValidMatch_ReturnsLocalTeam() {
        // When
        Optional<MatchTeam> result = matchTeamRepository.findLocalTeamByMatch(testMatch1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getTeam()).isEqualTo(testTeam1);
        assertThat(result.get().getEsLocal()).isTrue();
    }

    @Test
    void testFindVisitingTeamByMatch_ValidMatch_ReturnsVisitingTeam() {
        // When
        Optional<MatchTeam> result = matchTeamRepository.findVisitingTeamByMatch(testMatch1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getTeam()).isEqualTo(testTeam2);
        assertThat(result.get().getEsLocal()).isFalse();
    }

    @Test
    void testFindByMatchAndTeam_ValidMatchAndTeam_ReturnsSpecificParticipation() {
        // When
        Optional<MatchTeam> result = matchTeamRepository.findByMatchAndTeam(testMatch1, testTeam1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEsLocal()).isTrue();
        assertThat(result.get().getGoles()).isEqualTo(3);
    }

    @Test
    void testExistsByMatchAndTeam_ExistingParticipation_ReturnsTrue() {
        // When
        boolean exists = matchTeamRepository.existsByMatchAndTeam(testMatch1, testTeam1);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByMatchAndTeam_NonExistingParticipation_ReturnsFalse() {
        // When
        boolean exists = matchTeamRepository.existsByMatchAndTeam(testMatch2, testTeam1);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void testCountByMatch_ValidMatch_ReturnsCorrectCount() {
        // When
        long count = matchTeamRepository.countByMatch(testMatch1);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testFindHomeMatchesByTeam_ValidTeam_ReturnsHomeMatches() {
        // When
        List<MatchTeam> results = matchTeamRepository.findHomeMatchesByTeam(testTeam1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMatch()).isEqualTo(testMatch1);
        assertThat(results.get(0).getEsLocal()).isTrue();
    }

    @Test
    void testFindAwayMatchesByTeam_ValidTeam_ReturnsAwayMatches() {
        // When
        List<MatchTeam> results = matchTeamRepository.findAwayMatchesByTeam(testTeam2);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMatch()).isEqualTo(testMatch1);
        assertThat(results.get(0).getEsLocal()).isFalse();
    }

    @Test
    void testFindByGolesGreaterThanEqual_ValidThreshold_ReturnsMatchingTeams() {
        // When
        List<MatchTeam> results = matchTeamRepository.findByGolesGreaterThanEqual(2);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(MatchTeam::getGoles)
                .containsExactlyInAnyOrder(3, 2);
    }

    @Test
    void testFindByMatchOrderByGolesDesc_ValidMatch_ReturnsTeamsOrderedByGoals() {
        // When
        List<MatchTeam> results = matchTeamRepository.findByMatchOrderByGolesDesc(testMatch1);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getGoles()).isEqualTo(3); // testTeam1
        assertThat(results.get(1).getGoles()).isEqualTo(1); // testTeam2
    }

    @Test
    void testFindMatchesWithExactlyTwoTeams_WithValidMatches_ReturnsValidMatches() {
        // When
        List<Match> results = matchTeamRepository.findMatchesWithExactlyTwoTeams();

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(testMatch1);
    }

    @Test
    void testFindMatchesWithoutExactlyTwoTeams_WithInvalidMatches_ReturnsInvalidMatches() {
        // When
        List<Match> results = matchTeamRepository.findMatchesWithoutExactlyTwoTeams();

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(testMatch2); // Only has 1 team
    }

    @Test
    void testGetTotalGoalsByTeam_ValidTeam_ReturnsCorrectTotal() {
        // When
        Long totalGoals = matchTeamRepository.getTotalGoalsByTeam(testTeam1);

        // Then
        assertThat(totalGoals).isEqualTo(3L);
    }

    @Test
    void testGetAverageGoalsByTeam_ValidTeam_ReturnsCorrectAverage() {
        // When
        Double averageGoals = matchTeamRepository.getAverageGoalsByTeam(testTeam1);

        // Then
        assertThat(averageGoals).isEqualTo(3.0);
    }

    @Test
    void testFindTeamsOrderByTotalGoals_ReturnsTeamsOrderedByGoals() {
        // When
        List<Team> results = matchTeamRepository.findTeamsOrderByTotalGoals();

        // Then
        assertThat(results).hasSize(3);
        // testTeam1 has 3 goals, testTeam3 has 2 goals, testTeam2 has 1 goal
        assertThat(results.get(0)).isEqualTo(testTeam1);
        assertThat(results.get(1)).isEqualTo(testTeam3);
        assertThat(results.get(2)).isEqualTo(testTeam2);
    }

    @Test
    void testFindMatchesBetweenTeams_ValidTeams_ReturnsMatchesBetweenThem() {
        // When
        List<Match> results = matchTeamRepository.findMatchesBetweenTeams(testTeam1, testTeam2);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(testMatch1);
    }

    @Test
    void testFindMatchesBetweenTeams_TeamsNeverPlayed_ReturnsEmpty() {
        // When
        List<Match> results = matchTeamRepository.findMatchesBetweenTeams(testTeam1, testTeam3);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void testFindMatchesWithLargeGoalDifference_WithGoleadas_ReturnsMatchesWithLargeDifference() {
        // When
        List<Match> results = matchTeamRepository.findMatchesWithLargeGoalDifference();

        // Then
        assertThat(results).isEmpty(); // Current difference is 3-1=2, which is < 3
    }

    @Test
    void testFindMatchesWithLargeGoalDifference_WithActualGoleada_ReturnsMatches() {
        // Given - Modify goals to create a large difference
        testMatchTeam1.setGoles(5);
        testMatchTeam2.setGoles(0);
        matchTeamRepository.save(testMatchTeam1);
        matchTeamRepository.save(testMatchTeam2);

        // When
        List<Match> results = matchTeamRepository.findMatchesWithLargeGoalDifference();

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(testMatch1);
    }

    @Test
    void testSaveAndFindById_ValidMatchTeam_SavesAndRetrievesCorrectly() {
        // Given
        MatchTeam newMatchTeam = new MatchTeam();
        newMatchTeam.setMatch(testMatch2);
        newMatchTeam.setTeam(testTeam2);
        newMatchTeam.setEsLocal(false);
        newMatchTeam.setGoles(4);

        // When
        MatchTeam saved = matchTeamRepository.save(newMatchTeam);
        Optional<MatchTeam> found = matchTeamRepository.findById(saved.getId());

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getEsLocal()).isFalse();
        assertThat(found.get().getGoles()).isEqualTo(4);
        assertThat(found.get().getTeam().getNombre()).isEqualTo("Team Beta");
    }

    @Test
    void testDeleteById_ExistingMatchTeam_DeletesSuccessfully() {
        // Given
        Long matchTeamId = testMatchTeam1.getId();

        // When
        matchTeamRepository.deleteById(matchTeamId);

        // Then
        Optional<MatchTeam> found = matchTeamRepository.findById(matchTeamId);
        assertThat(found).isEmpty();
        assertThat(matchTeamRepository.count()).isEqualTo(2);
    }

    @Test
    void testFindAll_ReturnsAllMatchTeams() {
        // When
        List<MatchTeam> results = matchTeamRepository.findAll();

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void testUpdate_ExistingMatchTeam_UpdatesSuccessfully() {
        // Given
        testMatchTeam1.setGoles(5);

        // When
        MatchTeam updated = matchTeamRepository.save(testMatchTeam1);

        // Then
        assertThat(updated.getGoles()).isEqualTo(5);
        
        Optional<MatchTeam> found = matchTeamRepository.findById(testMatchTeam1.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getGoles()).isEqualTo(5);
    }

    @Test
    void testMatchWithExactlyTwoTeams_AfterAddingSecondTeam_BecomesValid() {
        // Given - Add a second team to testMatch2
        MatchTeam newMatchTeam = new MatchTeam();
        newMatchTeam.setMatch(testMatch2);
        newMatchTeam.setTeam(testTeam2);
        newMatchTeam.setEsLocal(false);
        newMatchTeam.setGoles(1);
        matchTeamRepository.save(newMatchTeam);

        // When
        List<Match> validMatches = matchTeamRepository.findMatchesWithExactlyTwoTeams();
        List<Match> invalidMatches = matchTeamRepository.findMatchesWithoutExactlyTwoTeams();

        // Then
        assertThat(validMatches).hasSize(2);
        assertThat(validMatches).containsExactlyInAnyOrder(testMatch1, testMatch2);
        assertThat(invalidMatches).isEmpty();
    }
}