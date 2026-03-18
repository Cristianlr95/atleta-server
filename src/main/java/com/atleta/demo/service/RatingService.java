package com.atleta.demo.service;

import com.atleta.demo.dto.request.PlayerPerformanceDto;
import com.atleta.demo.dto.request.RatingCalculationRequest;
import com.atleta.demo.dto.request.RotativeGoalkeeperRequest;
import com.atleta.demo.dto.response.LeaderboardEntryResponse;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.PlayerPosition;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.PlayerRating;
import com.atleta.demo.entity.RatingHistory;
import com.atleta.demo.enums.MatchResultType;
import com.atleta.demo.enums.PriorityLevel;
import com.atleta.demo.enums.RoleType;
import com.atleta.demo.exception.ConcurrentRatingUpdateException;
import com.atleta.demo.exception.InvalidPlayerDataException;
import com.atleta.demo.exception.MatchNotFoundException;
import com.atleta.demo.exception.PlayerNotFoundException;
import com.atleta.demo.exception.RatingCalculationException;
import com.atleta.demo.repository.MatchRepository;
import com.atleta.demo.repository.PlayerPositionRepository;
import com.atleta.demo.repository.PlayerProfileRepository;
import com.atleta.demo.repository.PlayerRatingRepository;
import com.atleta.demo.repository.RatingHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio principal para la gestiÃ³n de calificaciones de jugadores.
 * Orquesta el cÃ¡lculo y actualizaciÃ³n de calificaciones basÃ¡ndose en el rendimiento en partidos.
 * 
 * Implementa los requerimientos:
 * - 9.4: ValidaciÃ³n de datos de entrada y manejo de errores
 * - 9.5: Actualizaciones concurrentes de calificaciÃ³n de manera segura
 * - 7.1, 7.3, 7.4, 7.5: Modo arquero rotativo
 * - 9.1, 9.2: Consultas de calificaciones e historial
 */
@Service
@Transactional
public class RatingService {

    private static final Logger logger = LoggerFactory.getLogger(RatingService.class);

    private final RatingCalculationEngine calculationEngine;
    private final PlayerRatingRepository playerRatingRepository;
    private final RatingHistoryRepository ratingHistoryRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final PlayerPositionRepository playerPositionRepository;
    private final MatchRepository matchRepository;

    public RatingService(RatingCalculationEngine calculationEngine,
                        PlayerRatingRepository playerRatingRepository,
                        RatingHistoryRepository ratingHistoryRepository,
                        PlayerProfileRepository playerProfileRepository,
                        PlayerPositionRepository playerPositionRepository,
                        MatchRepository matchRepository) {
        this.calculationEngine = calculationEngine;
        this.playerRatingRepository = playerRatingRepository;
        this.ratingHistoryRepository = ratingHistoryRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.playerPositionRepository = playerPositionRepository;
        this.matchRepository = matchRepository;
    }

    /**
     * Inicializa (o completa) calificaciones base para TODOS los roles del jugador.
     *
     * Modelo base:
     * - Base comÃºn: 50 puntos para cada rol
     * - Bonos por prioridad seleccionada:
     *   prioridad 1 => +20 (70)
     *   prioridad 2 => +10 (60)
     *   prioridad 3 => +0  (50)
     *
     * Para roles sin selecciÃ³n explÃ­cita, se crea una calificaciÃ³n base de 50 (TERCIARIA),
     * de modo que siempre existan valores para todo el hexÃ¡gono.
     *
     * @param playerProfileId UUID del perfil de jugador
     * @return Lista completa de calificaciones del jugador tras la inicializaciÃ³n
     */
    public List<PlayerRating> initializeBaseRatings(UUID playerProfileId) {
        logger.info("Inicializando calificaciones base para jugador {}", playerProfileId);

        if (playerProfileId == null) {
            throw new InvalidPlayerDataException(
                "El ID del perfil del jugador es obligatorio",
                null,
                "playerProfileId",
                null
            );
        }

        PlayerProfile playerProfile = playerProfileRepository.findById(playerProfileId)
            .orElseThrow(() -> new PlayerNotFoundException(
                "No se encontrÃ³ el perfil de jugador",
                playerProfileId.toString()
            ));

        List<PlayerPosition> positions = playerPositionRepository.findByPlayerAtletaUuidOrderByPrioridad(playerProfileId);
        Map<RoleType, Integer> priorityByRole = new EnumMap<>(RoleType.class);
        for (PlayerPosition playerPosition : positions) {
            RoleType role = mapPositionToRole(playerPosition.getPosition().getNombre());
            if (role == null) {
                continue;
            }

            int priority = playerPosition.getPrioridad() == null ? 3 : playerPosition.getPrioridad();
            Integer previous = priorityByRole.get(role);
            if (previous == null || priority < previous) {
                priorityByRole.put(role, priority);
            }
        }

        final BigDecimal commonBase = BigDecimal.valueOf(50);
        for (RoleType role : RoleType.values()) {
            int selectedPriority = priorityByRole.getOrDefault(role, 3);
            PriorityLevel targetPriority = toPriorityLevel(selectedPriority);
            BigDecimal targetRating = commonBase.add(priorityBonus(selectedPriority));

            Optional<PlayerRating> existingForRole = playerRatingRepository
                .findByPlayerProfileIdAndRoleType(playerProfileId, role)
                .stream()
                .findFirst();

            if (existingForRole.isEmpty()) {
                PlayerRating seedRating = new PlayerRating(playerProfile, role, targetPriority, targetRating);
                playerRatingRepository.save(seedRating);
                continue;
            }

            PlayerRating existing = existingForRole.get();
            // Solo ajustamos "hacia arriba" seeds sin partidos para reflejar nueva prioridad.
            if (existing.getMatchesPlayed() == 0 && existing.getCurrentRating().compareTo(targetRating) < 0) {
                existing.setCurrentRating(targetRating);
                existing.setPriorityLevel(targetPriority);
                playerRatingRepository.save(existing);
            }
        }

        List<PlayerRating> initialized = playerRatingRepository.findByPlayerProfileId(playerProfileId);
        logger.info("InicializaciÃ³n base completada para jugador {} con {} roles", playerProfileId, initialized.size());
        return initialized;
    }

    private PriorityLevel toPriorityLevel(int priority) {
        return switch (priority) {
            case 1 -> PriorityLevel.PRINCIPAL;
            case 2 -> PriorityLevel.SECUNDARIA;
            default -> PriorityLevel.TERCIARIA;
        };
    }

    private BigDecimal priorityBonus(int priority) {
        return switch (priority) {
            case 1 -> BigDecimal.valueOf(20);
            case 2 -> BigDecimal.valueOf(10);
            default -> BigDecimal.ZERO;
        };
    }

    private RoleType mapPositionToRole(String positionName) {
        if (positionName == null) {
            return null;
        }

        String normalized = java.text.Normalizer.normalize(positionName, java.text.Normalizer.Form.NFD)
                .toLowerCase()
                .replaceAll("\\p{M}", "")
                .trim();

        if (normalized.contains("portero") || normalized.contains("arquero")) {
            return RoleType.ARQUERO;
        }
        if (normalized.contains("defensa")) {
            return RoleType.DEFENSA;
        }
        if (normalized.contains("carrilero") || normalized.contains("lateral")) {
            return RoleType.CARRILERO;
        }
        if (normalized.contains("medio")) {
            return RoleType.MEDIOCAMPO;
        }
        if (normalized.contains("delantero") || normalized.contains("ataque")) {
            return RoleType.ATAQUE;
        }
        if (normalized.equals("dt") || normalized.contains("tecnico")) {
            return RoleType.DT;
        }

        return null;
    }

