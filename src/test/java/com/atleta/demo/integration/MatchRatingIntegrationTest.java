package com.atleta.demo.integration;

import com.atleta.demo.dto.request.PlayerPerformanceDto;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchEvent;
import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.entity.MatchTeam;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Position;
import com.atleta.demo.entity.Team;
import com.atleta.demo.enums.EventType;
import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchStatus;
import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.repository.MatchEventRepository;
import com.atleta.demo.repository.MatchPlayerRepository;
import com.atleta.demo.repository.MatchRepository;
import com.atleta.demo.repository.MatchTeamRepository;
import com.atleta.demo.repository.PlayerHistoryRepository;
import com.atleta.demo.repository.PlayerPositionRepository;
import com.atleta.demo.repository.PlayerProfileRepository;
import com.atleta.demo.repository.PositionRepository;
import com.atleta.demo.repository.TeamMemberRepository;
import com.atleta.demo.repository.TeamRepository;
import com.atleta.demo.service.MatchFinalScoreService;
import com.atleta.demo.service.MatchPendingEventClosureService;
import com.atleta.demo.service.MatchPlayerHistoryService;
import com.atleta.demo.service.MatchService;
import com.atleta.demo.service.MatchStatusPolicy;
import com.atleta.demo.service.RatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test de integracion para verificar que el sistema de calificaciones
 * se integra correctamente con el flujo de finalizacion de partidos.
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
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private PlayerPositionRepository playerPositionRepository;
    @Mock
    private PlayerHistoryRepository playerHistoryRepository;
    @Mock
    private RatingService ratingService;

    private MatchService matchService;

    private Match sampleMatch;
    private List<MatchTeam> matchTeams;
    private List<MatchPlayer> matchPlayers;
    private List<MatchEvent> matchEvents;
    private PlayerProfile creator;

    @BeforeEach
    void setUp() {
        matchService = new MatchService(
                matchRepository,
                matchTeamRepository,
                matchPlayerRepository,
                matchEventRepository,
                playerProfileRepository,
                teamRepository,
                positionRepository,
                teamMemberRepository,
                playerPositionRepository,
                ratingService,
                new MatchStatusPolicy(),
                new MatchFinalScoreService(
                        matchEventRepository,
                        matchPlayerRepository,
                        matchTeamRepository
                ),
                new MatchPlayerHistoryService(
                        matchPlayerRepository,
                        matchEventRepository,
                        matchTeamRepository,
                        playerHistoryRepository,
                        playerPositionRepository
                ),
                new MatchPendingEventClosureService(
                        matchEventRepository,
                        matchTeamRepository
                )
        );

        creator = new PlayerProfile();
        creator.setAtletaUuid(UUID.randomUUID());
        creator.setAlias("Creador");

        sampleMatch = new Match();
        sampleMatch.setId(1L);
        sampleMatch.setCreador(creator);
        sampleMatch.setModalidad(MatchMode.CINCO_VS_CINCO);

        Team localTeam = new Team();
        localTeam.setId(1L);
        localTeam.setNombre("Equipo Local");

        Team visitingTeam = new Team();
        visitingTeam.setId(2L);
        visitingTeam.setNombre("Equipo Visitante");

        MatchTeam localMatchTeam = new MatchTeam(sampleMatch, localTeam, true);
        localMatchTeam.setGoles(2);
        MatchTeam visitingMatchTeam = new MatchTeam(sampleMatch, visitingTeam, false);
        visitingMatchTeam.setGoles(1);
        matchTeams = List.of(localMatchTeam, visitingMatchTeam);
        sampleMatch.setMatchTeams(new ArrayList<>(matchTeams));

        Position attackPosition = new Position();
        attackPosition.setId(1L);
        attackPosition.setNombre("Delantero");

        Position defensePosition = new Position();
        defensePosition.setId(2L);
        defensePosition.setNombre("Defensa");

        matchPlayers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            PlayerProfile localPlayer = new PlayerProfile();
            localPlayer.setAtletaUuid(UUID.randomUUID());
            localPlayer.setAlias("Local " + i);
            MatchPlayer matchPlayer = new MatchPlayer(sampleMatch, localTeam, localPlayer, attackPosition, PlayerRole.JUGADOR);
            matchPlayer.setConfirmado(true);
            matchPlayers.add(matchPlayer);
        }

        for (int i = 0; i < 4; i++) {
            PlayerProfile awayPlayer = new PlayerProfile();
            awayPlayer.setAtletaUuid(UUID.randomUUID());
            awayPlayer.setAlias("Visitante " + i);
            MatchPlayer matchPlayer = new MatchPlayer(sampleMatch, visitingTeam, awayPlayer, defensePosition, PlayerRole.JUGADOR);
            matchPlayer.setConfirmado(true);
            matchPlayers.add(matchPlayer);
        }

        MatchPlayer creatorParticipation = new MatchPlayer(sampleMatch, localTeam, creator, attackPosition, PlayerRole.CAPITAN);
        creatorParticipation.setConfirmado(true);
        matchPlayers.add(creatorParticipation);

        PlayerProfile scorer = matchPlayers.get(0).getPlayer();
        PlayerProfile assister = matchPlayers.get(1).getPlayer();

        MatchEvent goal1 = new MatchEvent(sampleMatch, EventType.GOL, scorer, localTeam, scorer);
        goal1.setConfirmedByHome(true);
        goal1.setConfirmedByAway(true);

        MatchEvent goal2 = new MatchEvent(sampleMatch, EventType.GOL, scorer, localTeam, scorer);
        goal2.setConfirmedByHome(true);
        goal2.setConfirmedByAway(true);
        goal2.setAssistPlayer(assister);

        matchEvents = List.of(goal1, goal2);
    }

    @Test
    void changeMatchStatus_ToFinalizado_ShouldTriggerRatingUpdate() {
        sampleMatch.setEstado(MatchStatus.INICIADO);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(sampleMatch));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchTeamRepository.findByMatch(any(Match.class))).thenReturn(matchTeams);
        when(matchPlayerRepository.findByMatch(any(Match.class))).thenReturn(matchPlayers);
        when(matchEventRepository.findByMatchOrderByCreatedAt(any(Match.class))).thenReturn(matchEvents);
        when(matchEventRepository.findPendingEventsByMatch(any(Match.class))).thenReturn(List.of());

        matchService.changeMatchStatus(1L, MatchStatus.FINALIZADO);

        ArgumentCaptor<Long> matchIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<List<PlayerPerformanceDto>> performanceCaptor = ArgumentCaptor.forClass(List.class);

        verify(ratingService, times(1)).updatePlayerRatings(matchIdCaptor.capture(), performanceCaptor.capture());
        assertEquals(1L, matchIdCaptor.getValue());
        assertEquals(matchPlayers.size(), performanceCaptor.getValue().size());
    }

    @Test
    void changeMatchStatus_ToFinalizado_WithNoPlayers_ShouldNotCallRatingService() {
        sampleMatch.setEstado(MatchStatus.INICIADO);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(sampleMatch));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchPlayerRepository.findByMatch(sampleMatch)).thenReturn(List.of());

        matchService.changeMatchStatus(1L, MatchStatus.FINALIZADO);

        verify(ratingService, never()).updatePlayerRatings(any(), any());
    }

    @Test
    void changeMatchStatus_ToIniciado_ShouldNotTriggerRatingUpdate() {
        sampleMatch.setEstado(MatchStatus.CREADO);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(sampleMatch));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        matchService.changeMatchStatus(1L, MatchStatus.INICIADO);

        verify(ratingService, never()).updatePlayerRatings(any(), any());
    }

    @Test
    void changeMatchStatus_RatingServiceThrowsException_ShouldNotFailStatusChange() {
        sampleMatch.setEstado(MatchStatus.INICIADO);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(sampleMatch));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchTeamRepository.findByMatch(sampleMatch)).thenReturn(matchTeams);
        when(matchPlayerRepository.findByMatch(sampleMatch)).thenReturn(matchPlayers);
        when(matchEventRepository.findByMatchOrderByCreatedAt(sampleMatch)).thenReturn(matchEvents);
        when(matchEventRepository.findPendingEventsByMatch(sampleMatch)).thenReturn(List.of());
        doThrow(new RuntimeException("Error en calificaciones")).when(ratingService).updatePlayerRatings(any(), any());

        assertDoesNotThrow(() -> matchService.changeMatchStatus(1L, MatchStatus.FINALIZADO));

        verify(matchRepository, times(1)).save(any(Match.class));
    }
}
