package com.atleta.demo.controller;

import com.atleta.demo.dto.request.CreateMatchRequest;
import com.atleta.demo.dto.request.JoinMatchRequest;
import com.atleta.demo.dto.request.CreateMatchEventRequest;
import com.atleta.demo.dto.request.MatchClosePreviewRequest;
import com.atleta.demo.dto.request.UpdateMatchTeamAssignmentsRequest;
import com.atleta.demo.dto.request.VoteMatchMvpRequest;
import com.atleta.demo.dto.response.MatchClosePreviewResponse;
import com.atleta.demo.dto.response.MatchResponse;
import com.atleta.demo.dto.response.MatchPlayerResponse;
import com.atleta.demo.dto.response.MatchEventResponse;
import com.atleta.demo.dto.response.MatchMvpResponse;
import com.atleta.demo.enums.MatchStatus;
import com.atleta.demo.security.AuthenticatedUserUtils;
import com.atleta.demo.service.MatchLiveEventService;
import com.atleta.demo.service.MatchMvpService;
import com.atleta.demo.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la gestión de partidos, participación y eventos.
 * Proporciona endpoints para gestión de partidos, participación de jugadores y eventos.
 * 
 * Requisitos implementados:
 * - 6.1: Modalidad del partido (5v5, 6v6, 7v7)
 * - 6.2: Programación con fecha, hora y ubicación (latitud, longitud)
 * - 6.3: Estado inicial 'CREADO'
 * - 6.4: Cuota económica para el partido
 * - 6.5: Cambios de estado (CREADO, INICIADO, FINALIZADO, INVALIDO)
 * - 7.1: Asociación de jugador a equipo específico
 * - 7.2: Especificación de posición para el partido
 * - 7.3: Confirmación del jugador
 * - 7.4: Roles específicos para el partido (JUGADOR, CAPITAN, DT)
 * - 7.5: Actualización de lista de jugadores del partido
 * - 8.1: Registro de goles y asistencias
 * - 8.2: Confirmación de equipos local y visitante para eventos
 * - 8.3: Asociación de asistente opcional para goles
 * - 8.4: Trazabilidad de quién registró cada evento
 * - 8.5: Actualización automática de estadísticas
 */
@RestController
@RequestMapping("/api/v1/matches")
@Tag(name = "Partidos", description = "Organización de partidos, participación de jugadores y eventos")
public class MatchController {

    private static final Logger logger = LoggerFactory.getLogger(MatchController.class);

    private final MatchService matchService;
    private final MatchLiveEventService matchLiveEventService;
    private final MatchMvpService matchMvpService;

    public MatchController(
            MatchService matchService,
            MatchLiveEventService matchLiveEventService,
            MatchMvpService matchMvpService
    ) {
        this.matchService = matchService;
        this.matchLiveEventService = matchLiveEventService;
        this.matchMvpService = matchMvpService;
    }

