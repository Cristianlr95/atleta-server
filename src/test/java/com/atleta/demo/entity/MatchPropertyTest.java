package com.atleta.demo.entity;

import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchStatus;
import net.jqwik.api.*;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Match entity validation.
 * Feature: api-foundation, Property 6: Validación de partidos
 * Validates: Requirements 6.1, 6.2, 6.3
 */
class MatchPropertyTest {

    @Property(tries = 100)
    @DisplayName("Match should have valid modalidad, coordinates within valid ranges, and initial CREADO status")
    void matchShouldHaveValidModalidadCoordinatesAndInitialStatus(
            @ForAll("validMatchMode") MatchMode modalidad,
            @ForAll("validLatitude") BigDecimal latitud,
            @ForAll("validLongitude") BigDecimal longitud,
            @ForAll("validCuota") BigDecimal cuota,
            @ForAll("futureDateTime") LocalDateTime fechaHoraProgramada,
            @ForAll String creatorEmail) {
        
        // Property: For any match created, it must have valid modalidad, coordinates within ranges, and initial CREADO status
        
        // Create a valid creator (PlayerProfile)
        Athlete athlete = new Athlete(creatorEmail, "passwordHash", "Creator Name");
        athlete.setAtletaUuid(UUID.randomUUID()); // Simulate persisted athlete
        PlayerProfile creator = new PlayerProfile(athlete);
        
        // Create match with all parameters
        Match match = new Match(modalidad, fechaHoraProgramada, creator, latitud, longitud, cuota);
        
        // Validate match properties
        assertThat(match.getModalidad()).isEqualTo(modalidad);
        assertThat(match.getFechaHoraProgramada()).isEqualTo(fechaHoraProgramada);
        assertThat(match.getCreador()).isEqualTo(creator);
        assertThat(match.getLatitud()).isEqualTo(latitud);
        assertThat(match.getLongitud()).isEqualTo(longitud);
        assertThat(match.getCuota()).isEqualTo(cuota);
        
        // Validate initial status is CREADO
        assertThat(match.getEstado()).isEqualTo(MatchStatus.CREADO);
        
        // Validate coordinates are within valid ranges
        assertThat(match.getLatitud()).isBetween(new BigDecimal("-90.0"), new BigDecimal("90.0"));
        assertThat(match.getLongitud()).isBetween(new BigDecimal("-180.0"), new BigDecimal("180.0"));
        
        // Validate cuota is non-negative
        assertThat(match.getCuota()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        
        // Validate collections are initialized
        assertThat(match.getMatchTeams()).isNotNull();
        assertThat(match.getMatchTeams()).isEmpty();
        assertThat(match.getPlayers()).isNotNull();
        assertThat(match.getPlayers()).isEmpty();
        assertThat(match.getEvents()).isNotNull();
        assertThat(match.getEvents()).isEmpty();
    }

    @Property(tries = 100)
    @DisplayName("Match should validate exactly two teams constraint")
    void matchShouldValidateExactlyTwoTeamsConstraint(
            @ForAll("validMatchMode") MatchMode modalidad,
            @ForAll("futureDateTime") LocalDateTime fechaHoraProgramada,
            @ForAll String creatorEmail,
            @ForAll String team1Name,
            @ForAll String team2Name) {
        
        Assume.that(!team1Name.equals(team2Name));
        
        // Property: For any match, it should validate exactly two teams constraint
        
        // Create creator and match
        Athlete athlete = new Athlete(creatorEmail, "passwordHash", "Creator Name");
        athlete.setAtletaUuid(UUID.randomUUID());
        PlayerProfile creator = new PlayerProfile(athlete);
        Match match = new Match(modalidad, fechaHoraProgramada, creator);
        
        // Initially should not have exactly two teams
        assertThat(match.hasExactlyTwoTeams()).isFalse();
        
        // Create two teams
        Team team1 = new Team(team1Name, creator);
        Team team2 = new Team(team2Name, creator);
        
        // Add first team
        MatchTeam matchTeam1 = new MatchTeam(match, team1, true); // local
        match.addMatchTeam(matchTeam1);
        
        // Still should not have exactly two teams
        assertThat(match.hasExactlyTwoTeams()).isFalse();
        assertThat(match.getMatchTeams()).hasSize(1);
        
        // Add second team
        MatchTeam matchTeam2 = new MatchTeam(match, team2, false); // visitante
        match.addMatchTeam(matchTeam2);
        
        // Now should have exactly two teams
        assertThat(match.hasExactlyTwoTeams()).isTrue();
        assertThat(match.getMatchTeams()).hasSize(2);
        
        // Validate one local and one visitante
        long localTeams = match.getMatchTeams().stream()
            .mapToLong(mt -> mt.isLocal() ? 1 : 0)
            .sum();
        long visitanteTeams = match.getMatchTeams().stream()
            .mapToLong(mt -> mt.isVisitante() ? 1 : 0)
            .sum();
        
        assertThat(localTeams).isEqualTo(1);
        assertThat(visitanteTeams).isEqualTo(1);
    }

    @Property(tries = 100)
    @DisplayName("Match status transitions should be valid")
    void matchStatusTransitionsShouldBeValid(
            @ForAll("validMatchMode") MatchMode modalidad,
            @ForAll("futureDateTime") LocalDateTime fechaHoraProgramada,
            @ForAll String creatorEmail) {
        
        // Property: For any match, status transitions should follow valid business rules
        
        // Create match
        Athlete athlete = new Athlete(creatorEmail, "passwordHash", "Creator Name");
        athlete.setAtletaUuid(UUID.randomUUID());
        PlayerProfile creator = new PlayerProfile(athlete);
        Match match = new Match(modalidad, fechaHoraProgramada, creator);
        
        // Initial status should be CREADO
        assertThat(match.getEstado()).isEqualTo(MatchStatus.CREADO);
        
        // Can transition to INICIADO
        match.setEstado(MatchStatus.INICIADO);
        match.setStartedAt(LocalDateTime.now());
        assertThat(match.getEstado()).isEqualTo(MatchStatus.INICIADO);
        assertThat(match.getStartedAt()).isNotNull();
        
        // Can transition to FINALIZADO
        match.setEstado(MatchStatus.FINALIZADO);
        assertThat(match.getEstado()).isEqualTo(MatchStatus.FINALIZADO);
        
        // Can also transition to INVALIDO from any state
        match.setEstado(MatchStatus.INVALIDO);
        assertThat(match.getEstado()).isEqualTo(MatchStatus.INVALIDO);
    }

    @Property(tries = 100)
    @DisplayName("Match coordinates should always be within valid geographic ranges")
    void matchCoordinatesShouldAlwaysBeWithinValidGeographicRanges(
            @ForAll("validLatitude") BigDecimal latitud,
            @ForAll("validLongitude") BigDecimal longitud,
            @ForAll("validMatchMode") MatchMode modalidad,
            @ForAll("futureDateTime") LocalDateTime fechaHoraProgramada,
            @ForAll String creatorEmail) {
        
        // Property: For any match coordinates, they should always be within valid geographic ranges
        
        // Create match with coordinates
        Athlete athlete = new Athlete(creatorEmail, "passwordHash", "Creator Name");
        athlete.setAtletaUuid(UUID.randomUUID());
        PlayerProfile creator = new PlayerProfile(athlete);
        Match match = new Match(modalidad, fechaHoraProgramada, creator, latitud, longitud, BigDecimal.ZERO);
        
        // Validate latitude range (-90 to 90)
        assertThat(match.getLatitud()).isGreaterThanOrEqualTo(new BigDecimal("-90.0"));
        assertThat(match.getLatitud()).isLessThanOrEqualTo(new BigDecimal("90.0"));
        
        // Validate longitude range (-180 to 180)
        assertThat(match.getLongitud()).isGreaterThanOrEqualTo(new BigDecimal("-180.0"));
        assertThat(match.getLongitud()).isLessThanOrEqualTo(new BigDecimal("180.0"));
        
        // Test coordinate updates
        match.setLatitud(new BigDecimal("45.5"));
        match.setLongitud(new BigDecimal("-122.3"));
        
        assertThat(match.getLatitud()).isEqualTo(new BigDecimal("45.5"));
        assertThat(match.getLongitud()).isEqualTo(new BigDecimal("-122.3"));
    }

    @Provide
    Arbitrary<MatchMode> validMatchMode() {
        return Arbitraries.of(MatchMode.values());
    }

    @Provide
    Arbitrary<BigDecimal> validLatitude() {
        return Arbitraries.bigDecimals()
            .between(new BigDecimal("-90.0"), new BigDecimal("90.0"))
            .ofScale(6);
    }

    @Provide
    Arbitrary<BigDecimal> validLongitude() {
        return Arbitraries.bigDecimals()
            .between(new BigDecimal("-180.0"), new BigDecimal("180.0"))
            .ofScale(6);
    }

    @Provide
    Arbitrary<BigDecimal> validCuota() {
        return Arbitraries.bigDecimals()
            .between(BigDecimal.ZERO, new BigDecimal("1000.0"))
            .ofScale(2);
    }

    @Provide
    Arbitrary<LocalDateTime> futureDateTime() {
        return Arbitraries.create(() -> LocalDateTime.now().plusDays(1));
    }
}