package com.atleta.demo.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public final class AuthenticatedUserUtils {

    private AuthenticatedUserUtils() {
    }

    public static UUID currentUserUuid(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new IllegalArgumentException("No se pudo identificar el usuario autenticado");
        }

        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El usuario autenticado no tiene un UUID valido");
        }
    }

    public static UUID requireSameUser(Jwt jwt, UUID expectedUserUuid) {
        UUID authenticatedUserUuid = currentUserUuid(jwt);
        if (!authenticatedUserUuid.equals(expectedUserUuid)) {
            throw new AccessDeniedException("El usuario autenticado no puede operar sobre otro usuario");
        }

        return authenticatedUserUuid;
    }
}
