package com.atleta.demo.exception;

import com.atleta.demo.validation.DatabaseConfigurationValidator.DatabaseConfigurationException;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for database-related exceptions.
 * Provides clear, user-friendly error messages while maintaining security.
 * Handles Flyway exceptions, data access exceptions, and configuration errors.
 * 
 * Validates requirement: 10.3
 */
@ControllerAdvice
public class DatabaseExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseExceptionHandler.class);
    private final Environment environment;

    public DatabaseExceptionHandler(Environment environment) {
        this.environment = environment;
    }

    /**
     * Handles database configuration validation failures.
     * These are critical startup errors that prevent the application from functioning.
     */
    @ExceptionHandler(DatabaseConfigurationException.class)
    public ResponseEntity<Map<String, Object>> handleDatabaseConfigurationException(
            DatabaseConfigurationException ex, WebRequest request) {
        
        logger.error("Database configuration error: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = createErrorResponse(
            "DATABASE_CONFIGURATION_ERROR",
            "Database configuration is invalid. Please check your environment settings.",
            HttpStatus.INTERNAL_SERVER_ERROR,
            request
        );
        
        // In development, include more details
        if (isDevelopmentEnvironment()) {
            errorResponse.put("details", ex.getMessage());
        }
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles Flyway migration exceptions.
     * These occur during database schema migrations and are critical for application startup.
     */
    @ExceptionHandler(FlywayException.class)
    public ResponseEntity<Map<String, Object>> handleFlywayException(
            FlywayException ex, WebRequest request) {
        
        logger.error("Flyway migration error: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = createErrorResponse(
            "DATABASE_MIGRATION_ERROR",
            "Database migration failed. Please check your migration scripts and database state.",
            HttpStatus.INTERNAL_SERVER_ERROR,
            request
        );
        
        // In development, include migration details
        if (isDevelopmentEnvironment()) {
            errorResponse.put("details", ex.getMessage());
            errorResponse.put("suggestion", "Check Flyway migration logs and ensure database is accessible");
        }
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles data integrity violations (foreign key, unique constraints, etc.).
     * These are business logic violations that should be handled gracefully.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        
        logger.warn("Data integrity violation: {}", ex.getMessage());
        
        String userMessage = parseDataIntegrityMessage(ex);
        
        Map<String, Object> errorResponse = createErrorResponse(
            "DATA_INTEGRITY_VIOLATION",
            userMessage,
            HttpStatus.CONFLICT,
            request
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handles cases where expected data is not found.
     */
    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleEmptyResultDataAccessException(
            EmptyResultDataAccessException ex, WebRequest request) {
        
        logger.debug("Empty result data access: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            "RESOURCE_NOT_FOUND",
            "The requested resource was not found.",
            HttpStatus.NOT_FOUND,
            request
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles general data access exceptions.
     * These are typically database connectivity or query execution issues.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(
            DataAccessException ex, WebRequest request) {
        
        logger.error("Data access error: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = createErrorResponse(
            "DATABASE_ACCESS_ERROR",
            "A database error occurred. Please try again later.",
            HttpStatus.INTERNAL_SERVER_ERROR,
            request
        );
        
        // In development, include more technical details
        if (isDevelopmentEnvironment()) {
            errorResponse.put("details", ex.getMostSpecificCause().getMessage());
        }
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles low-level SQL exceptions.
     * These are typically connection issues or SQL syntax errors.
     */
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<Map<String, Object>> handleSQLException(
            SQLException ex, WebRequest request) {
        
        logger.error("SQL error - Code: {}, State: {}, Message: {}", 
                    ex.getErrorCode(), ex.getSQLState(), ex.getMessage(), ex);
        
        String userMessage = parseSQLExceptionMessage(ex);
        
        Map<String, Object> errorResponse = createErrorResponse(
            "DATABASE_SQL_ERROR",
            userMessage,
            HttpStatus.INTERNAL_SERVER_ERROR,
            request
        );
        
        // In development, include SQL error details
        if (isDevelopmentEnvironment()) {
            errorResponse.put("sqlErrorCode", ex.getErrorCode());
            errorResponse.put("sqlState", ex.getSQLState());
            errorResponse.put("details", ex.getMessage());
        }
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Creates a standardized error response structure.
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
        
        return errorResponse;
    }

    /**
     * Parses data integrity violation messages to provide user-friendly feedback.
     */
    private String parseDataIntegrityMessage(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage().toLowerCase();
        
        if (message.contains("unique") || message.contains("duplicate")) {
            if (message.contains("email")) {
                return "An athlete with this email already exists.";
            } else if (message.contains("nombre") && message.contains("teams")) {
                return "A team with this name already exists.";
            } else if (message.contains("user_id") && message.contains("position_id")) {
                return "This player already has this position assigned.";
            } else if (message.contains("user_id") && message.contains("prioridad")) {
                return "This player already has a position with this priority level.";
            }
            return "This record already exists and cannot be duplicated.";
        }
        
        if (message.contains("foreign key") || message.contains("violates")) {
            if (message.contains("atleta_uuid")) {
                return "The specified athlete does not exist.";
            } else if (message.contains("team_id")) {
                return "The specified team does not exist.";
            } else if (message.contains("position_id")) {
                return "The specified position does not exist.";
            } else if (message.contains("match_id")) {
                return "The specified match does not exist.";
            }
            return "Referenced record does not exist.";
        }
        
        if (message.contains("check constraint")) {
            if (message.contains("trust_score")) {
                return "Trust score must be between 0 and 100.";
            } else if (message.contains("prioridad")) {
                return "Position priority must be between 1 and 3.";
            } else if (message.contains("xp")) {
                return "Experience points cannot be negative.";
            } else if (message.contains("goles")) {
                return "Goals cannot be negative.";
            } else if (message.contains("cuota")) {
                return "Match fee cannot be negative.";
            }
            return "Data validation constraint violated.";
        }
        
        return "Data integrity constraint violated. Please check your input.";
    }

    /**
     * Parses SQL exception messages to provide user-friendly feedback.
     */
    private String parseSQLExceptionMessage(SQLException ex) {
        String sqlState = ex.getSQLState();
        
        // PostgreSQL error codes
        switch (sqlState) {
            case "08001": // Connection unable to connect
            case "08003": // Connection does not exist
            case "08006": // Connection failure
                return "Database connection failed. Please try again later.";
            
            case "28000": // Invalid authorization
            case "28P01": // Invalid password
                return "Database authentication failed.";
            
            case "3D000": // Invalid catalog name
                return "Database does not exist.";
            
            case "42P01": // Undefined table
                return "Required database table does not exist. Database may not be properly initialized.";
            
            case "42703": // Undefined column
                return "Database schema is outdated. Please run database migrations.";
            
            case "53300": // Too many connections
                return "Database is currently overloaded. Please try again later.";
            
            default:
                return "A database error occurred. Please try again later.";
        }
    }

    /**
     * Determines if the application is running in development environment.
     */
    private boolean isDevelopmentEnvironment() {
        return environment.acceptsProfiles(Profiles.of("dev", "test"));
    }
}
