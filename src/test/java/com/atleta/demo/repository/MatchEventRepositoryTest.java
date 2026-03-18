package com.atleta.demo.repository;

import com.atleta.demo.entity.*;
import com.atleta.demo.enums.EventType;
import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchStatus;
import jakarta.persistence.EntityManager;
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
 * Tests unitarios para MatchEventRepository.
 * Verifica operaciones CRUD y consultas personalizadas.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MatchEventRepositoryTest {

    @Autowired
    private MatchEventRepository matchEventRepository;

    @Autowired
    private AthleteRepository athleteRepository;

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchTeamRepository matchTeamRepository;

    @Autowired
    private EntityManager entityManager;

    private PlayerProfile testPlayer1;
    private PlayerProfile testPlayer2;
    private PlayerProfile testPlayer3;
    private Team testTeam1;
    private Team testTeam2;
    private Match testMatch1;
    private Match testMatch2;
    private MatchEvent testEvent1;
    private MatchEvent testEvent2;
    private MatchEvent testEvent3;

    @BeforeEach
    void setUp() {
        // Limpiar datos existentes
        matchEventRepository.deleteAll();
        matchTeamRepository.deleteAll();
        matchRepository.deleteAll();
        teamRepository.deleteAll();
        playerProfileRepository.deleteAll();
        athleteRepository.deleteAll();
        entityManager.flush();

        // Crear atletas primero
        Athlete athlete1 = new Athlete("player1@example.com", "hash1", "Player One");
        Athlete athlete2 = new Athlete("player2@example.com", "hash2", "Player Two");
        Athlete athlete3 = new Athlete("player3@example.com", "hash3", "Player Three");
        
        athlete1 = athleteRepository.save(athlete1);
        athlete2 = athleteRepository.save(athlete2);
        athlete3 = athleteRepository.save(athlete3);
        entityManager.flush();

        // Crear perfiles de jugador usando persist para evitar merge
        testPlayer1 = new PlayerProfile(athlete1, "Player1");
        testPlayer2 = new PlayerProfile(athlete2, "Player2");
        testPlayer3 = new PlayerProfile(athlete3, "Player3");
        
        // Establecer relación bidireccional
        athlete1.setPlayerProfile(testPlayer1);
        athlete2.setPlayerProfile(testPlayer2);
        athlete3.setPlayerProfile(testPlayer3);
        
        entityManager.persist(testPlayer1);
        entityManager.persist(testPlayer2);
        entityManager.persist(testPlayer3);
        entityManager.flush();

        // Crear equipos
        testTeam1 = new Team("Team Alpha", testPlayer1);
        testTeam2 = new Team("Team Beta", testPlayer2);
        
        testTeam1 = teamRepository.save(testTeam1);
        testTeam2 = teamRepository.save(testTeam2);
        entityManager.flush();

        // Crear partidos
        testMatch1 = new Match(MatchMode.CINCO_VS_CINCO, LocalDateTime.now().plusDays(1), testPlayer1);
        testMatch1.setEstado(MatchStatus.INICIADO);
        
        testMatch2 = new Match(MatchMode.SEIS_VS_SEIS, LocalDateTime.now().plusDays(2), testPlayer2);
        testMatch2.setEstado(MatchStatus.INICIADO);
        
        testMatch1 = matchRepository.save(testMatch1);
        testMatch2 = matchRepository.save(testMatch2);
        entityManager.flush();

        // Crear equipos para los partidos (cada partido necesita exactamente 2 equipos)
        MatchTeam match1Team1 = new MatchTeam(testMatch1, testTeam1, true); // equipo local
        MatchTeam match1Team2 = new MatchTeam(testMatch1, testTeam2, false); // equipo visitante
        MatchTeam match2Team1 = new MatchTeam(testMatch2, testTeam1, true); // equipo local
        MatchTeam match2Team2 = new MatchTeam(testMatch2, testTeam2, false); // equipo visitante
        
        matchTeamRepository.save(match1Team1);
        matchTeamRepository.save(match1Team2);
        matchTeamRepository.save(match2Team1);
        matchTeamRepository.save(match2Team2);
        entityManager.flush();

        // Crear eventos de partido usando constructor
        testEvent1 = new MatchEvent(testMatch1, EventType.GOL, testPlayer1, testTeam1, testPlayer2, testPlayer1);
        testEvent1.setConfirmedByHome(true);
        testEvent1.setConfirmedByAway(true);

        testEvent2 = new MatchEvent(testMatch1, EventType.ASISTENCIA, testPlayer2, testTeam2, testPlayer2);
        testEvent2.setConfirmedByHome(true);
        testEvent2.setConfirmedByAway(false);

        testEvent3 = new MatchEvent(testMatch2, EventType.GOL, testPlayer3, testTeam1, testPlayer3);
        testEvent3.setConfirmedByHome(false);
        testEvent3.setConfirmedByAway(false);

        testEvent1 = matchEventRepository.save(testEvent1);
        testEvent2 = matchEventRepository.save(testEvent2);
        testEvent3 = matchEventRepository.save(testEvent3);
        entityManager.flush();
    }

    @Test
    void testFindByMatchOrderByCreatedAt_ValidMatch_ReturnsEventsOrderedByTime() {
        // When
        List<MatchEvent> results = matchEventRepository.findByMatchOrderByCreatedAt(testMatch1);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getCreatedAt()).isBeforeOrEqualTo(results.get(1).getCreatedAt());
    }

    @Test
    void testFindByPlayer_ValidPlayer_ReturnsPlayerEvents() {
        // When
        List<MatchEvent> results = matchEventRepository.findByPlayer(testPlayer1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTipoEvento()).isEqualTo(EventType.GOL);
    }

    @Test
    void testFindByMatchAndTipoEvento_ValidMatchAndType_ReturnsMatchingEvents() {
        // When
        List<MatchEvent> results = matchEventRepository.findByMatchAndTipoEvento(testMatch1, EventType.GOL);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPlayer()).isEqualTo(testPlayer1);
    }

    @Test
    void testFindGoalsByMatch_ValidMatch_ReturnsOnlyGoals() {
        // When
        List<MatchEvent> results = matchEventRepository.findGoalsByMatch(testMatch1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTipoEvento()).isEqualTo(EventType.GOL);
        assertThat(results.get(0).getPlayer()).isEqualTo(testPlayer1);
    }

    @Test
    void testFindAssistsByMatch_ValidMatch_ReturnsOnlyAssists() {
        // When
        List<MatchEvent> results = matchEventRepository.findAssistsByMatch(testMatch1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTipoEvento()).isEqualTo(EventType.ASISTENCIA);
        assertThat(results.get(0).getPlayer()).isEqualTo(testPlayer2);
    }

    @Test
    void testFindGoalsByPlayer_ValidPlayer_ReturnsPlayerGoals() {
        // When
        List<MatchEvent> results = matchEventRepository.findGoalsByPlayer(testPlayer1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTipoEvento()).isEqualTo(EventType.GOL);
    }

    @Test
    void testFindAssistsByPlayer_ValidPlayer_ReturnsPlayerAssists() {
        // When
        List<MatchEvent> results = matchEventRepository.findAssistsByPlayer(testPlayer2);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTipoEvento()).isEqualTo(EventType.ASISTENCIA);
    }

    @Test
    void testFindByAssistPlayer_ValidAssistant_ReturnsEventsWherePlayerAssisted() {
        // When
        List<MatchEvent> results = matchEventRepository.findByAssistPlayer(testPlayer2);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTipoEvento()).isEqualTo(EventType.GOL);
        assertThat(results.get(0).getPlayer()).isEqualTo(testPlayer1);
    }

    @Test
    void testFindConfirmedEventsByMatch_ValidMatch_ReturnsOnlyConfirmedEvents() {
        // When
        List<MatchEvent> results = matchEventRepository.findConfirmedEventsByMatch(testMatch1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTipoEvento()).isEqualTo(EventType.GOL);
        assertThat(results.get(0).getConfirmedByHome()).isTrue();
        assertThat(results.get(0).getConfirmedByAway()).isTrue();
    }

    @Test
    void testFindPendingEventsByMatch_ValidMatch_ReturnsOnlyPendingEvents() {
        // When
        List<MatchEvent> results = matchEventRepository.findPendingEventsByMatch(testMatch1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTipoEvento()).isEqualTo(EventType.ASISTENCIA);
        assertThat(results.get(0).getConfirmedByAway()).isFalse();
    }

    @Test
    void testCountGoalsByPlayer_ValidPlayer_ReturnsCorrectCount() {
        // When
        long goalCount = matchEventRepository.countGoalsByPlayer(testPlayer1);

        // Then
        assertThat(goalCount).isEqualTo(1);
    }

    @Test
    void testCountAssistsByPlayer_ValidPlayer_ReturnsCorrectCount() {
        // When
        long assistCount = matchEventRepository.countAssistsByPlayer(testPlayer2);

        // Then
        assertThat(assistCount).isEqualTo(1);
    }

    @Test
    void testCountByMatchAndTipoEvento_ValidMatchAndType_ReturnsCorrectCount() {
        // When
        long goalCount = matchEventRepository.countByMatchAndTipoEvento(testMatch1, EventType.GOL);
        long assistCount = matchEventRepository.countByMatchAndTipoEvento(testMatch1, EventType.ASISTENCIA);

        // Then
        assertThat(goalCount).isEqualTo(1);
        assertThat(assistCount).isEqualTo(1);
    }

    @Test
    void testFindTopScorersOrderByGoalCount_ReturnsPlayersOrderedByGoals() {
        // When
        List<PlayerProfile> topScorers = matchEventRepository.findTopScorersOrderByGoalCount();

        // Then
        assertThat(topScorers).hasSize(2);
        // Both testPlayer1 and testPlayer3 have 1 goal each
        assertThat(topScorers).containsExactlyInAnyOrder(testPlayer1, testPlayer3);
    }

    @Test
    void testFindTopAssistersOrderByAssistCount_ReturnsPlayersOrderedByAssists() {
        // When
        List<PlayerProfile> topAssisters = matchEventRepository.findTopAssistersOrderByAssistCount();

        // Then
        assertThat(topAssisters).hasSize(1);
        assertThat(topAssisters.get(0)).isEqualTo(testPlayer2);
    }

    @Test
    void testFindByPlayerAtletaUuid_ValidUuid_ReturnsPlayerEvents() {
        // When
        List<MatchEvent> results = matchEventRepository.findByPlayerAtletaUuid(testPlayer1.getAtletaUuid());

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPlayer()).isEqualTo(testPlayer1);
    }

    @Test
    void testFindMatchesOrderByGoalCount_ReturnsMatchesOrderedByGoals() {
        // When
        List<Match> matches = matchEventRepository.findMatchesOrderByGoalCount();

        // Then
        assertThat(matches).hasSize(2);
        // Both matches have 1 goal each
        assertThat(matches).containsExactlyInAnyOrder(testMatch1, testMatch2);
    }

    @Test
    void testGetEventStatsByPlayer_ValidPlayer_ReturnsCorrectStats() {
        // When
        Object[] stats = matchEventRepository.getEventStatsByPlayer(testPlayer1);

        // Then
        assertThat(stats).hasSize(3);
        assertThat(stats[0]).isEqualTo(1L); // total_goles
        assertThat(stats[1]).isEqualTo(0L); // total_asistencias
        assertThat(stats[2]).isEqualTo(1L); // total_eventos
    }

    @Test
    void testFindEventsPendingLocalConfirmation_ValidMatch_ReturnsPendingLocalEvents() {
        // When
        List<MatchEvent> results = matchEventRepository.findEventsPendingLocalConfirmation(testMatch2);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getConfirmedByHome()).isFalse();
    }

    @Test
    void testFindEventsPendingVisitorConfirmation_ValidMatch_ReturnsPendingVisitorEvents() {
        // When
        List<MatchEvent> results = matchEventRepository.findEventsPendingVisitorConfirmation(testMatch1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getConfirmedByAway()).isFalse();
    }

    @Test
    void testSaveAndFindById_ValidEvent_SavesAndRetrievesCorrectly() {
        // Given
        MatchEvent newEvent = new MatchEvent(testMatch2, EventType.GOL, testPlayer2, testTeam2, testPlayer2);
        newEvent.setConfirmedByHome(true);
        newEvent.setConfirmedByAway(true);

        // When
        MatchEvent saved = matchEventRepository.save(newEvent);
        Optional<MatchEvent> found = matchEventRepository.findById(saved.getId());

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getTipoEvento()).isEqualTo(EventType.GOL);
        assertThat(found.get().getPlayer()).isEqualTo(testPlayer2);
    }

    @Test
    void testDeleteById_ExistingEvent_DeletesSuccessfully() {
        // Given
        Long eventId = testEvent1.getId();

        // When
        matchEventRepository.deleteById(eventId);

        // Then
        Optional<MatchEvent> found = matchEventRepository.findById(eventId);
        assertThat(found).isEmpty();
        assertThat(matchEventRepository.count()).isEqualTo(2);
    }

    @Test
    void testFindAll_ReturnsAllEvents() {
        // When
        List<MatchEvent> results = matchEventRepository.findAll();

        // Then
        assertThat(results).hasSize(3);
    }
}