    /**
     * Actualiza las calificaciones de mÃºltiples jugadores basÃ¡ndose en su rendimiento en un partido.
     * 
     * Implementa los requerimientos:
     * - 9.4: ValidaciÃ³n de datos de entrada requeridos
     * - 9.5: Manejo seguro de actualizaciones concurrentes
     * 
     * @param matchId ID del partido para el cual se actualizan las calificaciones
     * @param performances Lista de rendimientos de jugadores en el partido
     * @throws InvalidPlayerDataException si los datos de entrada son invÃ¡lidos
     * @throws MatchNotFoundException si el partido no existe
     * @throws PlayerNotFoundException si algÃºn jugador no existe
     * @throws ConcurrentRatingUpdateException si ocurre un conflicto de concurrencia
     * @throws RatingCalculationException si ocurre un error durante el procesamiento
     */
    public void updatePlayerRatings(Long matchId, List<PlayerPerformanceDto> performances) {
        logger.info("Iniciando actualizaciÃ³n de calificaciones para partido {} con {} jugadores", 
                   matchId, performances.size());
        
        // ValidaciÃ³n de entrada (Requerimiento 9.4)
        validateUpdatePlayerRatingsInput(matchId, performances);
        
        // Verificar que el partido existe
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException("El partido con ID " + matchId + " no existe", matchId));
        
        // Validar que solo hay un MVP en el partido
        validateSingleMvp(performances);
        
        int processedCount = 0;
        int errorCount = 0;
        
        // Procesar cada rendimiento de jugador
        for (PlayerPerformanceDto performance : performances) {
            try {
                processPlayerRatingUpdate(match, performance);
                processedCount++;
                logger.debug("CalificaciÃ³n actualizada exitosamente para jugador {}", 
                           performance.getPlayerProfileId());
            } catch (InvalidPlayerDataException | PlayerNotFoundException e) {
                errorCount++;
                logger.error("Error de validaciÃ³n actualizando calificaciÃ³n para jugador {}: {}", 
                           performance.getPlayerProfileId(), e.getMessage());
                // Re-lanzar errores de validaciÃ³n inmediatamente
                throw e;
            } catch (ConcurrentRatingUpdateException e) {
                errorCount++;
                logger.warn("Conflicto de concurrencia actualizando calificaciÃ³n para jugador {}: {}", 
                           performance.getPlayerProfileId(), e.getMessage());
                // Re-lanzar errores de concurrencia para que puedan ser reintentados
                throw e;
            } catch (Exception e) {
                errorCount++;
                logger.error("Error inesperado actualizando calificaciÃ³n para jugador {}: {}", 
                           performance.getPlayerProfileId(), e.getMessage(), e);
                // Envolver errores inesperados en RatingCalculationException
                throw new RatingCalculationException(
                    "Error inesperado actualizando calificaciÃ³n para jugador " + performance.getPlayerProfileId(), e);
            }
        }
        
        logger.info("ActualizaciÃ³n de calificaciones completada para partido {}. " +
                   "Procesados: {}, Errores: {}", matchId, processedCount, errorCount);
        
