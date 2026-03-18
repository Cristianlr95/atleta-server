package com.atleta.demo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones específico para el sistema de calificación de jugadores.
 * Proporciona manejo centralizado y consistente de errores relacionados con el cálculo
 * y actualización de calificaciones, complementando el DatabaseExceptionHandler existente.
 * 
 * Implementa el requerimiento 9.4: Manejo robusto de errores y logging apropiado.
 */
@ControllerAdvice
public class RatingExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RatingExceptionHandler.class);

    /**
     * Maneja errores generales de cálculo de calificación.
     * Estos son errores de lógica de negocio o problemas en el algoritmo de cálculo.
     */
    @ExceptionHandler(RatingCalculationException.class)
    public ResponseEntity<Map<String, Object>> handleRatingCalculationException(
            RatingCalculationException ex, WebRequest request) {
        
        logger.error("Error en cálculo de calificación: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = createErrorResponse(
            "RATING_CALCULATION_ERROR",
            "Error al calcular la calificación del jugador. " + ex.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR,
            request
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Maneja errores de validación de datos de jugadores.
     * Estos son errores de entrada donde los datos proporcionados son inválidos.
     */
    @ExceptionHandler(InvalidPlayerDataException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPlayerDataException(
            InvalidPlayerDataException ex, WebRequest request) {
        
        logger.warn("Datos de jugador inválidos: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            "INVALID_PLAYER_DATA",
            ex.getMessage(),
            HttpStatus.BAD_REQUEST,
            request
        );
        
        // Agregar información adicional si está disponible
        if (ex.getPlayerProfileId() != null) {
            errorResponse.put("playerProfileId", ex.getPlayerProfileId());
        }
        if (ex.getFieldName() != null) {
            errorResponse.put("invalidField", ex.getFieldName());
        }
        if (ex.getInvalidValue() != null) {
            errorResponse.put("invalidValue", ex.getInvalidValue());
        }
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja errores de concurrencia en actualizaciones de calificación.
     * Estos errores indican que la operación debe reintentarse.
     */
    @ExceptionHandler(ConcurrentRatingUpdateException.class)
    public ResponseEntity<Map<String, Object>> handleConcurrentRatingUpdateException(
            ConcurrentRatingUpdateException ex, WebRequest request) {
        
        logger.warn("Conflicto de concurrencia en actualización de calificación: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            "CONCURRENT_UPDATE_CONFLICT",
            "La calificación fue modificada por otro proceso. Por favor, reintente la operación.",
            HttpStatus.CONFLICT,
            request
        );
        
        // Agregar información de contexto
        if (ex.getPlayerProfileId() != null) {
            errorResponse.put("playerProfileId", ex.getPlayerProfileId());
        }
        if (ex.getRoleType() != null) {
            errorResponse.put("roleType", ex.getRoleType());
        }
        if (ex.getPriorityLevel() != null) {
            errorResponse.put("priorityLevel", ex.getPriorityLevel());
        }
        if (ex.hasVersioningInfo()) {
            errorResponse.put("versionConflict", true);
            errorResponse.put("expectedVersion", ex.getExpectedVersion());
            errorResponse.put("actualVersion", ex.getActualVersion());
        }
        
        // Sugerir reintento
        errorResponse.put("retryable", true);
        errorResponse.put("suggestion", "Reintente la operación con los datos más recientes");
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Maneja errores cuando un jugador no se encuentra.
     * Estos son errores de recursos no encontrados.
     */
    @ExceptionHandler(PlayerNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePlayerNotFoundException(
            PlayerNotFoundException ex, WebRequest request) {
        
        logger.warn("Jugador no encontrado: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            "PLAYER_NOT_FOUND",
            ex.getMessage(),
            HttpStatus.NOT_FOUND,
            request
        );
        
        // Agregar información adicional si está disponible
        if (ex.getPlayerProfileId() != null) {
            errorResponse.put("playerProfileId", ex.getPlayerProfileId());
        }
        if (ex.getSearchCriteria() != null) {
            errorResponse.put("searchCriteria", ex.getSearchCriteria());
        }
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Maneja errores cuando un partido no se encuentra.
     * Estos son errores de recursos no encontrados.
     */
    @ExceptionHandler(MatchNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMatchNotFoundException(
            MatchNotFoundException ex, WebRequest request) {
        
        logger.warn("Partido no encontrado: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            "MATCH_NOT_FOUND",
            ex.getMessage(),
            HttpStatus.NOT_FOUND,
            request
        );
        
        // Agregar información adicional si está disponible
        if (ex.getMatchId() != null) {
            errorResponse.put("matchId", ex.getMatchId());
        }
        if (ex.getSearchCriteria() != null) {
            errorResponse.put("searchCriteria", ex.getSearchCriteria());
        }
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Maneja errores de optimistic locking de Spring Data JPA.
     * Convierte estos errores técnicos en errores de negocio más comprensibles.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLockingFailureException(
            OptimisticLockingFailureException ex, WebRequest request) {
        
        logger.warn("Fallo de optimistic locking: {}", ex.getMessage());
        
        // Convertir a excepción de negocio más específica
        ConcurrentRatingUpdateException ratingException = new ConcurrentRatingUpdateException(
            "La calificación fue modificada por otro proceso durante la actualización", ex);
        
        return handleConcurrentRatingUpdateException(ratingException, request);
    }

    /**
     * Maneja errores de argumentos ilegales relacionados con el sistema de calificación.
     * Estos son típicamente errores de validación de entrada.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        // Solo manejar si el error está relacionado con el sistema de calificación
        String message = ex.getMessage();
        if (message != null && isRatingRelatedError(message)) {
            logger.warn("Argumento inválido en sistema de calificación: {}", message);
            
            Map<String, Object> errorResponse = createErrorResponse(
                "INVALID_ARGUMENT",
                message,
                HttpStatus.BAD_REQUEST,
                request
            );
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
        
        // Si no es relacionado con calificación, dejar que otros manejadores lo procesen
        throw ex;
    }

    /**
     * Determina si un error está relacionado con el sistema de calificación.
     * Utiliza palabras clave para identificar errores de calificación.
     */
    private boolean isRatingRelatedError(String message) {
        if (message == null) {
            return false;
        }
        
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("calificación") ||
               lowerMessage.contains("rating") ||
               lowerMessage.contains("jugador") ||
               lowerMessage.contains("player") ||
               lowerMessage.contains("partido") ||
               lowerMessage.contains("match") ||
               lowerMessage.contains("rol") ||
               lowerMessage.contains("role") ||
               lowerMessage.contains("prioridad") ||
               lowerMessage.contains("priority") ||
               lowerMessage.contains("mvp") ||
               lowerMessage.contains("goles") ||
               lowerMessage.contains("goals") ||
               lowerMessage.contains("asistencias") ||
               lowerMessage.contains("assists");
    }

    /**
     * Crea una respuesta de error estandarizada.
     */
    private Map<String, Object> createErrorResponse(String errorCode, String message, 
                                                   HttpStatus status, WebRequest request) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("message", message);
        errorResponse.put("path", request.getDescription(false).replace("uri=", ""));
        errorResponse.put("component", "RatingSystem");
        
        return errorResponse;
    }
}