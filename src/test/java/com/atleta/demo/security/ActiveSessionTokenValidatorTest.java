package com.atleta.demo.security;

import com.atleta.demo.repository.RefreshSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActiveSessionTokenValidatorTest {
    private final RefreshSessionRepository repository = mock(RefreshSessionRepository.class);
    private final ActiveSessionTokenValidator validator = new ActiveSessionTokenValidator(repository);

    @Test
    void acceptsActiveSessionAndRejectsRevokedSession() {
        UUID sessionId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(UUID.randomUUID().toString())
                .claim("sid", sessionId.toString())
                .build();
        when(repository.existsByIdAndRevokedAtIsNullAndExpiresAtAfter(any(), any())).thenReturn(true, false);

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void keepsLegacyAccessTokensValidUntilTheirOwnExpiration() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }
}
