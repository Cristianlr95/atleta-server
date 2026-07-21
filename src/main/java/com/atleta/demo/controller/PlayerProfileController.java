package com.atleta.demo.controller;

import com.atleta.demo.dto.request.AddPlayerPositionRequest;
import com.atleta.demo.dto.request.CreatePlayerProfileRequest;
import com.atleta.demo.dto.request.UpdatePlayerProfileRequest;
import com.atleta.demo.dto.request.UpdateTrustScoreRequest;
import com.atleta.demo.dto.response.PlayerPositionResponse;
import com.atleta.demo.dto.response.PlayerProfileResponse;
import com.atleta.demo.dto.response.TrustLogResponse;
import com.atleta.demo.security.AuthenticatedUserUtils;
import com.atleta.demo.service.PlayerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/player-profiles")
@Tag(name = "Perfiles de Jugador", description = "Gestion de perfiles especificos de futbol, posiciones y trust score")
public class PlayerProfileController {

    private static final Logger logger = LoggerFactory.getLogger(PlayerProfileController.class);

    private final PlayerProfileService playerProfileService;

    public PlayerProfileController(PlayerProfileService playerProfileService) {
        this.playerProfileService = playerProfileService;
    }

    @PostMapping
    @Operation(summary = "Crear perfil de jugador",
            description = "Crea un nuevo perfil de jugador asociado a un atleta con trust score inicial de 100")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Perfil de jugador creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos"),
            @ApiResponse(responseCode = "404", description = "Atleta no encontrado"),
            @ApiResponse(responseCode = "409", description = "El atleta ya tiene un perfil o el alias ya existe")
    })
    public ResponseEntity<PlayerProfileResponse> createPlayerProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePlayerProfileRequest request
    ) {
        request.setAtletaUuid(AuthenticatedUserUtils.currentUserUuid(jwt));
        logger.info("Solicitud de creacion de perfil para atleta: {}", request.getAtletaUuid());

        try {
            PlayerProfileResponse response = playerProfileService.createPlayerProfile(request);
            logger.info("Perfil de jugador creado exitosamente para atleta: {}", request.getAtletaUuid());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error en creacion de perfil: {}", e.getMessage());
            String errorMessage = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (errorMessage.contains("atleta") && errorMessage.contains("no se encontr")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("/{atletaUuid}")
    @Operation(summary = "Obtener perfil de jugador por UUID",
            description = "Busca un perfil de jugador especifico por el UUID del atleta")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil de jugador encontrado"),
            @ApiResponse(responseCode = "404", description = "Perfil de jugador no encontrado"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<PlayerProfileResponse> getPlayerProfileByUuid(
            @Parameter(description = "UUID del atleta")
            @PathVariable UUID atletaUuid,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUserUtils.requireSameUser(jwt, atletaUuid);
        logger.debug("Buscando perfil de jugador por UUID: {}", atletaUuid);

        Optional<PlayerProfileResponse> profileOpt = playerProfileService.findByAtletaUuid(atletaUuid);
        return profileOpt.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{atletaUuid}/public")
    @Operation(summary = "Obtener vista publica de jugador",
            description = "Retorna datos competitivos del perfil sin exponer email, credenciales ni configuracion de seguridad")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil publico encontrado"),
            @ApiResponse(responseCode = "404", description = "Perfil publico no encontrado"),
            @ApiResponse(responseCode = "401", description = "Sesion requerida")
    })
    public ResponseEntity<PlayerProfileResponse> getPublicPlayerProfile(
            @Parameter(description = "UUID del atleta")
            @PathVariable UUID atletaUuid
    ) {
        Optional<PlayerProfileResponse> profileOpt = playerProfileService.findByAtletaUuid(atletaUuid);
        return profileOpt.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-alias/{alias}")
    @Operation(summary = "Obtener perfil de jugador por alias",
            description = "Busca un perfil de jugador especifico por su alias unico")
    public ResponseEntity<PlayerProfileResponse> getPlayerProfileByAlias(@PathVariable String alias) {
        logger.debug("Buscando perfil de jugador por alias: {}", alias);
        Optional<PlayerProfileResponse> profileOpt = playerProfileService.findByAlias(alias);
        return profileOpt.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{atletaUuid}")
    @Operation(summary = "Actualizar perfil de jugador", description = "Actualiza nombre, alias y posiciones en una transaccion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos"),
            @ApiResponse(responseCode = "404", description = "Perfil de jugador no encontrado"),
            @ApiResponse(responseCode = "409", description = "El alias ya esta en uso"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<PlayerProfileResponse> updatePlayerProfile(
            @Parameter(description = "UUID del atleta")
            @PathVariable UUID atletaUuid,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdatePlayerProfileRequest request
    ) {
        AuthenticatedUserUtils.requireSameUser(jwt, atletaUuid);
        logger.info("Actualizando perfil de jugador: {}", atletaUuid);

        try {
            PlayerProfileResponse response = playerProfileService.updateProfile(atletaUuid, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error actualizando perfil: {}", e.getMessage());
            if (e.getMessage().contains("No se encontro")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/positions")
    @Operation(summary = "Agregar posicion a jugador",
            description = "Agrega una posicion con prioridad especifica a un jugador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Posicion agregada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos"),
            @ApiResponse(responseCode = "404", description = "Jugador o posicion no encontrados"),
            @ApiResponse(responseCode = "409", description = "La prioridad ya esta ocupada o el jugador ya tiene esta posicion")
    })
    public ResponseEntity<PlayerPositionResponse> addPlayerPosition(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddPlayerPositionRequest request
    ) {
        request.setPlayerUuid(AuthenticatedUserUtils.currentUserUuid(jwt));
        logger.info("Agregando posicion {} con prioridad {} al jugador: {}",
                request.getPositionId(), request.getPrioridad(), request.getPlayerUuid());

        try {
            PlayerPositionResponse response = playerProfileService.addPlayerPosition(request);
            logger.info("Posicion agregada exitosamente al jugador: {}", request.getPlayerUuid());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error agregando posicion: {}", e.getMessage());
            if (e.getMessage().contains("No se encontro")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("/{atletaUuid}/positions")
    @Operation(summary = "Obtener posiciones de jugador",
            description = "Obtiene todas las posiciones de un jugador ordenadas por prioridad")
    public ResponseEntity<List<PlayerPositionResponse>> getPlayerPositions(
            @Parameter(description = "UUID del atleta")
            @PathVariable UUID atletaUuid,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUserUtils.requireSameUser(jwt, atletaUuid);
        logger.debug("Obteniendo posiciones del jugador: {}", atletaUuid);
        return ResponseEntity.ok(playerProfileService.getPlayerPositions(atletaUuid));
    }

    @DeleteMapping("/{atletaUuid}/positions/{positionId}")
    @Operation(summary = "Remover posicion de jugador",
            description = "Remueve una posicion especifica de un jugador")
    public ResponseEntity<Void> removePlayerPosition(
            @Parameter(description = "UUID del atleta")
            @PathVariable UUID atletaUuid,
            @Parameter(description = "ID de la posicion")
            @PathVariable Long positionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUserUtils.requireSameUser(jwt, atletaUuid);
        logger.info("Removiendo posicion {} del jugador: {}", positionId, atletaUuid);

        try {
            playerProfileService.removePlayerPosition(atletaUuid, positionId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Error removiendo posicion: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/trust-score")
    @Operation(summary = "Actualizar trust score",
            description = "Actualiza el trust score de un jugador y registra el cambio en trust_logs")
    public ResponseEntity<PlayerProfileResponse> updateTrustScore(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateTrustScoreRequest request
    ) {
        request.setPlayerUuid(AuthenticatedUserUtils.currentUserUuid(jwt));
        logger.info("Actualizando trust score del jugador: {} con cambio: {}",
                request.getPlayerUuid(), request.getCambio());

        try {
            PlayerProfileResponse response = playerProfileService.updateTrustScore(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error actualizando trust score: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{atletaUuid}/trust-history")
    @Operation(summary = "Obtener historial de trust score",
            description = "Obtiene el historial completo de cambios de trust score de un jugador")
    public ResponseEntity<List<TrustLogResponse>> getTrustScoreHistory(
            @Parameter(description = "UUID del atleta")
            @PathVariable UUID atletaUuid,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUserUtils.requireSameUser(jwt, atletaUuid);
        logger.debug("Obteniendo historial de trust score del jugador: {}", atletaUuid);
        return ResponseEntity.ok(playerProfileService.getTrustScoreHistory(atletaUuid));
    }

    @GetMapping("/by-trust-score")
    @Operation(summary = "Buscar jugadores por rango de trust score",
            description = "Busca jugadores que tengan un trust score dentro del rango especificado")
    public ResponseEntity<List<PlayerProfileResponse>> getPlayersByTrustScoreRange(
            @RequestParam Integer minScore,
            @RequestParam Integer maxScore
    ) {
        logger.debug("Buscando jugadores con trust score entre {} y {}", minScore, maxScore);
        return ResponseEntity.ok(playerProfileService.findByTrustScoreRange(minScore, maxScore));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar jugadores por nombre",
            description = "Busca jugadores que contengan el texto especificado en el nombre del atleta")
    public ResponseEntity<List<PlayerProfileResponse>> searchPlayersByAthleteName(@RequestParam String nombre) {
        logger.debug("Buscando jugadores por nombre de atleta: {}", nombre);
        return ResponseEntity.ok(playerProfileService.searchByAthleteName(nombre));
    }

    @PutMapping("/{atletaUuid}/positions/{positionId}/experience")
    @Operation(summary = "Agregar experiencia a posicion",
            description = "Agrega experiencia (XP) a una posicion especifica de un jugador")
    public ResponseEntity<Void> addExperienceToPosition(
            @Parameter(description = "UUID del atleta")
            @PathVariable UUID atletaUuid,
            @Parameter(description = "ID de la posicion")
            @PathVariable Long positionId,
            @Parameter(description = "XP a agregar")
            @RequestParam Integer xp,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUserUtils.requireSameUser(jwt, atletaUuid);
        logger.info("Agregando {} XP a la posicion {} del jugador: {}", xp, positionId, atletaUuid);

        try {
            playerProfileService.addExperienceToPosition(atletaUuid, positionId, xp);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Error agregando experiencia: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
