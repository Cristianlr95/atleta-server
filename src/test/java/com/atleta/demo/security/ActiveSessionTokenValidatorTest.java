package com.atleta.demo.security;

import com.atleta.demo.repository.AthleteRepository;
import com.atleta.demo.repository.RefreshSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActiveSessionTokenValidatorTest {
    private final RefreshSessionRepository repository = mock(RefreshSessionRepository.class);
    private final AthleteRepository athletes = mock(AthleteRepository.class);
    private final ActiveSessionTokenValidator validator = new ActiveSessionTokenValidator(repository, athletes);

    @Test
    void acceptsActiveSessionAndRejectsRevokedSession() {
        UUID sessionId = UUID.randomUUID();
        UUID athleteUuid = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(athleteUuid.toString())
                .claim("sid", sessionId.toString())
                .build();
        when(repository.hasActiveSession(eq(sessionId), eq(athleteUuid), any())).thenReturn(true, false);

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void keepsLegacyAccessTokensValidOnlyWhileTheAccountIsActive() {
        UUID athleteUuid = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(athleteUuid.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        when(athletes.existsByAtletaUuidAndDeletedAtIsNull(athleteUuid)).thenReturn(true, false);

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }
}
