package com.atleta.demo.controller;

import com.atleta.demo.dto.request.CreateTeamRequest;
import com.atleta.demo.dto.request.UpdateTeamRequest;
import com.atleta.demo.dto.response.TeamActiveMemberResponse;
import com.atleta.demo.dto.response.TeamResponse;
import com.atleta.demo.security.AuthenticatedUserUtils;
import com.atleta.demo.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teams")
@Tag(name = "Equipos", description = "Creacion y gestion de equipos, membresias y estadisticas")
public class TeamController {

    private static final Logger logger = LoggerFactory.getLogger(TeamController.class);

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir logo de equipo",
            description = "Recibe una imagen y retorna una URL publica para usarla al crear el equipo")
    public ResponseEntity<Map<String, String>> uploadTeamLogo(@RequestPart("file") MultipartFile file) {
        try {
            String logoUrl = teamService.storeTeamLogo(file);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("logoUrl", logoUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/by-player/{playerUuid}")
    @Operation(summary = "Listar equipos asociados al jugador",
            description = "Retorna equipos donde el jugador es miembro activo y/o creador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Equipos obtenidos exitosamente"),
            @ApiResponse(responseCode = "404", description = "Jugador no encontrado"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<List<TeamResponse>> getTeamsByPlayer(
            @PathVariable UUID playerUuid,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUserUtils.requireSameUser(jwt, playerUuid);
        try {
            return ResponseEntity.ok(teamService.getTeamsByPlayer(playerUuid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/by-creator/{creatorUuid}")
    @Operation(summary = "Listar equipos creados por un jugador",
            description = "Retorna equipos activos cuyo creador coincide con el jugador indicado")
    public ResponseEntity<List<TeamResponse>> getTeamsByCreator(
            @PathVariable UUID creatorUuid,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUserUtils.requireSameUser(jwt, creatorUuid);
        try {
            return ResponseEntity.ok(teamService.getTeamsByCreator(creatorUuid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{teamId}")
    @Operation(summary = "Obtener detalle de equipo",
            description = "Retorna la informacion competitiva visible de un equipo activo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Equipo obtenido exitosamente"),
            @ApiResponse(responseCode = "404", description = "Equipo no encontrado"),
            @ApiResponse(responseCode = "401", description = "Sesion requerida")
    })
    public ResponseEntity<TeamResponse> getTeamById(@PathVariable Long teamId) {
        try {
            return ResponseEntity.ok(teamService.getTeamById(teamId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{teamId}/members/active")
    @Operation(summary = "Listar miembros activos de equipo",
            description = "Retorna jugadores activos con posicion principal, partidos y goles con el equipo y goles totales "
                    + "del historial de partidos finalizados; sin historial para el alcance, goles null")
    public ResponseEntity<List<TeamActiveMemberResponse>> getActiveMembersByTeam(@PathVariable Long teamId) {
        try {
            return ResponseEntity.ok(teamService.getActiveMembersByTeam(teamId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Crear nuevo equipo",
            description = "Crea un nuevo equipo con creador responsable, nombre unico y estadisticas inicializadas en cero")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Equipo creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos"),
            @ApiResponse(responseCode = "404", description = "Creador no encontrado"),
            @ApiResponse(responseCode = "409", description = "Ya existe un equipo con el mismo nombre")
    })
    public ResponseEntity<TeamResponse> createTeam(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateTeamRequest request
    ) {
        request.setCreadorUuid(AuthenticatedUserUtils.currentUserUuid(jwt));
        logger.info("Solicitud de creacion de equipo: {} por creador: {}", request.getNombre(), request.getCreadorUuid());

        try {
            TeamResponse response = teamService.createTeam(request);
            logger.info("Equipo creado exitosamente");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error en creacion de equipo: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PatchMapping(value = "/{teamId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Editar el nombre de un equipo activo como capitán vigente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipo actualizado"),
            @ApiResponse(responseCode = "400", description = "Se requiere únicamente el campo nombre con un valor válido"),
            @ApiResponse(responseCode = "401", description = "Sesión requerida"),
            @ApiResponse(responseCode = "403", description = "El actor no es el capitán activo"),
            @ApiResponse(responseCode = "404", description = "Equipo activo no encontrado"),
            @ApiResponse(responseCode = "409", description = "El nombre entra en conflicto con un equipo existente")
    })
    public ResponseEntity<TeamResponse> updateTeam(
            @PathVariable Long teamId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateTeamRequest request
    ) {
        UUID actorUuid = AuthenticatedUserUtils.currentUserUuid(jwt);
        try {
            return ResponseEntity.ok(teamService.updateTeam(teamId, actorUuid, request));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PutMapping("/{teamId}/captain")
    @Operation(summary = "Transferir la capitanía a otro integrante activo")
    public ResponseEntity<CaptainResponse> transferCaptain(
            @PathVariable Long teamId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TransferCaptainRequest request
    ) {
        try {
            UUID actorUuid = AuthenticatedUserUtils.currentUserUuid(jwt);
            UUID captainUuid = teamService.transferCaptain(teamId, actorUuid, request.targetPlayerUuid());
            return ResponseEntity.ok(new CaptainResponse(teamId, captainUuid));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    public record TransferCaptainRequest(@NotNull UUID targetPlayerUuid) {}

    public record CaptainResponse(Long teamId, UUID captainPlayerUuid) {}

    @DeleteMapping("/{teamId}/members/me")
    @Operation(summary = "Abandonar un equipo", description = "El capitán debe transferir el cargo antes de abandonar")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Membresía desactivada"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida o membresía ambigua"),
            @ApiResponse(responseCode = "401", description = "Sesión requerida"),
            @ApiResponse(responseCode = "404", description = "Equipo activo o membresía activa no encontrados"),
            @ApiResponse(responseCode = "409", description = "El actor es el capitán activo")
    })
    public ResponseEntity<Void> leaveTeam(@PathVariable Long teamId, @AuthenticationPrincipal Jwt jwt) {
        try {
            teamService.leaveTeam(teamId, AuthenticatedUserUtils.currentUserUuid(jwt));
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{teamId}/members/{playerUuid}")
    @Operation(summary = "Retirar a un jugador del equipo como capitán activo",
            description = "Desactiva la membresía del jugador y conserva su historial deportivo")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Membresía desactivada"),
            @ApiResponse(responseCode = "400", description = "El destinatario no es otro jugador activo con una sola membresía"),
            @ApiResponse(responseCode = "401", description = "Sesión requerida"),
            @ApiResponse(responseCode = "403", description = "El actor no es el capitán activo del equipo"),
            @ApiResponse(responseCode = "404", description = "Equipo activo o membresía activa no encontrados")
    })
    public ResponseEntity<Void> removeMember(
            @PathVariable Long teamId,
            @PathVariable UUID playerUuid,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            teamService.removeMember(teamId, AuthenticatedUserUtils.currentUserUuid(jwt), playerUuid);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{teamId}")
    @Operation(summary = "Archivar equipo",
            description = "Archiva el equipo como capitán activo identificado por el JWT. "
                    + "Un reintento por el mismo capitán devuelve 204 si el equipo ya está archivado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Equipo archivado o ya archivado"),
            @ApiResponse(responseCode = "401", description = "Sesión requerida"),
            @ApiResponse(responseCode = "403", description = "El actor no es el capitán activo o actorUuid no coincide con el JWT"),
            @ApiResponse(responseCode = "404", description = "Equipo no encontrado")
    })
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long teamId,
            @RequestParam UUID actorUuid,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID authenticatedActorUuid = AuthenticatedUserUtils.requireSameUser(jwt, actorUuid);
        logger.info("Solicitud de eliminacion de equipo {} por actor {}", teamId, authenticatedActorUuid);

        try {
            teamService.deleteTeam(teamId, authenticatedActorUuid);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Error de eliminacion de equipo {}: {}", teamId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            logger.warn("Intento no autorizado de eliminar equipo {}: {}", teamId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
