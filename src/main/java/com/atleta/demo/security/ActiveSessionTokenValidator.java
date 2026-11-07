package com.atleta.demo.security;

import com.atleta.demo.repository.RefreshSessionRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class ActiveSessionTokenValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error REVOKED = new OAuth2Error(
            OAuth2ErrorCodes.INVALID_TOKEN, "La sesion fue revocada", null);
    private final RefreshSessionRepository refreshSessionRepository;

    public ActiveSessionTokenValidator(RefreshSessionRepository refreshSessionRepository) {
        this.refreshSessionRepository = refreshSessionRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String sessionId = token.getClaimAsString("sid");
        if (sessionId == null || sessionId.isBlank()) {
            return OAuth2TokenValidatorResult.success();
        }
        try {
            boolean active = refreshSessionRepository.existsByIdAndRevokedAtIsNullAndExpiresAtAfter(
                    UUID.fromString(sessionId), Instant.now());
            return active ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(REVOKED);
        } catch (RuntimeException exception) {
            return OAuth2TokenValidatorResult.failure(REVOKED);
        }
    }
}
