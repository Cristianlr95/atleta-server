package com.atleta.demo.service;

import com.atleta.demo.dto.request.RatingCalculationRequest;
import com.atleta.demo.dto.request.RotativeGoalkeeperRequest;
import com.atleta.demo.enums.PriorityLevel;
import com.atleta.demo.enums.RoleType;
import com.atleta.demo.exception.InvalidPlayerDataException;
import com.atleta.demo.exception.RatingCalculationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Motor de cálculo de calificaciones que implementa el algoritmo principal
 * para actualizar las calificaciones de los jugadores basándose en su rendimiento.
 * 
 * El algoritmo sigue este orden específico:
 * 1. Calcular delta base: resultado + goles ponderados + asistencias ponderadas + bono defensivo + bono MVP
 * 2. Aplicar multiplicador de prioridad al delta
 * 3. Sumar delta ajustado a calificación actual
 * 4. Aplicar límites mínimos basados en prioridad
 * 
 * Implementa el requerimiento 9.4: Validación robusta y manejo de errores.
 */
@Component
public class RatingCalculationEngine {

    private static final Logger logger = LoggerFactory.getLogger(RatingCalculationEngine.class);

    /**
     * Calcula una nueva calificación basada en el rendimiento del jugador.
     * 
     * @param request datos del rendimiento y contexto del jugador
     * @return nueva calificación calculada
     * @throws InvalidPlayerDataException si los datos de entrada son inválidos
     * @throws RatingCalculationException si ocurre un error durante el cálculo
     */
    public BigDecimal calculateNewRating(RatingCalculationRequest request) {
        logger.debug("Iniciando cálculo de calificación para rol {} con prioridad {}", 
                    request != null ? request.getRoleType() : "null", 
                    request != null ? request.getPriorityLevel() : "null");
        
        try {
            validateRequest(request);
            
            // Paso 1: Calcular delta base sumando todos los componentes
            BigDecimal delta = calculateBaseDelta(request);
            logger.debug("Delta base calculado: {}", delta);
            
            // Paso 2: Aplicar multiplicador de prioridad
            BigDecimal adjustedDelta = applyPriorityMultiplier(delta, request.getPriorityLevel());
            logger.debug("Delta ajustado con multiplicador de prioridad: {}", adjustedDelta);
            
            // Paso 3: Sumar delta ajustado a calificación actual
            BigDecimal newRating = request.getCurrentRating().add(adjustedDelta);
            logger.debug("Nueva calificación antes de límites: {}", newRating);
            
            // Paso 4: Aplicar límites mínimos
            BigDecimal finalRating = enforceMinimumRating(newRating, request.getPriorityLevel());
            logger.debug("Calificación final después de límites: {}", finalRating);
            
            return finalRating;
            
        } catch (InvalidPlayerDataException e) {
            logger.error("Error de validación en cálculo de calificación: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado durante cálculo de calificación: {}", e.getMessage(), e);
            throw new RatingCalculationException("Error inesperado durante el cálculo de calificación", e);
        }
    }

    /**
     * Calcula una nueva calificación para modo arquero rotativo.
     * En este modo, todos los jugadores reciben actualizaciones de calificación de arquero
     * con puntos de resultado modificados.
     * 
     * @param request datos para el cálculo de arquero rotativo
     * @return nueva calificación de arquero calculada
     * @throws InvalidPlayerDataException si los datos de entrada son inválidos
     * @throws RatingCalculationException si ocurre un error durante el cálculo
     */
    public BigDecimal calculateRotativeGoalkeeperRating(RotativeGoalkeeperRequest request) {
        logger.debug("Iniciando cálculo de calificación de arquero rotativo con prioridad {}", 
                    request != null ? request.getGoalkeeperPriority() : "null");
        
        try {
            validateRotativeRequest(request);
            
            // En modo arquero rotativo, solo se aplican puntos de resultado modificados
            BigDecimal delta = BigDecimal.valueOf(request.getMatchResult().getRotativeGoalkeeperPoints());
            logger.debug("Delta de arquero rotativo: {}", delta);
            
            // Aplicar multiplicador de prioridad de arquero
            BigDecimal adjustedDelta = applyPriorityMultiplier(delta, request.getGoalkeeperPriority());
            logger.debug("Delta ajustado para arquero rotativo: {}", adjustedDelta);
            
            // Sumar delta ajustado a calificación actual de arquero
            BigDecimal newRating = request.getCurrentGoalkeeperRating().add(adjustedDelta);
            logger.debug("Nueva calificación de arquero antes de límites: {}", newRating);
            
            // Aplicar límites mínimos basados en prioridad de arquero
            BigDecimal finalRating = enforceMinimumRating(newRating, request.getGoalkeeperPriority());
            logger.debug("Calificación final de arquero rotativo: {}", finalRating);
            
            return finalRating;
            
        } catch (InvalidPlayerDataException e) {
            logger.error("Error de validación en cálculo de arquero rotativo: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado durante cálculo de arquero rotativo: {}", e.getMessage(), e);
            throw new RatingCalculationException("Error inesperado durante el cálculo de arquero rotativo", e);
        }
    }

