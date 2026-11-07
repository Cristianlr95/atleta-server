package com.atleta.demo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class RatingExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RatingExceptionHandler.class);

    private final Environment environment;

    public RatingExceptionHandler(Environment environment) {
        this.environment = environment;
    }

    @ExceptionHandler(RatingCalculationException.class)
    public ResponseEntity<Map<String, Object>> handleRatingCalculationException(
        RatingCalculationException ex,
        WebRequest request
    ) {
        logger.error("Error en calculo de calificacion: {}", ex.getMessage(), ex);

        Map<String, Object> errorResponse = createErrorResponse(
            "RATING_CALCULATION_ERROR",
            "Error al calcular la calificacion del jugador.",
            HttpStatus.INTERNAL_SERVER_ERROR,
            request
        );

        if (isDevelopmentEnvironment()) {
            errorResponse.put("details", ex.getMessage());
        }

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InvalidPlayerDataException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPlayerDataException(
        InvalidPlayerDataException ex,
        WebRequest request
    ) {
        logger.warn("Datos de jugador invalidos: {}", ex.getMessage());

        Map<String, Object> errorResponse = createErrorResponse(
            "INVALID_PLAYER_DATA",
            ex.getMessage(),
            HttpStatus.BAD_REQUEST,
            request
        );

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

    @ExceptionHandler(ConcurrentRatingUpdateException.class)
    public ResponseEntity<Map<String, Object>> handleConcurrentRatingUpdateException(
        ConcurrentRatingUpdateException ex,
        WebRequest request
    ) {
        logger.warn("Conflicto de concurrencia en actualizacion de calificacion: {}", ex.getMessage());

        Map<String, Object> errorResponse = createErrorResponse(
            "CONCURRENT_UPDATE_CONFLICT",
            "La calificacion fue modificada por otro proceso. Por favor, reintente la operacion.",
            HttpStatus.CONFLICT,
            request
        );

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

        errorResponse.put("retryable", true);
        errorResponse.put("suggestion", "Reintente la operacion con los datos mas recientes");

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(PlayerNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePlayerNotFoundException(
        PlayerNotFoundException ex,
        WebRequest request
    ) {
        logger.warn("Jugador no encontrado: {}", ex.getMessage());

        Map<String, Object> errorResponse = createErrorResponse(
            "PLAYER_NOT_FOUND",
            ex.getMessage(),
            HttpStatus.NOT_FOUND,
            request
        );

        if (ex.getPlayerProfileId() != null) {
            errorResponse.put("playerProfileId", ex.getPlayerProfileId());
        }
        if (ex.getSearchCriteria() != null) {
            errorResponse.put("searchCriteria", ex.getSearchCriteria());
        }

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MatchNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMatchNotFoundException(
        MatchNotFoundException ex,
        WebRequest request
    ) {
        logger.warn("Partido no encontrado: {}", ex.getMessage());

        Map<String, Object> errorResponse = createErrorResponse(
            "MATCH_NOT_FOUND",
            ex.getMessage(),
            HttpStatus.NOT_FOUND,
            request
        );

        if (ex.getMatchId() != null) {
            errorResponse.put("matchId", ex.getMatchId());
        }
        if (ex.getSearchCriteria() != null) {
            errorResponse.put("searchCriteria", ex.getSearchCriteria());
        }

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLockingFailureException(
        OptimisticLockingFailureException ex,
        WebRequest request
    ) {
        logger.warn("Fallo de optimistic locking: {}", ex.getMessage());

        ConcurrentRatingUpdateException ratingException = new ConcurrentRatingUpdateException(
            "La calificacion fue modificada por otro proceso durante la actualizacion",
            ex
        );

        return handleConcurrentRatingUpdateException(ratingException, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
        IllegalArgumentException ex,
        WebRequest request
    ) {
        String message = ex.getMessage();
        if (message != null && isRatingRelatedError(message)) {
            logger.warn("Argumento invalido en sistema de calificacion: {}", message);

            Map<String, Object> errorResponse = createErrorResponse(
                "INVALID_ARGUMENT",
                message,
                HttpStatus.BAD_REQUEST,
                request
            );

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        throw ex;
    }

    private boolean isRatingRelatedError(String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("calificacion")
            || lowerMessage.contains("rating")
            || lowerMessage.contains("jugador")
            || lowerMessage.contains("player")
            || lowerMessage.contains("partido")
            || lowerMessage.contains("match")
            || lowerMessage.contains("rol")
            || lowerMessage.contains("role")
            || lowerMessage.contains("prioridad")
            || lowerMessage.contains("priority")
            || lowerMessage.contains("mvp")
            || lowerMessage.contains("goles")
            || lowerMessage.contains("goals")
            || lowerMessage.contains("asistencias")
            || lowerMessage.contains("assists");
    }

    private Map<String, Object> createErrorResponse(
        String errorCode,
        String message,
        HttpStatus status,
        WebRequest request
    ) {
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

    private boolean isDevelopmentEnvironment() {
        return environment.acceptsProfiles(Profiles.of("dev", "test"));
    }
}
