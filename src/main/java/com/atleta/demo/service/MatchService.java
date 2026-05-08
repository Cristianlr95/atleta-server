package com.atleta.demo.service;

import com.atleta.demo.dto.request.CreateMatchRequest;
import com.atleta.demo.dto.request.JoinMatchRequest;
import com.atleta.demo.dto.request.CreateMatchEventRequest;
import com.atleta.demo.dto.request.PlayerPerformanceDto;
import com.atleta.demo.dto.request.MatchClosePreviewRequest;
import com.atleta.demo.dto.response.*;
import com.atleta.demo.entity.*;
import com.atleta.demo.enums.MatchStatus;
import com.atleta.demo.enums.MatchValidationStatus;
import com.atleta.demo.enums.EventType;
import com.atleta.demo.enums.MatchResultType;
import com.atleta.demo.enums.MatchResult;
import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchGenderCategory;
import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.enums.RoleType;
import com.atleta.demo.enums.PriorityLevel;
import com.atleta.demo.enums.MatchTeamSide;
import com.atleta.demo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
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
import java.util.LinkedHashMap;
import java.math.BigDecimal;

/**
 * Servicio para la gestión de partidos, participación y eventos.
 * Implementa la lógica de negocio para los requisitos 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 7.4, 7.5
 * Integrado con el sistema de calificaciones para actualización automática al finalizar partidos.
 */
@Service
@Transactional
public class MatchService {

    private static final Logger logger = LoggerFactory.getLogger(MatchService.class);
    private static final long DATA_CAPTURE_WINDOW_HOURS = 3;
    private static final long MATCH_PLAY_WINDOW_HOURS = 1;

    private final MatchRepository matchRepository;
    private final MatchTeamRepository matchTeamRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchEventRepository matchEventRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final PositionRepository positionRepository;
    private final PlayerPositionRepository playerPositionRepository;
    private final PlayerHistoryRepository playerHistoryRepository;
    private final RatingService ratingService;

    public MatchService(MatchRepository matchRepository,
                        MatchTeamRepository matchTeamRepository,
                        MatchPlayerRepository matchPlayerRepository,
                        MatchEventRepository matchEventRepository,
                        PlayerProfileRepository playerProfileRepository,
                        TeamRepository teamRepository,
                        PositionRepository positionRepository,
                        TeamMemberRepository teamMemberRepository,
                        PlayerPositionRepository playerPositionRepository,
                        PlayerHistoryRepository playerHistoryRepository,
                        RatingService ratingService) {
        this.matchRepository = matchRepository;
        this.matchTeamRepository = matchTeamRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.matchEventRepository = matchEventRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.teamRepository = teamRepository;
        this.positionRepository = positionRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.playerPositionRepository = playerPositionRepository;
        this.playerHistoryRepository = playerHistoryRepository;
        this.ratingService = ratingService;
    }