    /**
     * Calcula el delta base sumando todos los componentes de rendimiento.
     * Orden: resultado + goles ponderados + asistencias ponderadas + bono defensivo + bono MVP
     */
    private BigDecimal calculateBaseDelta(RatingCalculationRequest request) {
        try {
            BigDecimal delta = BigDecimal.ZERO;
            
            // 1. Puntos de resultado de partido
            BigDecimal resultPoints = BigDecimal.valueOf(request.getMatchResult().getNormalPoints());
            delta = delta.add(resultPoints);
            logger.debug("Puntos de resultado agregados: {}, delta acumulado: {}", resultPoints, delta);
            
            // 2. Goles ponderados por rol
            BigDecimal weightedGoals = BigDecimal.valueOf(request.getGoalsScored())
                    .multiply(BigDecimal.valueOf(request.getRoleType().getGoalWeight()));
            delta = delta.add(weightedGoals);
            logger.debug("Goles ponderados agregados: {}, delta acumulado: {}", weightedGoals, delta);
            
            // 3. Asistencias ponderadas por rol
            BigDecimal weightedAssists = BigDecimal.valueOf(request.getAssistsMade())
                    .multiply(BigDecimal.valueOf(request.getRoleType().getAssistWeight()));
            delta = delta.add(weightedAssists);
            logger.debug("Asistencias ponderadas agregadas: {}, delta acumulado: {}", weightedAssists, delta);
            
            // 4. Bono defensivo (solo para DEFENSA y ARQUERO)
            BigDecimal defensiveBonus = calculateDefensiveBonus(request.getRoleType(), request.getGoalsConceded());
            delta = delta.add(defensiveBonus);
            logger.debug("Bono defensivo agregado: {}, delta acumulado: {}", defensiveBonus, delta);
            
            // 5. Bono MVP
            BigDecimal mvpBonus = BigDecimal.ZERO;
            if (Boolean.TRUE.equals(request.getWasMvp())) {
                mvpBonus = BigDecimal.valueOf(1.0);
                delta = delta.add(mvpBonus);
            }
            logger.debug("Bono MVP agregado: {}, delta final: {}", mvpBonus, delta);
            
            return delta;
            
        } catch (Exception e) {
            logger.error("Error calculando delta base: {}", e.getMessage(), e);
            throw new RatingCalculationException("Error calculando componentes del delta de calificación", e);
        }
    }

    /**
     * Calcula el bono defensivo basado en el rol y goles recibidos.
     * Solo aplica para roles DEFENSA y ARQUERO.
     */
    private BigDecimal calculateDefensiveBonus(RoleType role, Integer goalsConceded) {
        if (goalsConceded == null || goalsConceded < 0) {
            return BigDecimal.ZERO;
        }
        
        switch (role) {
            case DEFENSA:
                return calculateDefenseBonus(goalsConceded);
            case ARQUERO:
                return calculateGoalkeeperBonus(goalsConceded);
            default:
                return BigDecimal.ZERO;
        }
    }

    /**
     * Calcula el bono defensivo para jugadores con rol DEFENSA.
     */
    private BigDecimal calculateDefenseBonus(Integer goalsConceded) {
        switch (goalsConceded) {
            case 0:
                return BigDecimal.valueOf(2.0);
            case 1:
                return BigDecimal.valueOf(1.0);
            case 2:
                return BigDecimal.valueOf(0.5);
            default:
                return BigDecimal.ZERO; // 3 o más goles = 0 bono
        }
    }

    /**
     * Calcula el bono defensivo para jugadores con rol ARQUERO.
     */
    private BigDecimal calculateGoalkeeperBonus(Integer goalsConceded) {
        switch (goalsConceded) {
            case 0:
                return BigDecimal.valueOf(2.5);
            case 1:
                return BigDecimal.valueOf(1.2);
            case 2:
                return BigDecimal.valueOf(0.7);
            default:
                return BigDecimal.ZERO; // 3 o más goles = 0 bono
        }
    }

