package com.atleta.demo.controller;

import com.atleta.demo.dto.request.UpdatePlayerRatingsRequest;
import com.atleta.demo.dto.request.UpdateRotativeGoalkeeperRequest;
import com.atleta.demo.dto.response.LeaderboardEntryResponse;
import com.atleta.demo.dto.response.PlayerOverallStatsResponse;
import com.atleta.demo.dto.response.PlayerRatingResponse;
import com.atleta.demo.dto.response.RatingHistoryResponse;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.PlayerRating;
import com.atleta.demo.entity.RatingHistory;
import com.atleta.demo.enums.PriorityLevel;
import com.atleta.demo.enums.RoleType;
import com.atleta.demo.exception.ConcurrentRatingUpdateException;
import com.atleta.demo.exception.InvalidPlayerDataException;
import com.atleta.demo.exception.MatchNotFoundException;
import com.atleta.demo.exception.PlayerNotFoundException;
import com.atleta.demo.exception.RatingCalculationException;
import com.atleta.demo.repository.PlayerProfileRepository;
import com.atleta.demo.security.AuthenticatedUserUtils;
import com.atleta.demo.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controlador REST para la gestiÃ³n de calificaciones de jugadores.
 * Proporciona endpoints para actualizar calificaciones manualmente y consultar historial.
 * 
 * Implementa los requerimientos del sistema de calificaciÃ³n de jugadores:
 * - ActualizaciÃ³n manual de calificaciones
 * - Consulta de calificaciones actuales
 * - Consulta de historial de calificaciones
 * - Modo arquero rotativo
 */
@RestController
@RequestMapping("/api/v1/ratings")
@Tag(name = "Calificaciones", description = "GestiÃ³n de calificaciones de jugadores - cÃ¡lculo, actualizaciÃ³n y consulta")
public class RatingController {

    private static final Logger logger = LoggerFactory.getLogger(RatingController.class);

    private final RatingService ratingService;
    private final PlayerProfileRepository playerProfileRepository;

    public RatingController(RatingService ratingService, PlayerProfileRepository playerProfileRepository) {
        this.ratingService = ratingService;
        this.playerProfileRepository = playerProfileRepository;
    }