    /**
     * Crea un nuevo partido con modalidad, fecha/hora, ubicación y cuota.
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
        return convertToResponse(match);
    }

    /**
     * Permite que un jugador se una a un partido con un equipo y posición específicos.
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

        // Buscar la posición (Requisito 7.2)
        Position position = positionRepository.findById(request.getPositionId())
                .orElseThrow(() -> new IllegalArgumentException("Posición no encontrada: " + request.getPositionId()));

        // Verificar que el jugador no esté ya registrado en el partido
        if (matchPlayerRepository.existsByMatchAndPlayer(match, player)) {
            throw new IllegalArgumentException("El jugador ya está registrado en este partido");
        }

        // Verificar que el equipo participe en el partido
        if (!matchTeamRepository.existsByMatchAndTeam(match, team)) {
            throw new IllegalArgumentException("El equipo no participa en este partido");
        }

        int playersPerTeamLimit = playersPerTeamByModality(match.getModalidad());
        long currentPlayersInTeam = matchPlayerRepository.findByMatch(match).stream()
                .filter(item -> item.getTeam() != null && item.getTeam().getId().equals(team.getId()))
                .count();
        if (currentPlayersInTeam >= playersPerTeamLimit) {
            throw new IllegalArgumentException("El equipo ya completo su cupo para esta modalidad");
        }

        // Crear la participación (Requisito 7.3, 7.4, 7.5)
        MatchPlayer matchPlayer = new MatchPlayer(match, team, player, position, request.getRol());
        matchPlayer.setTeamSide(resolveTeamSide(match, team));
        matchPlayer = matchPlayerRepository.save(matchPlayer);

        return convertToMatchPlayerResponse(matchPlayer);
    }

    /**
     * Confirma la participación de un jugador en un partido.
     * Requisito 7.3
     */
    public MatchPlayerResponse confirmParticipation(Long matchId, UUID playerUuid) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + matchId));

        PlayerProfile player = playerProfileRepository.findById(playerUuid)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + playerUuid));

        MatchPlayer matchPlayer = matchPlayerRepository.findByMatchAndPlayer(match, player)
                .orElseThrow(() -> new IllegalArgumentException("El jugador no está registrado en este partido"));

        matchPlayer.confirmarParticipacion();
        matchPlayer = matchPlayerRepository.save(matchPlayer);

        return convertToMatchPlayerResponse(matchPlayer);
    }

    public MatchPlayerResponse removePlayerFromMatch(Long matchId, UUID playerUuid) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + matchId));

        PlayerProfile player = playerProfileRepository.findById(playerUuid)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + playerUuid));

        MatchPlayer matchPlayer = matchPlayerRepository.findByMatchAndPlayer(match, player)
                .orElseThrow(() -> new IllegalArgumentException("El jugador no está registrado en este partido"));

        MatchPlayerResponse removed = convertToMatchPlayerResponse(matchPlayer);
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
        int playersPerTeamLimit = playersPerTeamByModality(match.getModalidad());
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
            imported.add(convertToMatchPlayerResponse(matchPlayer));
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

        // Verificar que no haya más de 2 equipos
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

        // Verificar que el equipo no esté ya en el partido
        if (matchTeamRepository.existsByMatchAndTeam(match, team)) {
            throw new IllegalArgumentException("El equipo ya participa en este partido");
        }

        MatchTeam matchTeam = new MatchTeam(match, team, esLocal);
        matchTeamRepository.save(matchTeam);
        match.addMatchTeam(matchTeam);
        ensureCreatorParticipation(match, team);

        return convertToResponse(match);
    }

    public MatchResponse updateTeamAssignments(Long matchId, UUID actorUuid, List<UUID> homePlayerUuids, List<UUID> awayPlayerUuids) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + matchId));

        ensureCreatorParticipation(match, resolveDefaultTeamForCreator(match));
        validateResponsibleActor(match, actorUuid);
        validateTeamAssignmentWindow(match);

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

        validateGenderAssignmentRules(match, players, homeSet, awaySet);

        matchPlayerRepository.saveAll(players);
        return convertToResponse(match);
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

        // Crear el evento (Requisito 8.3) - registeredBy será el mismo player por ahora
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
            updateMatchTeamGoals(event);
        }
        return convertToMatchEventResponse(event);
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

        // Confirmar según el equipo
        if (isLocalTeam) {
            event.confirmByHome();
        } else {
            event.confirmByAway();
        }

        event = matchEventRepository.save(event);

        // Si está completamente confirmado, actualizar estadísticas (Requisito 8.5)
        if (event.isFullyConfirmed() && event.isGol()) {
            updateMatchTeamGoals(event);
        }

        return convertToMatchEventResponse(event);
    }

    /**
     * Cambia el estado de un partido.
     * Cuando un partido se marca como FINALIZADO, se actualizan automáticamente las calificaciones de los jugadores.
     * Requisito 6.5, 9.3
     */
    public MatchResponse changeMatchStatus(Long matchId, MatchStatus newStatus) {
        return changeMatchStatus(matchId, newStatus, null);
    }

    public MatchResponse changeMatchStatus(Long matchId, MatchStatus newStatus, UUID actorUuid) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + matchId));

        // Validar transiciones de estado
        validateStatusTransition(match.getEstado(), newStatus);

        if (newStatus == MatchStatus.INICIADO && (match.getMatchTeams() == null || match.getMatchTeams().isEmpty())) {
            throw new IllegalArgumentException("El partido debe tener al menos 1 equipo para iniciar");
        }

        if (newStatus == MatchStatus.INICIADO) {
            if (match.getStartedAt() == null) {
                match.setStartedAt(LocalDateTime.now());
            }
            match.setValidationStatus(MatchValidationStatus.PENDING);
            match.setValidationReason(null);
        }

        if (newStatus == MatchStatus.FINALIZADO) {
            UUID effectiveActorUuid = actorUuid != null ? actorUuid : match.getCreador().getAtletaUuid();
            validateResponsibleActor(match, effectiveActorUuid);
            validateMatchIniciado(match);
            if (!hasMinimumConfirmedPlayers(match)) {
                match.setEstado(MatchStatus.INVALIDO);
                match.setValidationStatus(MatchValidationStatus.INVALID_CONFIRMATION_THRESHOLD);
                match.setValidationReason("Partido invalidado: no se alcanzo el minimo de confirmados");
                match = matchRepository.save(match);
                return convertToResponse(match);
            }
            closePendingEventsForFinalization(match);
            applyFinalScoreSnapshot(match);
            persistPlayerHistoryForFinalization(match);
            match.setFinalizedAt(LocalDateTime.now());
            match.setValidationStatus(MatchValidationStatus.VALID);
            match.setValidationReason(null);
        }

        if (newStatus == MatchStatus.INVALIDO) {
            match.setValidationStatus(MatchValidationStatus.INVALID_STATE);
            match.setValidationReason("Partido marcado como invalido manualmente");
        }

        match.setEstado(newStatus);

        match = matchRepository.save(match);

        // Actualizar calificaciones automáticamente cuando el partido finaliza (Requisito 9.3)
        if (newStatus == MatchStatus.FINALIZADO) {
            try {
                updatePlayerRatingsAfterMatch(match);
                logger.info("Calificaciones actualizadas automáticamente para el partido {}", matchId);
            } catch (Exception e) {
                logger.error("Error actualizando calificaciones para el partido {}: {}", matchId, e.getMessage(), e);
                // No fallar el cambio de estado por errores en calificaciones
            }
        }

        return convertToResponse(match);
    }

    /**
     * Obtiene un partido por su ID con toda la información.
     */
    @Transactional
    public MatchResponse getMatchById(Long matchId) {
        refreshAutomatedMatchStates();
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + matchId));
        ensureCreatorParticipation(match, resolveDefaultTeamForCreator(match));
        return convertToResponse(match);
    }

    /**
     * Obtiene todos los partidos.
     */
    @Transactional
    public List<MatchResponse> getAllMatches() {
        refreshAutomatedMatchStates();
        return matchRepository.findAll().stream()
                .peek(match -> ensureCreatorParticipation(match, resolveDefaultTeamForCreator(match)))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene próximos partidos.
     */
    @Transactional
    public List<MatchResponse> getUpcomingMatches() {
        refreshAutomatedMatchStates();
        return matchRepository.findUpcomingMatches().stream()
                .peek(match -> ensureCreatorParticipation(match, resolveDefaultTeamForCreator(match)))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene partidos donde participa un jugador.
     */
    @Transactional
    public List<MatchResponse> getMatchesByPlayer(UUID playerUuid) {
        refreshAutomatedMatchStates();
        PlayerProfile player = playerProfileRepository.findById(playerUuid)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + playerUuid));

        return matchRepository.findByPlayer(player).stream()
                .peek(match -> ensureCreatorParticipation(match, resolveDefaultTeamForCreator(match)))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene partidos donde el usuario participa o es creador.
     */
    @Transactional
    public List<MatchResponse> getMatchesByPlayerOrCreator(UUID playerUuid) {
        refreshAutomatedMatchStates();
        PlayerProfile player = playerProfileRepository.findById(playerUuid)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + playerUuid));

        List<Match> byPlayer = matchRepository.findByPlayer(player);
        List<Match> byCreator = matchRepository.findByCreador(player);

        LinkedHashMap<Long, Match> mergedById = new LinkedHashMap<>();
        for (Match match : byPlayer) {
            mergedById.put(match.getId(), match);
        }
        for (Match match : byCreator) {
            mergedById.put(match.getId(), match);
        }

        return mergedById.values().stream()
                .peek(match -> ensureCreatorParticipation(match, resolveDefaultTeamForCreator(match)))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene partidos donde participa un equipo.
     */
    @Transactional
    public List<MatchResponse> getMatchesByTeam(Long teamId) {
        refreshAutomatedMatchStates();
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado: " + teamId));

        return matchRepository.findByTeam(team).stream()
                .peek(match -> ensureCreatorParticipation(match, resolveDefaultTeamForCreator(match)))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene eventos de un partido.
     */
    @Transactional(readOnly = true)
    public List<MatchEventResponse> getMatchEvents(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + matchId));

        return matchEventRepository.findByMatchOrderByCreatedAt(match).stream()
                .map(this::convertToMatchEventResponse)
                .collect(Collectors.toList());
    }

    // Métodos privados de utilidad

    /**
     * Actualiza las calificaciones de todos los jugadores después de que un partido finaliza.
     * Recopila datos de rendimiento de eventos del partido y los envía al servicio de calificaciones.
     * Requisito 9.3
     */
    private void updatePlayerRatingsAfterMatch(Match match) {
        logger.debug("Iniciando actualización de calificaciones para el partido {}", match.getId());

        // Obtener equipos del partido
        List<MatchTeam> matchTeams = matchTeamRepository.findByMatch(match);
        if (matchTeams.size() != 2) {
            logger.warn("El partido {} no tiene exactamente 2 equipos, saltando actualización de calificaciones", match.getId());
            return;
        }

        // Determinar resultado del partido
        MatchResultType matchResult = determineMatchResult(matchTeams);
        logger.debug("Resultado del partido {}: {}", match.getId(), matchResult);

        // Obtener todos los jugadores del partido
        List<MatchPlayer> matchPlayers = matchPlayerRepository.findByMatch(match);
        if (matchPlayers.isEmpty()) {
            logger.warn("No hay jugadores registrados en el partido {}", match.getId());
            return;
        }

        // Obtener eventos del partido
        List<MatchEvent> matchEvents = matchEventRepository.findByMatchOrderByCreatedAt(match);

        // Recopilar datos de rendimiento por jugador
        List<PlayerPerformanceDto> performanceData = new ArrayList<>();
        
        for (MatchPlayer matchPlayer : matchPlayers) {
            if (!Boolean.TRUE.equals(matchPlayer.getConfirmado())) {
                logger.debug("Saltando jugador {} - no confirmó participación", matchPlayer.getPlayer().getAtletaUuid());
                continue;
            }

            PlayerPerformanceDto performance = collectPlayerPerformance(matchPlayer, matchEvents, matchResult, matchTeams);
            performanceData.add(performance);
            
            logger.debug("Datos recopilados para jugador {}: {} goles, {} asistencias, MVP: {}", 
                        performance.getPlayerProfileId(), 
                        performance.getGoalsScored(), 
                        performance.getAssistsMade(),
                        performance.getWasMvp());
        }

        // Enviar datos al servicio de calificaciones
        if (!performanceData.isEmpty()) {
            ratingService.updatePlayerRatings(match.getId(), performanceData);
            logger.info("Calificaciones actualizadas para {} jugadores del partido {}", 
                       performanceData.size(), match.getId());
        }
    }

    /**
     * Determina el resultado del partido basado en los goles de cada equipo.
     */
    private MatchResultType determineMatchResult(List<MatchTeam> matchTeams) {
        MatchTeam localTeam = matchTeams.stream()
                .filter(MatchTeam::getEsLocal)
                .findFirst()
                .orElse(null);
        
        MatchTeam visitingTeam = matchTeams.stream()
                .filter(mt -> !mt.getEsLocal())
                .findFirst()
                .orElse(null);

        if (localTeam == null || visitingTeam == null) {
            return MatchResultType.EMPATE; // Default si no se puede determinar
        }

        int localGoals = localTeam.getGoles();
        int visitingGoals = visitingTeam.getGoles();

        if (localGoals > visitingGoals) {
            return MatchResultType.GANADO;
        } else if (localGoals < visitingGoals) {
            return MatchResultType.PERDIDO;
        } else {
            return MatchResultType.EMPATE;
        }
    }

    /**
     * Recopila los datos de rendimiento de un jugador específico en el partido.
     */
    private PlayerPerformanceDto collectPlayerPerformance(MatchPlayer matchPlayer, 
                                                         List<MatchEvent> matchEvents, 
                                                         MatchResultType matchResult,
                                                         List<MatchTeam> matchTeams) {
        UUID playerUuid = matchPlayer.getPlayer().getAtletaUuid();
        
        // Contar goles del jugador
        int goalsScored = (int) matchEvents.stream()
                .filter(event -> event.getTipoEvento() == EventType.GOL)
                .filter(event -> event.isFullyConfirmed())
                .filter(event -> playerUuid.equals(event.getPlayer().getAtletaUuid()))
                .count();

        // Contar asistencias del jugador
        int assistsMade = (int) matchEvents.stream()
                .filter(event -> event.getTipoEvento() == EventType.GOL)
                .filter(event -> event.isFullyConfirmed())
                .filter(event -> event.getAssistPlayer() != null)
                .filter(event -> playerUuid.equals(event.getAssistPlayer().getAtletaUuid()))
                .count();

        // Determinar si fue MVP (por ahora, el jugador con más goles + asistencias)
        // TODO: Implementar lógica más sofisticada para MVP
        boolean wasMvp = false;
        int playerContributions = goalsScored + assistsMade;
        if (playerContributions > 0) {
            // Verificar si es el jugador con más contribuciones
            // Esta es una implementación simple - se puede mejorar
            wasMvp = isPlayerMvp(playerUuid, matchEvents);
        }

        // Mapear rol del jugador
        RoleType roleType = mapPositionToRole(matchPlayer.getPosition());

        // Determinar prioridad (por ahora basado en el rol - se puede mejorar)
        PriorityLevel priorityLevel = determinePriorityLevel(matchPlayer);

        // Calcular goles recibidos para roles defensivos
        Integer goalsConceded = null;
        if (roleType == RoleType.DEFENSA || roleType == RoleType.ARQUERO) {
            goalsConceded = calculateGoalsConceded(matchPlayer, matchTeams);
        }

        return new PlayerPerformanceDto(
                playerUuid,
                roleType,
                priorityLevel,
                goalsScored,
                assistsMade,
                goalsConceded,
                wasMvp,
                matchResult
        );
    }

    /**
     * Determina si un jugador es MVP basado en sus contribuciones.
     * Implementación simple: el jugador con más goles + asistencias.
     */
    private boolean isPlayerMvp(UUID playerUuid, List<MatchEvent> matchEvents) {
        Map<UUID, Integer> playerContributions = new HashMap<>();

        // Contar contribuciones por jugador
        for (MatchEvent event : matchEvents) {
            if (event.getTipoEvento() == EventType.GOL && event.isFullyConfirmed()) {
                // Gol
                UUID goalScorer = event.getPlayer().getAtletaUuid();
                playerContributions.merge(goalScorer, 1, Integer::sum);
                
                // Asistencia
                if (event.getAssistPlayer() != null) {
                    UUID assistPlayer = event.getAssistPlayer().getAtletaUuid();
                    playerContributions.merge(assistPlayer, 1, Integer::sum);
                }
            }
        }

        // Encontrar el máximo de contribuciones
        int maxContributions = playerContributions.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        // El jugador es MVP si tiene el máximo de contribuciones y es > 0
        return maxContributions > 0 && 
               playerContributions.getOrDefault(playerUuid, 0) == maxContributions;
    }

    /**
     * Mapea la posición del jugador a un rol del sistema de calificaciones.
     */
    private RoleType mapPositionToRole(Position position) {
        if (position == null || position.getNombre() == null) {
            return RoleType.MEDIOCAMPO; // Default
        }

        String positionName = position.getNombre().toLowerCase();
        
        if (positionName.contains("arquero") || positionName.contains("portero")) {
            return RoleType.ARQUERO;
        } else if (positionName.contains("defensa") || positionName.contains("defensor")) {
            return RoleType.DEFENSA;
        } else if (positionName.contains("delantero") || positionName.contains("atacante")) {
            return RoleType.ATAQUE;
        } else {
            return RoleType.MEDIOCAMPO; // Default para mediocampo y otros
        }
    }

    /**
     * Determina el nivel de prioridad del jugador.
     * Por ahora usa una lógica simple - se puede mejorar con datos adicionales.
     */
    private PriorityLevel determinePriorityLevel(MatchPlayer matchPlayer) {
        // Implementación simple: todos los jugadores confirmados son PRINCIPAL
        // Se puede mejorar con datos como tiempo de juego, importancia, etc.
        return PriorityLevel.PRINCIPAL;
    }

    /**
     * Calcula los goles recibidos para jugadores defensivos.
     */
    private Integer calculateGoalsConceded(MatchPlayer matchPlayer, List<MatchTeam> matchTeams) {
        // Encontrar el equipo del jugador
        Team playerTeam = matchPlayer.getTeam();
        
        // Encontrar el equipo contrario
        MatchTeam opposingTeam = matchTeams.stream()
                .filter(mt -> !mt.getTeam().getId().equals(playerTeam.getId()))
                .findFirst()
                .orElse(null);

        return opposingTeam != null ? opposingTeam.getGoles() : 0;
    }

    // Métodos privados de utilidad

    private void validateStatusTransition(MatchStatus currentStatus, MatchStatus newStatus) {
        // Validar transiciones válidas de estado
        switch (currentStatus) {
            case CREADO:
                if (newStatus != MatchStatus.INICIADO && newStatus != MatchStatus.INVALIDO) {
                    throw new IllegalArgumentException("Desde CREADO solo se puede pasar a INICIADO o INVALIDO");
                }
                break;
            case INICIADO:
                if (newStatus != MatchStatus.FINALIZADO && newStatus != MatchStatus.INVALIDO) {
                    throw new IllegalArgumentException("Desde INICIADO solo se puede pasar a FINALIZADO o INVALIDO");
                }
                break;
            case FINALIZADO:
            case INVALIDO:
                throw new IllegalArgumentException("No se puede cambiar el estado desde " + currentStatus);
        }
    }

    private void updateMatchTeamGoals(MatchEvent event) {
        // Actualizar goles del equipo en el partido
        MatchTeam matchTeam = matchTeamRepository.findByMatchAndTeam(event.getMatch(), event.getTeam())
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado en el partido"));

        matchTeam.incrementarGoles();
        matchTeamRepository.save(matchTeam);
    }

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

    private void closePendingEventsForFinalization(Match match) {
        List<MatchEvent> pendingEvents = matchEventRepository.findPendingEventsByMatch(match);
        if (pendingEvents.isEmpty()) {
            return;
        }

        for (MatchEvent event : pendingEvents) {
            boolean wasFullyConfirmed = event.isFullyConfirmed();

            if (!Boolean.TRUE.equals(event.getConfirmedByHome())) {
                event.confirmByHome();
            }
            if (!Boolean.TRUE.equals(event.getConfirmedByAway())) {
                event.confirmByAway();
            }

            if (!wasFullyConfirmed && event.isFullyConfirmed() && event.isGol()) {
                updateMatchTeamGoals(event);
            }
        }

        matchEventRepository.saveAll(pendingEvents);
        logger.info("Se cerraron {} eventos pendientes al finalizar partido {}", pendingEvents.size(), match.getId());
    }

    private void applyFinalScoreSnapshot(Match match) {
        int localGoals = 0;
        int awayGoals = 0;

        List<MatchEvent> events = matchEventRepository.findByMatchOrderByCreatedAt(match);
        for (MatchEvent event : events) {
            if (event.getTipoEvento() != EventType.GOL || event.getPlayer() == null || !event.isFullyConfirmed()) {
                continue;
            }

            MatchTeamSide eventSide = resolveEventTeamSide(match, event);
            if (eventSide == MatchTeamSide.VISITA) {
                awayGoals += 1;
            } else {
                localGoals += 1;
            }
        }

        List<MatchTeam> teams = matchTeamRepository.findByMatch(match);
        MatchTeam localTeam = teams.stream()
                .filter(MatchTeam::getEsLocal)
                .findFirst()
                .orElse(null);
        MatchTeam awayTeam = teams.stream()
                .filter(mt -> !mt.getEsLocal())
                .findFirst()
                .orElse(null);

        if (localTeam != null) {
            localTeam.setGoles(localGoals);
        }

        if (awayTeam != null) {
            awayTeam.setGoles(awayGoals);
        }

        if (localTeam != null || awayTeam != null) {
            matchTeamRepository.saveAll(
                    teams.stream()
                            .filter(team -> team == localTeam || team == awayTeam)
                            .collect(Collectors.toList())
            );
        }

        match.setFinalScoreLocal(localGoals);
        match.setFinalScoreAway(awayGoals);
    }

    private MatchTeamSide resolveEventTeamSide(Match match, MatchEvent event) {
        if (event == null || event.getPlayer() == null) {
            return MatchTeamSide.LOCAL;
        }

        MatchPlayer matchPlayer = matchPlayerRepository.findByMatchAndPlayer(match, event.getPlayer()).orElse(null);
        if (matchPlayer != null) {
            if (matchPlayer.getTeamSide() != null) {
                return matchPlayer.getTeamSide();
            }
            if (matchPlayer.getTeam() != null) {
                MatchTeamSide side = resolveTeamSide(match, matchPlayer.getTeam());
                if (side != null) {
                    return side;
                }
            }
        }

        if (event.getTeam() != null) {
            MatchTeamSide side = resolveTeamSide(match, event.getTeam());
            if (side != null) {
                return side;
            }
        }

        return MatchTeamSide.LOCAL;
    }

    private void persistPlayerHistoryForFinalization(Match match) {
        List<MatchPlayer> players = matchPlayerRepository.findByMatch(match);
        if (players.isEmpty()) {
            return;
        }
        int createdHistoryRows = 0;

        int finalLocal = match.getFinalScoreLocal() != null ? match.getFinalScoreLocal() : 0;
        int finalAway = match.getFinalScoreAway() != null ? match.getFinalScoreAway() : 0;

        Map<UUID, Integer> goalsByPlayer = new HashMap<>();
        Map<UUID, Integer> assistsByPlayer = new HashMap<>();
        List<MatchEvent> events = matchEventRepository.findByMatchOrderByCreatedAt(match);
        for (MatchEvent event : events) {
            if (event.getTipoEvento() != EventType.GOL || event.getPlayer() == null || !event.isFullyConfirmed()) {
                continue;
            }

            UUID scorerUuid = event.getPlayer().getAtletaUuid();
            goalsByPlayer.merge(scorerUuid, 1, Integer::sum);

            if (event.getAssistPlayer() != null) {
                UUID assistUuid = event.getAssistPlayer().getAtletaUuid();
                assistsByPlayer.merge(assistUuid, 1, Integer::sum);
            }
        }

        for (MatchPlayer matchPlayer : players) {
            if (!Boolean.TRUE.equals(matchPlayer.getConfirmado()) || matchPlayer.getPlayer() == null) {
                continue;
            }

            PlayerProfile player = matchPlayer.getPlayer();
            if (playerHistoryRepository.findByMatchAndPlayer(match, player).isPresent()) {
                continue;
            }

            if (matchPlayer.getTeam() == null || matchPlayer.getPosition() == null) {
                logger.warn("Se omite historial del jugador {} en partido {} por datos incompletos", player.getAtletaUuid(), match.getId());
                continue;
            }

            MatchTeamSide side = matchPlayer.getTeamSide() != null
                    ? matchPlayer.getTeamSide()
                    : resolveTeamSide(match, matchPlayer.getTeam());

            MatchResult result = resolvePlayerResult(side, finalLocal, finalAway);
            int goals = goalsByPlayer.getOrDefault(player.getAtletaUuid(), 0);
            int assists = assistsByPlayer.getOrDefault(player.getAtletaUuid(), 0);
            int xp = estimateXpForPlayer(matchPlayer, goals, finalLocal, finalAway, side != null ? side : MatchTeamSide.LOCAL);

            PlayerHistory history = new PlayerHistory(
                    match,
                    player,
                    matchPlayer.getTeam(),
                    matchPlayer.getPosition(),
                    goals,
                    assists,
                    result,
                    xp
            );
            playerHistoryRepository.save(history);
            createdHistoryRows += 1;

            playerPositionRepository.findByPlayerAndPosition(player, matchPlayer.getPosition())
                    .ifPresent(playerPosition -> {
                        playerPosition.addXp(xp);
                        playerPositionRepository.save(playerPosition);
                    });
        }
        logger.info("Historial de jugadores persistido para partido {}: {} filas nuevas", match.getId(), createdHistoryRows);
    }

    private MatchResult resolvePlayerResult(MatchTeamSide side, int finalLocal, int finalAway) {
        if (finalLocal == finalAway) {
            return MatchResult.EMPATE;
        }

        boolean localWon = finalLocal > finalAway;
        if (side == MatchTeamSide.VISITA) {
            return localWon ? MatchResult.DERROTA : MatchResult.VICTORIA;
        }

        return localWon ? MatchResult.VICTORIA : MatchResult.DERROTA;
    }

    private int playersPerTeamByModality(MatchMode modality) {
        if (modality == MatchMode.SEIS_VS_SEIS) {
            return 6;
        }
        if (modality == MatchMode.SIETE_VS_SIETE) {
            return 7;
        }
        return 5;
    }

    // Métodos de conversión

    private MatchResponse convertToResponse(Match match) {
        MatchResponse response = new MatchResponse();
        response.setId(match.getId());
        response.setModalidad(match.getModalidad());
        response.setCategoriaGenero(match.getCategoriaGenero());
        response.setFechaHoraProgramada(match.getFechaHoraProgramada());
        response.setLatitud(match.getLatitud());
        response.setLongitud(match.getLongitud());
        response.setCuota(match.getCuota());
        response.setEstado(match.getEstado());
        response.setStartedAt(match.getStartedAt());
        response.setFinalizedAt(match.getFinalizedAt());
        response.setValidationStatus(match.getValidationStatus());
        response.setValidationReason(match.getValidationReason());
        response.setFinalScoreLocal(match.getFinalScoreLocal());
        response.setFinalScoreAway(match.getFinalScoreAway());
        response.setClosePending(isClosePending(match));
        response.setMvpVotingClosedAt(match.getMvpVotingClosedAt());
        response.setCreatedAt(match.getCreatedAt());

        // Convertir creador
        if (match.getCreador() != null) {
            response.setCreador(convertToPlayerProfileResponse(match.getCreador()));
        }

        if (match.getMvpUser() != null) {
            response.setMvpUser(convertToPlayerProfileResponse(match.getMvpUser()));
        }

        // Convertir equipos
        List<MatchTeam> matchTeams = matchTeamRepository.findByMatch(match);
        response.setMatchTeams(matchTeams.stream()
                .map(this::convertToMatchTeamResponse)
                .collect(Collectors.toList()));

        // Convertir jugadores
        List<MatchPlayer> players = matchPlayerRepository.findByMatch(match);
        List<MatchPlayerResponse> playerResponses = players.stream()
                .map(this::convertToMatchPlayerResponse)
                .collect(Collectors.toCollection(ArrayList::new));

        if (match.getCreador() != null) {
            boolean creatorIncluded = playerResponses.stream()
                    .anyMatch(player -> player.getPlayer() != null
                            && match.getCreador().getAtletaUuid().equals(player.getPlayer().getAtletaUuid()));

            if (!creatorIncluded) {
                MatchPlayerResponse creatorResponse = new MatchPlayerResponse();
                creatorResponse.setId(-match.getId());
                creatorResponse.setPlayer(convertToPlayerProfileResponse(match.getCreador()));
                creatorResponse.setRol(PlayerRole.CAPITAN);
                creatorResponse.setConfirmado(true);
                playerResponses.add(creatorResponse);
            }
        }

        response.setPlayers(playerResponses);

        // Convertir eventos
        List<MatchEvent> events = matchEventRepository.findByMatchOrderByCreatedAt(match);
        response.setEvents(events.stream()
                .map(this::convertToMatchEventResponse)
                .collect(Collectors.toList()));

        return response;
    }

    private MatchPlayerResponse convertToMatchPlayerResponse(MatchPlayer matchPlayer) {
        MatchPlayerResponse response = new MatchPlayerResponse();
        response.setId(matchPlayer.getId());
        response.setRol(matchPlayer.getRol());
        response.setConfirmado(matchPlayer.getConfirmado());
        response.setTeamSide(matchPlayer.getTeamSide());

        if (matchPlayer.getPlayer() != null) {
            response.setPlayer(convertToPlayerProfileResponse(matchPlayer.getPlayer()));
        }

        if (matchPlayer.getTeam() != null) {
            response.setTeam(convertToTeamResponse(matchPlayer.getTeam()));
        }

        if (matchPlayer.getPosition() != null) {
            response.setPosition(convertToPositionResponse(matchPlayer.getPosition()));
        }

        return response;
    }

    private MatchTeamResponse convertToMatchTeamResponse(MatchTeam matchTeam) {
        MatchTeamResponse response = new MatchTeamResponse();
        response.setId(matchTeam.getId());
        response.setEsLocal(matchTeam.getEsLocal());
        response.setGoles(matchTeam.getGoles());

        if (matchTeam.getTeam() != null) {
            response.setTeam(convertToTeamResponse(matchTeam.getTeam()));
        }

        return response;
    }

    private MatchEventResponse convertToMatchEventResponse(MatchEvent event) {
        MatchEventResponse response = new MatchEventResponse();
        response.setId(event.getId());
        response.setEventType(event.getTipoEvento());
        response.setConfirmedByLocal(event.getConfirmedByHome());
        response.setConfirmedByVisitante(event.getConfirmedByAway());
        response.setCreatedAt(event.getRegisteredAt());

        if (event.getPlayer() != null) {
            response.setPlayer(convertToPlayerProfileResponse(event.getPlayer()));
        }

        if (event.getTeam() != null) {
            response.setTeam(convertToTeamResponse(event.getTeam()));
        }

        if (event.getAssistPlayer() != null) {
            response.setAssistPlayer(convertToPlayerProfileResponse(event.getAssistPlayer()));
        }

        return response;
    }

    private PlayerProfileResponse convertToPlayerProfileResponse(PlayerProfile profile) {
        PlayerProfileResponse response = new PlayerProfileResponse();
        response.setAtletaUuid(profile.getAtletaUuid());
        response.setAlias(profile.getAlias());
        response.setGenero(profile.getAthlete() != null ? profile.getAthlete().getGenero() : null);
        response.setTrustScore(profile.getTrustScore());
        response.setCreatedAt(profile.getCreatedAt());

        return response;
    }

    private void validateGenderAssignmentRules(Match match, List<MatchPlayer> players, Set<UUID> homeSet, Set<UUID> awaySet) {
        if (match == null || match.getCategoriaGenero() == null) {
            return;
        }

        int homeWomen = 0;
        int homeMen = 0;
        int awayWomen = 0;
        int awayMen = 0;

        for (MatchPlayer item : players) {
            if (item.getPlayer() == null || item.getPlayer().getAthlete() == null || item.getPlayer().getAtletaUuid() == null) {
                continue;
            }

            UUID playerUuid = item.getPlayer().getAtletaUuid();
            com.atleta.demo.enums.GenderType gender = item.getPlayer().getAthlete().getGenero();
            boolean assigned = homeSet.contains(playerUuid) || awaySet.contains(playerUuid);
            if (assigned && gender == null) {
                throw new IllegalArgumentException(
                        "Hay jugadores sin genero definido. Completa su perfil para poder asignar equipos por convocatoria"
                );
            }
            if (!assigned || gender == null) {
                continue;
            }

            if (homeSet.contains(playerUuid)) {
                if (gender == com.atleta.demo.enums.GenderType.FEMENINO) {
                    homeWomen += 1;
                } else {
                    homeMen += 1;
                }
                continue;
            }

            if (awaySet.contains(playerUuid)) {
                if (gender == com.atleta.demo.enums.GenderType.FEMENINO) {
                    awayWomen += 1;
                } else {
                    awayMen += 1;
                }
            }
        }

        if (match.getCategoriaGenero() == MatchGenderCategory.SOLO_MUJERES && (homeMen > 0 || awayMen > 0)) {
            throw new IllegalArgumentException("En convocatoria solo mujeres no se permiten hombres en los equipos");
        }

        if (match.getCategoriaGenero() == MatchGenderCategory.SOLO_HOMBRES && (homeWomen > 0 || awayWomen > 0)) {
            throw new IllegalArgumentException("En convocatoria solo hombres no se permiten mujeres en los equipos");
        }

        if (match.getCategoriaGenero() == MatchGenderCategory.MIXTO) {
            if (Math.abs(homeWomen - homeMen) > 1 || Math.abs(awayWomen - awayMen) > 1) {
                throw new IllegalArgumentException(
                        "En convocatoria mixta cada equipo debe quedar balanceado por genero (diferencia maxima de 1)"
                );
            }
        }
    }

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

    private TeamResponse convertToTeamResponse(Team team) {
        TeamResponse response = new TeamResponse();
        response.setId(team.getId());
        response.setNombre(team.getNombre());
        response.setLogoUrl(team.getLogoUrl());
        response.setAnioFundacion(team.getAnioFundacion());
        response.setCreatedAt(team.getCreatedAt());

        if (team.getCreador() != null) {
            response.setCreador(convertToPlayerProfileResponse(team.getCreador()));
        }

        return response;
    }

    private PositionResponse convertToPositionResponse(Position position) {
        PositionResponse response = new PositionResponse();
        response.setId(position.getId());
        response.setNombre(position.getNombre());
        return response;
    }

    private boolean isClosePending(Match match) {
        if (match == null || match.getEstado() != MatchStatus.INICIADO || match.getStartedAt() == null) {
            return false;
        }

        return LocalDateTime.now().isAfter(match.getStartedAt().plusHours(1));
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

    private void validateTeamAssignmentWindow(Match match) {
        if (match.getEstado() == MatchStatus.FINALIZADO) {
            throw new IllegalArgumentException("No se puede editar equipos en partido finalizado");
        }

        if (match.getStartedAt() != null || match.getEstado() == MatchStatus.INICIADO) {
            throw new IllegalArgumentException("Los equipos se bloquean al comenzar el partido");
        }
    }

    @Transactional
    protected void refreshAutomatedMatchStates() {
        autoStartReadyMatches();
        autoInvalidateExpiredMatches();
    }

    @Transactional
    protected void autoStartReadyMatches() {
        LocalDateTime now = LocalDateTime.now();
        List<Match> readyMatches = matchRepository.findCreatedMatchesReadyToStart(now);
        if (readyMatches.isEmpty()) {
            return;
        }

        List<Match> toStart = new ArrayList<>();
        for (Match match : readyMatches) {
            if (match.getEstado() != MatchStatus.CREADO) {
                continue;
            }
            if (!canAutoStart(match)) {
                continue;
            }

            match.setEstado(MatchStatus.INICIADO);
            if (match.getStartedAt() == null) {
                LocalDateTime scheduledAt = match.getFechaHoraProgramada();
                match.setStartedAt(scheduledAt != null ? scheduledAt : now);
            }
            match.setValidationStatus(MatchValidationStatus.PENDING);
            match.setValidationReason(null);
            toStart.add(match);
        }

        if (!toStart.isEmpty()) {
            matchRepository.saveAll(toStart);
            logger.info("Partidos iniciados automaticamente por hora/cupo completo: {}", toStart.size());
        }
    }

    @Transactional
    protected void autoInvalidateExpiredMatches() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdCutoff = now.minusHours(MATCH_PLAY_WINDOW_HOURS);
        LocalDateTime startedCutoff = now.minusHours(DATA_CAPTURE_WINDOW_HOURS);

        List<Match> expiredCreated = matchRepository.findExpiredCreatedMatches(createdCutoff);
        List<Match> expiredStarted = matchRepository.findExpiredStartedMatches(startedCutoff);
        List<Match> finalized = matchRepository.findByEstado(MatchStatus.FINALIZADO);
        if (expiredCreated.isEmpty() && expiredStarted.isEmpty() && finalized.isEmpty()) {
            return;
        }

        int invalidated = 0;

        for (Match match : expiredCreated) {
            if (match.getEstado() != MatchStatus.CREADO) {
                continue;
            }
            match.setEstado(MatchStatus.INVALIDO);
            match.setValidationStatus(MatchValidationStatus.INVALID_TIME_WINDOW);
            match.setValidationReason("Partido vencido automaticamente por no iniciar a tiempo");
            invalidated += 1;
        }

        for (Match match : expiredStarted) {
            if (match.getEstado() != MatchStatus.INICIADO) {
                continue;
            }
            match.setEstado(MatchStatus.INVALIDO);
            match.setValidationStatus(MatchValidationStatus.INVALID_TIME_WINDOW);
            match.setValidationReason("Partido vencido automaticamente por no cerrarse en la ventana permitida");
            invalidated += 1;
        }

        for (Match match : finalized) {
            if (match.getEstado() != MatchStatus.FINALIZADO) {
                continue;
            }
            if (hasMinimumConfirmedPlayers(match)) {
                continue;
            }
            if (!playerHistoryRepository.findByMatch(match).isEmpty()) {
                continue;
            }
            match.setEstado(MatchStatus.INVALIDO);
            match.setValidationStatus(MatchValidationStatus.INVALID_CONFIRMATION_THRESHOLD);
            match.setValidationReason("Partido invalidado automaticamente por cierre inconsistente sin minimo de confirmados");
            invalidated += 1;
        }

        if (invalidated > 0) {
            List<Match> toSave = new ArrayList<>(expiredCreated.size() + expiredStarted.size() + finalized.size());
            toSave.addAll(expiredCreated);
            toSave.addAll(expiredStarted);
            toSave.addAll(finalized);
            matchRepository.saveAll(toSave);
            logger.info("Partidos invalidados automaticamente por tiempo: {}", invalidated);
        }
    }

    private boolean canAutoStart(Match match) {
        if (match == null || match.getEstado() != MatchStatus.CREADO) {
            return false;
        }

        if (match.getFechaHoraProgramada() == null || match.getFechaHoraProgramada().isAfter(LocalDateTime.now())) {
            return false;
        }

        List<MatchTeam> teams = matchTeamRepository.findByMatch(match);
        if (teams.isEmpty()) {
            return false;
        }

        return hasMinimumConfirmedPlayers(match);
    }

    private boolean hasMinimumConfirmedPlayers(Match match) {
        List<MatchPlayer> players = matchPlayerRepository.findByMatch(match);
        long confirmed = players.stream().filter(item -> Boolean.TRUE.equals(item.getConfirmado())).count();
        boolean creatorIncluded = players.stream()
                .anyMatch(item -> item.getPlayer() != null
                        && match.getCreador() != null
                        && match.getCreador().getAtletaUuid().equals(item.getPlayer().getAtletaUuid()));
        if (match.getCreador() != null && !creatorIncluded) {
            confirmed += 1;
        }
        int minimum = playersPerTeamByModality(match.getModalidad()) * 2;
        return confirmed >= minimum;
    }
}
