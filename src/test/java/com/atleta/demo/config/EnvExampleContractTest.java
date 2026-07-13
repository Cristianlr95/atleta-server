package com.atleta.demo.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class EnvExampleContractTest {

    private static final Path ENV_EXAMPLE = Path.of(".env.example");
    private static final Path DOCKER_COMPOSE = Path.of("docker-compose.yml");

    @Test
    void envExampleDocumentsRequiredRuntimeVariables() throws IOException {
        Map<String, String> env = readEnvExample();

        assertThat(env.keySet()).containsAll(Set.of(
                "SPRING_PROFILES_ACTIVE",
                "DB_HOST",
                "DB_PORT",
                "DB_NAME",
                "DB_USERNAME",
                "DB_PASSWORD",
                "JWT_SECRET",
                "JWT_ISSUER",
                "JWT_EXPIRATION",
                "SERVER_PORT",
                "CORS_ALLOWED_ORIGIN_PATTERNS",
                "CORS_ALLOWED_METHODS",
                "CORS_ALLOWED_HEADERS",
                "CORS_EXPOSED_HEADERS",
                "CORS_ALLOW_CREDENTIALS",
                "CORS_MAX_AGE"
        ));
    }

    @Test
    void envExampleDoesNotAdvertiseTrivialSecrets() throws IOException {
        Map<String, String> env = readEnvExample();

        assertThat(env.get("DB_PASSWORD"))
                .isNotBlank()
                .doesNotContainIgnoringCase("password123")
                .doesNotContainIgnoringCase("admin")
                .doesNotContainIgnoringCase("test");

        assertThat(env.get("JWT_SECRET"))
                .isNotBlank()
                .hasSizeGreaterThanOrEqualTo(32)
                .doesNotContainIgnoringCase("secret123")
                .doesNotContainIgnoringCase("test");
    }

    @Test
    void dockerComposeFailsFastWhenDatabaseVariablesAreMissing() throws IOException {
        String compose = Files.readString(DOCKER_COMPOSE);

        assertThat(compose).contains(
                "${DB_NAME:?DB_NAME is required}",
                "${DB_USERNAME:?DB_USERNAME is required}",
                "${DB_PASSWORD:?DB_PASSWORD is required}"
        );
    }

    private Map<String, String> readEnvExample() throws IOException {
        return Files.readAllLines(ENV_EXAMPLE)
                .stream()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> !line.startsWith("#"))
                .filter(line -> line.contains("="))
                .map(line -> line.split("=", 2))
                .collect(Collectors.toMap(parts -> parts[0], parts -> parts.length > 1 ? parts[1] : ""));
    }
}
