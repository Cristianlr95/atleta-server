package com.atleta.demo.service;

import com.atleta.demo.dto.request.CreateMatchInviteRequest;
import com.atleta.demo.dto.request.CreateMatchInvitesBatchRequest;
import com.atleta.demo.dto.request.RespondRequestDecision;
import com.atleta.demo.dto.response.MatchInviteDeliveryResponse;
import com.atleta.demo.dto.response.SocialRequestResponse;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchInvite;
import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.entity.MatchTeam;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Position;
import com.atleta.demo.entity.Team;
import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.enums.RequestStatus;
import com.atleta.demo.repository.AppNotificationRepository;
import com.atleta.demo.repository.FriendshipRepository;
import com.atleta.demo.repository.MatchInviteRepository;
import com.atleta.demo.repository.MatchPlayerRepository;
import com.atleta.demo.repository.MatchRepository;
import com.atleta.demo.repository.PlayerPositionRepository;
import com.atleta.demo.repository.PlayerProfileRepository;
import com.atleta.demo.repository.PositionRepository;
import com.atleta.demo.repository.PushNotificationTokenRepository;
import com.atleta.demo.repository.TeamInviteRepository;
import com.atleta.demo.repository.TeamMemberRepository;
import com.atleta.demo.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialServiceTest {

    @Mock
    private PlayerProfileRepository playerProfileRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private TeamInviteRepository teamInviteRepository;

    @Mock
    private MatchInviteRepository matchInviteRepository;

    @Mock
    private AppNotificationRepository appNotificationRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private PlayerPositionRepository playerPositionRepository;

    @Mock
    private MatchRosterPolicy matchRosterPolicy;

    @Mock
    private MatchLiveEventService matchLiveEventService;

    @Mock
    private PushNotificationTokenRepository pushNotificationTokenRepository;

    private SocialService socialService;

    private UUID creatorUuid;
    private UUID requesterUuid;
    private UUID targetUuid;
    private PlayerProfile creator;
    private PlayerProfile requester;
    private PlayerProfile target;
    private Match match;
    private Team matchTeam;

    @BeforeEach
    void setUp() {
        socialService = new SocialService(
                playerProfileRepository,
                friendshipRepository,
                teamInviteRepository,
                matchInviteRepository,
                appNotificationRepository,
                teamRepository,
                teamMemberRepository,
                matchRepository,
                matchPlayerRepository,
                positionRepository,
                playerPositionRepository,
                matchRosterPolicy,
                matchLiveEventService,
                pushNotificationTokenRepository
        );

        creatorUuid = UUID.randomUUID();
        requesterUuid = UUID.randomUUID();
        targetUuid = UUID.randomUUID();
        creator = player(creatorUuid, "creator");
        requester = player(requesterUuid, "requester");
        target = player(targetUuid, "target");

        matchTeam = new Team("Local", creator);
        matchTeam.setId(77L);

        match = new Match(MatchMode.CINCO_VS_CINCO, LocalDateTime.now().plusDays(1), creator);
        match.setId(42L);
        match.addMatchTeam(new MatchTeam(match, matchTeam, true));
    }

    @Test
    void createMatchInvite_rejectsRequesterOutsideMatch() {
        CreateMatchInviteRequest request = validInviteRequest(requesterUuid, targetUuid, matchTeam.getId());

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(teamRepository.findById(matchTeam.getId())).thenReturn(Optional.of(matchTeam));
        when(playerProfileRepository.findById(requesterUuid)).thenReturn(Optional.of(requester));
        when(playerProfileRepository.findById(targetUuid)).thenReturn(Optional.of(target));
        when(matchPlayerRepository.findByMatchAndPlayerAtletaUuid(match, requesterUuid)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> socialService.createMatchInvite(request));

        verify(matchInviteRepository, never()).save(any(MatchInvite.class));
    }

    @Test
    void createMatchInvite_rejectsTeamOutsideMatch() {
        Team outsiderTeam = new Team("Away", creator);
        outsiderTeam.setId(88L);
        CreateMatchInviteRequest request = validInviteRequest(creatorUuid, targetUuid, outsiderTeam.getId());

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(teamRepository.findById(outsiderTeam.getId())).thenReturn(Optional.of(outsiderTeam));
        when(playerProfileRepository.findById(creatorUuid)).thenReturn(Optional.of(creator));
        when(playerProfileRepository.findById(targetUuid)).thenReturn(Optional.of(target));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> socialService.createMatchInvite(request)
        );

        assertEquals("El equipo indicado no pertenece al partido", ex.getMessage());
        verify(matchInviteRepository, never()).save(any(MatchInvite.class));
    }

    @Test
    void createMatchInvite_allowsMatchCreator() {
        CreateMatchInviteRequest request = validInviteRequest(creatorUuid, targetUuid, matchTeam.getId());
        MatchInvite savedInvite = new MatchInvite(match, matchTeam, creator, target, "sumate");
        savedInvite.setId(900L);
        savedInvite.setStatus(RequestStatus.PENDIENTE);

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(teamRepository.findById(matchTeam.getId())).thenReturn(Optional.of(matchTeam));
        when(playerProfileRepository.findById(creatorUuid)).thenReturn(Optional.of(creator));
        when(playerProfileRepository.findById(targetUuid)).thenReturn(Optional.of(target));
        when(matchInviteRepository.existsByMatchAndTargetAndStatus(match, target, RequestStatus.PENDIENTE))
                .thenReturn(false);
        when(matchInviteRepository.save(any(MatchInvite.class))).thenReturn(savedInvite);

        assertEquals(900L, socialService.createMatchInvite(request).getId());
    }

    @Test
    void createMatchInvite_allowsExistingMatchParticipant() {
        CreateMatchInviteRequest request = validInviteRequest(requesterUuid, targetUuid, matchTeam.getId());
        MatchInvite savedInvite = new MatchInvite(match, matchTeam, requester, target, "sumate");
        savedInvite.setId(901L);
        savedInvite.setStatus(RequestStatus.PENDIENTE);

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(teamRepository.findById(matchTeam.getId())).thenReturn(Optional.of(matchTeam));
        when(playerProfileRepository.findById(requesterUuid)).thenReturn(Optional.of(requester));
        when(playerProfileRepository.findById(targetUuid)).thenReturn(Optional.of(target));
        when(matchPlayerRepository.findByMatchAndPlayerAtletaUuid(match, requesterUuid))
                .thenReturn(Optional.of(new MatchPlayer(match, matchTeam, requester, new Position(), PlayerRole.JUGADOR)));
        when(matchInviteRepository.existsByMatchAndTargetAndStatus(match, target, RequestStatus.PENDIENTE))
                .thenReturn(false);
        when(matchInviteRepository.save(any(MatchInvite.class))).thenReturn(savedInvite);

        assertEquals(901L, socialService.createMatchInvite(request).getId());
    }

    @Test
    void createMatchInvitesBatchDetailed_reportsPartialFailureWithoutHidingSuccessfulDelivery() {
        UUID missingUuid = UUID.randomUUID();
        CreateMatchInvitesBatchRequest request = new CreateMatchInvitesBatchRequest();
        request.setMatchId(match.getId());
        request.setTeamId(matchTeam.getId());
        request.setRequesterUuid(creatorUuid);
        request.setTargetUuids(List.of(targetUuid, missingUuid));
        request.setMessage("sumate");

        MatchInvite savedInvite = new MatchInvite(match, matchTeam, creator, target, "sumate");
        savedInvite.setId(902L);

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(teamRepository.findById(matchTeam.getId())).thenReturn(Optional.of(matchTeam));
        when(playerProfileRepository.findById(creatorUuid)).thenReturn(Optional.of(creator));
        when(playerProfileRepository.findById(targetUuid)).thenReturn(Optional.of(target));
        when(playerProfileRepository.findById(missingUuid)).thenReturn(Optional.empty());
        when(matchInviteRepository.findTopByMatchAndTargetOrderByCreatedAtDesc(match, target))
                .thenReturn(Optional.empty());
        when(matchInviteRepository.existsByMatchAndTargetAndStatus(match, target, RequestStatus.PENDIENTE))
                .thenReturn(false);
        when(matchInviteRepository.save(any(MatchInvite.class))).thenReturn(savedInvite);

        List<MatchInviteDeliveryResponse> result = socialService.createMatchInvitesBatchDetailed(request);

        assertEquals(2, result.size());
        assertEquals(MatchInviteDeliveryResponse.DeliveryStatus.SENT, result.get(0).getStatus());
        assertEquals(902L, result.get(0).getInvitation().getId());
        assertEquals(MatchInviteDeliveryResponse.DeliveryStatus.FAILED, result.get(1).getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(result.get(1).getMessage().startsWith("Jugador no encontrado"));
    }

    @Test
    void createMatchInvitesBatchDetailed_retryDoesNotDuplicatePendingInvitation() {
        CreateMatchInvitesBatchRequest request = new CreateMatchInvitesBatchRequest();
        request.setMatchId(match.getId());
        request.setTeamId(matchTeam.getId());
        request.setRequesterUuid(creatorUuid);
        request.setTargetUuids(List.of(targetUuid));

        MatchInvite pending = new MatchInvite(match, matchTeam, creator, target, "sumate");
        pending.setId(903L);

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(teamRepository.findById(matchTeam.getId())).thenReturn(Optional.of(matchTeam));
        when(playerProfileRepository.findById(creatorUuid)).thenReturn(Optional.of(creator));
        when(playerProfileRepository.findById(targetUuid)).thenReturn(Optional.of(target));
        when(matchInviteRepository.findTopByMatchAndTargetOrderByCreatedAtDesc(match, target))
                .thenReturn(Optional.of(pending));

        List<MatchInviteDeliveryResponse> result = socialService.createMatchInvitesBatchDetailed(request);

        assertEquals(MatchInviteDeliveryResponse.DeliveryStatus.ALREADY_SENT, result.get(0).getStatus());
        assertEquals(903L, result.get(0).getInvitation().getId());
        verify(matchInviteRepository, never()).save(any(MatchInvite.class));
    }

    @Test
    void createMatchInvitesBatchDetailed_validatesRequesterBeforeReturningExistingState() {
        CreateMatchInvitesBatchRequest request = new CreateMatchInvitesBatchRequest();
        request.setMatchId(match.getId());
        request.setTeamId(matchTeam.getId());
        request.setRequesterUuid(requesterUuid);
        request.setTargetUuids(List.of(targetUuid));

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(teamRepository.findById(matchTeam.getId())).thenReturn(Optional.of(matchTeam));
        when(playerProfileRepository.findById(requesterUuid)).thenReturn(Optional.of(requester));
        when(matchPlayerRepository.findByMatchAndPlayerAtletaUuid(match, requesterUuid)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> socialService.createMatchInvitesBatchDetailed(request));
        verify(matchInviteRepository, never()).findTopByMatchAndTargetOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void respondMatchInvite_placesAcceptedResponseOnWaitlistWhenRosterIsFull() {
        MatchInvite invite = new MatchInvite(match, matchTeam, creator, target, "sumate");
        invite.setId(950L);
        RespondRequestDecision decision = new RespondRequestDecision();
        decision.setActorUuid(targetUuid);
        decision.setAccept(true);

        when(matchInviteRepository.findById(invite.getId())).thenReturn(Optional.of(invite));
        when(matchRepository.findByIdForUpdate(match.getId())).thenReturn(Optional.of(match));
        when(matchRosterPolicy.playersPerTeamByModality(MatchMode.CINCO_VS_CINCO)).thenReturn(5);
        when(matchPlayerRepository.findByMatch(match)).thenReturn(List.of());
        when(matchRosterPolicy.confirmedPlayerCount(match, List.of())).thenReturn(10L);
        when(matchInviteRepository.save(any(MatchInvite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SocialRequestResponse response = socialService.respondMatchInvite(invite.getId(), decision);

        assertEquals(RequestStatus.LISTA_ESPERA, response.getStatus());
        verify(matchPlayerRepository, never()).save(any(MatchPlayer.class));
    }

    @Test
    void respondMatchInvite_withdrawsConfirmedPlayerAndPromotesFirstWaitlisted() {
        UUID waitingUuid = UUID.randomUUID();
        PlayerProfile waitingPlayer = player(waitingUuid, "waiting");
        Position position = new Position();
        position.setId(9L);
        position.setNombre("Mediocampo");

        MatchInvite confirmedInvite = new MatchInvite(match, matchTeam, creator, target, "sumate");
        confirmedInvite.setId(951L);
        confirmedInvite.setStatus(RequestStatus.ACEPTADA);
        MatchInvite waitingInvite = new MatchInvite(match, matchTeam, creator, waitingPlayer, "sumate");
        waitingInvite.setId(952L);
        waitingInvite.setStatus(RequestStatus.LISTA_ESPERA);
        waitingInvite.setRespondedAt(LocalDateTime.now().minusMinutes(5));

        MatchPlayer confirmedPlayer = new MatchPlayer(match, matchTeam, target, position, PlayerRole.JUGADOR);
        confirmedPlayer.setConfirmado(true);

        RespondRequestDecision decision = new RespondRequestDecision();
        decision.setActorUuid(targetUuid);
        decision.setAccept(false);

        when(matchInviteRepository.findById(confirmedInvite.getId())).thenReturn(Optional.of(confirmedInvite));
        when(matchRepository.findByIdForUpdate(match.getId())).thenReturn(Optional.of(match));
        when(matchInviteRepository.save(any(MatchInvite.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchPlayerRepository.findByMatchAndPlayer(match, target)).thenReturn(Optional.of(confirmedPlayer));
        when(matchRosterPolicy.playersPerTeamByModality(MatchMode.CINCO_VS_CINCO)).thenReturn(5);
        when(matchPlayerRepository.findByMatch(match)).thenReturn(List.of(confirmedPlayer));
        when(matchRosterPolicy.confirmedPlayerCount(match, List.of(confirmedPlayer))).thenReturn(9L);
        when(matchInviteRepository.findByMatchAndStatusOrderByRespondedAtAscCreatedAtAsc(
                match, RequestStatus.LISTA_ESPERA)).thenReturn(List.of(waitingInvite));
        when(matchPlayerRepository.findByMatchAndPlayer(match, waitingPlayer)).thenReturn(Optional.empty());
        when(playerPositionRepository.findPrimaryPositionByPlayer(waitingPlayer)).thenReturn(Optional.empty());
        when(positionRepository.findAllOrderByNombre()).thenReturn(List.of(position));

        SocialRequestResponse response = socialService.respondMatchInvite(confirmedInvite.getId(), decision);

        assertEquals(RequestStatus.CANCELADA, response.getStatus());
        assertEquals(RequestStatus.ACEPTADA, waitingInvite.getStatus());
        org.junit.jupiter.api.Assertions.assertFalse(confirmedPlayer.getConfirmado());
        verify(matchPlayerRepository, times(2)).save(any(MatchPlayer.class));
        verify(matchLiveEventService).publishInviteDecision(
                eq(match.getId()), eq(waitingInvite.getId()), eq(RequestStatus.ACEPTADA.name()));
    }

    private CreateMatchInviteRequest validInviteRequest(UUID requesterUuid, UUID targetUuid, Long teamId) {
        CreateMatchInviteRequest request = new CreateMatchInviteRequest();
        request.setMatchId(match.getId());
        request.setTeamId(teamId);
        request.setRequesterUuid(requesterUuid);
        request.setTargetUuid(targetUuid);
        request.setMessage("sumate");
        return request;
    }

    private PlayerProfile player(UUID uuid, String alias) {
        PlayerProfile player = new PlayerProfile();
        player.setAtletaUuid(uuid);
        player.setAlias(alias);
        return player;
    }
}