    /**
     * Crea un nuevo partido con modalidad, fecha/hora, ubicación y cuota.
     * Requisitos: 6.1, 6.2, 6.3, 6.4
     */
    @PostMapping
    @Operation(summary = "Crear nuevo partido", 
               description = "Crea un nuevo partido con modalidad específica, fecha/hora programada, ubicación y cuota económica")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Partido creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Creador no encontrado")
    })
    public ResponseEntity<MatchResponse> createMatch(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateMatchRequest request
    ) {
        request.setCreadorUuid(AuthenticatedUserUtils.currentUserUuid(jwt));
        logger.info("Solicitud de creación de partido: modalidad {} por creador: {}", 
                   request.getModalidad(), request.getCreadorUuid());
        
        try {
            MatchResponse response = matchService.createMatch(request);
            logger.info("Partido creado exitosamente con ID: {}", response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error en creación de partido: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Obtiene un partido por su ID con toda la información.
     * Requisito: Consulta de información completa del partido
     */
    @GetMapping("/{matchId}")
    @Operation(summary = "Obtener partido por ID", 
               description = "Busca un partido específico por su ID con información completa de equipos, jugadores y eventos")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Partido encontrado"),
        @ApiResponse(responseCode = "404", description = "Partido no encontrado")
    })
    public ResponseEntity<MatchResponse> getMatchById(
            @Parameter(description = "ID del partido")
            @PathVariable Long matchId) {
        
        logger.debug("Buscando partido por ID: {}", matchId);
        
        try {
            MatchResponse response = matchService.getMatchById(matchId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Partido no encontrado: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Obtiene todos los partidos.
     * Requisito: Consulta general de partidos
     */
    @GetMapping
    @Operation(summary = "Obtener todos los partidos", 
               description = "Obtiene la lista completa de todos los partidos registrados")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de partidos obtenida exitosamente")
    })
    public ResponseEntity<List<MatchResponse>> getAllMatches() {
        logger.debug("Obteniendo todos los partidos");
        
        List<MatchResponse> matches = matchService.getAllMatches();
        return ResponseEntity.ok(matches);
    }

    /**
     * Obtiene próximos partidos programados.
     * Requisito: Consulta de partidos futuros
     */
    @GetMapping("/upcoming")
    @Operation(summary = "Obtener próximos partidos", 
               description = "Obtiene la lista de partidos programados para fechas futuras")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de próximos partidos obtenida exitosamente")
    })
    public ResponseEntity<List<MatchResponse>> getUpcomingMatches() {
        logger.debug("Obteniendo próximos partidos");
        
        List<MatchResponse> matches = matchService.getUpcomingMatches();
        return ResponseEntity.ok(matches);
    }

    /**
     * Obtiene partidos donde participa un jugador específico.
     * Requisito: 7.1 (consulta por participación de jugador)
     */
    @GetMapping("/by-player/{playerUuid}")
    @Operation(summary = "Obtener partidos por jugador", 
               description = "Obtiene todos los partidos donde participa un jugador específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de partidos del jugador obtenida exitosamente"),
        @ApiResponse(responseCode = "404", description = "Jugador no encontrado")
    })
    public ResponseEntity<List<MatchResponse>> getMatchesByPlayer(
            @Parameter(description = "UUID del jugador")
            @PathVariable UUID playerUuid,
            @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUserUtils.requireSameUser(jwt, playerUuid);
        
        logger.debug("Obteniendo partidos del jugador: {}", playerUuid);
        
        try {
            List<MatchResponse> matches = matchService.getMatchesByPlayer(playerUuid);
            return ResponseEntity.ok(matches);
        } catch (IllegalArgumentException e) {
            logger.warn("Jugador no encontrado: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Obtiene partidos donde participa un equipo específico.
     * Requisito: Consulta por participación de equipo
     */
    @GetMapping("/by-team/{teamId}")
    @Operation(summary = "Obtener partidos por equipo", 
               description = "Obtiene todos los partidos donde participa un equipo específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de partidos del equipo obtenida exitosamente"),
        @ApiResponse(responseCode = "404", description = "Equipo no encontrado")
    })
    public ResponseEntity<List<MatchResponse>> getMatchesByTeam(
            @Parameter(description = "ID del equipo")
            @PathVariable Long teamId) {
        
        logger.debug("Obteniendo partidos del equipo: {}", teamId);
        
        try {
            List<MatchResponse> matches = matchService.getMatchesByTeam(teamId);
            return ResponseEntity.ok(matches);
        } catch (IllegalArgumentException e) {
            logger.warn("Equipo no encontrado: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Cambia el estado de un partido.
     * Requisito: 6.5 (cambios de estado)
     */
    @PutMapping("/{matchId}/status")
    @Operation(summary = "Cambiar estado del partido", 
               description = "Cambia el estado del partido (CREADO → INICIADO → FINALIZADO o INVALIDO)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estado del partido actualizado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Transición de estado inválida"),
        @ApiResponse(responseCode = "404", description = "Partido no encontrado")
    })
    public ResponseEntity<MatchResponse> changeMatchStatus(
            @Parameter(description = "ID del partido")
            @PathVariable Long matchId,
            @Parameter(description = "Nuevo estado del partido")
            @RequestParam MatchStatus status,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "UUID del responsable que ejecuta el cambio (opcional)")
            @RequestParam(required = false) UUID actorUuid) {

        UUID authenticatedActorUuid = AuthenticatedUserUtils.currentUserUuid(jwt);
        logger.info("Cambiando estado del partido {} a: {} por {}", matchId, status, authenticatedActorUuid);

        try {
            MatchResponse response = matchService.changeMatchStatus(matchId, status, authenticatedActorUuid);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error cambiando estado del partido: {}", e.getMessage());
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        }
    }

    /**
     * Agrega un equipo a un partido (local o visitante).
     * Requisito: Un partido debe tener exactamente 2 equipos
     */
    @PostMapping("/{matchId}/teams/{teamId}")
    @Operation(summary = "Agregar equipo al partido", 
               description = "Agrega un equipo al partido como local o visitante (máximo 2 equipos por partido)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Equipo agregado al partido exitosamente"),
        @ApiResponse(responseCode = "400", description = "El partido ya tiene 2 equipos o el equipo ya participa"),
        @ApiResponse(responseCode = "404", description = "Partido o equipo no encontrado")
    })
    public ResponseEntity<MatchResponse> addTeamToMatch(
            @Parameter(description = "ID del partido")
            @PathVariable Long matchId,
            @Parameter(description = "ID del equipo")
            @PathVariable Long teamId,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Indica si el equipo es local (true) o visitante (false)")
            @RequestParam Boolean esLocal) {
        UUID authenticatedActorUuid = AuthenticatedUserUtils.currentUserUuid(jwt);

        logger.info("Agregando equipo {} al partido {} como {}",
                   teamId, matchId, esLocal ? "local" : "visitante");

        try {
            MatchResponse response = matchService.addTeamToMatch(matchId, teamId, esLocal, authenticatedActorUuid);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error agregando equipo al partido: {}", e.getMessage());
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        }
    }

    /**
     * Permite que un jugador se una a un partido con un equipo y posición específicos.
     * Requisitos: 7.1, 7.2, 7.4, 7.5
     */
    @PostMapping("/join")
    @Operation(summary = "Unirse a un partido", 
               description = "Permite que un jugador se una a un partido con un equipo y posición específicos")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Jugador unido al partido exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o el jugador ya está registrado"),
        @ApiResponse(responseCode = "404", description = "Partido, jugador, equipo o posición no encontrados")
    })
    public ResponseEntity<MatchPlayerResponse> joinMatch(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody JoinMatchRequest request
    ) {
        request.setPlayerUuid(AuthenticatedUserUtils.currentUserUuid(jwt));
        logger.info("Jugador {} uniéndose al partido {} con equipo {} en posición {}", 
                   request.getPlayerUuid(), request.getMatchId(), 
                   request.getTeamId(), request.getPositionId());
        
        try {
            MatchPlayerResponse response = matchService.joinMatch(request);
            logger.info("Jugador unido al partido exitosamente");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error uniendo jugador al partido: {}", e.getMessage());
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        }
    }

    /**
     * Confirma la participación de un jugador en un partido.
     * Requisito: 7.3 (confirmación del jugador)
     */

    @PostMapping("/{matchId}/teams/{teamId}/players/import")
    @Operation(summary = "Importar jugadores activos del equipo al partido",
               description = "Agrega al partido los jugadores activos del equipo con su posicion principal")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Jugadores importados exitosamente"),
        @ApiResponse(responseCode = "400", description = "El equipo no participa en el partido o no hay jugadores importables"),
        @ApiResponse(responseCode = "404", description = "Partido o equipo no encontrado")
    })
    public ResponseEntity<List<MatchPlayerResponse>> importTeamPlayers(
            @Parameter(description = "ID del partido")
            @PathVariable Long matchId,
            @Parameter(description = "ID del equipo")
            @PathVariable Long teamId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID authenticatedActorUuid = AuthenticatedUserUtils.currentUserUuid(jwt);

        logger.info("Importando jugadores del equipo {} al partido {}", teamId, matchId);

        try {
            List<MatchPlayerResponse> response = matchService.importTeamPlayers(matchId, teamId, authenticatedActorUuid);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error importando jugadores del equipo: {}", e.getMessage());
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        }
    }

    /**
     * Obtiene partidos donde el usuario participa o es creador.
     */
    @GetMapping("/by-player-or-creator/{playerUuid}")
    @Operation(summary = "Obtener partidos por jugador o creador",
               description = "Obtiene partidos donde el usuario participa o fue quien creo el partido")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de partidos obtenida exitosamente"),
        @ApiResponse(responseCode = "404", description = "Jugador no encontrado")
    })
    public ResponseEntity<List<MatchResponse>> getMatchesByPlayerOrCreator(
            @Parameter(description = "UUID del jugador")
            @PathVariable UUID playerUuid,
            @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUserUtils.requireSameUser(jwt, playerUuid);

        logger.debug("Obteniendo partidos por jugador o creador: {}", playerUuid);

        try {
            List<MatchResponse> matches = matchService.getMatchesByPlayerOrCreator(playerUuid);
            return ResponseEntity.ok(matches);
        } catch (IllegalArgumentException e) {
            logger.warn("Jugador no encontrado: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{matchId}/teams/assignment")
    @Operation(summary = "Guardar asignacion local/visita por jugador",
            description = "Persiste la asignacion de jugadores a LOCAL o VISITA para el partido")
    public ResponseEntity<MatchResponse> updateTeamAssignments(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateMatchTeamAssignmentsRequest request
    ) {
        request.setActorUuid(AuthenticatedUserUtils.currentUserUuid(jwt));
        logger.info("Actualizando asignacion de equipos del partido {}. home={}, away={}",
                matchId,
                request.getHomePlayerUuids() != null ? request.getHomePlayerUuids().size() : 0,
                request.getAwayPlayerUuids() != null ? request.getAwayPlayerUuids().size() : 0);

        try {
            MatchResponse response = matchService.updateTeamAssignments(
                    matchId,
                    request.getActorUuid(),
                    request.getHomePlayerUuids(),
                    request.getAwayPlayerUuids()
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error actualizando asignacion de equipos: {}", e.getMessage());
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{matchId}/players/{playerUuid}")
    @Operation(summary = "Quitar jugador del partido",
               description = "Elimina un jugador previamente agregado al partido")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Jugador eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Partido, jugador o participacion no encontrados")
    })
    public ResponseEntity<MatchPlayerResponse> removePlayerFromMatch(
            @Parameter(description = "ID del partido")
            @PathVariable Long matchId,
            @Parameter(description = "UUID del jugador")
            @PathVariable UUID playerUuid,
            @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUserUtils.requireSameUser(jwt, playerUuid);

        logger.info("Eliminando jugador {} del partido {}", playerUuid, matchId);

        try {
            MatchPlayerResponse response = matchService.removePlayerFromMatch(matchId, playerUuid);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error eliminando jugador del partido: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    @PutMapping("/{matchId}/players/{playerUuid}/confirm")
    @Operation(summary = "Confirmar participación en partido", 
               description = "Confirma la participación de un jugador en un partido específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Participación confirmada exitosamente"),
        @ApiResponse(responseCode = "404", description = "Partido, jugador no encontrados o el jugador no está registrado en el partido")
    })
    public ResponseEntity<MatchPlayerResponse> confirmParticipation(
            @Parameter(description = "ID del partido")
            @PathVariable Long matchId,
            @Parameter(description = "UUID del jugador")
            @PathVariable UUID playerUuid,
            @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUserUtils.requireSameUser(jwt, playerUuid);
        
        logger.info("Confirmando participación del jugador {} en el partido {}", playerUuid, matchId);
        
        try {
            MatchPlayerResponse response = matchService.confirmParticipation(matchId, playerUuid);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error confirmando participación: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Registra un evento durante un partido (gol o asistencia).
     * Requisitos: 8.1, 8.3, 8.4
     */
    @PostMapping("/events")
    @Operation(summary = "Registrar evento de partido", 
               description = "Registra un evento (gol o asistencia) durante un partido con asistente opcional")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Evento registrado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o el jugador no participa en el partido"),
        @ApiResponse(responseCode = "404", description = "Partido, jugador o equipo no encontrados")
    })
    public ResponseEntity<MatchEventResponse> registerEvent(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateMatchEventRequest request
    ) {
        request.setRegisteredByUuid(AuthenticatedUserUtils.currentUserUuid(jwt));
        logger.info("Registrando evento {} del jugador {} en el partido {}",
                   request.getEventType(), request.getPlayerUuid(), request.getMatchId());
        
        try {
            MatchEventResponse response = matchService.registerEvent(request);
            logger.info("Evento registrado exitosamente con ID: {}", response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error registrando evento: {}", e.getMessage());
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        }
    }

    /**
     * Confirma un evento por parte de un equipo (local o visitante).
     * Requisitos: 8.2, 8.5 (confirmación de equipos y actualización automática de estadísticas)
     */
    @PutMapping("/events/{eventId}/confirm")
    @Operation(summary = "Confirmar evento de partido", 
               description = "Confirma un evento por parte del equipo local o visitante, actualizando estadísticas automáticamente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Evento confirmado exitosamente"),
        @ApiResponse(responseCode = "400", description = "El jugador no participa en el partido"),
        @ApiResponse(responseCode = "404", description = "Evento o jugador confirmador no encontrados")
    })
    public ResponseEntity<MatchEventResponse> confirmEvent(
            @Parameter(description = "ID del evento")
            @PathVariable Long eventId,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "UUID del jugador que confirma")
            @RequestParam UUID confirmingPlayerUuid,
            @Parameter(description = "Indica si confirma el equipo local (true) o visitante (false)")
            @RequestParam Boolean isLocalTeam) {

        UUID authenticatedConfirmingUuid = AuthenticatedUserUtils.currentUserUuid(jwt);
        logger.info("Confirmando evento {} por jugador {} del equipo {}",
                   eventId, authenticatedConfirmingUuid, isLocalTeam ? "local" : "visitante");

        try {
            MatchEventResponse response = matchService.confirmEvent(eventId, authenticatedConfirmingUuid, isLocalTeam);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error confirmando evento: {}", e.getMessage());
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        }
    }

    /**
     * Obtiene todos los eventos de un partido específico.
     * Requisito: 8.4 (consulta de eventos con trazabilidad)
     */
    @GetMapping("/{matchId}/events")
    @Operation(summary = "Obtener eventos del partido", 
               description = "Obtiene todos los eventos registrados en un partido específico ordenados por fecha")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de eventos obtenida exitosamente"),
        @ApiResponse(responseCode = "404", description = "Partido no encontrado")
    })
    public ResponseEntity<List<MatchEventResponse>> getMatchEvents(
            @Parameter(description = "ID del partido")
            @PathVariable Long matchId) {
        
        logger.debug("Obteniendo eventos del partido: {}", matchId);
        
        try {
            List<MatchEventResponse> events = matchService.getMatchEvents(matchId);
            return ResponseEntity.ok(events);
        } catch (IllegalArgumentException e) {
            logger.warn("Partido no encontrado: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/{matchId}/live", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Suscribirse al stream en vivo del partido",
               description = "SSE para cambios de invitaciones y estado del partido")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stream conectado"),
        @ApiResponse(responseCode = "401", description = "Sesion requerida"),
        @ApiResponse(responseCode = "403", description = "No participa en el partido"),
        @ApiResponse(responseCode = "404", description = "Partido no encontrado")
    })
    public ResponseEntity<SseEmitter> subscribeMatchLive(
            @Parameter(description = "ID del partido")
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID viewerUuid = AuthenticatedUserUtils.currentUserUuid(jwt);
        matchService.requireLiveStreamAccess(matchId, viewerUuid);
        logger.debug("Abriendo stream SSE para partido {} y usuario {}", matchId, viewerUuid);
        return ResponseEntity.ok(matchLiveEventService.subscribe(matchId));
    }

    @PostMapping("/{matchId}/close/preview")
    @Operation(summary = "Vista previa de cierre del partido",
            description = "Calcula XP estimada y OVR actual por jugador para confirmar cierre")
    public ResponseEntity<MatchClosePreviewResponse> getClosePreview(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody(required = false) MatchClosePreviewRequest request
    ) {
        try {
            UUID authenticatedActorUuid = AuthenticatedUserUtils.currentUserUuid(jwt);
            return ResponseEntity.ok(matchService.getClosePreview(matchId, request, authenticatedActorUuid));
        } catch (IllegalArgumentException e) {
            logger.warn("Error generando preview de cierre para partido {}: {}", matchId, e.getMessage());
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{matchId}/mvp")
    @Operation(summary = "Obtener estado de votacion MVP",
            description = "Devuelve estado de la votacion MVP, candidatos, voto actual y ganador si ya cerro")
    public ResponseEntity<MatchMvpResponse> getMvpState(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            UUID voterUserId = AuthenticatedUserUtils.currentUserUuid(jwt);
            return ResponseEntity.ok(matchMvpService.getMvpState(matchId, voterUserId));
        } catch (IllegalArgumentException e) {
            logger.warn("Error obteniendo MVP para partido {}: {}", matchId, e.getMessage());
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{matchId}/mvp/vote")
    @Operation(summary = "Votar MVP",
            description = "Registra o actualiza voto MVP de un participante confirmado dentro de la ventana de 3 horas")
    public ResponseEntity<MatchMvpResponse> voteMvp(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody VoteMatchMvpRequest request
    ) {
        try {
            UUID voterUserId = AuthenticatedUserUtils.currentUserUuid(jwt);
            return ResponseEntity.ok(matchMvpService.vote(matchId, voterUserId, request.getVotedUserId()));
        } catch (IllegalArgumentException e) {
            logger.warn("Error votando MVP para partido {}: {}", matchId, e.getMessage());
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

}





