package com.atleta.demo.service;

import com.atleta.demo.dto.request.CreateMatchRequest;
import com.atleta.demo.dto.request.JoinMatchRequest;
import com.atleta.demo.dto.request.CreateMatchEventRequest;
import com.atleta.demo.dto.request.MatchClosePreviewRequest;
import com.atleta.demo.dto.response.*;
import com.atleta.demo.entity.*;
import com.atleta.demo.enums.MatchStatus;
import com.atleta.demo.enums.MatchValidationStatus;
import com.atleta.demo.enums.EventType;
import com.atleta.demo.enums.MatchGenderCategory;
import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.enums.MatchTeamSide;
import com.atleta.demo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.math.BigDecimal;

/**
 * Servicio para la gestiÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n de partidos, participaciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n y eventos.
 * Implementa la lÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³gica de negocio para los requisitos 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 7.4, 7.5
 * Integrado con el sistema de calificaciones para actualizaciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n automÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡tica al finalizar partidos.
 */
@Service
@Transactional
public class MatchService {

    private static final Logger logger = LoggerFactory.getLogger(MatchService.class);
    private static final long DATA_CAPTURE_WINDOW_HOURS = 3;

    private final MatchRepository matchRepository;
    private final MatchTeamRepository matchTeamRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchEventRepository matchEventRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final PositionRepository positionRepository;
    private final PlayerPositionRepository playerPositionRepository;
    private final RatingService ratingService;
    private final MatchStatusPolicy matchStatusPolicy;
    private final MatchFinalScoreService matchFinalScoreService;
    private final MatchPlayerHistoryService matchPlayerHistoryService;
    private final MatchPendingEventClosureService matchPendingEventClosureService;
    private final MatchRosterPolicy matchRosterPolicy;
    private final MatchPostMatchRatingService matchPostMatchRatingService;
    private final MatchResponseMapper matchResponseMapper;
    private final MatchQueryService matchQueryService;

    public MatchService(MatchRepository matchRepository,
                        MatchTeamRepository matchTeamRepository,
                        MatchPlayerRepository matchPlayerRepository,
                        MatchEventRepository matchEventRepository,
                        PlayerProfileRepository playerProfileRepository,
                        TeamRepository teamRepository,
                        PositionRepository positionRepository,
                        TeamMemberRepository teamMemberRepository,
                        PlayerPositionRepository playerPositionRepository,
                        RatingService ratingService,
                        MatchStatusPolicy matchStatusPolicy,
                        MatchFinalScoreService matchFinalScoreService,
                        MatchPlayerHistoryService matchPlayerHistoryService,
                        MatchPendingEventClosureService matchPendingEventClosureService,
                        MatchRosterPolicy matchRosterPolicy,
                        MatchPostMatchRatingService matchPostMatchRatingService,
                        MatchResponseMapper matchResponseMapper,
                        MatchQueryService matchQueryService) {
        this.matchRepository = matchRepository;
        this.matchTeamRepository = matchTeamRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.matchEventRepository = matchEventRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.teamRepository = teamRepository;
        this.positionRepository = positionRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.playerPositionRepository = playerPositionRepository;
        this.ratingService = ratingService;
        this.matchStatusPolicy = matchStatusPolicy;
        this.matchFinalScoreService = matchFinalScoreService;
        this.matchPlayerHistoryService = matchPlayerHistoryService;
        this.matchPendingEventClosureService = matchPendingEventClosureService;
        this.matchRosterPolicy = matchRosterPolicy;
        this.matchPostMatchRatingService = matchPostMatchRatingService;
        this.matchResponseMapper = matchResponseMapper;
        this.matchQueryService = matchQueryService;
    }

