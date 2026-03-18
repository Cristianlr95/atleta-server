package com.atleta.demo.controller;

import com.atleta.demo.dto.request.CreateTeamRequest;
import com.atleta.demo.dto.response.TeamActiveMemberResponse;
import com.atleta.demo.dto.response.TeamResponse;
import com.atleta.demo.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
            @ApiResponse(responseCode = "404", description = "Jugador no encontrado")
    })
    public ResponseEntity<List<TeamResponse>> getTeamsByPlayer(@PathVariable UUID playerUuid) {
        try {
            return ResponseEntity.ok(teamService.getTeamsByPlayer(playerUuid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/by-creator/{creatorUuid}")
    @Operation(summary = "Listar equipos creados por un jugador",
            description = "Retorna equipos activos cuyo creador coincide con el jugador indicado")
    public ResponseEntity<List<TeamResponse>> getTeamsByCreator(@PathVariable UUID creatorUuid) {
        try {
            return ResponseEntity.ok(teamService.getTeamsByCreator(creatorUuid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{teamId}/members/active")
    @Operation(summary = "Listar miembros activos de equipo",
            description = "Retorna jugadores activos del equipo con su posicion principal")
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
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
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

    @DeleteMapping("/{teamId}")
    @Operation(summary = "Archivar equipo",
            description = "Archiva logicamente un equipo solo si el actor es el creador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Equipo archivado exitosamente"),
            @ApiResponse(responseCode = "403", description = "No autorizado para eliminar el equipo"),
            @ApiResponse(responseCode = "404", description = "Equipo o jugador no encontrado")
    })
    public ResponseEntity<Void> deleteTeam(@PathVariable Long teamId, @RequestParam UUID actorUuid) {
        logger.info("Solicitud de eliminacion de equipo {} por actor {}", teamId, actorUuid);

        try {
            teamService.deleteTeam(teamId, actorUuid);
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
