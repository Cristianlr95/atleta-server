package com.atleta.demo.service;

import com.atleta.demo.config.JwtProperties;
import com.atleta.demo.dto.response.AuthResponse;
import com.atleta.demo.entity.Athlete;
import com.atleta.demo.entity.PasswordResetToken;
import com.atleta.demo.entity.RefreshSession;
import com.atleta.demo.repository.AthleteRepository;
import com.atleta.demo.repository.PasswordResetTokenRepository;
import com.atleta.demo.repository.RefreshSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionLifecycleServiceTest {
    @Mock RefreshSessionRepository refreshSessions;
    @Mock PasswordResetTokenRepository resetTokens;
    @Mock AthleteRepository athletes;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock PasswordResetDeliveryService delivery;

    private SessionLifecycleService service;
    private Athlete athlete;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setRefreshExpiration(Duration.ofDays(30));
        properties.setPasswordResetExpiration(Duration.ofMinutes(30));
        service = new SessionLifecycleService(
                refreshSessions, resetTokens, athletes, passwordEncoder, jwtService, properties, delivery);
        athlete = new Athlete("player@atleta.test", "hash", "Player");
        athlete.setAtletaUuid(UUID.randomUUID());
    }

    @Test
    void createSessionStoresOnlyHashAndReturnsOpaqueRefreshToken() {
        UUID sessionId = UUID.randomUUID();
        when(refreshSessions.save(any())).thenAnswer(invocation -> {
            RefreshSession session = invocation.getArgument(0);
            ReflectionTestUtils.setField(session, "id", sessionId);
            return session;
        });
        when(jwtService.generateToken(athlete, sessionId)).thenReturn("access-token");

        AuthResponse response = service.createSession(athlete);

        ArgumentCaptor<RefreshSession> captor = ArgumentCaptor.forClass(RefreshSession.class);
        verify(refreshSessions).save(captor.capture());
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(captor.getValue().getTokenHash()).hasSize(64).doesNotContain(response.getRefreshToken());
        assertThat(response.getAccessToken()).isEqualTo("access-token");
    }

    @Test
    void rotateRefreshTokenRevokesTheUsedToken() {
        RefreshSession current = new RefreshSession(athlete, "hash", Instant.now().plusSeconds(60), Instant.now());
        when(refreshSessions.findByTokenHash(anyString())).thenReturn(Optional.of(current));
        when(refreshSessions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(), any())).thenReturn("new-access");

        AuthResponse response = service.rotateRefreshToken("old-refresh");

        assertThat(current.getRevokedAt()).isNotNull();
        assertThat(response.getRefreshToken()).isNotBlank();
    }

    @Test
    void rejectsReuseOfRevokedRefreshToken() {
        RefreshSession current = new RefreshSession(athlete, "hash", Instant.now().plusSeconds(60), Instant.now());
        current.revoke(Instant.now());
        when(refreshSessions.findByTokenHash(anyString())).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service.rotateRefreshToken("reused"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void passwordResetDoesNotRevealUnknownEmailToDeliveryLayer() {
        when(athletes.findByEmailIgnoreCase("missing@atleta.test")).thenReturn(Optional.empty());

        service.requestPasswordReset("MISSING@atleta.test");

        verify(delivery, never()).sendResetLink(anyString(), anyString(), any());
    }

    @Test
    void passwordResetInvalidatesPreviousTokenAndDeliversNewOpaqueToken() {
        when(athletes.findByEmailIgnoreCase("player@atleta.test")).thenReturn(Optional.of(athlete));

        service.requestPasswordReset("PLAYER@atleta.test");

        verify(resetTokens).invalidateAllByAthleteUuid(any(), any());
        verify(resetTokens).save(any(PasswordResetToken.class));
        verify(delivery).sendResetLink(anyString(), anyString(), any());
    }

    @Test
    void confirmingPasswordResetConsumesTokenAndRevokesAllSessions() {
        PasswordResetToken token = new PasswordResetToken(
                athlete, "hash", Instant.now().plusSeconds(60), Instant.now());
        when(resetTokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        service.confirmPasswordReset("raw-token", "new-password");

        assertThat(token.getUsedAt()).isNotNull();
        assertThat(athlete.getPasswordHash()).isEqualTo("new-hash");
        verify(refreshSessions).revokeAllByAthleteUuid(any(), any());
        verify(athletes).save(athlete);
    }
}