    /**
     * Actualiza las calificaciones de jugadores manualmente basÃ¡ndose en su rendimiento en un partido.
     * Permite la actualizaciÃ³n manual de calificaciones cuando se necesita procesar resultados
     * de partidos de forma independiente del flujo automÃ¡tico.
     */
    @PostMapping("/update")
    @Operation(summary = "Actualizar calificaciones manualmente", 
               description = "Actualiza las calificaciones de mÃºltiples jugadores basÃ¡ndose en su rendimiento en un partido especÃ­fico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Calificaciones actualizadas exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada invÃ¡lidos"),
        @ApiResponse(responseCode = "404", description = "Partido o jugador no encontrado"),
        @ApiResponse(responseCode = "409", description = "Conflicto de concurrencia en actualizaciÃ³n"),
        @ApiResponse(responseCode = "500", description = "Error interno en el cÃ¡lculo de calificaciones")
    })
    public ResponseEntity<String> updatePlayerRatings(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdatePlayerRatingsRequest request) {
        requireOnlyAuthenticatedPlayerPerformances(jwt, request);
        
        logger.info("Solicitud de actualizaciÃ³n manual de calificaciones para partido {} con {} jugadores", 
                   request.getMatchId(), request.getPerformances().size());
        
        try {
            ratingService.updatePlayerRatings(request.getMatchId(), request.getPerformances());
            
            String message = String.format("Calificaciones actualizadas exitosamente para %d jugadores en el partido %d", 
                                         request.getPerformances().size(), request.getMatchId());
            logger.info(message);
            return ResponseEntity.ok(message);
            
        } catch (InvalidPlayerDataException e) {
            logger.warn("Datos de entrada invÃ¡lidos: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error de validaciÃ³n: " + e.getMessage());
            
        } catch (MatchNotFoundException e) {
            logger.warn("Partido no encontrado: {}", e.getMessage());
            return ResponseEntity.notFound().build();
            
        } catch (PlayerNotFoundException e) {
            logger.warn("Jugador no encontrado: {}", e.getMessage());
            return ResponseEntity.notFound().build();
            
        } catch (ConcurrentRatingUpdateException e) {
            logger.warn("Conflicto de concurrencia: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Conflicto de concurrencia: " + e.getMessage());
                    
        } catch (RatingCalculationException e) {
            logger.error("Error en cÃ¡lculo de calificaciones: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno: " + e.getMessage());
        }
    }

    /**
     * Actualiza las calificaciones en modo arquero rotativo.
     * En este modo especial, todos los jugadores reciben actualizaciones de calificaciÃ³n de arquero.
     */
    @PostMapping("/update-rotative-goalkeeper")
    @Operation(summary = "Actualizar calificaciones en modo arquero rotativo", 
               description = "Actualiza las calificaciones de arquero para todos los jugadores en un partido con modo arquero rotativo")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Calificaciones de arquero rotativo actualizadas exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada invÃ¡lidos"),
        @ApiResponse(responseCode = "404", description = "Partido no encontrado"),
        @ApiResponse(responseCode = "500", description = "Error interno en el cÃ¡lculo de calificaciones")
    })
    public ResponseEntity<String> updateRotativeGoalkeeperRatings(
            @Valid @RequestBody UpdateRotativeGoalkeeperRequest request) {
        
        logger.info("Solicitud de actualizaciÃ³n de arquero rotativo para partido {} con resultado {}", 
                   request.getMatchId(), request.getMatchResult());
        
        try {
            ratingService.updateRotativeGoalkeeperRatings(request.getMatchId(), request.getMatchResult());
            
            String message = String.format("Calificaciones de arquero rotativo actualizadas exitosamente para el partido %d", 
                                         request.getMatchId());
            logger.info(message);
            return ResponseEntity.ok(message);
            
        } catch (InvalidPlayerDataException e) {
            logger.warn("Datos de entrada invÃ¡lidos: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error de validaciÃ³n: " + e.getMessage());
            
        } catch (MatchNotFoundException e) {
            logger.warn("Partido no encontrado: {}", e.getMessage());
            return ResponseEntity.notFound().build();
            
        } catch (RatingCalculationException e) {
            logger.error("Error en cÃ¡lculo de calificaciones de arquero rotativo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno: " + e.getMessage());
        }
    }

    /**
     * Obtiene todas las calificaciones actuales de un jugador especÃ­fico.
     * Retorna todas las calificaciones del jugador organizadas por rol y prioridad.
     */
    @GetMapping("/player/{playerProfileId}")
    @Operation(summary = "Obtener calificaciones de jugador", 
               description = "Obtiene todas las calificaciones actuales de un jugador especÃ­fico organizadas por rol y prioridad")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Calificaciones obtenidas exitosamente"),
        @ApiResponse(responseCode = "400", description = "ID de jugador invÃ¡lido"),
        @ApiResponse(responseCode = "404", description = "Jugador no encontrado o sin calificaciones")
    })
    public ResponseEntity<List<PlayerRatingResponse>> getPlayerRatings(
            @Parameter(description = "UUID del perfil del jugador")
            @PathVariable UUID playerProfileId,
            @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUserUtils.requireSameUser(jwt, playerProfileId);
        
        logger.debug("Consultando calificaciones para jugador {}", playerProfileId);
        
        try {
            List<PlayerRating> ratings = ratingService.getPlayerRatings(playerProfileId);
            
            if (ratings.isEmpty()) {
                logger.debug("No se encontraron calificaciones para jugador {}", playerProfileId);
                return ResponseEntity.notFound().build();
            }
            
            List<PlayerRatingResponse> response = ratings.stream()
                    .map(this::convertToPlayerRatingResponse)
                    .collect(Collectors.toList());
            
            logger.debug("Retornando {} calificaciones para jugador {}", response.size(), playerProfileId);
            return ResponseEntity.ok(response);
            
        } catch (InvalidPlayerDataException e) {
            logger.warn("ID de jugador invÃ¡lido: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obtiene las calificaciones de un jugador filtradas por rol especÃ­fico.
     */
    /**
     * Inicializa calificaciones base para TODOS los roles del jugador.
     * Crea (o completa) valores para que siempre existan ratings en las 6 posiciones/roles.
     */
    @PostMapping("/player/{playerProfileId}/initialize-base")
    @Operation(summary = "Inicializar calificaciones base del jugador",
               description = "Genera calificaciones base para todos los roles del jugador usando sus prioridades seleccionadas")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Calificaciones base inicializadas exitosamente"),
        @ApiResponse(responseCode = "400", description = "ID de jugador invalido"),
        @ApiResponse(responseCode = "404", description = "Jugador no encontrado")
    })
    public ResponseEntity<List<PlayerRatingResponse>> initializePlayerBaseRatings(
            @Parameter(description = "UUID del perfil del jugador")
            @PathVariable UUID playerProfileId,
            @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUserUtils.requireSameUser(jwt, playerProfileId);

        logger.info("Inicializando calificaciones base para jugador {}", playerProfileId);

        try {
            List<PlayerRating> ratings = ratingService.initializeBaseRatings(playerProfileId);
            List<PlayerRatingResponse> response = ratings.stream()
                    .map(this::convertToPlayerRatingResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (InvalidPlayerDataException e) {
            logger.warn("ID de jugador invalido para inicializacion base: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (PlayerNotFoundException e) {
            logger.warn("Jugador no encontrado para inicializacion base: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    @GetMapping("/player/{playerProfileId}/role/{roleType}")
    @Operation(summary = "Obtener calificaciones por rol", 
               description = "Obtiene las calificaciones de un jugador filtradas por un rol especÃ­fico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Calificaciones obtenidas exitosamente"),
        @ApiResponse(responseCode = "400", description = "ParÃ¡metros invÃ¡lidos"),
        @ApiResponse(responseCode = "404", description = "No se encontraron calificaciones para el rol especificado")
    })
    public ResponseEntity<List<PlayerRatingResponse>> getPlayerRatingsByRole(
            @Parameter(description = "UUID del perfil del jugador")
            @PathVariable UUID playerProfileId,
            @Parameter(description = "Tipo de rol a filtrar")
            @PathVariable RoleType roleType,
            @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUserUtils.requireSameUser(jwt, playerProfileId);
        
        logger.debug("Consultando calificaciones para jugador {} y rol {}", playerProfileId, roleType);
        
        try {
            List<PlayerRating> ratings = ratingService.getPlayerRatingsByRole(playerProfileId, roleType);
            
            if (ratings.isEmpty()) {
                logger.debug("No se encontraron calificaciones para jugador {} y rol {}", playerProfileId, roleType);
                return ResponseEntity.notFound().build();
            }
            
            List<PlayerRatingResponse> response = ratings.stream()
                    .map(this::convertToPlayerRatingResponse)
                    .collect(Collectors.toList());
            
            logger.debug("Retornando {} calificaciones para jugador {} y rol {}", 
                        response.size(), playerProfileId, roleType);
            return ResponseEntity.ok(response);
            
        } catch (InvalidPlayerDataException e) {
            logger.warn("ParÃ¡metros invÃ¡lidos: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obtiene las calificaciones de un jugador filtradas por nivel de prioridad.
     */
    @GetMapping("/player/{playerProfileId}/priority/{priorityLevel}")
    @Operation(summary = "Obtener calificaciones por prioridad", 
               description = "Obtiene las calificaciones de un jugador filtradas por nivel de prioridad")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Calificaciones obtenidas exitosamente"),
        @ApiResponse(responseCode = "400", description = "ParÃ¡metros invÃ¡lidos"),
        @ApiResponse(responseCode = "404", description = "No se encontraron calificaciones para la prioridad especificada")
    })
    public ResponseEntity<List<PlayerRatingResponse>> getPlayerRatingsByPriority(
            @Parameter(description = "UUID del perfil del jugador")
            @PathVariable UUID playerProfileId,
            @Parameter(description = "Nivel de prioridad a filtrar")
            @PathVariable PriorityLevel priorityLevel,
            @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUserUtils.requireSameUser(jwt, playerProfileId);
        
        logger.debug("Consultando calificaciones para jugador {} y prioridad {}", playerProfileId, priorityLevel);
        
        try {
            List<PlayerRating> ratings = ratingService.getPlayerRatingsByPriority(playerProfileId, priorityLevel);
            
            if (ratings.isEmpty()) {
                logger.debug("No se encontraron calificaciones para jugador {} y prioridad {}", 
                           playerProfileId, priorityLevel);
                return ResponseEntity.notFound().build();
            }
            
            List<PlayerRatingResponse> response = ratings.stream()
                    .map(this::convertToPlayerRatingResponse)
                    .collect(Collectors.toList());
            
            logger.debug("Retornando {} calificaciones para jugador {} y prioridad {}", 
                        response.size(), playerProfileId, priorityLevel);
            return ResponseEntity.ok(response);
            
        } catch (InvalidPlayerDataException e) {
            logger.warn("ParÃ¡metros invÃ¡lidos: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obtiene el historial completo de calificaciones de un jugador.
     * Retorna todos los cambios de calificaciÃ³n ordenados por fecha descendente.
     */
    @GetMapping("/player/{playerProfileId}/history")
    @Operation(summary = "Obtener historial de calificaciones", 
               description = "Obtiene el historial completo de cambios de calificaciÃ³n de un jugador ordenado por fecha descendente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Historial obtenido exitosamente"),
        @ApiResponse(responseCode = "400", description = "ID de jugador invÃ¡lido"),
        @ApiResponse(responseCode = "404", description = "No se encontrÃ³ historial para el jugador")
    })
    public ResponseEntity<List<RatingHistoryResponse>> getRatingHistory(
            @Parameter(description = "UUID del perfil del jugador")
            @PathVariable UUID playerProfileId,
            @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUserUtils.requireSameUser(jwt, playerProfileId);
        
        logger.debug("Consultando historial de calificaciones para jugador {}", playerProfileId);
        
        try {
            List<RatingHistory> history = ratingService.getRatingHistory(playerProfileId);
            
            if (history.isEmpty()) {
                logger.debug("No se encontrÃ³ historial para jugador {}", playerProfileId);
                return ResponseEntity.notFound().build();
            }
            
            List<RatingHistoryResponse> response = history.stream()
                    .map(this::convertToRatingHistoryResponse)
                    .collect(Collectors.toList());
            
            logger.debug("Retornando {} registros de historial para jugador {}", response.size(), playerProfileId);
            return ResponseEntity.ok(response);
            
        } catch (InvalidPlayerDataException e) {
            logger.warn("ID de jugador invÃ¡lido: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obtiene el historial de calificaciones de un jugador filtrado por rol.
     */
    @GetMapping("/player/{playerProfileId}/history/role/{roleType}")
    @Operation(summary = "Obtener historial por rol", 
               description = "Obtiene el historial de calificaciones de un jugador filtrado por rol especÃ­fico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Historial obtenido exitosamente"),
        @ApiResponse(responseCode = "400", description = "ParÃ¡metros invÃ¡lidos"),
        @ApiResponse(responseCode = "404", description = "No se encontrÃ³ historial para el rol especificado")
    })
    public ResponseEntity<List<RatingHistoryResponse>> getRatingHistoryByRole(
            @Parameter(description = "UUID del perfil del jugador")
            @PathVariable UUID playerProfileId,
            @Parameter(description = "Tipo de rol a filtrar")
            @PathVariable RoleType roleType,
            @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUserUtils.requireSameUser(jwt, playerProfileId);
        
        logger.debug("Consultando historial para jugador {} y rol {}", playerProfileId, roleType);
        
        try {
            List<RatingHistory> history = ratingService.getRatingHistoryByRole(playerProfileId, roleType);
            
            if (history.isEmpty()) {
                logger.debug("No se encontrÃ³ historial para jugador {} y rol {}", playerProfileId, roleType);
                return ResponseEntity.notFound().build();
            }
            
            List<RatingHistoryResponse> response = history.stream()
                    .map(this::convertToRatingHistoryResponse)
                    .collect(Collectors.toList());
            
            logger.debug("Retornando {} registros de historial para jugador {} y rol {}", 
                        response.size(), playerProfileId, roleType);
            return ResponseEntity.ok(response);
            
        } catch (InvalidPlayerDataException e) {
            logger.warn("ParÃ¡metros invÃ¡lidos: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obtiene el historial de calificaciones de un jugador en un perÃ­odo de tiempo especÃ­fico.
     */
    @GetMapping("/player/{playerProfileId}/history/period")
    @Operation(summary = "Obtener historial por perÃ­odo", 
               description = "Obtiene el historial de calificaciones de un jugador en un perÃ­odo de tiempo especÃ­fico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Historial obtenido exitosamente"),
        @ApiResponse(responseCode = "400", description = "ParÃ¡metros invÃ¡lidos o fechas incorrectas"),
        @ApiResponse(responseCode = "404", description = "No se encontrÃ³ historial en el perÃ­odo especificado")
    })
    public ResponseEntity<List<RatingHistoryResponse>> getRatingHistoryByPeriod(
            @Parameter(description = "UUID del perfil del jugador")
            @PathVariable UUID playerProfileId,
            @Parameter(description = "Fecha de inicio del perÃ­odo (formato: yyyy-MM-ddTHH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Fecha de fin del perÃ­odo (formato: yyyy-MM-ddTHH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUserUtils.requireSameUser(jwt, playerProfileId);
        
        logger.debug("Consultando historial para jugador {} entre {} y {}", playerProfileId, startDate, endDate);
        
        try {
            List<RatingHistory> history = ratingService.getRatingHistoryByPeriod(playerProfileId, startDate, endDate);
            
            if (history.isEmpty()) {
                logger.debug("No se encontrÃ³ historial para jugador {} en el perÃ­odo especificado", playerProfileId);
                return ResponseEntity.notFound().build();
            }
            
            List<RatingHistoryResponse> response = history.stream()
                    .map(this::convertToRatingHistoryResponse)
                    .collect(Collectors.toList());
            
            logger.debug("Retornando {} registros de historial para jugador {} en el perÃ­odo especificado", 
                        response.size(), playerProfileId);
            return ResponseEntity.ok(response);
            
        } catch (InvalidPlayerDataException e) {
            logger.warn("ParÃ¡metros invÃ¡lidos: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obtiene estadÃ­sticas de rendimiento de un jugador.
     */
    @GetMapping("/player/{playerProfileId}/statistics")
    @Operation(summary = "Obtener estadÃ­sticas de rendimiento", 
               description = "Obtiene estadÃ­sticas generales de rendimiento de un jugador")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "EstadÃ­sticas obtenidas exitosamente"),
        @ApiResponse(responseCode = "400", description = "ID de jugador invÃ¡lido")
    })
    public ResponseEntity<Object[]> getPlayerPerformanceStatistics(
            @Parameter(description = "UUID del perfil del jugador")
            @PathVariable UUID playerProfileId,
            @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUserUtils.requireSameUser(jwt, playerProfileId);
        
        logger.debug("Consultando estadÃ­sticas de rendimiento para jugador {}", playerProfileId);
        
        try {
            Object[] statistics = ratingService.getPlayerPerformanceStatistics(playerProfileId);
            
            logger.debug("Retornando estadÃ­sticas para jugador {}: {} partidos, {} goles, {} asistencias, {} MVPs", 
                        playerProfileId, statistics[0], statistics[1], statistics[2], statistics[3]);
            return ResponseEntity.ok(statistics);
            
        } catch (InvalidPlayerDataException e) {
            logger.warn("ID de jugador invÃ¡lido: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obtiene estadÃ­sticas de rendimiento de un jugador para un rol especÃ­fico.
     */
    @GetMapping("/player/{playerProfileId}/statistics/role/{roleType}")
    @Operation(summary = "Obtener estadÃ­sticas por rol", 
               description = "Obtiene estadÃ­sticas de rendimiento de un jugador para un rol especÃ­fico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "EstadÃ­sticas obtenidas exitosamente"),
        @ApiResponse(responseCode = "400", description = "ParÃ¡metros invÃ¡lidos")
    })
    public ResponseEntity<Object[]> getPlayerPerformanceStatisticsByRole(
            @Parameter(description = "UUID del perfil del jugador")
            @PathVariable UUID playerProfileId,
            @Parameter(description = "Tipo de rol a filtrar")
            @PathVariable RoleType roleType,
            @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUserUtils.requireSameUser(jwt, playerProfileId);
        
        logger.debug("Consultando estadÃ­sticas de rendimiento para jugador {} y rol {}", playerProfileId, roleType);
        
        try {
            Object[] statistics = ratingService.getPlayerPerformanceStatisticsByRole(playerProfileId, roleType);
            
            logger.debug("Retornando estadÃ­sticas por rol para jugador {} y rol {}: {} partidos, {} goles, {} asistencias, {} MVPs", 
                        playerProfileId, roleType, statistics[0], statistics[1], statistics[2], statistics[3]);
            return ResponseEntity.ok(statistics);
            
        } catch (InvalidPlayerDataException e) {
            logger.warn("ParÃ¡metros invÃ¡lidos: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obtiene la calificaciÃ³n general (OVR) completa de un jugador.
     * Incluye OVR hÃ­brido, ponderado, simple y mÃ©tricas adicionales.
     */
    @GetMapping("/player/{playerProfileId}/overall")
    @Operation(summary = "Obtener calificaciÃ³n general del jugador", 
               description = "Calcula y retorna el OVR (Overall Rating) completo del jugador con todas las mÃ©tricas")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OVR calculado exitosamente"),
        @ApiResponse(responseCode = "400", description = "ID de jugador invÃ¡lido"),
        @ApiResponse(responseCode = "404", description = "Jugador no encontrado o sin calificaciones")
    })
    public ResponseEntity<PlayerOverallStatsResponse> getOverallRating(
            @Parameter(description = "UUID del perfil del jugador")
            @PathVariable UUID playerProfileId,
            @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUserUtils.requireSameUser(jwt, playerProfileId);
        
        logger.debug("Consultando OVR completo para jugador {}", playerProfileId);
        
        try {
            // Calcular estadÃ­sticas completas
            RatingService.PlayerOverallStats stats = ratingService.calculateCompleteOverall(playerProfileId);
            
            // Obtener todas las calificaciones para el desglose
            List<PlayerRating> ratings = ratingService.getPlayerRatings(playerProfileId);
            
            // Obtener perfil del jugador para el alias
            PlayerProfile playerProfile = playerProfileRepository.findById(playerProfileId)
                    .orElseThrow(() -> new PlayerNotFoundException("Jugador no encontrado", playerProfileId.toString()));
            
            // Construir respuesta
            PlayerOverallStatsResponse response = new PlayerOverallStatsResponse();
            response.setPlayerProfileId(playerProfileId);
            response.setAlias(playerProfile.getAlias());
            response.setHybridOVR(stats.getHybridOVR());
            response.setWeightedOVR(stats.getWeightedOVR());
            response.setSimpleOVR(stats.getSimpleOVR());
            response.setClassification(stats.getClassification());
            response.setVersatilityIndex(stats.getVersatilityIndex());
            response.setConsistencyScore(stats.getConsistencyScore());
            response.setBestRole(stats.getBestRole());
            response.setBestRoleRating(stats.getBestRoleRating());
            response.setTotalRatings(stats.getTotalRatings());
            response.setTotalMatchesPlayed(stats.getTotalMatchesPlayed());
            
            // Agregar desglose por rol
            Map<RoleType, BigDecimal> roleBreakdown = new HashMap<>();
            for (PlayerRating rating : ratings) {
                roleBreakdown.put(rating.getRoleType(), rating.getCurrentRating());
            }
            response.setRoleBreakdown(roleBreakdown);
            
            logger.info("OVR completo calculado para jugador {}: HÃ­brido={}, ClasificaciÃ³n={}", 
                       playerProfileId, stats.getHybridOVR(), stats.getClassification());
            
            return ResponseEntity.ok(response);
            
        } catch (InvalidPlayerDataException e) {
            logger.warn("ID de jugador invÃ¡lido: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (PlayerNotFoundException e) {
            logger.warn("Jugador no encontrado: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Obtener leaderboard de jugadores",
            description = "Retorna ranking de jugadores ordenado por rating desc. Permite filtrar por rol")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Leaderboard obtenido exitosamente")
    })
    public ResponseEntity<List<LeaderboardEntryResponse>> getLeaderboard(
            @RequestParam(required = false) RoleType roleType,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(ratingService.getLeaderboard(roleType, limit));
    }

    private void requireOnlyAuthenticatedPlayerPerformances(Jwt jwt, UpdatePlayerRatingsRequest request) {
        if (request == null || request.getPerformances() == null) {
            return;
        }

        request.getPerformances().stream()
                .map(performance -> performance.getPlayerProfileId())
                .filter(playerProfileId -> playerProfileId != null)
                .forEach(playerProfileId -> AuthenticatedUserUtils.requireSameUser(jwt, playerProfileId));
    }

    /**
     * Convierte una entidad PlayerRating a PlayerRatingResponse.
     */
    private PlayerRatingResponse convertToPlayerRatingResponse(PlayerRating rating) {
        return new PlayerRatingResponse(
                rating.getId(),
                rating.getPlayerProfile().getAtletaUuid(),
                rating.getPlayerProfile().getAlias(),
                rating.getRoleType(),
                rating.getPriorityLevel(),
                rating.getCurrentRating(),
                rating.getMatchesPlayed(),
                rating.getLastUpdated()
        );
    }

    /**
     * Convierte una entidad RatingHistory a RatingHistoryResponse.
     */
    private RatingHistoryResponse convertToRatingHistoryResponse(RatingHistory history) {
        RatingHistoryResponse response = new RatingHistoryResponse(
                history.getId(),
                history.getPlayerRating().getPlayerProfile().getAtletaUuid(),
                history.getPlayerRating().getPlayerProfile().getAlias(),
                history.getPlayerRating().getRoleType(),
                history.getPlayerRating().getPriorityLevel(),
                history.getMatch().getId(),
                history.getPreviousRating(),
                history.getNewRating(),
                history.getRatingDelta(),
                history.getGoalsScored(),
                history.getAssistsMade(),
                history.getGoalsConceded(),
                history.getWasMvp(),
                history.getMatchResult(),
                history.getRotativeGoalkeeperMode(),
                history.getCreatedAt()
        );
        
        // Agregar componentes detallados del cÃ¡lculo si estÃ¡n disponibles
        response.setResultPoints(history.getResultPoints());
        response.setWeightedGoalPoints(history.getWeightedGoalPoints());
        response.setWeightedAssistPoints(history.getWeightedAssistPoints());
        response.setDefensiveBonus(history.getDefensiveBonus());
        response.setMvpBonus(history.getMvpBonus());
        response.setPriorityMultiplier(history.getPriorityMultiplier());
        
        return response;
    }
}