        if (errorCount > 0) {
            logger.warn("Se encontraron {} errores durante la actualizaciÃ³n de calificaciones", errorCount);
        }
    }

    /**
     * Procesa la actualizaciÃ³n de calificaciÃ³n para un jugador individual.
     * Maneja la creaciÃ³n de nuevas calificaciones si no existen.
     * 
     * @throws PlayerNotFoundException si el jugador no existe
     * @throws ConcurrentRatingUpdateException si ocurre un conflicto de concurrencia
     * @throws RatingCalculationException si ocurre un error en el cÃ¡lculo
     */
    private void processPlayerRatingUpdate(Match match, PlayerPerformanceDto performance) {
        try {
            // Obtener o crear la calificaciÃ³n del jugador
            PlayerRating playerRating = getOrCreatePlayerRating(
                    performance.getPlayerProfileId(),
                    performance.getRoleType(),
                    performance.getPriorityLevel()
            );
            
            // Preparar solicitud de cÃ¡lculo
            RatingCalculationRequest calculationRequest = new RatingCalculationRequest(
                    playerRating.getCurrentRating(),
                    performance.getRoleType(),
                    performance.getPriorityLevel(),
                    performance.getMatchResult(),
                    performance.getGoalsScored(),
                    performance.getAssistsMade(),
                    performance.getGoalsConceded(),
                    performance.getWasMvp()
            );
            
            // Calcular nueva calificaciÃ³n
            BigDecimal previousRating = playerRating.getCurrentRating();
            BigDecimal newRating = calculationEngine.calculateNewRating(calculationRequest);
            BigDecimal ratingDelta = newRating.subtract(previousRating);
            
            // Actualizar la calificaciÃ³n del jugador con manejo de concurrencia
            playerRating.updateRating(newRating);
            try {
                playerRatingRepository.save(playerRating);
            } catch (OptimisticLockingFailureException e) {
                throw new ConcurrentRatingUpdateException(
                    "La calificaciÃ³n fue modificada por otro proceso durante la actualizaciÃ³n",
                    e
                );
            }
            
            // Crear registro de historial
            createRatingHistoryRecord(playerRating, match, previousRating, newRating, 
                                    ratingDelta, performance, false);
            
            logger.debug("CalificaciÃ³n actualizada: jugador={}, rol={}, prioridad={}, " +
                        "anterior={}, nueva={}, delta={}", 
                        performance.getPlayerProfileId(), performance.getRoleType(), 
                        performance.getPriorityLevel(), previousRating, newRating, ratingDelta);
                        
        } catch (InvalidPlayerDataException | PlayerNotFoundException | ConcurrentRatingUpdateException e) {
            // Re-lanzar excepciones especÃ­ficas del sistema de calificaciÃ³n
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado procesando actualizaciÃ³n de calificaciÃ³n: {}", e.getMessage(), e);
            throw new RatingCalculationException("Error inesperado durante el procesamiento de calificaciÃ³n", e);
        }
    }

    /**
     * Obtiene una calificaciÃ³n existente o crea una nueva si no existe.
     * Implementa el patrÃ³n "get or create" de manera thread-safe.
     * 
     * @throws PlayerNotFoundException si el jugador no existe
     */
    private PlayerRating getOrCreatePlayerRating(UUID playerProfileId, RoleType roleType, 
                                                PriorityLevel priorityLevel) {
        // Intentar obtener calificaciÃ³n existente
        Optional<PlayerRating> existingRating = playerRatingRepository
                .findByPlayerProfileIdAndRoleTypeAndPriorityLevel(playerProfileId, roleType, priorityLevel);
        
        if (existingRating.isPresent()) {
            return existingRating.get();
        }
        
        // Crear nueva calificaciÃ³n si no existe
        PlayerProfile playerProfile = playerProfileRepository.findById(playerProfileId)
                .orElseThrow(() -> new PlayerNotFoundException(
                        "El perfil de jugador con ID " + playerProfileId + " no existe", 
                        playerProfileId.toString()));
        
        PlayerRating newRating = new PlayerRating(playerProfile, roleType, priorityLevel);
        try {
            return playerRatingRepository.save(newRating);
        } catch (Exception e) {
            logger.error("Error creando nueva calificaciÃ³n para jugador {}: {}", playerProfileId, e.getMessage(), e);
            throw new RatingCalculationException("Error creando nueva calificaciÃ³n para el jugador", e);
        }
    }

    /**
     * Crea un registro de historial de calificaciÃ³n con todos los detalles del cÃ¡lculo.
     */
    private void createRatingHistoryRecord(PlayerRating playerRating, Match match, 
                                         BigDecimal previousRating, BigDecimal newRating,
                                         BigDecimal ratingDelta, PlayerPerformanceDto performance,
                                         boolean rotativeGoalkeeperMode) {
        RatingHistory history = new RatingHistory(
                playerRating, match, previousRating, newRating, ratingDelta,
                performance.getGoalsScored(), performance.getAssistsMade(),
                performance.getWasMvp(), performance.getMatchResult()
        );
        
        history.setGoalsConceded(performance.getGoalsConceded());
        history.setRotativeGoalkeeperMode(rotativeGoalkeeperMode);
        
        // Calcular y almacenar componentes detallados del cÃ¡lculo
        calculateAndSetHistoryComponents(history, performance);
        
        ratingHistoryRepository.save(history);
    }

    /**
     * Calcula y establece los componentes detallados del cÃ¡lculo en el historial.
     */
    private void calculateAndSetHistoryComponents(RatingHistory history, PlayerPerformanceDto performance) {
        // Puntos de resultado
        history.setResultPoints(BigDecimal.valueOf(performance.getMatchResult().getNormalPoints()));
        
        // Puntos de goles ponderados
        BigDecimal weightedGoalPoints = BigDecimal.valueOf(performance.getGoalsScored())
                .multiply(BigDecimal.valueOf(performance.getRoleType().getGoalWeight()));
        history.setWeightedGoalPoints(weightedGoalPoints);
        
        // Puntos de asistencias ponderadas
        BigDecimal weightedAssistPoints = BigDecimal.valueOf(performance.getAssistsMade())
                .multiply(BigDecimal.valueOf(performance.getRoleType().getAssistWeight()));
        history.setWeightedAssistPoints(weightedAssistPoints);
        
        // Bono defensivo
        BigDecimal defensiveBonus = calculateDefensiveBonusForHistory(
                performance.getRoleType(), performance.getGoalsConceded());
        history.setDefensiveBonus(defensiveBonus);
        
        // Bono MVP
        BigDecimal mvpBonus = Boolean.TRUE.equals(performance.getWasMvp()) ? 
                BigDecimal.valueOf(1.0) : BigDecimal.ZERO;
        history.setMvpBonus(mvpBonus);
        
        // Multiplicador de prioridad
        history.setPriorityMultiplier(BigDecimal.valueOf(performance.getPriorityLevel().getMultiplier()));
    }

    /**
     * Calcula el bono defensivo para el historial (duplica lÃ³gica del motor para auditorÃ­a).
     */
    private BigDecimal calculateDefensiveBonusForHistory(RoleType role, Integer goalsConceded) {
        if (goalsConceded == null || goalsConceded < 0) {
            return BigDecimal.ZERO;
        }
        
        switch (role) {
            case DEFENSA:
                switch (goalsConceded) {
                    case 0: return BigDecimal.valueOf(2.0);
                    case 1: return BigDecimal.valueOf(1.0);
                    case 2: return BigDecimal.valueOf(0.5);
                    default: return BigDecimal.ZERO;
                }
            case ARQUERO:
                switch (goalsConceded) {
                    case 0: return BigDecimal.valueOf(2.5);
                    case 1: return BigDecimal.valueOf(1.2);
                    case 2: return BigDecimal.valueOf(0.7);
                    default: return BigDecimal.ZERO;
                }
            default:
                return BigDecimal.ZERO;
        }
    }

    /**
     * Valida los datos de entrada para updatePlayerRatings.
     * Implementa el requerimiento 9.4: validaciÃ³n de datos requeridos.
     * 
     * @throws InvalidPlayerDataException si los datos son invÃ¡lidos
     */
    private void validateUpdatePlayerRatingsInput(Long matchId, List<PlayerPerformanceDto> performances) {
        if (matchId == null) {
            throw new InvalidPlayerDataException("El ID del partido es obligatorio", null, "matchId", null);
        }
        
        if (performances == null || performances.isEmpty()) {
            throw new InvalidPlayerDataException("La lista de rendimientos de jugadores no puede estar vacÃ­a", 
                                               null, "performances", performances);
        }
        
        // Validar cada rendimiento individual
        for (int i = 0; i < performances.size(); i++) {
            PlayerPerformanceDto performance = performances.get(i);
            if (performance == null) {
                throw new InvalidPlayerDataException("El rendimiento del jugador en posiciÃ³n " + i + " no puede ser nulo", 
                                                   null, "performance[" + i + "]", null);
            }
            
            validatePlayerPerformance(performance, i);
        }
        
        logger.debug("ValidaciÃ³n de entrada completada para {} jugadores en partido {}", 
                    performances.size(), matchId);
    }

    /**
     * Valida un rendimiento individual de jugador.
     * 
     * @throws InvalidPlayerDataException si los datos del jugador son invÃ¡lidos
     */
    private void validatePlayerPerformance(PlayerPerformanceDto performance, int index) {
        String playerIdStr = performance.getPlayerProfileId() != null ? 
                           performance.getPlayerProfileId().toString() : null;
        
        if (performance.getPlayerProfileId() == null) {
            throw new InvalidPlayerDataException("El ID del perfil del jugador es obligatorio (posiciÃ³n " + index + ")", 
                                               null, "playerProfileId", null);
        }
        
        if (performance.getRoleType() == null) {
            throw new InvalidPlayerDataException("El tipo de rol es obligatorio (posiciÃ³n " + index + ")", 
                                               playerIdStr, "roleType", null);
        }
        
        if (performance.getPriorityLevel() == null) {
            throw new InvalidPlayerDataException("El nivel de prioridad es obligatorio (posiciÃ³n " + index + ")", 
                                               playerIdStr, "priorityLevel", null);
        }
        
        if (performance.getMatchResult() == null) {
            throw new InvalidPlayerDataException("El resultado del partido es obligatorio (posiciÃ³n " + index + ")", 
                                               playerIdStr, "matchResult", null);
        }
        
        if (performance.getGoalsScored() == null || performance.getGoalsScored() < 0) {
            throw new InvalidPlayerDataException("Los goles anotados deben ser un nÃºmero no negativo (posiciÃ³n " + index + ")", 
                                               playerIdStr, "goalsScored", performance.getGoalsScored());
        }
        
        if (performance.getAssistsMade() == null || performance.getAssistsMade() < 0) {
            throw new InvalidPlayerDataException("Las asistencias realizadas deben ser un nÃºmero no negativo (posiciÃ³n " + index + ")", 
                                               playerIdStr, "assistsMade", performance.getAssistsMade());
        }
        
        if (performance.getWasMvp() == null) {
            throw new InvalidPlayerDataException("El estatus MVP es obligatorio (posiciÃ³n " + index + ")", 
                                               playerIdStr, "wasMvp", null);
        }
        
        // Validar goles recibidos para roles defensivos
        if ((performance.getRoleType() == RoleType.DEFENSA || performance.getRoleType() == RoleType.ARQUERO) 
            && (performance.getGoalsConceded() == null || performance.getGoalsConceded() < 0)) {
            throw new InvalidPlayerDataException(
                "Los goles recibidos son obligatorios y no pueden ser negativos para roles defensivos (posiciÃ³n " + index + ")", 
                playerIdStr, "goalsConceded", performance.getGoalsConceded());
        }
    }

    /**
     * Actualiza las calificaciones de arquero rotativo para todos los jugadores en un partido.
     * En este modo especial, todos los jugadores reciben actualizaciones de calificaciÃ³n de arquero
     * con puntos de resultado modificados, procesÃ¡ndose independientemente del sistema regular.
     * 
     * Implementa los requerimientos:
     * - 7.1: CÃ¡lculo de calificaciones de arquero para TODOS los jugadores
     * - 7.3: Uso de puntos de resultado modificados
     * - 7.4: AplicaciÃ³n de reglas de mÃ­nimo base para prioridad de arquero
     * - 7.5: Procesamiento independiente del sistema regular
     * 
     * @param matchId ID del partido en modo arquero rotativo
     * @param matchResult Resultado del partido desde la perspectiva del equipo
     * @throws InvalidPlayerDataException si los datos de entrada son invÃ¡lidos
     * @throws MatchNotFoundException si el partido no existe
     * @throws RatingCalculationException si ocurre un error durante el procesamiento
     */
    public void updateRotativeGoalkeeperRatings(Long matchId, MatchResultType matchResult) {
        logger.info("Iniciando actualizaciÃ³n de calificaciones de arquero rotativo para partido {}", matchId);
        
        // ValidaciÃ³n de entrada
        validateRotativeGoalkeeperInput(matchId, matchResult);
        
        // Verificar que el partido existe
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException("El partido con ID " + matchId + " no existe", matchId));
        
        // Obtener todos los jugadores que participaron en el partido
        // Nota: En una implementaciÃ³n real, esto vendrÃ­a de MatchPlayer o similar
        // Por ahora, obtenemos todos los jugadores con calificaciones de arquero existentes
        List<PlayerRating> goalkeeperRatings = playerRatingRepository.findByRoleType(RoleType.ARQUERO);
        
        if (goalkeeperRatings.isEmpty()) {
            logger.warn("No se encontraron calificaciones de arquero para actualizar en modo rotativo");
            return;
        }
        
        int processedCount = 0;
        int errorCount = 0;
        
        // Procesar cada calificaciÃ³n de arquero
        for (PlayerRating goalkeeperRating : goalkeeperRatings) {
            try {
                processRotativeGoalkeeperUpdate(match, goalkeeperRating, matchResult);
                processedCount++;
                logger.debug("CalificaciÃ³n de arquero rotativo actualizada para jugador {}", 
                           goalkeeperRating.getPlayerProfile().getAtletaUuid());
            } catch (Exception e) {
                errorCount++;
                logger.error("Error actualizando calificaciÃ³n de arquero rotativo para jugador {}: {}", 
                           goalkeeperRating.getPlayerProfile().getAtletaUuid(), e.getMessage(), e);
                // Continuar procesando otros jugadores
            }
        }
        
        logger.info("ActualizaciÃ³n de calificaciones de arquero rotativo completada para partido {}. " +
                   "Procesados: {}, Errores: {}", matchId, processedCount, errorCount);
        
        if (errorCount > 0) {
            logger.warn("Se encontraron {} errores durante la actualizaciÃ³n de arquero rotativo", errorCount);
        }
    }

    /**
     * Procesa la actualizaciÃ³n de calificaciÃ³n de arquero rotativo para un jugador individual.
     */
    private void processRotativeGoalkeeperUpdate(Match match, PlayerRating goalkeeperRating, 
                                               MatchResultType matchResult) {
        // Preparar solicitud de cÃ¡lculo para arquero rotativo
        RotativeGoalkeeperRequest rotativeRequest = new RotativeGoalkeeperRequest(
                goalkeeperRating.getCurrentRating(),
                goalkeeperRating.getPriorityLevel(),
                matchResult
        );
        
        // Calcular nueva calificaciÃ³n usando el motor de arquero rotativo
        BigDecimal previousRating = goalkeeperRating.getCurrentRating();
        BigDecimal newRating = calculationEngine.calculateRotativeGoalkeeperRating(rotativeRequest);
        BigDecimal ratingDelta = newRating.subtract(previousRating);
        
        // Actualizar la calificaciÃ³n del jugador
        goalkeeperRating.updateRating(newRating);
        playerRatingRepository.save(goalkeeperRating);
        
        // Crear registro de historial para arquero rotativo
        createRotativeGoalkeeperHistoryRecord(goalkeeperRating, match, previousRating, 
                                            newRating, ratingDelta, matchResult);
        
        logger.debug("CalificaciÃ³n de arquero rotativo actualizada: jugador={}, " +
                    "anterior={}, nueva={}, delta={}", 
                    goalkeeperRating.getPlayerProfile().getAtletaUuid(), 
                    previousRating, newRating, ratingDelta);
    }

    /**
     * Crea un registro de historial especÃ­fico para modo arquero rotativo.
     */
    private void createRotativeGoalkeeperHistoryRecord(PlayerRating goalkeeperRating, Match match,
                                                     BigDecimal previousRating, BigDecimal newRating,
                                                     BigDecimal ratingDelta, MatchResultType matchResult) {
        RatingHistory history = new RatingHistory(
                goalkeeperRating, match, previousRating, newRating, ratingDelta,
                0, 0, false, matchResult // En modo rotativo: 0 goles, 0 asistencias, no MVP
        );
        
        // Marcar como modo arquero rotativo
        history.setRotativeGoalkeeperMode(true);
        
        // Establecer componentes especÃ­ficos del cÃ¡lculo rotativo
        history.setResultPoints(BigDecimal.valueOf(matchResult.getRotativeGoalkeeperPoints()));
        history.setWeightedGoalPoints(BigDecimal.ZERO);
        history.setWeightedAssistPoints(BigDecimal.ZERO);
        history.setDefensiveBonus(BigDecimal.ZERO);
        history.setMvpBonus(BigDecimal.ZERO);
        history.setPriorityMultiplier(BigDecimal.valueOf(goalkeeperRating.getPriorityLevel().getMultiplier()));
        
        ratingHistoryRepository.save(history);
    }

    /**
     * Obtiene todas las calificaciones actuales de un jugador especÃ­fico.
     * 
     * Implementa el requerimiento 9.1: consulta de calificaciones por jugador.
     * 
     * @param playerProfileId UUID del perfil del jugador
     * @return Lista de calificaciones del jugador ordenadas por rol y prioridad
     * @throws InvalidPlayerDataException si el playerProfileId es nulo
     */
    @Transactional(readOnly = true)
    public List<PlayerRating> getPlayerRatings(UUID playerProfileId) {
        logger.debug("Consultando calificaciones para jugador {}", playerProfileId);
        
        if (playerProfileId == null) {
            throw new InvalidPlayerDataException("El ID del perfil del jugador es obligatorio", 
                                               null, "playerProfileId", null);
        }
        
        List<PlayerRating> ratings = playerRatingRepository.findByPlayerProfileId(playerProfileId);
        
        logger.debug("Encontradas {} calificaciones para jugador {}", ratings.size(), playerProfileId);
        return ratings;
    }

    /**
     * Obtiene las calificaciones de un jugador filtradas por rol especÃ­fico.
     * 
     * @param playerProfileId UUID del perfil del jugador
     * @param roleType Tipo de rol a filtrar
     * @return Lista de calificaciones del jugador para el rol especificado
     * @throws InvalidPlayerDataException si los parÃ¡metros son nulos
     */
    @Transactional(readOnly = true)
    public List<PlayerRating> getPlayerRatingsByRole(UUID playerProfileId, RoleType roleType) {
        logger.debug("Consultando calificaciones para jugador {} y rol {}", playerProfileId, roleType);
        
        if (playerProfileId == null) {
            throw new InvalidPlayerDataException("El ID del perfil del jugador es obligatorio", 
                                               null, "playerProfileId", null);
        }
        if (roleType == null) {
            throw new InvalidPlayerDataException("El tipo de rol es obligatorio", 
                                               null, "roleType", null);
        }
        
        List<PlayerRating> ratings = playerRatingRepository.findByPlayerProfileIdAndRoleType(playerProfileId, roleType);
        
        logger.debug("Encontradas {} calificaciones para jugador {} y rol {}", 
                    ratings.size(), playerProfileId, roleType);
        return ratings;
    }

    /**
     * Obtiene las calificaciones de un jugador filtradas por nivel de prioridad.
     * 
     * @param playerProfileId UUID del perfil del jugador
     * @param priorityLevel Nivel de prioridad a filtrar
     * @return Lista de calificaciones del jugador para el nivel de prioridad especificado
     * @throws InvalidPlayerDataException si los parÃ¡metros son nulos
     */
    @Transactional(readOnly = true)
    public List<PlayerRating> getPlayerRatingsByPriority(UUID playerProfileId, PriorityLevel priorityLevel) {
        logger.debug("Consultando calificaciones para jugador {} y prioridad {}", playerProfileId, priorityLevel);
        
        if (playerProfileId == null) {
            throw new InvalidPlayerDataException("El ID del perfil del jugador es obligatorio", 
                                               null, "playerProfileId", null);
        }
        if (priorityLevel == null) {
            throw new InvalidPlayerDataException("El nivel de prioridad es obligatorio", 
                                               null, "priorityLevel", null);
        }
        
        List<PlayerRating> ratings = playerRatingRepository.findByPlayerProfileIdAndPriorityLevel(playerProfileId, priorityLevel);
        
        logger.debug("Encontradas {} calificaciones para jugador {} y prioridad {}", 
                    ratings.size(), playerProfileId, priorityLevel);
        return ratings;
    }

    /**
     * Obtiene el historial completo de calificaciones de un jugador.
     * 
     * Implementa el requerimiento 9.2: consulta de historial de calificaciones.
     * 
     * @param playerProfileId UUID del perfil del jugador
     * @return Lista del historial de calificaciones ordenado por fecha descendente
     * @throws InvalidPlayerDataException si el playerProfileId es nulo
     */
    @Transactional(readOnly = true)
    public List<RatingHistory> getRatingHistory(UUID playerProfileId) {
        logger.debug("Consultando historial de calificaciones para jugador {}", playerProfileId);
        
        if (playerProfileId == null) {
            throw new InvalidPlayerDataException("El ID del perfil del jugador es obligatorio", 
                                               null, "playerProfileId", null);
        }
        
        List<RatingHistory> history = ratingHistoryRepository.findByPlayerProfileId(playerProfileId);
        
        logger.debug("Encontrados {} registros de historial para jugador {}", history.size(), playerProfileId);
        return history;
    }

    /**
     * Obtiene el historial de calificaciones de un jugador filtrado por rol.
     * 
     * @param playerProfileId UUID del perfil del jugador
     * @param roleType Tipo de rol a filtrar
     * @return Lista del historial de calificaciones para el rol ordenado por fecha descendente
     * @throws InvalidPlayerDataException si los parÃ¡metros son nulos
     */
    @Transactional(readOnly = true)
    public List<RatingHistory> getRatingHistoryByRole(UUID playerProfileId, RoleType roleType) {
        logger.debug("Consultando historial de calificaciones para jugador {} y rol {}", playerProfileId, roleType);
        
        if (playerProfileId == null) {
            throw new InvalidPlayerDataException("El ID del perfil del jugador es obligatorio", 
                                               null, "playerProfileId", null);
        }
        if (roleType == null) {
            throw new InvalidPlayerDataException("El tipo de rol es obligatorio", 
                                               null, "roleType", null);
        }
        
        List<RatingHistory> history = ratingHistoryRepository.findByPlayerProfileIdAndRoleType(playerProfileId, roleType);
        
        logger.debug("Encontrados {} registros de historial para jugador {} y rol {}", 
                    history.size(), playerProfileId, roleType);
        return history;
    }

    /**
     * Obtiene el historial de calificaciones de un jugador en un perÃ­odo de tiempo especÃ­fico.
     * 
     * @param playerProfileId UUID del perfil del jugador
     * @param startDate Fecha de inicio del perÃ­odo (inclusive)
     * @param endDate Fecha de fin del perÃ­odo (inclusive)
     * @return Lista del historial de calificaciones en el perÃ­odo ordenado por fecha descendente
     * @throws InvalidPlayerDataException si los parÃ¡metros son nulos o las fechas son invÃ¡lidas
     */
    @Transactional(readOnly = true)
    public List<RatingHistory> getRatingHistoryByPeriod(UUID playerProfileId, 
                                                       LocalDateTime startDate, 
                                                       LocalDateTime endDate) {
        logger.debug("Consultando historial de calificaciones para jugador {} entre {} y {}", 
                    playerProfileId, startDate, endDate);
        
        if (playerProfileId == null) {
            throw new InvalidPlayerDataException("El ID del perfil del jugador es obligatorio", 
                                               null, "playerProfileId", null);
        }
        if (startDate == null) {
            throw new InvalidPlayerDataException("La fecha de inicio es obligatoria", 
                                               null, "startDate", null);
        }
        if (endDate == null) {
            throw new InvalidPlayerDataException("La fecha de fin es obligatoria", 
                                               null, "endDate", null);
        }
        if (startDate.isAfter(endDate)) {
            throw new InvalidPlayerDataException("La fecha de inicio no puede ser posterior a la fecha de fin", 
                                               null, "dateRange", startDate + " > " + endDate);
        }
        
        List<RatingHistory> history = ratingHistoryRepository.findByPlayerProfileIdAndCreatedAtBetween(
                playerProfileId, startDate, endDate);
        
        logger.debug("Encontrados {} registros de historial para jugador {} en el perÃ­odo especificado", 
                    history.size(), playerProfileId);
        return history;
    }

    /**
     * Obtiene el historial de calificaciones de un jugador filtrado por rol y perÃ­odo.
     * 
     * @param playerProfileId UUID del perfil del jugador
     * @param roleType Tipo de rol a filtrar
     * @param startDate Fecha de inicio del perÃ­odo (inclusive)
     * @param endDate Fecha de fin del perÃ­odo (inclusive)
     * @return Lista del historial de calificaciones filtrado ordenado por fecha descendente
     * @throws InvalidPlayerDataException si los parÃ¡metros son nulos o las fechas son invÃ¡lidas
     */
    @Transactional(readOnly = true)
    public List<RatingHistory> getRatingHistoryByRoleAndPeriod(UUID playerProfileId, 
                                                              RoleType roleType,
                                                              LocalDateTime startDate, 
                                                              LocalDateTime endDate) {
        logger.debug("Consultando historial de calificaciones para jugador {}, rol {} entre {} y {}", 
                    playerProfileId, roleType, startDate, endDate);
        
        if (playerProfileId == null) {
            throw new InvalidPlayerDataException("El ID del perfil del jugador es obligatorio", 
                                               null, "playerProfileId", null);
        }
        if (roleType == null) {
            throw new InvalidPlayerDataException("El tipo de rol es obligatorio", 
                                               null, "roleType", null);
        }
        if (startDate == null) {
            throw new InvalidPlayerDataException("La fecha de inicio es obligatoria", 
                                               null, "startDate", null);
        }
        if (endDate == null) {
            throw new InvalidPlayerDataException("La fecha de fin es obligatoria", 
                                               null, "endDate", null);
        }
        if (startDate.isAfter(endDate)) {
            throw new InvalidPlayerDataException("La fecha de inicio no puede ser posterior a la fecha de fin", 
                                               null, "dateRange", startDate + " > " + endDate);
        }
        
        // Obtener historial por jugador y perÃ­odo, luego filtrar por rol
        List<RatingHistory> allHistory = ratingHistoryRepository.findByPlayerProfileIdAndCreatedAtBetween(
                playerProfileId, startDate, endDate);
        
        List<RatingHistory> filteredHistory = allHistory.stream()
                .filter(h -> h.getPlayerRating().getRoleType() == roleType)
                .toList();
        
        logger.debug("Encontrados {} registros de historial para jugador {}, rol {} en el perÃ­odo especificado", 
                    filteredHistory.size(), playerProfileId, roleType);
        return filteredHistory;
    }

    /**
     * Obtiene estadÃ­sticas de rendimiento de un jugador.
     * 
     * @param playerProfileId UUID del perfil del jugador
     * @return Array con [total_partidos, total_goles, total_asistencias, veces_mvp, promedio_delta]
     * @throws InvalidPlayerDataException si el playerProfileId es nulo
     */
    @Transactional(readOnly = true)
    public Object[] getPlayerPerformanceStatistics(UUID playerProfileId) {
        logger.debug("Consultando estadÃ­sticas de rendimiento para jugador {}", playerProfileId);
        
        if (playerProfileId == null) {
            throw new InvalidPlayerDataException("El ID del perfil del jugador es obligatorio", 
                                               null, "playerProfileId", null);
        }
        
        Object[] stats = ratingHistoryRepository.getPlayerPerformanceStatistics(playerProfileId);
        
        logger.debug("EstadÃ­sticas obtenidas para jugador {}: {} partidos, {} goles, {} asistencias, {} MVPs", 
                    playerProfileId, stats[0], stats[1], stats[2], stats[3]);
        return stats;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> getLeaderboard(RoleType roleType, Integer limit) {
        int resolvedLimit = resolveLeaderboardLimit(limit);
        List<PlayerRating> ratings = roleType != null
                ? playerRatingRepository.findByRoleType(roleType)
                : playerRatingRepository.findActivePlayerRatings();

        List<LeaderboardEntryResponse> rows = new java.util.ArrayList<>();
        Set<UUID> processed = new HashSet<>();

        for (PlayerRating rating : ratings) {
            if (rating == null || rating.getPlayerProfile() == null || rating.getPlayerProfile().getAtletaUuid() == null) {
                continue;
            }

            UUID playerUuid = rating.getPlayerProfile().getAtletaUuid();
            if (processed.contains(playerUuid)) {
                continue;
            }

            processed.add(playerUuid);
            rows.add(toLeaderboardRow(rating, roleType));
            if (rows.size() >= resolvedLimit) {
                break;
            }
        }

        return rows;
    }

    /**
     * Obtiene estadÃ­sticas de rendimiento de un jugador para un rol especÃ­fico.
     * 
     * @param playerProfileId UUID del perfil del jugador
     * @param roleType Tipo de rol a filtrar
     * @return Array con [total_partidos, total_goles, total_asistencias, veces_mvp, promedio_delta]
     * @throws InvalidPlayerDataException si los parÃ¡metros son nulos
     */
    @Transactional(readOnly = true)
    public Object[] getPlayerPerformanceStatisticsByRole(UUID playerProfileId, RoleType roleType) {
        logger.debug("Consultando estadÃ­sticas de rendimiento para jugador {} y rol {}", playerProfileId, roleType);
        
        if (playerProfileId == null) {
            throw new InvalidPlayerDataException("El ID del perfil del jugador es obligatorio", 
                                               null, "playerProfileId", null);
        }
        if (roleType == null) {
            throw new InvalidPlayerDataException("El tipo de rol es obligatorio", 
                                               null, "roleType", null);
        }
        
        Object[] stats = ratingHistoryRepository.getPlayerPerformanceStatisticsByRole(playerProfileId, roleType);
        
        logger.debug("EstadÃ­sticas por rol obtenidas para jugador {} y rol {}: {} partidos, {} goles, {} asistencias, {} MVPs", 
                    playerProfileId, roleType, stats[0], stats[1], stats[2], stats[3]);
        return stats;
    }

    private LeaderboardEntryResponse toLeaderboardRow(PlayerRating rating, RoleType filterRole) {
        UUID playerUuid = rating.getPlayerProfile().getAtletaUuid();
        String alias = rating.getPlayerProfile().getAlias();
        Object[] resultBreakdown = normalizeBreakdown(ratingHistoryRepository.getResultBreakdownByPlayerProfileId(playerUuid));

        long wins = asLong(resultBreakdown, 0);
        long losses = asLong(resultBreakdown, 1);
        long draws = asLong(resultBreakdown, 2);
        int matches = rating.getMatchesPlayed() != null ? rating.getMatchesPlayed() : (int) (wins + losses + draws);

        LeaderboardEntryResponse row = new LeaderboardEntryResponse();
        row.setPlayerProfileId(playerUuid);
        row.setPlayerId(playerUuid.toString());
        row.setAlias(alias);
        row.setName(alias);
        row.setScore(rating.getCurrentRating());
        row.setRating(rating.getCurrentRating());
        row.setRoleType(filterRole != null ? filterRole : rating.getRoleType());
        row.setMatchesPlayed(matches);
        row.setWins(wins);
        row.setLosses(losses);
        row.setDraws(draws);
        return row;
    }

    private int resolveLeaderboardLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return 50;
        }
        return Math.min(requestedLimit, 200);
    }

    private long asLong(Object[] values, int index) {
        if (values == null || index >= values.length || values[index] == null) {
            return 0L;
        }
        return ((Number) values[index]).longValue();
    }

    /**
     * Some JPA providers may wrap aggregate rows as Object[]{ Object[]{...} }.
     * Normalize to a flat Object[] to keep leaderboard mapping resilient.
     */
    private Object[] normalizeBreakdown(Object[] raw) {
        if (raw == null) {
            return null;
        }
        if (raw.length > 0 && raw[0] instanceof Object[] nested) {
            return nested;
        }
        return raw;
    }

    /**
     * Valida los datos de entrada para updateRotativeGoalkeeperRatings.
     * 
     * @throws InvalidPlayerDataException si los datos son invÃ¡lidos
     */
    private void validateRotativeGoalkeeperInput(Long matchId, MatchResultType matchResult) {
        if (matchId == null) {
            throw new InvalidPlayerDataException("El ID del partido es obligatorio", null, "matchId", null);
        }
        
        if (matchResult == null) {
            throw new InvalidPlayerDataException("El resultado del partido es obligatorio", null, "matchResult", null);
        }
        
        logger.debug("ValidaciÃ³n de entrada de arquero rotativo completada para partido {}", matchId);
    }

    /**
     * Valida que solo haya un MVP en el partido.
     * Implementa el requerimiento 6.4: restricciÃ³n de MVP Ãºnico.
     * 
     * @throws InvalidPlayerDataException si hay mÃºltiples MVPs
     */
    private void validateSingleMvp(List<PlayerPerformanceDto> performances) {
        long mvpCount = performances.stream()
                .mapToLong(p -> Boolean.TRUE.equals(p.getWasMvp()) ? 1 : 0)
                .sum();
        
        if (mvpCount > 1) {
            throw new InvalidPlayerDataException("Solo puede haber un MVP por partido. Se encontraron " + mvpCount + " MVPs", 
                                               null, "mvpCount", mvpCount);
        }
        
        logger.debug("ValidaciÃ³n de MVP Ãºnico completada: {} MVPs encontrados", mvpCount);
    }

    /**
     * Calcula la calificaciÃ³n general (OVR) de un jugador usando el mÃ©todo hÃ­brido.
     * 
     * FÃ³rmula: OVR = (Mejor Ã— 0.4) + (Top3_Promedio Ã— 0.4) + (Todos_Promedio Ã— 0.2)
     * 
     * @param playerProfileId UUID del perfil del jugador
     * @return CalificaciÃ³n general del jugador (0-100)
     * @throws InvalidPlayerDataException si el playerProfileId es nulo
     * @throws PlayerNotFoundException si no se encuentran calificaciones para el jugador
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateHybridOVR(UUID playerProfileId) {
        logger.debug("Calculando OVR hÃ­brido para jugador {}", playerProfileId);
        
        if (playerProfileId == null) {
            throw new InvalidPlayerDataException("El ID del perfil del jugador es obligatorio", 
                                               null, "playerProfileId", null);
        }
        
        List<PlayerRating> ratings = playerRatingRepository.findByPlayerProfileId(playerProfileId);
        
        if (ratings.isEmpty()) {
            throw new PlayerNotFoundException("No se encontraron calificaciones para el jugador", 
                                            playerProfileId.toString());
        }
        
        return calculateHybridOVRFromRatings(ratings);
    }

    /**
     * Calcula el OVR hÃ­brido a partir de una lista de calificaciones.
     */
    private BigDecimal calculateHybridOVRFromRatings(List<PlayerRating> ratings) {
        // Ordenar por calificaciÃ³n descendente
        ratings.sort((r1, r2) -> r2.getCurrentRating().compareTo(r1.getCurrentRating()));
        
        // 1. Mejor calificaciÃ³n (40%)
        BigDecimal bestRating = ratings.get(0).getCurrentRating();
        BigDecimal bestComponent = bestRating.multiply(BigDecimal.valueOf(0.4));
        
        // 2. Promedio de top 3 (40%)
        BigDecimal top3Sum = BigDecimal.ZERO;
        int top3Count = Math.min(3, ratings.size());
        for (int i = 0; i < top3Count; i++) {
            top3Sum = top3Sum.add(ratings.get(i).getCurrentRating());
        }
        BigDecimal top3Average = top3Sum.divide(BigDecimal.valueOf(top3Count), 2, RoundingMode.HALF_UP);
        BigDecimal top3Component = top3Average.multiply(BigDecimal.valueOf(0.4));
        
        // 3. Promedio de todos (20%)
        BigDecimal allSum = ratings.stream()
                .map(PlayerRating::getCurrentRating)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal allAverage = allSum.divide(BigDecimal.valueOf(ratings.size()), 2, RoundingMode.HALF_UP);
        BigDecimal allComponent = allAverage.multiply(BigDecimal.valueOf(0.2));
        
        // Calcular OVR final
        BigDecimal overallRating = bestComponent.add(top3Component).add(allComponent);
        overallRating = overallRating.setScale(2, RoundingMode.HALF_UP);
        
        logger.debug("OVR hÃ­brido calculado: {} (mejor: {}, top3: {}, todos: {})", 
                    overallRating, bestRating, top3Average, allAverage);
        
        return overallRating;
    }

    /**
     * Calcula la calificaciÃ³n general ponderada por prioridades.
     * 
     * @param playerProfileId UUID del perfil del jugador
     * @return CalificaciÃ³n general ponderada
     * @throws InvalidPlayerDataException si el playerProfileId es nulo
     * @throws PlayerNotFoundException si no se encuentran calificaciones para el jugador
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateWeightedOVR(UUID playerProfileId) {
        logger.debug("Calculando OVR ponderado para jugador {}", playerProfileId);
        
        if (playerProfileId == null) {
            throw new InvalidPlayerDataException("El ID del perfil del jugador es obligatorio", 
                                               null, "playerProfileId", null);
        }
        
        List<PlayerRating> ratings = playerRatingRepository.findByPlayerProfileId(playerProfileId);
        
        if (ratings.isEmpty()) {
            throw new PlayerNotFoundException("No se encontraron calificaciones para el jugador", 
                                            playerProfileId.toString());
        }
        
        return calculateWeightedOVRFromRatings(ratings);
    }

    /**
     * Calcula el OVR ponderado a partir de una lista de calificaciones.
     */
    private BigDecimal calculateWeightedOVRFromRatings(List<PlayerRating> ratings) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        
        for (PlayerRating rating : ratings) {
            // Obtener peso segÃºn prioridad
            BigDecimal weight = BigDecimal.valueOf(rating.getPriorityLevel().getMultiplier());
            
            // Si el peso es 0, usar peso mÃ­nimo
            if (weight.compareTo(BigDecimal.ZERO) == 0) {
                weight = BigDecimal.valueOf(0.1);
            }
            
            weightedSum = weightedSum.add(rating.getCurrentRating().multiply(weight));
            totalWeight = totalWeight.add(weight);
        }
        
        BigDecimal overallRating = weightedSum.divide(totalWeight, 2, RoundingMode.HALF_UP);
        
        logger.debug("OVR ponderado calculado: {}", overallRating);
        
        return overallRating;
    }

    /**
     * Calcula el OVR simple (promedio de todas las calificaciones).
     * 
     * @param playerProfileId UUID del perfil del jugador
     * @return CalificaciÃ³n general simple
     * @throws InvalidPlayerDataException si el playerProfileId es nulo
     * @throws PlayerNotFoundException si no se encuentran calificaciones para el jugador
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateSimpleOVR(UUID playerProfileId) {
        logger.debug("Calculando OVR simple para jugador {}", playerProfileId);
        
        if (playerProfileId == null) {
            throw new InvalidPlayerDataException("El ID del perfil del jugador es obligatorio", 
                                               null, "playerProfileId", null);
        }
        
        List<PlayerRating> ratings = playerRatingRepository.findByPlayerProfileId(playerProfileId);
        
        if (ratings.isEmpty()) {
            throw new PlayerNotFoundException("No se encontraron calificaciones para el jugador", 
                                            playerProfileId.toString());
        }
        
        BigDecimal sum = ratings.stream()
                .map(PlayerRating::getCurrentRating)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal average = sum.divide(BigDecimal.valueOf(ratings.size()), 2, RoundingMode.HALF_UP);
        
        logger.debug("OVR simple calculado: {}", average);
        
        return average;
    }

    /**
     * Calcula el Ã­ndice de versatilidad del jugador.
     * Versatilidad = (NÃºmero de roles >= OVR - 10) / Total de roles
     * 
     * @param ratings Lista de calificaciones del jugador
     * @param overallRating OVR del jugador
     * @return Ãndice de versatilidad (0.0 - 1.0)
     */
    private BigDecimal calculateVersatility(List<PlayerRating> ratings, BigDecimal overallRating) {
        BigDecimal threshold = overallRating.subtract(BigDecimal.valueOf(10));
        
        long competitiveRoles = ratings.stream()
                .filter(r -> r.getCurrentRating().compareTo(threshold) >= 0)
                .count();
        
        BigDecimal versatility = BigDecimal.valueOf(competitiveRoles)
                .divide(BigDecimal.valueOf(6), 2, RoundingMode.HALF_UP);
        
        logger.debug("Versatilidad calculada: {} ({} roles competitivos de 6)", versatility, competitiveRoles);
        
        return versatility;
    }

    /**
     * Calcula el score de consistencia del jugador.
     * Consistencia = 100 - (DesviaciÃ³n_EstÃ¡ndar Ã— 2)
     * 
     * @param ratings Lista de calificaciones del jugador
     * @return Score de consistencia (0-100)
     */
    private BigDecimal calculateConsistency(List<PlayerRating> ratings) {
        // Calcular promedio
        BigDecimal sum = ratings.stream()
                .map(PlayerRating::getCurrentRating)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(ratings.size()), 2, RoundingMode.HALF_UP);
        
        // Calcular desviaciÃ³n estÃ¡ndar
        BigDecimal varianceSum = BigDecimal.ZERO;
        for (PlayerRating rating : ratings) {
            BigDecimal diff = rating.getCurrentRating().subtract(average);
            varianceSum = varianceSum.add(diff.multiply(diff));
        }
        
        BigDecimal variance = varianceSum.divide(BigDecimal.valueOf(ratings.size()), 2, RoundingMode.HALF_UP);
        double stdDev = Math.sqrt(variance.doubleValue());
        
        BigDecimal consistency = BigDecimal.valueOf(100 - (stdDev * 2));
        consistency = consistency.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        
        logger.debug("Consistencia calculada: {} (desviaciÃ³n estÃ¡ndar: {})", consistency, stdDev);
        
        return consistency;
    }

    /**
     * Encuentra el mejor rol del jugador.
     * 
     * @param ratings Lista de calificaciones del jugador
     * @return Rol con la calificaciÃ³n mÃ¡s alta
     */
    private RoleType findBestRole(List<PlayerRating> ratings) {
        return ratings.stream()
                .max((r1, r2) -> r1.getCurrentRating().compareTo(r2.getCurrentRating()))
                .map(PlayerRating::getRoleType)
                .orElse(null);
    }

    /**
     * Calcula las estadÃ­sticas completas de OVR para un jugador.
     * Incluye OVR hÃ­brido, ponderado, simple, versatilidad, consistencia y mejor rol.
     * 
     * @param playerProfileId UUID del perfil del jugador
     * @return EstadÃ­sticas completas de OVR
     * @throws InvalidPlayerDataException si el playerProfileId es nulo
     * @throws PlayerNotFoundException si no se encuentran calificaciones para el jugador
     */
    @Transactional(readOnly = true)
    public PlayerOverallStats calculateCompleteOverall(UUID playerProfileId) {
        logger.info("Calculando estadÃ­sticas completas de OVR para jugador {}", playerProfileId);
        
        if (playerProfileId == null) {
            throw new InvalidPlayerDataException("El ID del perfil del jugador es obligatorio", 
                                               null, "playerProfileId", null);
        }
        
        List<PlayerRating> ratings = playerRatingRepository.findByPlayerProfileId(playerProfileId);
        
        if (ratings.isEmpty()) {
            throw new PlayerNotFoundException("No se encontraron calificaciones para el jugador", 
                                            playerProfileId.toString());
        }
        
        PlayerOverallStats stats = new PlayerOverallStats();
        stats.setPlayerProfileId(playerProfileId);
        
        // Calcular OVRs
        BigDecimal hybridOVR = calculateHybridOVRFromRatings(ratings);
        stats.setHybridOVR(hybridOVR);
        stats.setWeightedOVR(calculateWeightedOVRFromRatings(ratings));
        stats.setSimpleOVR(calculateSimpleOVR(playerProfileId));
        
        // Calcular mÃ©tricas adicionales
        stats.setVersatilityIndex(calculateVersatility(ratings, hybridOVR));
        stats.setConsistencyScore(calculateConsistency(ratings));
        
        // Encontrar mejor rol
        RoleType bestRole = findBestRole(ratings);
        stats.setBestRole(bestRole);
        
        // Obtener calificaciÃ³n del mejor rol
        ratings.stream()
                .filter(r -> r.getRoleType() == bestRole)
                .findFirst()
                .ifPresent(r -> stats.setBestRoleRating(r.getCurrentRating()));
        
        // ClasificaciÃ³n basada en OVR hÃ­brido
        stats.setClassification(getClassificationFromOVR(hybridOVR));
        
        // EstadÃ­sticas generales
        stats.setTotalRatings(ratings.size());
        stats.setTotalMatchesPlayed(ratings.stream()
                .mapToInt(PlayerRating::getMatchesPlayed)
                .sum());
        
        logger.info("EstadÃ­sticas completas calculadas para jugador {}: OVR={}, ClasificaciÃ³n={}", 
                   playerProfileId, hybridOVR, stats.getClassification());
        
        return stats;
    }

    /**
     * Obtiene la clasificaciÃ³n basada en el OVR.
     * 
     * @param ovr CalificaciÃ³n general
     * @return ClasificaciÃ³n del jugador
     */
    private String getClassificationFromOVR(BigDecimal ovr) {
        if (ovr.compareTo(BigDecimal.valueOf(95)) >= 0) return "LEYENDA";
        if (ovr.compareTo(BigDecimal.valueOf(85)) >= 0) return "Ã‰LITE";
        if (ovr.compareTo(BigDecimal.valueOf(75)) >= 0) return "EXPERTO";
        if (ovr.compareTo(BigDecimal.valueOf(65)) >= 0) return "AVANZADO";
        if (ovr.compareTo(BigDecimal.valueOf(55)) >= 0) return "INTERMEDIO";
        if (ovr.compareTo(BigDecimal.valueOf(50)) >= 0) return "PRINCIPIANTE";
        return "NOVATO";
    }

    /**
     * Clase interna para encapsular las estadÃ­sticas de OVR.
     */
    public static class PlayerOverallStats {
        private UUID playerProfileId;
        private BigDecimal hybridOVR;
        private BigDecimal weightedOVR;
        private BigDecimal simpleOVR;
        private String classification;
        private BigDecimal versatilityIndex;
        private BigDecimal consistencyScore;
        private RoleType bestRole;
        private BigDecimal bestRoleRating;
        private Integer totalRatings;
        private Integer totalMatchesPlayed;

        // Getters and Setters
        public UUID getPlayerProfileId() { return playerProfileId; }
        public void setPlayerProfileId(UUID playerProfileId) { this.playerProfileId = playerProfileId; }
        
        public BigDecimal getHybridOVR() { return hybridOVR; }
        public void setHybridOVR(BigDecimal hybridOVR) { this.hybridOVR = hybridOVR; }
        
        public BigDecimal getWeightedOVR() { return weightedOVR; }
        public void setWeightedOVR(BigDecimal weightedOVR) { this.weightedOVR = weightedOVR; }
        
        public BigDecimal getSimpleOVR() { return simpleOVR; }
        public void setSimpleOVR(BigDecimal simpleOVR) { this.simpleOVR = simpleOVR; }
        
        public String getClassification() { return classification; }
        public void setClassification(String classification) { this.classification = classification; }
        
        public BigDecimal getVersatilityIndex() { return versatilityIndex; }
        public void setVersatilityIndex(BigDecimal versatilityIndex) { this.versatilityIndex = versatilityIndex; }
        
        public BigDecimal getConsistencyScore() { return consistencyScore; }
        public void setConsistencyScore(BigDecimal consistencyScore) { this.consistencyScore = consistencyScore; }
        
        public RoleType getBestRole() { return bestRole; }
        public void setBestRole(RoleType bestRole) { this.bestRole = bestRole; }
        
        public BigDecimal getBestRoleRating() { return bestRoleRating; }
        public void setBestRoleRating(BigDecimal bestRoleRating) { this.bestRoleRating = bestRoleRating; }
        
        public Integer getTotalRatings() { return totalRatings; }
        public void setTotalRatings(Integer totalRatings) { this.totalRatings = totalRatings; }
        
        public Integer getTotalMatchesPlayed() { return totalMatchesPlayed; }
        public void setTotalMatchesPlayed(Integer totalMatchesPlayed) { this.totalMatchesPlayed = totalMatchesPlayed; }
    }
}

