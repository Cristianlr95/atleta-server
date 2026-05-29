package com.atleta.demo.service;

import com.atleta.demo.entity.Match;
import com.atleta.demo.enums.MatchStatus;
import com.atleta.demo.enums.MatchValidationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchStatusPolicyTest {

    private final MatchStatusPolicy policy = new MatchStatusPolicy();

    @Test
    void validateTransition_AllowsValidLifecycleMoves() {
        policy.validateTransition(MatchStatus.CREADO, MatchStatus.INICIADO);
        policy.validateTransition(MatchStatus.CREADO, MatchStatus.INVALIDO);
        policy.validateTransition(MatchStatus.INICIADO, MatchStatus.FINALIZADO);
        policy.validateTransition(MatchStatus.INICIADO, MatchStatus.INVALIDO);
    }

    @Test
    void validateTransition_RejectsInvalidLifecycleMoves() {
        IllegalArgumentException createdException = assertThrows(
                IllegalArgumentException.class,
                () -> policy.validateTransition(MatchStatus.CREADO, MatchStatus.FINALIZADO)
        );
        assertEquals("Desde CREADO solo se puede pasar a INICIADO o INVALIDO", createdException.getMessage());

        IllegalArgumentException closedException = assertThrows(
                IllegalArgumentException.class,
                () -> policy.validateTransition(MatchStatus.FINALIZADO, MatchStatus.INVALIDO)
        );
        assertEquals("No se puede cambiar el estado desde FINALIZADO", closedException.getMessage());
    }

    @Test
    void markStarted_KeepsExistingStartedAtAndResetsValidationState() {
        Match match = new Match();
        LocalDateTime originalStartedAt = LocalDateTime.now().minusMinutes(10);
        match.setStartedAt(originalStartedAt);
        match.setValidationStatus(MatchValidationStatus.INVALID_STATE);
        match.setValidationReason("old reason");

        policy.markStarted(match, LocalDateTime.now());

        assertEquals(originalStartedAt, match.getStartedAt());
        assertEquals(MatchValidationStatus.PENDING, match.getValidationStatus());
        assertNull(match.getValidationReason());
    }

    @Test
    void markManualInvalid_SetsInvalidMetadata() {
        Match match = new Match();

        policy.markManualInvalid(match);

        assertEquals(MatchValidationStatus.INVALID_STATE, match.getValidationStatus());
        assertEquals("Partido marcado como invalido manualmente", match.getValidationReason());
    }

    @Test
    void isClosePending_UsesConfiguredWindow() {
        Match match = new Match();
        match.setEstado(MatchStatus.INICIADO);
        match.setStartedAt(LocalDateTime.of(2026, 5, 29, 10, 0));

        assertTrue(policy.isClosePending(match, 1, LocalDateTime.of(2026, 5, 29, 11, 1)));
        assertFalse(policy.isClosePending(match, 1, LocalDateTime.of(2026, 5, 29, 10, 30)));

        match.setEstado(MatchStatus.CREADO);
        assertFalse(policy.isClosePending(match, 1, LocalDateTime.of(2026, 5, 29, 12, 0)));
        assertNotNull(match.getStartedAt());
    }
}
