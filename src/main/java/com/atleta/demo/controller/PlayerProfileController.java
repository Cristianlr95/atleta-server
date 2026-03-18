package com.atleta.demo.controller;

import com.atleta.demo.dto.request.AddPlayerPositionRequest;
import com.atleta.demo.dto.request.CreatePlayerProfileRequest;
import com.atleta.demo.dto.request.UpdatePlayerProfileRequest;
import com.atleta.demo.dto.request.UpdateTrustScoreRequest;
import com.atleta.demo.dto.response.PlayerPositionResponse;
import com.atleta.demo.dto.response.PlayerProfileResponse;
import com.atleta.demo.dto.response.TrustLogResponse;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controlador REST para la gestión de perfiles de jugadores.
 * Proporciona endpoints para gestión de perfiles y posiciones.
 * 
 * Requisitos implementados:
 * - 2.1: Asociación de perfil a atleta existente
 * - 2.2: Trust score inicial de 100
 * - 2.3: Alias único para contexto de fútbol
 * - 2.4: Relación uno-a-uno entre atleta y perfil
 * - 2.5: Registro de cambios de trust score en trust_logs
 * - 3.2: Asignación de prioridades (1, 2, 3)
 * - 3.3: Contador de experiencia (XP) por posición
 * - 3.4: Validación de prioridades únicas por jugador
 * - 3.5: Múltiples posiciones con diferentes prioridades
 */
@RestController
@RequestMapping("/api/v1/player-profiles")
@Tag(name = "Perfiles de Jugador", description = "Gestión de perfiles específicos de fútbol, posiciones y trust score")
public class PlayerProfileController {

    private static final Logger logger = LoggerFactory.getLogger(PlayerProfileController.class);

    private final PlayerProfileService playerProfileService;

    public PlayerProfileController(PlayerProfileService playerProfileService) {
        this.playerProfileService = playerProfileService;
    }

