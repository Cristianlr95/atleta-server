package com.atleta.demo.validation;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

@Component
public class DatabaseConfigurationValidator {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfigurationValidator.class);

    @Autowired
    private Environment environment;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    /**
     * Required variables per environment.
     * DEV: none (uses application.yml)
     * PROD: strict
     */
    private static final Map<String, List<String>> REQUIRED_ENV_VARS = Map.of(
        "dev", List.of(), // 👈 NO exigir en dev
        "test", List.of(),
        "staging", List.of("DB_HOST", "DB_NAME", "DB_USERNAME", "DB_PASSWORD"),
        "prod", List.of("DB_HOST", "DB_PORT", "DB_NAME", "DB_USERNAME", "DB_PASSWORD", "OAUTH2_ISSUER_URI")
    );

    private static final Map<String, FlywayValidationRules> FLYWAY_RULES = Map.of(
        "dev", new FlywayValidationRules(false, true, true),
        "test", new FlywayValidationRules(false, true, false),
        "staging", new FlywayValidationRules(true, false, true),
        "prod", new FlywayValidationRules(true, false, true)
    );

    @EventListener(ApplicationReadyEvent.class)
    public void validateDatabaseConfiguration() {
        String[] activeProfiles = environment.getActiveProfiles();
        String currentProfile = activeProfiles.length > 0 ? activeProfiles[0] : "dev";

        logger.info("Starting database configuration validation for profile: {}", currentProfile);

        if ("test".equals(currentProfile)) {
            logger.info("Skipping database configuration validation for test profile");
            return;
        }

        try {
            validateEnvironmentVariables(currentProfile);
            validateDatabaseConnectivity();
            validateFlywayConfiguration(currentProfile);
            logger.info("Database configuration validation completed successfully");
        } catch (DatabaseConfigurationException e) {
            logger.error("Database configuration validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during database configuration validation", e);
            throw new DatabaseConfigurationException("Unexpected validation error: " + e.getMessage(), e);
        }
    }

    private void validateEnvironmentVariables(String currentProfile) {
        logger.debug("Validating required configuration for profile: {}", currentProfile);

        List<String> requiredVars = REQUIRED_ENV_VARS.getOrDefault(currentProfile, List.of());

        for (String varName : requiredVars) {
            String envValue = System.getenv(varName);
            String springValue = mapEnvToSpringProperty(varName);

            boolean missingEnv = envValue == null || envValue.trim().isEmpty();
            boolean missingSpring = springValue == null || springValue.trim().isEmpty();

            if (missingEnv && missingSpring) {
                throw new DatabaseConfigurationException(
                    String.format("Required configuration '%s' is missing for profile '%s'", varName, currentProfile)
                );
            }

            logger.debug("Configuration '{}' is present", varName);
        }

        if ("prod".equals(currentProfile)) {
            validateProductionSslConfiguration();
        }

        logger.debug("Configuration validation completed");
    }

    private String mapEnvToSpringProperty(String envVar) {
        return switch (envVar) {
            case "DB_USERNAME" -> environment.getProperty("spring.datasource.username");
            case "DB_PASSWORD" -> environment.getProperty("spring.datasource.password");
            case "DB_HOST" -> environment.getProperty("spring.datasource.url");
            case "OAUTH2_ISSUER_URI" -> environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri");
            default -> null;
        };
    }

    private void validateProductionSslConfiguration() {
        String datasourceUrl = environment.getProperty("spring.datasource.url", "");

        if (!datasourceUrl.contains("ssl=true") || !datasourceUrl.contains("sslmode=require")) {
            throw new DatabaseConfigurationException(
                "Production environment must use SSL connections. " +
                "Database URL must contain 'ssl=true&sslmode=require'"
            );
        }

        logger.debug("Production SSL configuration validated");
    }

    private void validateDatabaseConnectivity() {
        logger.debug("Validating database connectivity");

        try (Connection connection = dataSource.getConnection()) {
            if (connection == null || !connection.isValid(5)) {
                throw new DatabaseConfigurationException("Database connection is not valid");
            }

            try (var statement = connection.createStatement();
                 var resultSet = statement.executeQuery("SELECT 1")) {

                if (!resultSet.next() || resultSet.getInt(1) != 1) {
                    throw new DatabaseConfigurationException("Database connectivity test query failed");
                }
            }

            logger.debug("Database connectivity validated successfully");
        } catch (SQLException e) {
            throw new DatabaseConfigurationException("Failed to establish database connection: " + e.getMessage(), e);
        }
    }

    private void validateFlywayConfiguration(String currentProfile) {
        logger.debug("Validating Flyway configuration for profile: {}", currentProfile);

        FlywayValidationRules rules = FLYWAY_RULES.get(currentProfile);
        if (rules == null) {
            logger.warn("No Flyway validation rules defined for profile: {}", currentProfile);
            return;
        }

        boolean cleanDisabled = environment.getProperty("spring.flyway.clean-disabled", Boolean.class, false);
        if (rules.cleanMustBeDisabled && !cleanDisabled) {
            throw new DatabaseConfigurationException(
                String.format("Flyway clean must be disabled for profile '%s'", currentProfile)
            );
        }

        boolean baselineOnMigrate = environment.getProperty("spring.flyway.baseline-on-migrate", Boolean.class, false);
        if (!rules.baselineAllowed && baselineOnMigrate) {
            throw new DatabaseConfigurationException(
                String.format("Flyway baseline-on-migrate must be disabled for profile '%s'", currentProfile)
            );
        }

        boolean validateOnMigrate = environment.getProperty("spring.flyway.validate-on-migrate", Boolean.class, true);
        if (rules.validateRequired && !validateOnMigrate) {
            throw new DatabaseConfigurationException(
                String.format("Flyway validate-on-migrate must be enabled for profile '%s'", currentProfile)
            );
        }

        validateMigrationLocations();
        validateFlywaySchemaHistory();

        logger.debug("Flyway configuration validation completed");
    }

    private void validateMigrationLocations() {
        String[] locations = environment.getProperty(
            "spring.flyway.locations", String[].class, new String[]{"classpath:db/migration"}
        );

        if (locations.length == 0) {
            throw new DatabaseConfigurationException("Flyway migration locations cannot be empty");
        }

        boolean hasPrimaryLocation = Arrays.stream(locations)
            .anyMatch(location -> location.contains("db/migration"));

        if (!hasPrimaryLocation) {
            throw new DatabaseConfigurationException("Flyway must include 'db/migration' as a migration location");
        }

        logger.debug("Migration locations validated: {}", Arrays.toString(locations));
    }

    private void validateFlywaySchemaHistory() {
        try {
            var info = flyway.info();
            var all = info.all();

            long failedCount = Arrays.stream(all)
                .filter(migration -> migration.getState().isFailed())
                .count();

            if (failedCount > 0) {
                throw new DatabaseConfigurationException(
                    String.format("Flyway has %d failed migrations. Database state is inconsistent.", failedCount)
                );
            }

            logger.debug("Flyway schema history validated successfully");
        } catch (Exception e) {
            throw new DatabaseConfigurationException("Failed to validate Flyway schema history: " + e.getMessage(), e);
        }
    }

    private static class FlywayValidationRules {
        final boolean cleanMustBeDisabled;
        final boolean baselineAllowed;
        final boolean validateRequired;

        FlywayValidationRules(boolean cleanMustBeDisabled, boolean baselineAllowed, boolean validateRequired) {
            this.cleanMustBeDisabled = cleanMustBeDisabled;
            this.baselineAllowed = baselineAllowed;
            this.validateRequired = validateRequired;
        }
    }

    public static class DatabaseConfigurationException extends RuntimeException {
        public DatabaseConfigurationException(String message) {
            super(message);
        }

        public DatabaseConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
