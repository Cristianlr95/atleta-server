package com.atleta.demo.integration;

import com.atleta.demo.dto.request.PlayerPerformanceDto;
import com.atleta.demo.entity.*;
import com.atleta.demo.enums.*;
import com.atleta.demo.repository.*;
import com.atleta.demo.service.MatchService;
import com.atleta.demo.service.RatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test de integración para verificar que el sistema de calificaciones
 * se integra correctamente con el flujo de finalización de partidos.
 */
@ExtendWith(MockitoExtension.class)
class MatchRatingIntegrationTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchTeamRepository matchTeamRepository;
    @Mock
    private MatchPlayerRepository matchPlayerRepository;
    @Mock
    private MatchEventRepository matchEventRepository;
    @Mock
    private PlayerProfileRepository playerProfileRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private PositionRepository positionRepository;
    @Mock
    private RatingService ratingService;

    @InjectMocks
    private MatchService matchService;

    private Match sampleMatch;
    private List<MatchTeam> matchTeams;
    private List<MatchPlayer> matchPlayers;
    private List<MatchEvent> matchEvents;

    @BeforeEach
    void setUp() {
        // Crear partido de muestra
        sampleMatch = new Match();
        sampleMatch.setId(1L);
        // No establecer estado aquí - se hará en cada test individual

        // Crear equipos
        Team localTeam = new Team();
        localTeam.setId(1L);
        localTeam.setNombre("Equipo Local");

        Team visitingTeam = new Team();
        visitingTeam.setId(2L);
        visitingTeam.setNombre("Equipo Visitante");

        MatchTeam localMatchTeam = new MatchTeam(sampleMatch, localTeam, true);
        localMatchTeam.setGoles(2); // Local gana 2-1

        MatchTeam visitingMatchTeam = new MatchTeam(sampleMatch, visitingTeam, false);
        visitingMatchTeam.setGoles(1);

        matchTeams = Arrays.asList(localMatchTeam, visitingMatchTeam);

        // Crear jugadores
        PlayerProfile player1 = new PlayerProfile();
        player1.setAtletaUuid(UUID.randomUUID());
        player1.setAlias("Jugador 1");

        PlayerProfile player2 = new PlayerProfile();
        player2.setAtletaUuid(UUID.randomUUID());
        player2.setAlias("Jugador 2");

        Position attackPosition = new Position();
        attackPosition.setId(1L);
        attackPosition.setNombre("Delantero");

        Position defensePosition = new Position();
        defensePosition.setId(2L);
        defensePosition.setNombre("Defensa");

        MatchPlayer matchPlayer1 = new MatchPlayer(sampleMatch, localTeam, player1, attackPosition, PlayerRole.JUGADOR);
        matchPlayer1.setConfirmado(true);

        MatchPlayer matchPlayer2 = new MatchPlayer(sampleMatch, visitingTeam, player2, defensePosition, PlayerRole.JUGADOR);
        matchPlayer2.setConfirmado(true);

        matchPlayers = Arrays.asList(matchPlayer1, matchPlayer2);

        // Crear eventos (goles)
        MatchEvent goal1 = new MatchEvent(sampleMatch, EventType.GOL, player1, localTeam, player1);
        goal1.setConfirmedByHome(true);
        goal1.setConfirmedByAway(true);

        MatchEvent goal2 = new MatchEvent(sampleMatch, EventType.GOL, player1, localTeam, player1);
        goal2.setConfirmedByHome(true);
        goal2.setConfirmedByAway(true);
        goal2.setAssistPlayer(player2); // Player2 asiste a Player1

        matchEvents = Arrays.asList(goal1, goal2);
    }

    @Test
    void changeMatchStatus_ToFinalizado_ShouldTriggerRatingUpdate() {
        // Arrange - Create fresh match with correct status
        Match testMatch = new Match();
        testMatch.setId(1L);
        testMatch.setEstado(MatchStatus.INICIADO); // Correct initial status for transition to FINALIZADO
        
        // Mock the save method to return the match with FINALIZADO status
        Match savedMatch = new Match();
        savedMatch.setId(1L);
        savedMatch.setEstado(MatchStatus.FINALIZADO);
        
        when(matchRepository.findById(1L)).thenReturn(Optional.of(testMatch));
        when(matchRepository.save(any(Match.class))).thenReturn(savedMatch);
        when(matchTeamRepository.findByMatch(any(Match.class))).thenReturn(matchTeams);
        when(matchPlayerRepository.findByMatch(any(Match.class))).thenReturn(matchPlayers);
        when(matchEventRepository.findByMatchOrderByCreatedAt(any(Match.class))).thenReturn(matchEvents);

        // Act
        matchService.changeMatchStatus(1L, MatchStatus.FINALIZADO);

        // Assert
        // Verificar que se llamó al servicio de calificaciones
        ArgumentCaptor<Long> matchIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<List<PlayerPerformanceDto>> performanceCaptor = ArgumentCaptor.forClass(List.class);
        
        verify(ratingService, times(1)).updatePlayerRatings(matchIdCaptor.capture(), performanceCaptor.capture());
        
        // Verificar parámetros
        assertEquals(1L, matchIdCaptor.getValue());
        
        List<PlayerPerformanceDto> performanceData = performanceCaptor.getValue();
        assertEquals(2, performanceData.size());
    }

    @Test
    void changeMatchStatus_ToFinalizado_WithNoPlayers_ShouldNotCallRatingService() {
        // Arrange - Create fresh match with correct status
        Match testMatch = new Match();
        testMatch.setId(1L);
        testMatch.setEstado(MatchStatus.INICIADO); // Correct initial status for transition to FINALIZADO
        
        when(matchRepository.findById(1L)).thenReturn(Optional.of(testMatch));
        when(matchRepository.save(any(Match.class))).thenReturn(testMatch);
        when(matchTeamRepository.findByMatch(testMatch)).thenReturn(matchTeams);
        when(matchPlayerRepository.findByMatch(testMatch)).thenReturn(Arrays.asList()); // Sin jugadores
        when(matchEventRepository.findByMatchOrderByCreatedAt(testMatch)).thenReturn(matchEvents);

        // Act
        matchService.changeMatchStatus(1L, MatchStatus.FINALIZADO);

        // Assert
        verify(ratingService, never()).updatePlayerRatings(any(), any());
    }

    @Test
    void changeMatchStatus_ToIniciado_ShouldNotTriggerRatingUpdate() {
        // Arrange - Create fresh match with correct status
        Match testMatch = new Match();
        testMatch.setId(1L);
        testMatch.setEstado(MatchStatus.CREADO); // Correct initial status for transition to INICIADO
        
        when(matchRepository.findById(1L)).thenReturn(Optional.of(testMatch));
        when(matchRepository.save(any(Match.class))).thenReturn(testMatch);

        // Act
        matchService.changeMatchStatus(1L, MatchStatus.INICIADO);

        // Assert
        verify(ratingService, never()).updatePlayerRatings(any(), any());
    }

    @Test
    void changeMatchStatus_RatingServiceThrowsException_ShouldNotFailStatusChange() {
        // Arrange - Create fresh match with correct status
        Match testMatch = new Match();
        testMatch.setId(1L);
        testMatch.setEstado(MatchStatus.INICIADO); // Correct initial status for transition to FINALIZADO
        
        when(matchRepository.findById(1L)).thenReturn(Optional.of(testMatch));
        when(matchRepository.save(any(Match.class))).thenReturn(testMatch);
        when(matchTeamRepository.findByMatch(testMatch)).thenReturn(matchTeams);
        when(matchPlayerRepository.findByMatch(testMatch)).thenReturn(matchPlayers);
        when(matchEventRepository.findByMatchOrderByCreatedAt(testMatch)).thenReturn(matchEvents);
        
        // Simular error en el servicio de calificaciones
        doThrow(new RuntimeException("Error en calificaciones")).when(ratingService)
                .updatePlayerRatings(any(), any());

        // Act & Assert
        assertDoesNotThrow(() -> {
            matchService.changeMatchStatus(1L, MatchStatus.FINALIZADO);
        });
        
        // Verificar que el estado del partido se cambió a pesar del error
        verify(matchRepository, times(1)).save(any(Match.class));
    }
}