package com.atleta.demo.service;

import com.atleta.demo.dto.request.CreateMatchRequest;
import com.atleta.demo.dto.request.CreateMatchEventRequest;
import com.atleta.demo.dto.request.JoinMatchRequest;
import com.atleta.demo.dto.request.MatchClosePreviewRequest;
import com.atleta.demo.dto.response.MatchResponse;
import com.atleta.demo.dto.response.MatchPlayerResponse;
import com.atleta.demo.entity.*;
import com.atleta.demo.enums.EventType;
import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchStatus;
import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para MatchService.
 * Valida la lógica de negocio para gestión de partidos, participación y eventos.
 */
@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

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

    @Spy
    private MatchRosterPolicy matchRosterPolicy = new MatchRosterPolicy();

    private MatchStatusPolicy matchStatusPolicy;
    private MatchFinalScoreService matchFinalScoreService;
    private MatchPlayerHistoryService matchPlayerHistoryService;
    private MatchPendingEventClosureService matchPendingEventClosureService;

    private MatchService matchService;

    private CreateMatchRequest validCreateRequest;
    private JoinMatchRequest validJoinRequest;
    private PlayerProfile samplePlayer;
    private Team sampleTeam;
    private Position samplePosition;
    private Match sampleMatch;

    @Test
    void liveStreamAllowsCreator() {
        when(matchRepository.findById(1L)).thenReturn(Optional.of(sampleMatch));
        when(matchPlayerRepository.findByMatchAndPlayerAtletaUuid(sampleMatch, samplePlayer.getAtletaUuid()))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> matchService.requireLiveStreamAccess(1L, samplePlayer.getAtletaUuid()));
    }

    @Test
    void liveStreamAllowsParticipant() {
        PlayerProfile participant = new PlayerProfile();
        participant.setAtletaUuid(UUID.randomUUID());
        when(matchRepository.findById(1L)).thenReturn(Optional.of(sampleMatch));
        when(matchPlayerRepository.findByMatchAndPlayerAtletaUuid(sampleMatch, participant.getAtletaUuid()))
                .thenReturn(Optional.of(new MatchPlayer()));

        assertDoesNotThrow(() -> matchService.requireLiveStreamAccess(1L, participant.getAtletaUuid()));
    }

    @Test
    void liveStreamRejectsOutsider() {
        UUID outsider = UUID.randomUUID();
        when(matchRepository.findById(1L)).thenReturn(Optional.of(sampleMatch));
        when(matchPlayerRepository.findByMatchAndPlayerAtletaUuid(sampleMatch, outsider))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> matchService.requireLiveStreamAccess(1L, outsider));
    }

    @BeforeEach
    void setUp() {
        matchStatusPolicy = new MatchStatusPolicy();
        matchFinalScoreService = new MatchFinalScoreService(
                matchEventRepository,
                matchPlayerRepository,
                matchTeamRepository
        );
        matchPlayerHistoryService = new MatchPlayerHistoryService(
                matchPlayerRepository,
                matchEventRepository,
                matchTeamRepository,
                playerHistoryRepository,
                playerPositionRepository
        );
        matchPendingEventClosureService = new MatchPendingEventClosureService(
                matchEventRepository,
                matchTeamRepository
        );
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
                matchStatusPolicy,
                matchFinalScoreService,
                matchPlayerHistoryService,
                matchPendingEventClosureService,
                matchRosterPolicy,
                new MatchPostMatchRatingService(
                        matchTeamRepository,
                        matchPlayerRepository,
                        matchEventRepository,
                        ratingService
                ),
                new MatchResponseMapper(
                        matchTeamRepository,
                        matchPlayerRepository,
                        matchEventRepository,
                        matchStatusPolicy
                ),
                new MatchQueryService(
                        matchRepository,
                        matchTeamRepository,
                        matchPlayerRepository,
                        matchEventRepository,
                        playerProfileRepository,
                        teamRepository,
                        positionRepository,
                        playerPositionRepository,
                        new MatchAutomatedStatusService(
                                matchRepository,
                                matchTeamRepository,
                                matchPlayerRepository,
                                matchPlayerHistoryService,
                                new MatchRosterPolicy()
                        ),
                        new MatchResponseMapper(
                                matchTeamRepository,
                                matchPlayerRepository,
                                matchEventRepository,
                                matchStatusPolicy
                        )
                )
        );

        UUID playerId = UUID.randomUUID();
        
        samplePlayer = new PlayerProfile();
        samplePlayer.setAtletaUuid(playerId);
        samplePlayer.setAlias("TestPlayer");
        samplePlayer.setTrustScore(100);

        sampleTeam = new Team();
        sampleTeam.setId(1L);
        sampleTeam.setNombre("Test Team");
        sampleTeam.setCreador(samplePlayer);

        samplePosition = new Position();
        samplePosition.setId(1L);
        samplePosition.setNombre("Delantero");

        validCreateRequest = new CreateMatchRequest();
        validCreateRequest.setModalidad(MatchMode.CINCO_VS_CINCO);
        validCreateRequest.setFechaHoraProgramada(LocalDateTime.now().plusDays(1));
        validCreateRequest.setLatitud(new BigDecimal("40.7128"));
        validCreateRequest.setLongitud(new BigDecimal("-74.0060"));
        validCreateRequest.setCuota(new BigDecimal("25.00"));
        validCreateRequest.setCreadorUuid(playerId);

        sampleMatch = new Match();
        sampleMatch.setId(1L);
        sampleMatch.setModalidad(MatchMode.CINCO_VS_CINCO);
        sampleMatch.setFechaHoraProgramada(validCreateRequest.getFechaHoraProgramada());
        sampleMatch.setLatitud(validCreateRequest.getLatitud());
        sampleMatch.setLongitud(validCreateRequest.getLongitud());
        sampleMatch.setCuota(validCreateRequest.getCuota());
        sampleMatch.setCreador(samplePlayer);
        sampleMatch.setEstado(MatchStatus.CREADO);

        validJoinRequest = new JoinMatchRequest();
        validJoinRequest.setPlayerUuid(playerId);
        validJoinRequest.setMatchId(1L);
        validJoinRequest.setTeamId(1L);
        validJoinRequest.setPositionId(1L);
        validJoinRequest.setRol(PlayerRole.JUGADOR);
    }

    @Test
    void createMatch_ValidRequest_ShouldCreateMatch() {
        // Arrange
        when(playerProfileRepository.findById(validCreateRequest.getCreadorUuid())).thenReturn(Optional.of(samplePlayer));
        when(matchRepository.save(any(Match.class))).thenReturn(sampleMatch);
        when(matchTeamRepository.findByMatch(sampleMatch)).thenReturn(java.util.Collections.emptyList());
        when(matchPlayerRepository.findByMatch(sampleMatch)).thenReturn(java.util.Collections.emptyList());
        when(matchEventRepository.findByMatchOrderByCreatedAt(sampleMatch)).thenReturn(java.util.Collections.emptyList());

        // Act
        MatchResponse response = matchService.createMatch(validCreateRequest);

        // Assert
        assertNotNull(response);
        assertEquals(sampleMatch.getModalidad(), response.getModalidad());
        assertEquals(sampleMatch.getFechaHoraProgramada(), response.getFechaHoraProgramada());
        assertEquals(sampleMatch.getLatitud(), response.getLatitud());
        assertEquals(sampleMatch.getLongitud(), response.getLongitud());
        assertEquals(sampleMatch.getCuota(), response.getCuota());
        assertEquals(MatchStatus.CREADO, response.getEstado());

        verify(playerProfileRepository).findById(validCreateRequest.getCreadorUuid());
        verify(matchRepository).save(any(Match.class));
    }

    @Test
    void createMatch_CreatorNotFound_ShouldThrowException() {
        // Arrange
        when(playerProfileRepository.findById(validCreateRequest.getCreadorUuid())).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.createMatch(validCreateRequest)
        );

        assertEquals("Creador no encontrado: " + validCreateRequest.getCreadorUuid(), exception.getMessage());
        verify(playerProfileRepository).findById(validCreateRequest.getCreadorUuid());
        verify(matchRepository, never()).save(any(Match.class));
    }

    @Test
    void joinMatch_ValidRequest_ShouldAddPlayerToMatch() {
        // Arrange
        when(matchRepository.findById(validJoinRequest.getMatchId())).thenReturn(Optional.of(sampleMatch));
        when(playerProfileRepository.findById(validJoinRequest.getPlayerUuid())).thenReturn(Optional.of(samplePlayer));
        when(teamRepository.findById(validJoinRequest.getTeamId())).thenReturn(Optional.of(sampleTeam));
        when(positionRepository.findById(validJoinRequest.getPositionId())).thenReturn(Optional.of(samplePosition));
        when(matchPlayerRepository.existsByMatchAndPlayer(sampleMatch, samplePlayer)).thenReturn(false);
        when(matchTeamRepository.existsByMatchAndTeam(sampleMatch, sampleTeam)).thenReturn(true);
        
        MatchPlayer newMatchPlayer = new MatchPlayer(sampleMatch, sampleTeam, samplePlayer, samplePosition, validJoinRequest.getRol());
        newMatchPlayer.setId(1L);
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenReturn(newMatchPlayer);

        // Act
        MatchPlayerResponse response = matchService.joinMatch(validJoinRequest);

        // Assert
        assertNotNull(response);
        assertEquals(validJoinRequest.getRol(), response.getRol());
        assertFalse(response.getConfirmado()); // Initially not confirmed

        verify(matchRepository).findById(validJoinRequest.getMatchId());
        verify(playerProfileRepository).findById(validJoinRequest.getPlayerUuid());
        verify(teamRepository).findById(validJoinRequest.getTeamId());
        verify(positionRepository).findById(validJoinRequest.getPositionId());
        verify(matchPlayerRepository).existsByMatchAndPlayer(sampleMatch, samplePlayer);
        verify(matchTeamRepository).existsByMatchAndTeam(sampleMatch, sampleTeam);
        verify(matchPlayerRepository).save(any(MatchPlayer.class));
    }

    @Test
    void joinMatch_MatchNotFound_ShouldThrowException() {
        // Arrange
        when(matchRepository.findById(validJoinRequest.getMatchId())).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.joinMatch(validJoinRequest)
        );

        assertEquals("Partido no encontrado: " + validJoinRequest.getMatchId(), exception.getMessage());
        verify(matchRepository).findById(validJoinRequest.getMatchId());
        verify(playerProfileRepository, never()).findById(any());
    }

    @Test
    void joinMatch_PlayerAlreadyRegistered_ShouldThrowException() {
        // Arrange
        when(matchRepository.findById(validJoinRequest.getMatchId())).thenReturn(Optional.of(sampleMatch));
        when(playerProfileRepository.findById(validJoinRequest.getPlayerUuid())).thenReturn(Optional.of(samplePlayer));
        when(teamRepository.findById(validJoinRequest.getTeamId())).thenReturn(Optional.of(sampleTeam));
        when(positionRepository.findById(validJoinRequest.getPositionId())).thenReturn(Optional.of(samplePosition));
        when(matchPlayerRepository.existsByMatchAndPlayer(sampleMatch, samplePlayer)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.joinMatch(validJoinRequest)
        );

        assertEquals("El jugador ya está registrado en este partido", exception.getMessage());
        verify(matchPlayerRepository).existsByMatchAndPlayer(sampleMatch, samplePlayer);
        verify(matchPlayerRepository, never()).save(any(MatchPlayer.class));
    }

    @Test
    void joinMatch_TeamNotInMatch_ShouldThrowException() {
        // Arrange
        when(matchRepository.findById(validJoinRequest.getMatchId())).thenReturn(Optional.of(sampleMatch));
        when(playerProfileRepository.findById(validJoinRequest.getPlayerUuid())).thenReturn(Optional.of(samplePlayer));
        when(teamRepository.findById(validJoinRequest.getTeamId())).thenReturn(Optional.of(sampleTeam));
        when(positionRepository.findById(validJoinRequest.getPositionId())).thenReturn(Optional.of(samplePosition));
        when(matchPlayerRepository.existsByMatchAndPlayer(sampleMatch, samplePlayer)).thenReturn(false);
        when(matchTeamRepository.existsByMatchAndTeam(sampleMatch, sampleTeam)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.joinMatch(validJoinRequest)
        );

        assertEquals("El equipo no participa en este partido", exception.getMessage());
        verify(matchTeamRepository).existsByMatchAndTeam(sampleMatch, sampleTeam);
        verify(matchPlayerRepository, never()).save(any(MatchPlayer.class));
    }

    @Test
    void confirmParticipation_ValidRequest_ShouldConfirmPlayer() {
        // Arrange
        Long matchId = 1L;
        UUID playerUuid = samplePlayer.getAtletaUuid();
        MatchPlayer matchPlayer = new MatchPlayer(sampleMatch, sampleTeam, samplePlayer, samplePosition, PlayerRole.JUGADOR);
        matchPlayer.setId(1L);
        matchPlayer.setConfirmado(false);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(sampleMatch));
        when(playerProfileRepository.findById(playerUuid)).thenReturn(Optional.of(samplePlayer));
        when(matchPlayerRepository.findByMatchAndPlayer(sampleMatch, samplePlayer)).thenReturn(Optional.of(matchPlayer));
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenReturn(matchPlayer);

        // Act
        MatchPlayerResponse response = matchService.confirmParticipation(matchId, playerUuid);

        // Assert
        assertNotNull(response);
        assertTrue(response.getConfirmado());

        verify(matchRepository).findById(matchId);
        verify(playerProfileRepository).findById(playerUuid);
        verify(matchPlayerRepository).findByMatchAndPlayer(sampleMatch, samplePlayer);
        verify(matchPlayerRepository).save(matchPlayer);
    }

    @Test
    void addTeamToMatch_ValidRequest_ShouldAddTeam() {
        // Arrange
        Long matchId = 1L;
        Long teamId = 1L;
        Boolean esLocal = true;

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(sampleMatch));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(sampleTeam));
        when(matchTeamRepository.countByMatch(sampleMatch)).thenReturn(0L);
        when(matchTeamRepository.findLocalTeamByMatch(sampleMatch)).thenReturn(Optional.empty());
        when(matchTeamRepository.existsByMatchAndTeam(sampleMatch, sampleTeam)).thenReturn(false);
        when(matchPlayerRepository.existsByMatchAndPlayer(sampleMatch, samplePlayer)).thenReturn(false);
        when(positionRepository.findAllOrderByNombre()).thenReturn(List.of(samplePosition));
        when(matchTeamRepository.save(any(MatchTeam.class))).thenReturn(new MatchTeam());
        when(matchTeamRepository.findByMatch(sampleMatch)).thenReturn(java.util.Collections.emptyList());
        when(matchPlayerRepository.findByMatch(sampleMatch)).thenReturn(java.util.Collections.emptyList());
        when(matchEventRepository.findByMatchOrderByCreatedAt(sampleMatch)).thenReturn(java.util.Collections.emptyList());

        // Act
        MatchResponse response = matchService.addTeamToMatch(matchId, teamId, esLocal);

        // Assert
        assertNotNull(response);
        assertEquals(sampleMatch.getId(), response.getId());

        verify(matchRepository).findById(matchId);
        verify(teamRepository).findById(teamId);
        verify(matchTeamRepository).countByMatch(sampleMatch);
        verify(matchTeamRepository).findLocalTeamByMatch(sampleMatch);
        verify(matchTeamRepository).existsByMatchAndTeam(sampleMatch, sampleTeam);
        verify(matchTeamRepository).save(any(MatchTeam.class));
    }

    @Test
    void addTeamToMatch_TooManyTeams_ShouldThrowException() {
        // Arrange
        Long matchId = 1L;
        Long teamId = 1L;
        Boolean esLocal = true;

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(sampleMatch));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(sampleTeam));
        when(matchTeamRepository.countByMatch(sampleMatch)).thenReturn(2L);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.addTeamToMatch(matchId, teamId, esLocal)
        );

        assertEquals("Un partido no puede tener más de 2 equipos", exception.getMessage());
        verify(matchTeamRepository).countByMatch(sampleMatch);
        verify(matchTeamRepository, never()).save(any(MatchTeam.class));
    }

    @Test
    void changeMatchStatus_ValidTransition_ShouldChangeStatus() {
        // Arrange
        Long matchId = 1L;
        MatchStatus newStatus = MatchStatus.INICIADO;
        sampleMatch.setEstado(MatchStatus.CREADO);
        sampleMatch.getMatchTeams().add(new MatchTeam(sampleMatch, sampleTeam, true));

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(sampleMatch));
        when(matchRepository.save(any(Match.class))).thenReturn(sampleMatch);
        when(matchTeamRepository.findByMatch(sampleMatch)).thenReturn(java.util.Collections.emptyList());
        when(matchPlayerRepository.findByMatch(sampleMatch)).thenReturn(java.util.Collections.emptyList());
        doReturn(true).when(matchRosterPolicy)
                .hasMinimumConfirmedPlayers(sampleMatch, java.util.Collections.emptyList());
        when(matchEventRepository.findByMatchOrderByCreatedAt(sampleMatch)).thenReturn(java.util.Collections.emptyList());

        // Act
        MatchResponse response = matchService.changeMatchStatus(matchId, newStatus);

        // Assert
        assertNotNull(response);
        assertEquals(newStatus, response.getEstado());
        assertNotNull(response.getStartedAt()); // Should be set when status changes to INICIADO

        verify(matchRepository).findById(matchId);
        verify(matchRepository).save(sampleMatch);
    }

    @Test
    void closePreview_RejectsInvalidMatchBeforeLoadingPlayers() {
        sampleMatch.setEstado(MatchStatus.INVALIDO);
        sampleMatch.setValidationReason("Partido vencido automaticamente por no cerrarse en la ventana permitida");
        when(matchRepository.findById(1L)).thenReturn(Optional.of(sampleMatch));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> matchService.getClosePreview(1L, new MatchClosePreviewRequest())
        );

        assertEquals(sampleMatch.getValidationReason(), exception.getMessage());
        verify(matchPlayerRepository, never()).findByMatch(sampleMatch);
    }

    @Test
    void closePreview_RejectsFinalizedMatchBeforeLoadingPlayers() {
        sampleMatch.setEstado(MatchStatus.FINALIZADO);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(sampleMatch));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> matchService.getClosePreview(1L, new MatchClosePreviewRequest())
        );

        assertTrue(exception.getMessage().contains("ya fue finalizado"));
        verify(matchPlayerRepository, never()).findByMatch(sampleMatch);
    }

    @Test
    void changeMatchStatus_StartWithoutMinimumConfirmedPlayers_ShouldFail() {
        Long matchId = 2L;
        sampleMatch.setEstado(MatchStatus.CREADO);
        sampleMatch.getMatchTeams().add(new MatchTeam(sampleMatch, sampleTeam, true));

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(sampleMatch));
        when(matchPlayerRepository.findByMatch(sampleMatch)).thenReturn(java.util.Collections.emptyList());
        when(matchRosterPolicy.hasMinimumConfirmedPlayers(sampleMatch, java.util.Collections.emptyList()))
                .thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.changeMatchStatus(matchId, MatchStatus.INICIADO)
        );

        assertEquals("No se puede iniciar: faltan jugadores confirmados para la modalidad", exception.getMessage());
        verify(matchRepository, never()).save(any(Match.class));
    }

    @Test
    void registerEvent_OutsideCaptureWindow_ShouldThrowException() {
        // Arrange
        Match startedMatch = new Match();
        startedMatch.setId(99L);
        startedMatch.setEstado(MatchStatus.INICIADO);
        startedMatch.setStartedAt(LocalDateTime.now().minusHours(4));
        startedMatch.setCreador(samplePlayer);

        CreateMatchEventRequest request = new CreateMatchEventRequest();
        request.setMatchId(99L);
        request.setPlayerUuid(samplePlayer.getAtletaUuid());
        request.setTeamId(sampleTeam.getId());
        request.setEventType(EventType.GOL);
        request.setRegisteredByUuid(samplePlayer.getAtletaUuid());

        when(matchRepository.findById(99L)).thenReturn(Optional.of(startedMatch));

        // Act + Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.registerEvent(request)
        );
        assertEquals("Se supero la ventana de 3 horas para cargar/cerrar datos", exception.getMessage());
    }

    @Test
    void changeMatchStatus_FinalizeByUnauthorizedActor_ShouldThrowException() {
        // Arrange
        UUID unauthorizedUuid = UUID.randomUUID();
        Match startedMatch = new Match();
        startedMatch.setId(100L);
        startedMatch.setEstado(MatchStatus.INICIADO);
        startedMatch.setStartedAt(LocalDateTime.now().minusMinutes(10));
        startedMatch.setCreador(samplePlayer);

        when(matchRepository.findById(100L)).thenReturn(Optional.of(startedMatch));
        when(matchPlayerRepository.findCaptainsByMatch(startedMatch)).thenReturn(java.util.Collections.emptyList());

        // Act + Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.changeMatchStatus(100L, MatchStatus.FINALIZADO, unauthorizedUuid)
        );
        assertEquals("Solo el creador o capitanes pueden registrar/confirmar/finalizar", exception.getMessage());
    }

    @Test
    void changeMatchStatus_InvalidTransition_ShouldThrowException() {
        // Arrange
        Long matchId = 1L;
        MatchStatus newStatus = MatchStatus.FINALIZADO;
        sampleMatch.setEstado(MatchStatus.CREADO); // Can't go directly from CREADO to FINALIZADO

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(sampleMatch));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.changeMatchStatus(matchId, newStatus)
        );

        assertEquals("Desde CREADO solo se puede pasar a INICIADO o INVALIDO", exception.getMessage());
        verify(matchRepository).findById(matchId);
        verify(matchRepository, never()).save(any(Match.class));
    }
}
