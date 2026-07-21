package com.atleta.demo.service;

import com.atleta.demo.dto.request.CreateFriendRequest;
import com.atleta.demo.dto.request.CreateMatchInviteRequest;
import com.atleta.demo.dto.request.CreateMatchInvitesBatchRequest;
import com.atleta.demo.dto.response.MatchInviteDeliveryResponse;
import com.atleta.demo.dto.response.MatchInviteDeliveryResponse.DeliveryStatus;
import com.atleta.demo.dto.request.CreateTeamInviteRequest;
import com.atleta.demo.dto.request.RegisterPushTokenRequest;
import com.atleta.demo.dto.request.RespondRequestDecision;
import com.atleta.demo.dto.response.AppNotificationResponse;
import com.atleta.demo.dto.response.PushTokenResponse;
import com.atleta.demo.dto.response.SocialPlayerLookupResponse;
import com.atleta.demo.dto.response.SocialRequestResponse;
import com.atleta.demo.dto.response.UnreadNotificationCountResponse;
import com.atleta.demo.entity.*;
import com.atleta.demo.enums.NotificationType;
import com.atleta.demo.enums.MatchTeamSide;
import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.enums.RequestStatus;
import com.atleta.demo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class SocialService {
    private static final Logger logger = LoggerFactory.getLogger(SocialService.class);
    private final PlayerProfileRepository playerProfileRepository;
    private final FriendshipRepository friendshipRepository;
    private final TeamInviteRepository teamInviteRepository;
    private final MatchInviteRepository matchInviteRepository;
    private final AppNotificationRepository appNotificationRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final PositionRepository positionRepository;
    private final PlayerPositionRepository playerPositionRepository;
    private final MatchLiveEventService matchLiveEventService;
    private final PushNotificationTokenRepository pushNotificationTokenRepository;

    public SocialService(
            PlayerProfileRepository playerProfileRepository,
            FriendshipRepository friendshipRepository,
            TeamInviteRepository teamInviteRepository,
            MatchInviteRepository matchInviteRepository,
            AppNotificationRepository appNotificationRepository,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            MatchRepository matchRepository,
            MatchPlayerRepository matchPlayerRepository,
            PositionRepository positionRepository,
            PlayerPositionRepository playerPositionRepository,
            MatchLiveEventService matchLiveEventService,
            PushNotificationTokenRepository pushNotificationTokenRepository
    ) {
        this.playerProfileRepository = playerProfileRepository;
        this.friendshipRepository = friendshipRepository;
        this.teamInviteRepository = teamInviteRepository;
        this.matchInviteRepository = matchInviteRepository;
        this.appNotificationRepository = appNotificationRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.matchRepository = matchRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.positionRepository = positionRepository;
        this.playerPositionRepository = playerPositionRepository;
        this.matchLiveEventService = matchLiveEventService;
        this.pushNotificationTokenRepository = pushNotificationTokenRepository;
    }

    public SocialRequestResponse createFriendRequest(CreateFriendRequest request) {
        PlayerProfile requester = getPlayer(request.getRequesterUuid());
        PlayerProfile target = getPlayer(request.getTargetUuid());
        validateDistinctUsers(requester.getAtletaUuid(), target.getAtletaUuid());

        if (friendshipRepository.existsPairWithStatus(requester, target, RequestStatus.ACEPTADA)) {
            throw new IllegalArgumentException("Ya son amigos");
        }
        if (friendshipRepository.existsPairWithStatus(requester, target, RequestStatus.PENDIENTE)) {
            throw new IllegalArgumentException("Ya existe una solicitud pendiente entre estos jugadores");
        }

        Friendship friendship = friendshipRepository.save(new Friendship(requester, target));
        createNotification(
                target,
                NotificationType.SOLICITUD_AMISTAD,
                "Nueva solicitud de amistad",
                aliasOf(requester) + " quiere agregarte como amigo",
                "FRIENDSHIP_REQUEST",
                friendship.getId()
        );

        return toSocialResponse(friendship);
    }

    public SocialRequestResponse respondFriendRequest(Long requestId, RespondRequestDecision decision) {
        Friendship friendship = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud de amistad no encontrada"));
        if (!friendship.getTarget().getAtletaUuid().equals(decision.getActorUuid())) {
            throw new IllegalArgumentException("Solo el receptor puede responder la solicitud");
        }
        if (friendship.getStatus() != RequestStatus.PENDIENTE) {
            throw new IllegalArgumentException("La solicitud ya fue respondida");
        }

        friendship.setStatus(Boolean.TRUE.equals(decision.getAccept()) ? RequestStatus.ACEPTADA : RequestStatus.RECHAZADA);
        friendship.setRespondedAt(LocalDateTime.now());
        friendship = friendshipRepository.save(friendship);

        createNotification(
                friendship.getRequester(),
                NotificationType.RESPUESTA_AMISTAD,
                "Solicitud de amistad actualizada",
                aliasOf(friendship.getTarget()) + (Boolean.TRUE.equals(decision.getAccept()) ? " acepto tu solicitud" : " rechazo tu solicitud"),
                "FRIENDSHIP_REQUEST",
                friendship.getId()
        );

        return toSocialResponse(friendship);
    }

    @Transactional(readOnly = true)
    public List<SocialRequestResponse> getFriendships(UUID playerUuid) {
        PlayerProfile player = getPlayer(playerUuid);
        return friendshipRepository.findByPlayer(player).stream()
                .map(this::toSocialResponse)
                .toList();
    }

    public SocialRequestResponse createTeamInvite(CreateTeamInviteRequest request) {
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado"));
        PlayerProfile requester = getPlayer(request.getRequesterUuid());
        PlayerProfile target = getPlayer(request.getTargetUuid());
        validateDistinctUsers(requester.getAtletaUuid(), target.getAtletaUuid());

        if (!teamMemberRepository.isActiveMember(team, requester)) {
            throw new IllegalArgumentException("Solo un miembro activo del equipo puede invitar");
        }
        if (teamMemberRepository.isActiveMember(team, target)) {
            throw new IllegalArgumentException("El jugador ya pertenece al equipo");
        }
        if (teamInviteRepository.existsByTeamAndTargetAndStatus(team, target, RequestStatus.PENDIENTE)) {
            throw new IllegalArgumentException("Ya existe invitacion pendiente para este jugador y equipo");
        }

        TeamInvite invite = teamInviteRepository.save(new TeamInvite(team, requester, target, normalizeMessage(request.getMessage())));
        createNotification(
                target,
                NotificationType.INVITACION_EQUIPO,
                "Invitacion a equipo",
                aliasOf(requester) + " te invito al equipo " + team.getNombre(),
                "TEAM_INVITE",
                invite.getId()
        );
        return toSocialResponse(invite);
    }

    public SocialRequestResponse respondTeamInvite(Long inviteId, RespondRequestDecision decision) {
        TeamInvite invite = teamInviteRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("Invitacion de equipo no encontrada"));
        if (!invite.getTarget().getAtletaUuid().equals(decision.getActorUuid())) {
            throw new IllegalArgumentException("Solo el receptor puede responder la invitacion");
        }
        if (invite.getStatus() != RequestStatus.PENDIENTE) {
            throw new IllegalArgumentException("La invitacion ya fue respondida");
        }

        boolean accepted = Boolean.TRUE.equals(decision.getAccept());
        invite.setStatus(accepted ? RequestStatus.ACEPTADA : RequestStatus.RECHAZADA);
        invite.setRespondedAt(LocalDateTime.now());
        invite = teamInviteRepository.save(invite);

        if (accepted && !teamMemberRepository.isActiveMember(invite.getTeam(), invite.getTarget())) {
            TeamMember member = new TeamMember(invite.getTeam(), invite.getTarget(), PlayerRole.JUGADOR);
            member.setActivo(true);
            teamMemberRepository.save(member);
        }

        createNotification(
                invite.getRequester(),
                NotificationType.RESPUESTA_INVITACION_EQUIPO,
                "Invitacion de equipo respondida",
                aliasOf(invite.getTarget()) + (accepted ? " acepto" : " rechazo") + " la invitacion al equipo " + invite.getTeam().getNombre(),
                "TEAM_INVITE",
                invite.getId()
        );
        return toSocialResponse(invite);
    }

    @Transactional(readOnly = true)
    public List<SocialRequestResponse> getTeamInvites(UUID playerUuid) {
        PlayerProfile player = getPlayer(playerUuid);
        return teamInviteRepository.findByPlayer(player).stream()
                .map(this::toSocialResponse)
                .toList();
    }

    public SocialRequestResponse createMatchInvite(CreateMatchInviteRequest request) {
        long startedAtNanos = System.nanoTime();
        Match match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));
        Team team = null;
        if (request.getTeamId() != null) {
            team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado"));
        }
        final Team resolvedTeam = team;

        PlayerProfile requester = getPlayer(request.getRequesterUuid());
        PlayerProfile target = getPlayer(request.getTargetUuid());
        validateMatchInviteRequester(match, requester.getAtletaUuid());
        validateTeamBelongsToMatch(match, resolvedTeam);
        boolean selfInvite = requester.getAtletaUuid().equals(target.getAtletaUuid());

        if (selfInvite) {
            if (match.getCreador() == null || !match.getCreador().getAtletaUuid().equals(requester.getAtletaUuid())) {
                throw new IllegalArgumentException("Solo el creador del partido puede auto-confirmarse");
            }

            MatchInvite invite = matchInviteRepository
                    .findTopByMatchAndTargetOrderByCreatedAtDesc(match, target)
                    .orElseGet(() -> new MatchInvite(match, resolvedTeam, requester, target, normalizeMessage(request.getMessage())));

            invite.setStatus(RequestStatus.ACEPTADA);
            invite.setRespondedAt(LocalDateTime.now());
            invite.setMessage(normalizeMessage(request.getMessage()));
            if (invite.getTeam() == null && resolvedTeam != null) {
                invite.setTeam(resolvedTeam);
            }

            invite = matchInviteRepository.save(invite);
            ensureMatchParticipationFromInvite(invite, true);
            matchLiveEventService.publishInviteCreated(match.getId(), invite.getId());
            matchLiveEventService.publishInviteDecision(match.getId(), invite.getId(), invite.getStatus().name());
            SocialRequestResponse response = toSocialResponse(invite);
            logInviteLatency(match.getId(), request.getTargetUuid(), startedAtNanos);
            return response;
        }

        validateDistinctUsers(requester.getAtletaUuid(), target.getAtletaUuid());

        if (matchInviteRepository.existsByMatchAndTargetAndStatus(match, target, RequestStatus.PENDIENTE)) {
            throw new IllegalArgumentException("Ya existe una invitacion pendiente para este partido");
        }

        MatchInvite invite = matchInviteRepository.save(
                new MatchInvite(match, resolvedTeam, requester, target, normalizeMessage(request.getMessage()))
        );
        createNotification(
                target,
                NotificationType.INVITACION_PARTIDO,
                "Invitacion a partido",
                aliasOf(requester) + " te invito al partido #" + match.getId(),
                "MATCH_INVITE",
                invite.getId()
        );
        matchLiveEventService.publishInviteCreated(match.getId(), invite.getId());
        SocialRequestResponse response = toSocialResponse(invite);
        logInviteLatency(match.getId(), request.getTargetUuid(), startedAtNanos);
        return response;
    }

    public List<SocialRequestResponse> createMatchInvitesBatch(CreateMatchInvitesBatchRequest request) {
        long startedAtNanos = System.nanoTime();
        List<UUID> safeTargets = request.getTargetUuids() == null
                ? List.of()
                : request.getTargetUuids().stream()
                .filter(targetUuid -> targetUuid != null)
                .distinct()
                .toList();

        if (safeTargets.isEmpty()) {
            throw new IllegalArgumentException("Debes indicar al menos un usuario para invitar");
        }

        List<SocialRequestResponse> created = new java.util.ArrayList<>();
        for (UUID targetUuid : new LinkedHashSet<>(safeTargets)) {
            try {
                CreateMatchInviteRequest single = new CreateMatchInviteRequest();
                single.setMatchId(request.getMatchId());
                single.setTeamId(request.getTeamId());
                single.setRequesterUuid(request.getRequesterUuid());
                single.setTargetUuid(targetUuid);
                single.setMessage(request.getMessage());
                created.add(createMatchInvite(single));
            } catch (IllegalArgumentException ex) {
                logger.warn("No se pudo enviar invitacion en lote para target {} en match {}: {}",
                        targetUuid, request.getMatchId(), ex.getMessage());
            }
        }

        if (created.isEmpty()) {
            throw new IllegalArgumentException("No se pudo enviar ninguna invitacion del lote");
        }

        long elapsedMs = (System.nanoTime() - startedAtNanos) / 1_000_000;
        if (elapsedMs > 2000) {
            logger.warn("createMatchInvitesBatch lento match={} total={} elapsedMs={}",
                    request.getMatchId(), created.size(), elapsedMs);
        } else if (logger.isDebugEnabled()) {
            logger.debug("createMatchInvitesBatch match={} total={} elapsedMs={}",
                    request.getMatchId(), created.size(), elapsedMs);
        }

        return created;
    }

    public List<MatchInviteDeliveryResponse> createMatchInvitesBatchDetailed(CreateMatchInvitesBatchRequest request) {
        List<UUID> safeTargets = request.getTargetUuids() == null
                ? List.of()
                : request.getTargetUuids().stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        if (safeTargets.isEmpty()) {
            throw new IllegalArgumentException("Debes indicar al menos un usuario para invitar");
        }

        return safeTargets.stream()
                .map(targetUuid -> deliverMatchInvite(request, targetUuid))
                .toList();
    }

    private MatchInviteDeliveryResponse deliverMatchInvite(
            CreateMatchInvitesBatchRequest request,
            UUID targetUuid
    ) {
        try {
            Match match = matchRepository.findById(request.getMatchId())
                    .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));
            PlayerProfile target = getPlayer(targetUuid);
            Optional<MatchInvite> previous = matchInviteRepository
                    .findTopByMatchAndTargetOrderByCreatedAtDesc(match, target);

            if (previous.isPresent() && previous.get().getStatus() == RequestStatus.PENDIENTE) {
                return new MatchInviteDeliveryResponse(
                        targetUuid,
                        DeliveryStatus.ALREADY_SENT,
                        toSocialResponse(previous.get()),
                        "La invitacion ya estaba pendiente"
                );
            }
            if (previous.isPresent() && previous.get().getStatus() == RequestStatus.ACEPTADA) {
                return new MatchInviteDeliveryResponse(
                        targetUuid,
                        DeliveryStatus.ALREADY_ACCEPTED,
                        toSocialResponse(previous.get()),
                        "El jugador ya habia aceptado la invitacion"
                );
            }

            CreateMatchInviteRequest single = new CreateMatchInviteRequest();
            single.setMatchId(request.getMatchId());
            single.setTeamId(request.getTeamId());
            single.setRequesterUuid(request.getRequesterUuid());
            single.setTargetUuid(targetUuid);
            single.setMessage(request.getMessage());
            SocialRequestResponse invitation = createMatchInvite(single);
            return new MatchInviteDeliveryResponse(targetUuid, DeliveryStatus.SENT, invitation, null);
        } catch (IllegalArgumentException ex) {
            logger.warn("No se pudo entregar invitacion para target {} en match {}: {}",
                    targetUuid, request.getMatchId(), ex.getMessage());
            return new MatchInviteDeliveryResponse(targetUuid, DeliveryStatus.FAILED, null, ex.getMessage());
        }
    }

    public SocialRequestResponse respondMatchInvite(Long inviteId, RespondRequestDecision decision) {
        MatchInvite invite = matchInviteRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("Invitacion de partido no encontrada"));
        if (!invite.getTarget().getAtletaUuid().equals(decision.getActorUuid())) {
            throw new IllegalArgumentException("Solo el receptor puede responder la invitacion");
        }
        if (invite.getStatus() != RequestStatus.PENDIENTE) {
            throw new IllegalArgumentException("La invitacion ya fue respondida");
        }

        boolean accepted = Boolean.TRUE.equals(decision.getAccept());
        invite.setStatus(accepted ? RequestStatus.ACEPTADA : RequestStatus.RECHAZADA);
        invite.setRespondedAt(LocalDateTime.now());
        invite = matchInviteRepository.save(invite);
        ensureMatchParticipationFromInvite(invite, accepted);

        createNotification(
                invite.getRequester(),
                NotificationType.RESPUESTA_INVITACION_PARTIDO,
                "Invitacion de partido respondida",
                aliasOf(invite.getTarget()) + (accepted ? " acepto" : " rechazo") + " la invitacion al partido #" + invite.getMatch().getId(),
                "MATCH_INVITE",
                invite.getId()
        );
        matchLiveEventService.publishInviteDecision(invite.getMatch().getId(), invite.getId(), invite.getStatus().name());
        return toSocialResponse(invite);
    }

    @Transactional
    public List<SocialRequestResponse> getMatchInvites(UUID playerUuid) {
        purgeExpiredPendingMatchInvites();
        PlayerProfile player = getPlayer(playerUuid);
        return matchInviteRepository.findByPlayer(player).stream()
                .map(this::toSocialResponse)
                .toList();
    }

    @Transactional
    public List<SocialRequestResponse> getMatchInvitesByMatch(Long matchId, UUID actorUuid) {
        purgeExpiredPendingMatchInvites();
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));
        validateMatchInviteViewer(match, actorUuid);
        return matchInviteRepository.findByMatchId(matchId).stream()
                .map(this::toSocialResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppNotificationResponse> getNotifications(UUID playerUuid) {
        PlayerProfile player = getPlayer(playerUuid);
        return appNotificationRepository.findByRecipient(player).stream().map(this::toNotificationResponse).toList();
    }

    public AppNotificationResponse markNotificationAsRead(Long notificationId, UUID playerUuid) {
        PlayerProfile player = getPlayer(playerUuid);
        AppNotification notification = appNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notificacion no encontrada"));
        if (!notification.getRecipient().getAtletaUuid().equals(player.getAtletaUuid())) {
            throw new IllegalArgumentException("La notificacion no pertenece al usuario");
        }
        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        return toNotificationResponse(appNotificationRepository.save(notification));
    }

    public AppNotificationResponse sendIncompleteFormReminder(UUID playerUuid) {
        PlayerProfile player = getPlayer(playerUuid);
        boolean missingAlias = player.getAlias() == null || player.getAlias().trim().isEmpty();
        int positions = playerPositionRepository.findByPlayerOrderByPrioridad(player).size();

        StringBuilder details = new StringBuilder();
        if (missingAlias) {
            details.append("Alias pendiente. ");
        }
        if (positions < 3) {
            details.append("Faltan ").append(3 - positions).append(" posiciones para completar tu perfil.");
        }

        if (details.length() == 0) {
            details.append("Tu perfil principal esta completo.");
        }

        AppNotification notification = createNotification(
                player,
                NotificationType.FORMULARIO_INCOMPLETO,
                "Estado de formularios",
                details.toString().trim(),
                "PROFILE_FORM",
                null
        );
        return toNotificationResponse(notification);
    }

    public PushTokenResponse registerPushToken(UUID playerUuid, RegisterPushTokenRequest request) {
        PlayerProfile player = getPlayer(playerUuid);
        String normalizedToken = normalizeRequired(request.getToken(), "El push token es obligatorio");
        String normalizedPlatform = normalizeRequired(request.getPlatform(), "La plataforma es obligatoria");
        String normalizedDeviceId = normalizeOptional(request.getDeviceId());

        PushNotificationToken entity = normalizedDeviceId != null
                ? pushNotificationTokenRepository.findByRecipientAndDeviceId(player, normalizedDeviceId).orElse(null)
                : null;

        if (entity == null) {
            entity = pushNotificationTokenRepository.findByToken(normalizedToken).orElse(null);
        }

        if (entity == null) {
            entity = new PushNotificationToken(player, normalizedToken, normalizedPlatform, normalizedDeviceId);
        } else {
            entity.setRecipient(player);
            entity.setToken(normalizedToken);
            entity.setPlatform(normalizedPlatform);
            entity.setDeviceId(normalizedDeviceId);
            entity.setActive(true);
            entity.setLastSeenAt(LocalDateTime.now());
        }

        return toPushTokenResponse(pushNotificationTokenRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadNotificationCount(UUID playerUuid) {
        PlayerProfile player = getPlayer(playerUuid);
        return new UnreadNotificationCountResponse(appNotificationRepository.countUnreadByRecipient(player));
    }

    @Transactional(readOnly = true)
    public List<SocialPlayerLookupResponse> searchPlayers(String query) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.length() < 2) {
            return List.of();
        }

        return playerProfileRepository.searchForSocialLookup(normalized).stream()
                .limit(12)
                .map(player -> new SocialPlayerLookupResponse(
                        player.getAtletaUuid(),
                        player.getAlias(),
                        player.getAthlete() != null ? player.getAthlete().getNombre() : null,
                        player.getAthlete() != null ? player.getAthlete().getEmail() : null
                ))
                .toList();
    }

    private PlayerProfile getPlayer(UUID playerUuid) {
        return playerProfileRepository.findById(playerUuid)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + playerUuid));
    }

    private void validateDistinctUsers(UUID requesterUuid, UUID targetUuid) {
        if (requesterUuid.equals(targetUuid)) {
            throw new IllegalArgumentException("No puedes enviarte solicitudes a ti mismo");
        }
    }

    private void validateMatchInviteViewer(Match match, UUID actorUuid) {
        if (actorUuid == null) {
            throw new AccessDeniedException("Se requiere usuario autenticado");
        }

        if (match.getCreador() != null && actorUuid.equals(match.getCreador().getAtletaUuid())) {
            return;
        }

        boolean isInviteActor = matchInviteRepository.findByMatchId(match.getId()).stream()
                .anyMatch(invite -> isInviteParticipant(invite, actorUuid));
        if (isInviteActor) {
            return;
        }

        boolean isMatchParticipant = matchPlayerRepository.findByMatchAndPlayerAtletaUuid(match, actorUuid).isPresent();
        if (!isMatchParticipant) {
            throw new AccessDeniedException("No puedes ver invitaciones de un partido ajeno");
        }
    }

    private void validateMatchInviteRequester(Match match, UUID actorUuid) {
        if (actorUuid == null) {
            throw new AccessDeniedException("Se requiere usuario autenticado");
        }

        if (match.getCreador() != null && actorUuid.equals(match.getCreador().getAtletaUuid())) {
            return;
        }

        boolean isMatchParticipant = matchPlayerRepository.findByMatchAndPlayerAtletaUuid(match, actorUuid).isPresent();
        if (!isMatchParticipant) {
            throw new AccessDeniedException("No puedes invitar jugadores a un partido ajeno");
        }
    }

    private void validateTeamBelongsToMatch(Match match, Team team) {
        if (team == null) {
            return;
        }

        boolean belongsToMatch = match.getMatchTeams() != null && match.getMatchTeams().stream()
                .anyMatch(matchTeam -> matchTeam.getTeam() != null
                        && team.getId() != null
                        && team.getId().equals(matchTeam.getTeam().getId()));

        if (!belongsToMatch) {
            throw new IllegalArgumentException("El equipo indicado no pertenece al partido");
        }
    }

    private boolean isInviteParticipant(MatchInvite invite, UUID actorUuid) {
        return (invite.getRequester() != null && actorUuid.equals(invite.getRequester().getAtletaUuid()))
                || (invite.getTarget() != null && actorUuid.equals(invite.getTarget().getAtletaUuid()));
    }

    private String normalizeMessage(String message) {
        return normalizeOptional(message);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeRequired(String value, String errorMessage) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(errorMessage);
        }
        return normalized;
    }

    private String aliasOf(PlayerProfile player) {
        if (player == null) {
            return "Jugador";
        }
        if (player.getAlias() != null && !player.getAlias().trim().isEmpty()) {
            return player.getAlias().trim();
        }
        return player.getAtletaUuid().toString();
    }

    private AppNotification createNotification(
            PlayerProfile recipient,
            NotificationType type,
            String title,
            String message,
            String contextType,
            Long contextId
    ) {
        AppNotification notification = new AppNotification(recipient, type, title, message);
        notification.setContextType(contextType);
        notification.setContextId(contextId);
        return appNotificationRepository.save(notification);
    }

    private SocialRequestResponse toSocialResponse(Friendship friendship) {
        SocialRequestResponse response = new SocialRequestResponse();
        response.setId(friendship.getId());
        response.setType("FRIENDSHIP");
        response.setStatus(friendship.getStatus());
        response.setRequesterUuid(friendship.getRequester().getAtletaUuid());
        response.setRequesterAlias(aliasOf(friendship.getRequester()));
        response.setTargetUuid(friendship.getTarget().getAtletaUuid());
        response.setTargetAlias(aliasOf(friendship.getTarget()));
        response.setCreatedAt(friendship.getCreatedAt());
        response.setRespondedAt(friendship.getRespondedAt());
        return response;
    }

    private SocialRequestResponse toSocialResponse(TeamInvite invite) {
        SocialRequestResponse response = new SocialRequestResponse();
        response.setId(invite.getId());
        response.setType("TEAM_INVITE");
        response.setStatus(invite.getStatus());
        response.setRequesterUuid(invite.getRequester().getAtletaUuid());
        response.setRequesterAlias(aliasOf(invite.getRequester()));
        response.setTargetUuid(invite.getTarget().getAtletaUuid());
        response.setTargetAlias(aliasOf(invite.getTarget()));
        response.setTeamId(invite.getTeam().getId());
        response.setTeamName(invite.getTeam().getNombre());
        response.setMessage(invite.getMessage());
        response.setCreatedAt(invite.getCreatedAt());
        response.setRespondedAt(invite.getRespondedAt());
        return response;
    }

    private SocialRequestResponse toSocialResponse(MatchInvite invite) {
        SocialRequestResponse response = new SocialRequestResponse();
        response.setId(invite.getId());
        response.setType("MATCH_INVITE");
        response.setStatus(invite.getStatus());
        response.setRequesterUuid(invite.getRequester().getAtletaUuid());
        response.setRequesterAlias(aliasOf(invite.getRequester()));
        response.setTargetUuid(invite.getTarget().getAtletaUuid());
        response.setTargetAlias(aliasOf(invite.getTarget()));
        response.setMatchId(invite.getMatch().getId());
        if (invite.getTeam() != null) {
            response.setTeamId(invite.getTeam().getId());
            response.setTeamName(invite.getTeam().getNombre());
        }
        response.setMessage(invite.getMessage());
        response.setCreatedAt(invite.getCreatedAt());
        response.setRespondedAt(invite.getRespondedAt());
        return response;
    }

    private AppNotificationResponse toNotificationResponse(AppNotification notification) {
        AppNotificationResponse response = new AppNotificationResponse();
        response.setId(notification.getId());
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setContextType(notification.getContextType());
        response.setContextId(notification.getContextId());
        response.setRead(notification.getIsRead());
        response.setReadAt(notification.getReadAt());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }

    private PushTokenResponse toPushTokenResponse(PushNotificationToken token) {
        PushTokenResponse response = new PushTokenResponse();
        response.setId(token.getId());
        response.setPlayerUuid(token.getRecipient().getAtletaUuid());
        response.setPlatform(token.getPlatform());
        response.setDeviceId(token.getDeviceId());
        response.setActive(token.getActive());
        response.setLastSeenAt(token.getLastSeenAt());
        response.setCreatedAt(token.getCreatedAt());
        return response;
    }

    private void ensureMatchParticipationFromInvite(MatchInvite invite, boolean accepted) {
        Match match = invite.getMatch();
        PlayerProfile player = invite.getTarget();

        MatchPlayer matchPlayer = matchPlayerRepository.findByMatchAndPlayer(match, player).orElse(null);

        if (!accepted) {
            if (matchPlayer != null) {
                matchPlayer.setConfirmado(false);
                matchPlayerRepository.save(matchPlayer);
            }
            return;
        }

        Team team = resolveTeamForInvite(invite);
        if (team == null) {
            throw new IllegalArgumentException("No se pudo determinar equipo para registrar participacion en el partido");
        }

        Position position = resolvePrimaryPosition(player);
        if (position == null) {
            throw new IllegalArgumentException("El jugador no tiene posicion registrada para confirmar participacion");
        }

        if (matchPlayer == null) {
            matchPlayer = new MatchPlayer(match, team, player, position, PlayerRole.JUGADOR);
        } else {
            matchPlayer.setTeam(team);
            matchPlayer.setPosition(position);
            if (matchPlayer.getRol() == null) {
                matchPlayer.setRol(PlayerRole.JUGADOR);
            }
        }

        matchPlayer.setTeamSide(resolveTeamSideForInvite(match, team, player.getAtletaUuid(), matchPlayer.getTeamSide()));
        matchPlayer.setConfirmado(true);
        matchPlayerRepository.save(matchPlayer);
    }

    private Team resolveTeamForInvite(MatchInvite invite) {
        if (invite.getTeam() != null) {
            return invite.getTeam();
        }

        Match match = invite.getMatch();
        if (match.getMatchTeams() == null || match.getMatchTeams().isEmpty()) {
            return null;
        }

        if (match.getMatchTeams().size() == 1) {
            return match.getMatchTeams().get(0).getTeam();
        }

        MatchTeam localTeam = match.getMatchTeams().stream()
                .filter(MatchTeam::getEsLocal)
                .findFirst()
                .orElse(null);
        MatchTeam awayTeam = match.getMatchTeams().stream()
                .filter(mt -> !Boolean.TRUE.equals(mt.getEsLocal()))
                .findFirst()
                .orElse(null);

        if (localTeam == null && awayTeam == null) {
            return match.getMatchTeams().get(0).getTeam();
        }
        if (localTeam == null) {
            return awayTeam != null ? awayTeam.getTeam() : null;
        }
        if (awayTeam == null) {
            return localTeam.getTeam();
        }

        int confirmedLocal = countConfirmedByTeam(match, localTeam.getTeam().getId());
        int confirmedAway = countConfirmedByTeam(match, awayTeam.getTeam().getId());
        if (confirmedAway < confirmedLocal) {
            return awayTeam.getTeam();
        }

        return localTeam.getTeam();
    }

    private Position resolvePrimaryPosition(PlayerProfile player) {
        PlayerPosition primary = playerPositionRepository.findPrimaryPositionByPlayer(player).orElse(null);
        if (primary != null && primary.getPosition() != null) {
            return primary.getPosition();
        }

        return positionRepository.findAllOrderByNombre().stream().findFirst().orElse(null);
    }

    private MatchTeamSide resolveTeamSide(Match match, Team team) {
        if (match.getMatchTeams() == null) {
            return null;
        }

        for (MatchTeam matchTeam : match.getMatchTeams()) {
            if (matchTeam.getTeam() == null || !matchTeam.getTeam().getId().equals(team.getId())) {
                continue;
            }
            return Boolean.TRUE.equals(matchTeam.getEsLocal()) ? MatchTeamSide.LOCAL : MatchTeamSide.VISITA;
        }

        return null;
    }

    private MatchTeamSide resolveTeamSideForInvite(
            Match match,
            Team team,
            UUID playerUuid,
            MatchTeamSide existingSide
    ) {
        if (existingSide != null) {
            return existingSide;
        }

        MatchTeamSide mappedSide = resolveTeamSide(match, team);
        if (!requiresAutoSideBalance(match)) {
            return mappedSide != null ? mappedSide : MatchTeamSide.LOCAL;
        }

        int localCount = 0;
        int awayCount = 0;
        List<MatchPlayer> confirmedPlayers = matchPlayerRepository.findConfirmedPlayersByMatch(match);
        for (MatchPlayer participant : confirmedPlayers) {
            if (participant.getPlayer() != null && playerUuid.equals(participant.getPlayer().getAtletaUuid())) {
                continue;
            }

            if (participant.getTeamSide() == MatchTeamSide.VISITA) {
                awayCount += 1;
            } else {
                localCount += 1;
            }
        }

        return localCount <= awayCount ? MatchTeamSide.LOCAL : MatchTeamSide.VISITA;
    }

    private boolean requiresAutoSideBalance(Match match) {
        if (match.getMatchTeams() == null || match.getMatchTeams().isEmpty()) {
            return true;
        }

        MatchTeam localTeam = match.getMatchTeams().stream()
                .filter(MatchTeam::getEsLocal)
                .findFirst()
                .orElse(null);
        MatchTeam awayTeam = match.getMatchTeams().stream()
                .filter(mt -> !Boolean.TRUE.equals(mt.getEsLocal()))
                .findFirst()
                .orElse(null);

        if (localTeam == null || awayTeam == null) {
            return true;
        }

        if (localTeam.getTeam() == null || awayTeam.getTeam() == null) {
            return true;
        }

        return localTeam.getTeam().getId().equals(awayTeam.getTeam().getId());
    }

    private int countConfirmedByTeam(Match match, Long teamId) {
        if (teamId == null) {
            return 0;
        }

        int count = 0;
        List<MatchPlayer> confirmedPlayers = matchPlayerRepository.findConfirmedPlayersByMatch(match);
        for (MatchPlayer matchPlayer : confirmedPlayers) {
            if (matchPlayer.getTeam() == null || matchPlayer.getTeam().getId() == null) {
                continue;
            }
            if (teamId.equals(matchPlayer.getTeam().getId())) {
                count += 1;
            }
        }
        return count;
    }

    private void logInviteLatency(Long matchId, UUID targetUuid, long startedAtNanos) {
        long elapsedMs = (System.nanoTime() - startedAtNanos) / 1_000_000;
        if (elapsedMs > 1500) {
            logger.warn("createMatchInvite lento match={} target={} elapsedMs={}", matchId, targetUuid, elapsedMs);
        } else if (logger.isDebugEnabled()) {
            logger.debug("createMatchInvite match={} target={} elapsedMs={}", matchId, targetUuid, elapsedMs);
        }
    }

    private void purgeExpiredPendingMatchInvites() {
        int deleted = matchInviteRepository.deletePendingInvitesForExpiredMatches(LocalDateTime.now());
        if (deleted > 0) {
            logger.info("Invitaciones pendientes expiradas eliminadas: {}", deleted);
        }
    }
}



