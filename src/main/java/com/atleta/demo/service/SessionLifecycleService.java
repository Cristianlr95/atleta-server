package com.atleta.demo.service;

import com.atleta.demo.config.JwtProperties;
import com.atleta.demo.dto.response.AuthResponse;
import com.atleta.demo.entity.Athlete;
import com.atleta.demo.entity.PasswordResetToken;
import com.atleta.demo.entity.RefreshSession;
import com.atleta.demo.repository.AthleteRepository;
import com.atleta.demo.repository.PasswordResetTokenRepository;
import com.atleta.demo.repository.RefreshSessionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
public class SessionLifecycleService {
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AthleteRepository athleteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final PasswordResetDeliveryService passwordResetDeliveryService;
    private final SecureRandom secureRandom = new SecureRandom();

    public SessionLifecycleService(
            RefreshSessionRepository refreshSessionRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            AthleteRepository athleteRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            PasswordResetDeliveryService passwordResetDeliveryService
    ) {
        this.refreshSessionRepository = refreshSessionRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.athleteRepository = athleteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.passwordResetDeliveryService = passwordResetDeliveryService;
    }

    @Transactional
    public AuthResponse createSession(Athlete athlete) {
        Instant now = Instant.now();
        String rawRefreshToken = randomToken();
        RefreshSession session = refreshSessionRepository.save(new RefreshSession(
                athlete,
                hash(rawRefreshToken),
                now.plus(jwtProperties.getRefreshExpiration()),
                now
        ));
        String accessToken = jwtService.generateToken(athlete, session.getId());
        return response(athlete, accessToken, rawRefreshToken);
    }

    @Transactional
    public AuthResponse rotateRefreshToken(String rawRefreshToken) {
        Instant now = Instant.now();
        RefreshSession current = refreshSessionRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new IllegalArgumentException("Refresh token invalido"));
        if (current.getRevokedAt() != null || !current.getExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException("Refresh token expirado o revocado");
        }
        current.revoke(now);
        return createSession(current.getAthlete());
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        refreshSessionRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(session -> {
            if (session.getRevokedAt() == null) {
                session.revoke(Instant.now());
            }
        });
    }

    @Transactional
    public void requestPasswordReset(String email) {
        athleteRepository.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT)).ifPresent(athlete -> {
            if (athlete.getPasswordHash() == null) {
                return;
            }
            Instant now = Instant.now();
            passwordResetTokenRepository.invalidateAllByAthleteUuid(athlete.getAtletaUuid(), now);
            String rawToken = randomToken();
            Instant expiresAt = now.plus(jwtProperties.getPasswordResetExpiration());
            passwordResetTokenRepository.save(new PasswordResetToken(athlete, hash(rawToken), expiresAt, now));
            passwordResetDeliveryService.sendResetLink(athlete.getEmail(), rawToken, expiresAt);
        });
    }

    @Transactional
    public void confirmPasswordReset(String rawToken, String newPassword) {
        Instant now = Instant.now();
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new IllegalArgumentException("Token de recuperacion invalido"));
        if (resetToken.getUsedAt() != null || !resetToken.getExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException("Token de recuperacion expirado o utilizado");
        }
        Athlete athlete = resetToken.getAthlete();
        athlete.setPasswordHash(passwordEncoder.encode(newPassword));
        resetToken.markUsed(now);
        refreshSessionRepository.revokeAllByAthleteUuid(athlete.getAtletaUuid(), now);
        athleteRepository.save(athlete);
    }

    private AuthResponse response(Athlete athlete, String accessToken, String refreshToken) {
        return new AuthResponse(
                athlete.getAtletaUuid(), athlete.getEmail(), athlete.getNombre(), athlete.getGenero(),
                athlete.getAuthProvider(), accessToken, refreshToken
        );
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
