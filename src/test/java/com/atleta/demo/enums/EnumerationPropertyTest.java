package com.atleta.demo.enums;

import net.jqwik.api.*;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for domain enumerations.
 * Feature: api-foundation, Property 6: Validación de partidos
 * Validates: Requirements 6.1, 6.2, 6.3
 */
class EnumerationPropertyTest {

    @Property(tries = 100)
    @DisplayName("MatchMode enumerations should have valid display names")
    void matchModesShouldHaveValidDisplayNames(@ForAll MatchMode mode) {
        // Property: For any MatchMode, the display name should be non-null and follow expected format
        String displayName = mode.getDisplayName();
        
        assertThat(displayName).isNotNull();
        assertThat(displayName).matches("\\d+v\\d+"); // Format like "5v5", "6v6", "7v7"
        assertThat(mode.toString()).isEqualTo(displayName);
    }

    @Property(tries = 100)
    @DisplayName("MatchStatus enumerations should represent valid match lifecycle states")
    void matchStatusShouldRepresentValidStates(@ForAll MatchStatus status) {
        // Property: For any MatchStatus, it should be one of the valid lifecycle states
        assertThat(status).isIn(
            MatchStatus.CREADO,
            MatchStatus.INICIADO, 
            MatchStatus.FINALIZADO,
            MatchStatus.INVALIDO
        );
        
        // Verify enum name is not null or empty
        assertThat(status.name()).isNotBlank();
    }

    @Property(tries = 100)
    @DisplayName("PlayerRole enumerations should represent valid team roles")
    void playerRolesShouldRepresentValidRoles(@ForAll PlayerRole role) {
        // Property: For any PlayerRole, it should be one of the valid team roles
        assertThat(role).isIn(
            PlayerRole.JUGADOR,
            PlayerRole.CAPITAN,
            PlayerRole.DT
        );
        
        // Verify enum name is not null or empty
        assertThat(role.name()).isNotBlank();
    }

    @Property(tries = 100)
    @DisplayName("EventType enumerations should represent valid match events")
    void eventTypesShouldRepresentValidEvents(@ForAll EventType eventType) {
        // Property: For any EventType, it should be one of the valid match events
        assertThat(eventType).isIn(
            EventType.GOL,
            EventType.ASISTENCIA
        );
        
        // Verify enum name is not null or empty
        assertThat(eventType.name()).isNotBlank();
    }

    @Property(tries = 100)
    @DisplayName("MatchResult enumerations should represent valid match outcomes")
    void matchResultsShouldRepresentValidOutcomes(@ForAll MatchResult result) {
        // Property: For any MatchResult, it should be one of the valid match outcomes
        assertThat(result).isIn(
            MatchResult.VICTORIA,
            MatchResult.DERROTA,
            MatchResult.EMPATE
        );
        
        // Verify enum name is not null or empty
        assertThat(result.name()).isNotBlank();
    }

    @Property(tries = 100)
    @DisplayName("All enumerations should be serializable and have consistent behavior")
    void enumerationsShouldBeConsistent() {
        // Property: All enumerations should have consistent valueOf and values() behavior
        
        // Test MatchMode consistency
        for (MatchMode mode : MatchMode.values()) {
            assertThat(MatchMode.valueOf(mode.name())).isEqualTo(mode);
        }
        
        // Test MatchStatus consistency
        for (MatchStatus status : MatchStatus.values()) {
            assertThat(MatchStatus.valueOf(status.name())).isEqualTo(status);
        }
        
        // Test PlayerRole consistency
        for (PlayerRole role : PlayerRole.values()) {
            assertThat(PlayerRole.valueOf(role.name())).isEqualTo(role);
        }
        
        // Test EventType consistency
        for (EventType eventType : EventType.values()) {
            assertThat(EventType.valueOf(eventType.name())).isEqualTo(eventType);
        }
        
        // Test MatchResult consistency
        for (MatchResult result : MatchResult.values()) {
            assertThat(MatchResult.valueOf(result.name())).isEqualTo(result);
        }
    }
}