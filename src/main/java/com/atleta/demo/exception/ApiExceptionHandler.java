package com.atleta.demo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final Environment environment;

    public ApiExceptionHandler(Environment environment) {
        this.environment = environment;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException ex,
        WebRequest request
    ) {
        Map<String, Object> errorResponse = createErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed.",
            HttpStatus.BAD_REQUEST,
            request
        );

        Map<String, String> validationErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        errorResponse.put("validationErrors", validationErrors);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequestExceptions(Exception ex, WebRequest request) {
        logger.warn("Bad request: {}", ex.getMessage());

        Map<String, Object> errorResponse = createErrorResponse(
            "BAD_REQUEST",
            "Request is malformed or contains invalid parameters.",
            HttpStatus.BAD_REQUEST,
            request
        );

        if (isDevelopmentEnvironment()) {
            errorResponse.put("details", ex.getMessage());
        }

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
        AccessDeniedException ex,
        WebRequest request
    ) {
        logger.warn("Access denied: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            createErrorResponse(
                "ACCESS_DENIED",
                "You do not have permission to perform this action.",
                HttpStatus.FORBIDDEN,
                request
            )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(Exception ex, WebRequest request) {
        logger.error("Unhandled exception", ex);

        Map<String, Object> errorResponse = createErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An internal error occurred. Please try again later.",
            HttpStatus.INTERNAL_SERVER_ERROR,
            request
        );

        if (isDevelopmentEnvironment()) {
            errorResponse.put("details", ex.getMessage());
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
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
        return errorResponse;
    }

    private boolean isDevelopmentEnvironment() {
        return environment.acceptsProfiles(Profiles.of("dev", "test"));
    }
}