    /**
     * Crea un nuevo partido con modalidad, fecha/hora, ubicaciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n y cuota.
     * Requisitos: 6.1, 6.2, 6.3, 6.4, 6.5
     */
    public MatchResponse createMatch(CreateMatchRequest request) {
        // Buscar el creador (Requisito 6.1)
        PlayerProfile creador = playerProfileRepository.findById(request.getCreadorUuid())
                .orElseThrow(() -> new IllegalArgumentException("Creador no encontrado: " + request.getCreadorUuid()));

        // Crear el partido con estado inicial CREADO (Requisito 6.3)
        Match match = new Match(
                request.getModalidad(),
                request.getFechaHoraProgramada(),
                creador,
                request.getLatitud(),
                request.getLongitud(),
                request.getCuota()
        );
        match.setCategoriaGenero(
                request.getCategoriaGenero() != null ? request.getCategoriaGenero() : MatchGenderCategory.MIXTO
        );

        match = matchRepository.save(match);
        return matchResponseMapper.toMatchResponse(match);
    }

    /**
     * Permite que un jugador se una a un partido con un equipo y posiciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n especÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â­ficos.
     * Requisitos: 7.1, 7.2, 7.3, 7.4, 7.5
     */
    public MatchPlayerResponse joinMatch(JoinMatchRequest request) {
        // Buscar el partido
        Match match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + request.getMatchId()));

        // Buscar el jugador (Requisito 7.1)
        PlayerProfile player = playerProfileRepository.findById(request.getPlayerUuid())
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + request.getPlayerUuid()));

        // Buscar el equipo (Requisito 7.1)
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado: " + request.getTeamId()));

        // Buscar la posiciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n (Requisito 7.2)
        Position position = positionRepository.findById(request.getPositionId())
                .orElseThrow(() -> new IllegalArgumentException("PosiciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n no encontrada: " + request.getPositionId()));

        // Verificar que el jugador no estÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© ya registrado en el partido
        if (matchPlayerRepository.existsByMatchAndPlayer(match, player)) {
            throw new IllegalArgumentException("El jugador ya está registrado en este partido");
        }

        // Verificar que el equipo participe en el partido
        if (!matchTeamRepository.existsByMatchAndTeam(match, team)) {
            throw new IllegalArgumentException("El equipo no participa en este partido");
        }

        int playersPerTeamLimit = matchRosterPolicy.playersPerTeamByModality(match.getModalidad());
        long currentPlayersInTeam = matchPlayerRepository.findByMatch(match).stream()
                .filter(item -> item.getTeam() != null && item.getTeam().getId().equals(team.getId()))
                .count();
        if (currentPlayersInTeam >= playersPerTeamLimit) {
            throw new IllegalArgumentException("El equipo ya completo su cupo para esta modalidad");
        }

        // Crear la participaciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n (Requisito 7.3, 7.4, 7.5)
        MatchPlayer matchPlayer = new MatchPlayer(match, team, player, position, request.getRol());
        matchPlayer.setTeamSide(resolveTeamSide(match, team));
        matchPlayer = matchPlayerRepository.save(matchPlayer);

        return matchResponseMapper.toMatchPlayerResponse(matchPlayer);
    }

    /**
     * Confirma la participaciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n de un jugador en un partido.
     * Requisito 7.3
     */
    public MatchPlayerResponse confirmParticipation(Long matchId, UUID playerUuid) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + matchId));

        PlayerProfile player = playerProfileRepository.findById(playerUuid)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + playerUuid));

        MatchPlayer matchPlayer = matchPlayerRepository.findByMatchAndPlayer(match, player)
                .orElseThrow(() -> new IllegalArgumentException("El jugador no estÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ registrado en este partido"));

        matchPlayer.confirmarParticipacion();
        matchPlayer = matchPlayerRepository.save(matchPlayer);

        return matchResponseMapper.toMatchPlayerResponse(matchPlayer);
    }

    public MatchPlayerResponse removePlayerFromMatch(Long matchId, UUID playerUuid) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + matchId));

        PlayerProfile player = playerProfileRepository.findById(playerUuid)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + playerUuid));

        MatchPlayer matchPlayer = matchPlayerRepository.findByMatchAndPlayer(match, player)
                .orElseThrow(() -> new IllegalArgumentException("El jugador no estÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ registrado en este partido"));

        MatchPlayerResponse removed = matchResponseMapper.toMatchPlayerResponse(matchPlayer);
        matchPlayerRepository.delete(matchPlayer);
        return removed;
    }

    public List<MatchPlayerResponse> importTeamPlayers(Long matchId, Long teamId) {
        return importTeamPlayers(matchId, teamId, null);
    }

    public List<MatchPlayerResponse> importTeamPlayers(Long matchId, Long teamId, UUID actorUuid) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + matchId));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado: " + teamId));

        if (actorUuid != null) {
            validateResponsibleActor(match, actorUuid);
        }

        if (!matchTeamRepository.existsByMatchAndTeam(match, team)) {
            throw new IllegalArgumentException("El equipo no participa en este partido");
        }

        List<TeamMember> activeMembers = teamMemberRepository.findActiveByTeam(team);
        int playersPerTeamLimit = matchRosterPolicy.playersPerTeamByModality(match.getModalidad());
        long currentPlayersInTeam = matchPlayerRepository.findByMatch(match).stream()
                .filter(item -> item.getTeam() != null && item.getTeam().getId().equals(team.getId()))
                .count();

        List<MatchPlayerResponse> imported = new ArrayList<>();

        for (TeamMember member : activeMembers) {
            if (currentPlayersInTeam >= playersPerTeamLimit) {
                break;
            }

            PlayerProfile player = member.getPlayer();
            if (matchPlayerRepository.existsByMatchAndPlayer(match, player)) {
                continue;
            }

            PlayerPosition playerPosition = playerPositionRepository.findPrimaryPositionByPlayer(player).orElse(null);
            if (playerPosition == null || playerPosition.getPosition() == null) {
                continue;
            }

            MatchPlayer matchPlayer = new MatchPlayer(match, team, player, playerPosition.getPosition(), PlayerRole.JUGADOR);
            matchPlayer.setTeamSide(resolveTeamSide(match, team));
            matchPlayer = matchPlayerRepository.save(matchPlayer);
            imported.add(matchResponseMapper.toMatchPlayerResponse(matchPlayer));
            currentPlayersInTeam += 1;
        }

        return imported;
    }

    /**
     * Agrega un equipo a un partido (local o visitante).
     * Un partido debe tener exactamente 2 equipos.
     */
    public MatchResponse addTeamToMatch(Long matchId, Long teamId, Boolean esLocal) {
        return addTeamToMatch(matchId, teamId, esLocal, null);
    }

    public MatchResponse addTeamToMatch(Long matchId, Long teamId, Boolean esLocal, UUID actorUuid) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + matchId));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado: " + teamId));

        if (actorUuid != null) {
            validateResponsibleActor(match, actorUuid);
        }

        // Verificar que no haya mÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡s de 2 equipos
        long teamCount = matchTeamRepository.countByMatch(match);
        if (teamCount >= 2) {
            throw new IllegalArgumentException("Un partido no puede tener más de 2 equipos");
        }

        // Verificar que no haya otro equipo con el mismo tipo (local/visitante)
        if (esLocal && matchTeamRepository.findLocalTeamByMatch(match).isPresent()) {
            throw new IllegalArgumentException("Ya existe un equipo local en este partido");
        }
        if (!esLocal && matchTeamRepository.findVisitingTeamByMatch(match).isPresent()) {
            throw new IllegalArgumentException("Ya existe un equipo visitante en este partido");
        }

        // Verificar que el equipo no estÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© ya en el partido
        if (matchTeamRepository.existsByMatchAndTeam(match, team)) {
            throw new IllegalArgumentException("El equipo ya participa en este partido");
        }

        MatchTeam matchTeam = new MatchTeam(match, team, esLocal);
        matchTeamRepository.save(matchTeam);
        match.addMatchTeam(matchTeam);
        ensureCreatorParticipation(match, team);

        return matchResponseMapper.toMatchResponse(match);
    }

    public MatchResponse updateTeamAssignments(Long matchId, UUID actorUuid, List<UUID> homePlayerUuids, List<UUID> awayPlayerUuids) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + matchId));

        ensureCreatorParticipation(match, resolveDefaultTeamForCreator(match));
        validateResponsibleActor(match, actorUuid);
        matchRosterPolicy.validateTeamAssignmentWindow(match);

        List<MatchPlayer> players = matchPlayerRepository.findByMatch(match);
        if (players.isEmpty()) {
            throw new IllegalArgumentException("El partido no tiene jugadores para asignar");
        }

        List<UUID> safeHome = homePlayerUuids != null ? homePlayerUuids : List.of();
        List<UUID> safeAway = awayPlayerUuids != null ? awayPlayerUuids : List.of();

        Set<UUID> overlap = new HashSet<>(safeHome);
        overlap.retainAll(new HashSet<>(safeAway));
        if (!overlap.isEmpty()) {
            throw new IllegalArgumentException("Un jugador no puede estar en local y visita al mismo tiempo");
        }

        Set<UUID> validPlayers = players.stream()
                .map(item -> item.getPlayer().getAtletaUuid())
                .collect(Collectors.toSet());

        for (UUID uuid : safeHome) {
            if (!validPlayers.contains(uuid)) {
                throw new IllegalArgumentException("Jugador no encontrado en partido: " + uuid);
            }
        }

        for (UUID uuid : safeAway) {
            if (!validPlayers.contains(uuid)) {
                throw new IllegalArgumentException("Jugador no encontrado en partido: " + uuid);
            }
        }

        Set<UUID> homeSet = new HashSet<>(safeHome);
        Set<UUID> awaySet = new HashSet<>(safeAway);

        for (MatchPlayer player : players) {
            UUID playerUuid = player.getPlayer().getAtletaUuid();
            if (homeSet.contains(playerUuid)) {
                player.setTeamSide(MatchTeamSide.LOCAL);
            } else if (awaySet.contains(playerUuid)) {
                player.setTeamSide(MatchTeamSide.VISITA);
            }
        }

        matchRosterPolicy.validateGenderAssignmentRules(match, players, homeSet, awaySet);

        matchPlayerRepository.saveAll(players);
        return matchResponseMapper.toMatchResponse(match);
    }

    @Transactional(readOnly = true)
    public MatchClosePreviewResponse getClosePreview(Long matchId, MatchClosePreviewRequest request) {
        return getClosePreview(matchId, request, null);
    }

    @Transactional(readOnly = true)
    public MatchClosePreviewResponse getClosePreview(Long matchId, MatchClosePreviewRequest request, UUID actorUuid) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + matchId));

        if (actorUuid != null) {
            validateResponsibleActor(match, actorUuid);
        }

        List<MatchPlayer> players = matchPlayerRepository.findByMatch(match).stream()
                .filter(item -> Boolean.TRUE.equals(item.getConfirmado()))
                .collect(Collectors.toList());

        if (players.isEmpty()) {
            throw new IllegalArgumentException("No hay jugadores confirmados en el partido");
        }

        Map<UUID, Integer> requestedGoals = request != null && request.getGoalsByPlayer() != null
                ? request.getGoalsByPlayer()
                : Map.of();
        Map<UUID, Integer> persistedGoals = buildPersistedGoalsMap(match);

        int computedLocalGoals = 0;
        int computedAwayGoals = 0;
        for (MatchPlayer player : players) {
            UUID playerUuid = player.getPlayer().getAtletaUuid();
            int goals = requestedGoals.getOrDefault(playerUuid, persistedGoals.getOrDefault(playerUuid, 0));
            if (player.getTeamSide() == MatchTeamSide.VISITA) {
                computedAwayGoals += goals;
            } else {
                computedLocalGoals += goals;
            }
        }

        int finalScoreLocal = request != null && request.getFinalScoreLocal() != null
                ? request.getFinalScoreLocal()
                : computedLocalGoals;
        int finalScoreAway = request != null && request.getFinalScoreAway() != null
                ? request.getFinalScoreAway()
                : computedAwayGoals;

        MatchClosePreviewResponse response = new MatchClosePreviewResponse();
        response.setMatchId(matchId);
        response.setFinalScoreLocal(Math.max(finalScoreLocal, 0));
        response.setFinalScoreAway(Math.max(finalScoreAway, 0));

        List<MatchClosePreviewPlayerResponse> playerRows = new ArrayList<>();
        for (MatchPlayer player : players) {
            UUID playerUuid = player.getPlayer().getAtletaUuid();
            int goals = requestedGoals.getOrDefault(playerUuid, persistedGoals.getOrDefault(playerUuid, 0));
            MatchTeamSide side = player.getTeamSide() != null ? player.getTeamSide() : MatchTeamSide.LOCAL;
            int xp = estimateXpForPlayer(player, goals, response.getFinalScoreLocal(), response.getFinalScoreAway(), side);
            BigDecimal currentOvr = resolveCurrentHybridOvr(playerUuid);

            MatchClosePreviewPlayerResponse row = new MatchClosePreviewPlayerResponse();
            row.setPlayerUuid(playerUuid);
            row.setAlias(player.getPlayer() != null ? player.getPlayer().getAlias() : "Jugador");
            row.setPosition(player.getPosition() != null ? player.getPosition().getNombre() : "Sin posicion");
            row.setTeamSide(side);
            row.setGoals(goals);
            row.setEstimatedXp(xp);
            row.setCurrentHybridOvr(currentOvr);
            playerRows.add(row);
        }

        response.setPlayers(playerRows);
        return response;
    }

    /**
     * Registra un evento durante un partido (gol o asistencia).
     * Requisitos: 8.1, 8.2, 8.3, 8.4, 8.5
     */
    public MatchEventResponse registerEvent(CreateMatchEventRequest request) {
        // Buscar el partido
        Match match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + request.getMatchId()));
        validateMatchIniciado(match);
        validateDataCaptureWindow(match);

        // Buscar el jugador que realiza el evento (Requisito 8.1)
        PlayerProfile player = playerProfileRepository.findById(request.getPlayerUuid())
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + request.getPlayerUuid()));

        // Buscar el equipo
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado: " + request.getTeamId()));

        // Verificar que el jugador participe en el partido
        if (!matchPlayerRepository.existsByMatchAndPlayer(match, player)) {
            throw new IllegalArgumentException("El jugador no participa en este partido");
        }

        // Crear el evento (Requisito 8.3) - registeredBy serÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ el mismo player por ahora
        UUID registeredByUuid = request.getRegisteredByUuid() != null
                ? request.getRegisteredByUuid()
                : request.getPlayerUuid();
        validateResponsibleActor(match, registeredByUuid);
        PlayerProfile registeredBy = playerProfileRepository.findById(registeredByUuid)
                .orElseThrow(() -> new IllegalArgumentException("Usuario responsable no encontrado: " + registeredByUuid));

        MatchEvent event = new MatchEvent(match, request.getEventType(), player, team, registeredBy);

        // Agregar asistente si es un gol y se especifica (Requisito 8.3)
        if (request.getEventType() == EventType.GOL && request.getAssistPlayerUuid() != null) {
            PlayerProfile assistPlayer = playerProfileRepository.findById(request.getAssistPlayerUuid())
                    .orElseThrow(() -> new IllegalArgumentException("Jugador asistente no encontrado: " + request.getAssistPlayerUuid()));
            event.setAssistPlayer(assistPlayer);
        }

        // En flujo de cierre centralizado (creador/capitan), dejamos el evento cerrado de inmediato
        // para evitar bloqueo por eventos pendientes al finalizar el partido.
        event.confirmByHome();
        event.confirmByAway();

        event = matchEventRepository.save(event);

        if (event.isGol()) {
            matchPendingEventClosureService.applyGoalToTeamScore(event);
        }
        return matchResponseMapper.toMatchEventResponse(event);
    }

    /**
     * Confirma un evento por parte de un equipo (local o visitante).
     * Requisito 8.2
     */
    public MatchEventResponse confirmEvent(Long eventId, UUID confirmingPlayerUuid, Boolean isLocalTeam) {
        MatchEvent event = matchEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado: " + eventId));
        validateMatchIniciado(event.getMatch());
        validateDataCaptureWindow(event.getMatch());

        PlayerProfile confirmingPlayer = playerProfileRepository.findById(confirmingPlayerUuid)
                .orElseThrow(() -> new IllegalArgumentException("Jugador confirmador no encontrado: " + confirmingPlayerUuid));

        // Verificar que el jugador participe en el partido
        if (!matchPlayerRepository.existsByMatchAndPlayer(event.getMatch(), confirmingPlayer)) {
            throw new IllegalArgumentException("El jugador no participa en este partido");
        }
        validateResponsibleActor(event.getMatch(), confirmingPlayerUuid);

        // Confirmar segÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Âºn el equipo
        if (isLocalTeam) {
            event.confirmByHome();
        } else {
            event.confirmByAway();
        }

        event = matchEventRepository.save(event);

        // Si estÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ completamente confirmado, actualizar estadÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â­sticas (Requisito 8.5)
        if (event.isFullyConfirmed() && event.isGol()) {
            matchPendingEventClosureService.applyGoalToTeamScore(event);
        }

        return matchResponseMapper.toMatchEventResponse(event);
    }

    /**
     * Cambia el estado de un partido.
     * Cuando un partido se marca como FINALIZADO, se actualizan automÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ticamente las calificaciones de los jugadores.
     * Requisito 6.5, 9.3
     */
    public MatchResponse changeMatchStatus(Long matchId, MatchStatus newStatus) {
        return changeMatchStatus(matchId, newStatus, null);
    }

    public MatchResponse changeMatchStatus(Long matchId, MatchStatus newStatus, UUID actorUuid) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + matchId));

        // Validar transiciones de estado
        matchStatusPolicy.validateTransition(match.getEstado(), newStatus);

        if (newStatus == MatchStatus.INICIADO && (match.getMatchTeams() == null || match.getMatchTeams().isEmpty())) {
            throw new IllegalArgumentException("El partido debe tener al menos 1 equipo para iniciar");
        }

        if (newStatus == MatchStatus.INICIADO) {
            matchStatusPolicy.markStarted(match, LocalDateTime.now());
        }

        if (newStatus == MatchStatus.FINALIZADO) {
            UUID effectiveActorUuid = actorUuid != null ? actorUuid : match.getCreador().getAtletaUuid();
            validateResponsibleActor(match, effectiveActorUuid);
            validateMatchIniciado(match);
            if (!matchRosterPolicy.hasMinimumConfirmedPlayers(match, matchPlayerRepository.findByMatch(match))) {
                match.setEstado(MatchStatus.INVALIDO);
                match.setValidationStatus(MatchValidationStatus.INVALID_CONFIRMATION_THRESHOLD);
                match.setValidationReason("Partido invalidado: no se alcanzo el minimo de confirmados");
                match = matchRepository.save(match);
                return matchResponseMapper.toMatchResponse(match);
            }
            matchPendingEventClosureService.closePendingEventsForFinalization(match);
            matchFinalScoreService.applyFinalScoreSnapshot(match);
            matchPlayerHistoryService.persistForFinalization(match);
            match.setFinalizedAt(LocalDateTime.now());
            match.setValidationStatus(MatchValidationStatus.VALID);
            match.setValidationReason(null);
        }

        if (newStatus == MatchStatus.INVALIDO) {
            matchStatusPolicy.markManualInvalid(match);
        }

        match.setEstado(newStatus);

        match = matchRepository.save(match);

        // Actualizar calificaciones automÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ticamente cuando el partido finaliza (Requisito 9.3)
        if (newStatus == MatchStatus.FINALIZADO) {
            try {
                matchPostMatchRatingService.updatePlayerRatingsAfterMatch(match);
                logger.info("Calificaciones actualizadas automÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ticamente para el partido {}", matchId);
            } catch (Exception e) {
                logger.error("Error actualizando calificaciones para el partido {}: {}", matchId, e.getMessage(), e);
                // No fallar el cambio de estado por errores en calificaciones
            }
        }

        return matchResponseMapper.toMatchResponse(match);
    }

    /**
     * Obtiene un partido por su ID con toda la informaciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n.
     */
    @Transactional
    public MatchResponse getMatchById(Long matchId) {
        return matchQueryService.getMatchById(matchId);
    }

    /**
     * Obtiene todos los partidos.
     */
    @Transactional
    public List<MatchResponse> getAllMatches() {
        return matchQueryService.getAllMatches();
    }

    /**
     * Obtiene prÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³ximos partidos.
     */
    @Transactional
    public List<MatchResponse> getUpcomingMatches() {
        return matchQueryService.getUpcomingMatches();
    }

    /**
     * Obtiene partidos donde participa un jugador.
     */
    @Transactional
    public List<MatchResponse> getMatchesByPlayer(UUID playerUuid) {
        return matchQueryService.getMatchesByPlayer(playerUuid);
    }

    /**
     * Obtiene partidos donde el usuario participa o es creador.
     */
    @Transactional
    public List<MatchResponse> getMatchesByPlayerOrCreator(UUID playerUuid) {
        return matchQueryService.getMatchesByPlayerOrCreator(playerUuid);
    }

    @Transactional(readOnly = true)
    public void requireLiveStreamAccess(Long matchId, UUID viewerUuid) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + matchId));
        boolean isCreator = match.getCreador() != null
                && match.getCreador().getAtletaUuid().equals(viewerUuid);
        boolean isParticipant = matchPlayerRepository
                .findByMatchAndPlayerAtletaUuid(match, viewerUuid)
                .isPresent();
        if (!isCreator && !isParticipant) {
            throw new AccessDeniedException("El stream es visible solo para participantes del partido");
        }
    }

    /**
     * Obtiene partidos donde participa un equipo.
     */
    @Transactional
    public List<MatchResponse> getMatchesByTeam(Long teamId) {
        return matchQueryService.getMatchesByTeam(teamId);
    }

    /**
     * Obtiene eventos de un partido.
     */
    @Transactional(readOnly = true)
    public List<MatchEventResponse> getMatchEvents(Long matchId) {
        return matchQueryService.getMatchEvents(matchId);
    }

    // MÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©todos privados de utilidad

    private Map<UUID, Integer> buildPersistedGoalsMap(Match match) {
        Map<UUID, Integer> goalsByPlayer = new HashMap<>();
        List<MatchEvent> events = matchEventRepository.findByMatchOrderByCreatedAt(match);
        for (MatchEvent event : events) {
            if (event.getTipoEvento() != EventType.GOL || event.getPlayer() == null || !event.isFullyConfirmed()) {
                continue;
            }
            UUID playerUuid = event.getPlayer().getAtletaUuid();
            goalsByPlayer.merge(playerUuid, 1, Integer::sum);
        }
        return goalsByPlayer;
    }

    private int estimateXpForPlayer(MatchPlayer player, int goals, int finalLocal, int finalAway, MatchTeamSide side) {
        int xp = 10; // jugar partido
        if (finalLocal == finalAway) {
            xp += 0;
        } else {
            boolean won = (side == MatchTeamSide.LOCAL && finalLocal > finalAway)
                    || (side == MatchTeamSide.VISITA && finalAway > finalLocal);
            xp += won ? 10 : 5;
        }

        String position = player.getPosition() != null && player.getPosition().getNombre() != null
                ? player.getPosition().getNombre().toUpperCase()
                : "";
        int goalXp;
        if (position.contains("DEF")) {
            goalXp = 15;
        } else if (position.contains("MED")) {
            goalXp = 12;
        } else {
            goalXp = 10;
        }
        xp += goals * goalXp;
        return xp;
    }

    private BigDecimal resolveCurrentHybridOvr(UUID playerUuid) {
        try {
            return ratingService.calculateHybridOVR(playerUuid);
        } catch (Exception ex) {
            return null;
        }
    }

    private void validateMatchIniciado(Match match) {
        if (match.getEstado() != MatchStatus.INICIADO) {
            throw new IllegalArgumentException("Solo se pueden registrar/confirmar eventos con el partido INICIADO");
        }
    }

    private void validateResponsibleActor(Match match, UUID actorUuid) {
        if (actorUuid == null) {
            throw new IllegalArgumentException("Se requiere usuario responsable");
        }

        if (match.getCreador() != null && actorUuid.equals(match.getCreador().getAtletaUuid())) {
            return;
        }

        List<MatchPlayer> captains = matchPlayerRepository.findCaptainsByMatch(match);
        boolean isCaptain = captains.stream()
                .anyMatch(mp -> mp.getPlayer() != null && actorUuid.equals(mp.getPlayer().getAtletaUuid()));
        if (!isCaptain) {
            throw new IllegalArgumentException("Solo el creador o capitanes pueden registrar/confirmar/finalizar");
        }
    }

    private void validateDataCaptureWindow(Match match) {
        if (match.getStartedAt() == null) {
            throw new IllegalArgumentException("El partido aun no tiene hora de inicio registrada");
        }

        LocalDateTime deadline = match.getStartedAt().plusHours(DATA_CAPTURE_WINDOW_HOURS);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new IllegalArgumentException("Se supero la ventana de 3 horas para cargar/cerrar datos");
        }
    }

    private void validateAllEventsClosed(Match match) {
        List<MatchEvent> pendingEvents = matchEventRepository.findPendingEventsByMatch(match);
        if (!pendingEvents.isEmpty()) {
            throw new IllegalArgumentException("No se puede finalizar: hay eventos pendientes de confirmacion");
        }
    }

    // MÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©todos de conversiÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n

    private void ensureCreatorParticipation(Match match, Team preferredTeam) {
        if (match == null || match.getCreador() == null) {
            return;
        }

        if (matchPlayerRepository.existsByMatchAndPlayer(match, match.getCreador())) {
            return;
        }

        Team team = preferredTeam != null ? preferredTeam : resolveDefaultTeamForCreator(match);
        if (team == null) {
            return;
        }

        Position position = resolveDefaultPositionForPlayer(match.getCreador());
        if (position == null) {
            throw new IllegalStateException("No se pudo resolver una posicion para el creador del partido");
        }

        MatchPlayer creatorParticipation = new MatchPlayer(match, team, match.getCreador(), position, PlayerRole.CAPITAN);
        creatorParticipation.setConfirmado(true);
        matchPlayerRepository.save(creatorParticipation);
    }

    private Team resolveDefaultTeamForCreator(Match match) {
        MatchTeam localTeam = matchTeamRepository.findLocalTeamByMatch(match).orElse(null);
        if (localTeam != null && localTeam.getTeam() != null) {
            return localTeam.getTeam();
        }

        MatchTeam awayTeam = matchTeamRepository.findVisitingTeamByMatch(match).orElse(null);
        if (awayTeam != null && awayTeam.getTeam() != null) {
            return awayTeam.getTeam();
        }

        List<MatchTeam> teams = matchTeamRepository.findByMatch(match);
        if (!teams.isEmpty() && teams.get(0).getTeam() != null) {
            return teams.get(0).getTeam();
        }

        return null;
    }

    private Position resolveDefaultPositionForPlayer(PlayerProfile player) {
        if (player == null) {
            return null;
        }

        PlayerPosition primaryPosition = playerPositionRepository.findPrimaryPositionByPlayer(player).orElse(null);
        if (primaryPosition != null && primaryPosition.getPosition() != null) {
            return primaryPosition.getPosition();
        }

        return positionRepository.findAllOrderByNombre().stream().findFirst().orElse(null);
    }

    private MatchTeamSide resolveTeamSide(Match match, Team team) {
        MatchTeam localTeam = matchTeamRepository.findLocalTeamByMatch(match).orElse(null);
        MatchTeam awayTeam = matchTeamRepository.findVisitingTeamByMatch(match).orElse(null);

        if (localTeam != null && localTeam.getTeam() != null && localTeam.getTeam().getId().equals(team.getId())) {
            return MatchTeamSide.LOCAL;
        }

        if (awayTeam != null && awayTeam.getTeam() != null && awayTeam.getTeam().getId().equals(team.getId())) {
            return MatchTeamSide.VISITA;
        }

        return null;
    }

}