    /**
     * Aplica el multiplicador de prioridad al delta de calificación.
     */
    private BigDecimal applyPriorityMultiplier(BigDecimal delta, PriorityLevel priority) {
        return delta.multiply(BigDecimal.valueOf(priority.getMultiplier()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Aplica los límites mínimos de calificación basados en el nivel de prioridad.
     * Si la nueva calificación está por debajo del mínimo base, se establece al mínimo.
     */
    private BigDecimal enforceMinimumRating(BigDecimal newRating, PriorityLevel priority) {
        BigDecimal minimumRating = BigDecimal.valueOf(priority.getBaseRating());
        
        if (newRating.compareTo(minimumRating) < 0) {
            return minimumRating;
        }
        
        return newRating.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Valida que la solicitud de cálculo normal contenga todos los datos requeridos.
     * 
     * @throws InvalidPlayerDataException si los datos son inválidos o están incompletos
     */
    private void validateRequest(RatingCalculationRequest request) {
        if (request == null) {
            throw new InvalidPlayerDataException("La solicitud de cálculo no puede ser nula");
        }
        
        if (request.getCurrentRating() == null) {
            throw new InvalidPlayerDataException("La calificación actual es obligatoria", 
                                               null, "currentRating", null);
        }
        
        if (request.getCurrentRating().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPlayerDataException("La calificación actual no puede ser negativa", 
                                               null, "currentRating", request.getCurrentRating());
        }
        
        if (request.getRoleType() == null) {
            throw new InvalidPlayerDataException("El tipo de rol es obligatorio", 
                                               null, "roleType", null);
        }
        
        if (request.getPriorityLevel() == null) {
            throw new InvalidPlayerDataException("El nivel de prioridad es obligatorio", 
                                               null, "priorityLevel", null);
        }
        
        if (request.getMatchResult() == null) {
            throw new InvalidPlayerDataException("El resultado del partido es obligatorio", 
                                               null, "matchResult", null);
        }
        
        if (request.getGoalsScored() == null || request.getGoalsScored() < 0) {
            throw new InvalidPlayerDataException("Los goles anotados deben ser un número no negativo", 
                                               null, "goalsScored", request.getGoalsScored());
        }
        
        if (request.getAssistsMade() == null || request.getAssistsMade() < 0) {
            throw new InvalidPlayerDataException("Las asistencias realizadas deben ser un número no negativo", 
                                               null, "assistsMade", request.getAssistsMade());
        }
        
        if (request.getWasMvp() == null) {
            throw new InvalidPlayerDataException("El estatus MVP es obligatorio", 
                                               null, "wasMvp", null);
        }
        
        // Validar goles recibidos para roles defensivos
        if ((request.getRoleType() == RoleType.DEFENSA || request.getRoleType() == RoleType.ARQUERO) 
            && (request.getGoalsConceded() == null || request.getGoalsConceded() < 0)) {
            throw new InvalidPlayerDataException(
                "Los goles recibidos son obligatorios y no pueden ser negativos para roles defensivos", 
                null, "goalsConceded", request.getGoalsConceded());
        }
        
        logger.debug("Validación de solicitud completada exitosamente para rol {} con prioridad {}", 
                    request.getRoleType(), request.getPriorityLevel());
    }

    /**
     * Valida que la solicitud de arquero rotativo contenga todos los datos requeridos.
     * 
     * @throws InvalidPlayerDataException si los datos son inválidos o están incompletos
     */
    private void validateRotativeRequest(RotativeGoalkeeperRequest request) {
        if (request == null) {
            throw new InvalidPlayerDataException("La solicitud de arquero rotativo no puede ser nula");
        }
        
        if (request.getCurrentGoalkeeperRating() == null) {
            throw new InvalidPlayerDataException("La calificación actual de arquero es obligatoria", 
                                               null, "currentGoalkeeperRating", null);
        }
        
        if (request.getCurrentGoalkeeperRating().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPlayerDataException("La calificación actual de arquero no puede ser negativa", 
                                               null, "currentGoalkeeperRating", request.getCurrentGoalkeeperRating());
        }
        
        if (request.getGoalkeeperPriority() == null) {
            throw new InvalidPlayerDataException("El nivel de prioridad de arquero es obligatorio", 
                                               null, "goalkeeperPriority", null);
        }
        
        if (request.getMatchResult() == null) {
            throw new InvalidPlayerDataException("El resultado del partido es obligatorio", 
                                               null, "matchResult", null);
        }
        
        logger.debug("Validación de solicitud de arquero rotativo completada exitosamente con prioridad {}", 
                    request.getGoalkeeperPriority());
    }
}