    /**
     * Crea un nuevo perfil de jugador asociado a un atleta.
     * Requisitos: 2.1, 2.2, 2.3, 2.4
     */
    @PostMapping
    @Operation(summary = "Crear perfil de jugador", 
               description = "Crea un nuevo perfil de jugador asociado a un atleta con trust score inicial de 100")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Perfil de jugador creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Atleta no encontrado"),
        @ApiResponse(responseCode = "409", description = "El atleta ya tiene un perfil o el alias ya existe")
    })
    public ResponseEntity<PlayerProfileResponse> createPlayerProfile(
            @Valid @RequestBody CreatePlayerProfileRequest request) {
        
        logger.info("Solicitud de creación de perfil para atleta: {}", request.getAtletaUuid());
        
        try {
            PlayerProfileResponse response = playerProfileService.createPlayerProfile(request);
            logger.info("Perfil de jugador creado exitosamente para atleta: {}", request.getAtletaUuid());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error en creación de perfil: {}", e.getMessage());
            if (e.getMessage().contains("No se encontró atleta")) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        }
    }

    /**
     * Obtiene un perfil de jugador por UUID del atleta.
     * Requisito: 2.1 (búsqueda por asociación con atleta)
     */
    @GetMapping("/{atletaUuid}")
    @Operation(summary = "Obtener perfil de jugador por UUID", 
               description = "Busca un perfil de jugador específico por el UUID del atleta")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil de jugador encontrado"),
        @ApiResponse(responseCode = "404", description = "Perfil de jugador no encontrado")
    })
    public ResponseEntity<PlayerProfileResponse> getPlayerProfileByUuid(
            @Parameter(description = "UUID del atleta")
            @PathVariable UUID atletaUuid) {
        
        logger.debug("Buscando perfil de jugador por UUID: {}", atletaUuid);
        
        Optional<PlayerProfileResponse> profileOpt = playerProfileService.findByAtletaUuid(atletaUuid);
        
        return profileOpt.map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtiene un perfil de jugador por alias.
     * Requisito: 2.3 (búsqueda por alias único)
     */
    @GetMapping("/by-alias/{alias}")
    @Operation(summary = "Obtener perfil de jugador por alias", 
               description = "Busca un perfil de jugador específico por su alias único")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil de jugador encontrado"),
        @ApiResponse(responseCode = "404", description = "Perfil de jugador no encontrado")
    })
    public ResponseEntity<PlayerProfileResponse> getPlayerProfileByAlias(
            @Parameter(description = "Alias único del jugador")
            @PathVariable String alias) {
        
        logger.debug("Buscando perfil de jugador por alias: {}", alias);
        
        Optional<PlayerProfileResponse> profileOpt = playerProfileService.findByAlias(alias);
        
        return profileOpt.map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Actualiza el alias de un perfil de jugador.
     * Requisito: 2.3 (actualización de alias único)
     */
    @PutMapping("/{atletaUuid}")
    @Operation(summary = "Actualizar perfil de jugador", 
               description = "Actualiza el alias del perfil de jugador")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil actualizado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Perfil de jugador no encontrado"),
        @ApiResponse(responseCode = "409", description = "El alias ya está en uso")
    })
    public ResponseEntity<PlayerProfileResponse> updatePlayerProfile(
            @Parameter(description = "UUID del atleta")
            @PathVariable UUID atletaUuid,
            @Valid @RequestBody UpdatePlayerProfileRequest request) {
        
        logger.info("Actualizando perfil de jugador: {}", atletaUuid);
        
        try {
            PlayerProfileResponse response = playerProfileService.updateAlias(atletaUuid, request.getAlias());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error actualizando perfil: {}", e.getMessage());
            if (e.getMessage().contains("No se encontró")) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        }
    }

    /**
     * Agrega una posición a un jugador con prioridad específica.
     * Requisitos: 3.2, 3.4, 3.5
     */
    @PostMapping("/positions")
    @Operation(summary = "Agregar posición a jugador", 
               description = "Agrega una posición con prioridad específica a un jugador")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Posición agregada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Jugador o posición no encontrados"),
        @ApiResponse(responseCode = "409", description = "La prioridad ya está ocupada o el jugador ya tiene esta posición")
    })
    public ResponseEntity<PlayerPositionResponse> addPlayerPosition(
            @Valid @RequestBody AddPlayerPositionRequest request) {
        
        logger.info("Agregando posición {} con prioridad {} al jugador: {}", 
                   request.getPositionId(), request.getPrioridad(), request.getPlayerUuid());
        
        try {
            PlayerPositionResponse response = playerProfileService.addPlayerPosition(request);
            logger.info("Posición agregada exitosamente al jugador: {}", request.getPlayerUuid());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error agregando posición: {}", e.getMessage());
            if (e.getMessage().contains("No se encontró")) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        }
    }

    /**
     * Obtiene todas las posiciones de un jugador ordenadas por prioridad.
     * Requisitos: 3.2, 3.4, 3.5
     */
    @GetMapping("/{atletaUuid}/positions")
    @Operation(summary = "Obtener posiciones de jugador", 
               description = "Obtiene todas las posiciones de un jugador ordenadas por prioridad")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Posiciones obtenidas exitosamente")
    })
    public ResponseEntity<List<PlayerPositionResponse>> getPlayerPositions(
            @Parameter(description = "UUID del atleta")
            @PathVariable UUID atletaUuid) {
        
        logger.debug("Obteniendo posiciones del jugador: {}", atletaUuid);
        
        List<PlayerPositionResponse> positions = playerProfileService.getPlayerPositions(atletaUuid);
        return ResponseEntity.ok(positions);
    }

    /**
     * Remueve una posición de un jugador.
     * Requisito: 3.5 (gestión de múltiples posiciones)
     */
    @DeleteMapping("/{atletaUuid}/positions/{positionId}")
    @Operation(summary = "Remover posición de jugador", 
               description = "Remueve una posición específica de un jugador")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Posición removida exitosamente"),
        @ApiResponse(responseCode = "404", description = "Jugador, posición no encontrados o el jugador no tiene esta posición")
    })
    public ResponseEntity<Void> removePlayerPosition(
            @Parameter(description = "UUID del atleta")
            @PathVariable UUID atletaUuid,
            @Parameter(description = "ID de la posición")
            @PathVariable Long positionId) {
        
        logger.info("Removiendo posición {} del jugador: {}", positionId, atletaUuid);
        
        try {
            playerProfileService.removePlayerPosition(atletaUuid, positionId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Error removiendo posición: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Actualiza el trust score de un jugador.
     * Requisito: 2.5 (registro de cambios en trust_logs)
     */
    @PutMapping("/trust-score")
    @Operation(summary = "Actualizar trust score", 
               description = "Actualiza el trust score de un jugador y registra el cambio en trust_logs")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trust score actualizado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Jugador no encontrado")
    })
    public ResponseEntity<PlayerProfileResponse> updateTrustScore(
            @Valid @RequestBody UpdateTrustScoreRequest request) {
        
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

    /**
     * Obtiene el historial de cambios de trust score de un jugador.
     * Requisito: 2.5 (consulta de trust_logs)
     */
    @GetMapping("/{atletaUuid}/trust-history")
    @Operation(summary = "Obtener historial de trust score", 
               description = "Obtiene el historial completo de cambios de trust score de un jugador")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Historial obtenido exitosamente")
    })
    public ResponseEntity<List<TrustLogResponse>> getTrustScoreHistory(
            @Parameter(description = "UUID del atleta")
            @PathVariable UUID atletaUuid) {
        
        logger.debug("Obteniendo historial de trust score del jugador: {}", atletaUuid);
        
        List<TrustLogResponse> history = playerProfileService.getTrustScoreHistory(atletaUuid);
        return ResponseEntity.ok(history);
    }

    /**
     * Busca jugadores por rango de trust score.
     * Requisito: 2.2 (consulta por trust score)
     */
    @GetMapping("/by-trust-score")
    @Operation(summary = "Buscar jugadores por rango de trust score", 
               description = "Busca jugadores que tengan un trust score dentro del rango especificado")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Búsqueda completada exitosamente")
    })
    public ResponseEntity<List<PlayerProfileResponse>> getPlayersByTrustScoreRange(
            @Parameter(description = "Puntuación mínima de trust score")
            @RequestParam Integer minScore,
            @Parameter(description = "Puntuación máxima de trust score")
            @RequestParam Integer maxScore) {
        
        logger.debug("Buscando jugadores con trust score entre {} y {}", minScore, maxScore);
        
        List<PlayerProfileResponse> players = playerProfileService.findByTrustScoreRange(minScore, maxScore);
        return ResponseEntity.ok(players);
    }

    /**
     * Busca jugadores por nombre del atleta.
     * Requisito: 2.1 (búsqueda por información del atleta asociado)
     */
    @GetMapping("/search")
    @Operation(summary = "Buscar jugadores por nombre", 
               description = "Busca jugadores que contengan el texto especificado en el nombre del atleta")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Búsqueda completada exitosamente")
    })
    public ResponseEntity<List<PlayerProfileResponse>> searchPlayersByAthleteName(
            @Parameter(description = "Texto a buscar en el nombre del atleta")
            @RequestParam String nombre) {
        
        logger.debug("Buscando jugadores por nombre de atleta: {}", nombre);
        
        List<PlayerProfileResponse> players = playerProfileService.searchByAthleteName(nombre);
        return ResponseEntity.ok(players);
    }

    /**
     * Agrega experiencia (XP) a una posición específica de un jugador.
     * Requisito: 3.3 (contador de experiencia por posición)
     */
    @PutMapping("/{atletaUuid}/positions/{positionId}/experience")
    @Operation(summary = "Agregar experiencia a posición", 
               description = "Agrega experiencia (XP) a una posición específica de un jugador")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Experiencia agregada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Jugador, posición no encontrados o el jugador no tiene esta posición")
    })
    public ResponseEntity<Void> addExperienceToPosition(
            @Parameter(description = "UUID del atleta")
            @PathVariable UUID atletaUuid,
            @Parameter(description = "ID de la posición")
            @PathVariable Long positionId,
            @Parameter(description = "XP a agregar")
            @RequestParam Integer xp) {
        
        logger.info("Agregando {} XP a la posición {} del jugador: {}", xp, positionId, atletaUuid);
        
        try {
            playerProfileService.addExperienceToPosition(atletaUuid, positionId, xp);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Error agregando experiencia: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}