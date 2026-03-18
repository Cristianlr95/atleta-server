package com.atleta.demo.entity;

import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.PlayerRole;
import net.jqwik.api.*;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for MatchPlayer entity participation integrity.
 * Feature: api-foundation, Property 7: Integridad de participación
 * Validates: Requirements 7.1, 7.2, 7.4
 */
class MatchPlayerPropertyTest {

    @Property(tries = 100)
    @DisplayName("MatchPlayer should have valid player, team, and position references")
    void matchPlayerShouldHaveValidPlayerTeamAndPositionReferences(
            @ForAll("validPlayerRole") PlayerRole rol,
            @ForAll("validMatchMode") MatchMode matchMode,
            @ForAll String playerEmail,
            @ForAll String playerName,
            @ForAll String teamName,
            @ForAll String positionName,
            @ForAll boolean confirmado) {
        
        // Property: For any participation in match, the player must exist, team must exist, and position must be valid
        
        // Create valid entities
        Athlete athlete = new Athlete(playerEmail, "passwordHash", playerName);
        athlete.setAtletaUuid(UUID.randomUUID()); // Simulate persisted athlete
        PlayerProfile player = new PlayerProfile(athlete);
        
        Team team = new Team(teamName, player);
        team.setId(1L); // Simulate persisted team
        
        Position position = new Position(positionName);
        position.setId(1L); // Simulate persisted position
        
        Match match = new Match(matchMode, LocalDateTime.now().plusDays(1), player);
        match.setId(1L); // Simulate persisted match
        
        // Create MatchPlayer with all required references
        MatchPlayer matchPlayer = new MatchPlayer(match, team, player, position, rol);
        matchPlayer.setConfirmado(confirmado);
        
        // Validate all references exist and are valid
        assertThat(matchPlayer.getPlayer()).isNotNull();
        assertThat(matchPlayer.getPlayer()).isEqualTo(player);
        assertThat(matchPlayer.getPlayer().getAtletaUuid()).isNotNull();
        
        assertThat(matchPlayer.getTeam()).isNotNull();
        assertThat(matchPlayer.getTeam()).isEqualTo(team);
        assertThat(matchPlayer.getTeam().getId()).isNotNull();
        
        assertThat(matchPlayer.getPosition()).isNotNull();
        assertThat(matchPlayer.getPosition()).isEqualTo(position);
        assertThat(matchPlayer.getPosition().getId()).isNotNull();
        
        assertThat(matchPlayer.getMatch()).isNotNull();
        assertThat(matchPlayer.getMatch()).isEqualTo(match);
        assertThat(matchPlayer.getMatch().getId()).isNotNull();
        
        // Validate role is valid
        assertThat(matchPlayer.getRol()).isNotNull();
        assertThat(matchPlayer.getRol()).isEqualTo(rol);
        
        // Validate confirmation status
        assertThat(matchPlayer.getConfirmado()).isEqualTo(confirmado);
        assertThat(matchPlayer.isConfirmado()).isEqualTo(confirmado);
    }

    @Property(tries = 100)
    @DisplayName("MatchPlayer should maintain referential integrity between match and team")
    void matchPlayerShouldMaintainReferentialIntegrityBetweenMatchAndTeam(
            @ForAll("validPlayerRole") PlayerRole rol,
            @ForAll("validMatchMode") MatchMode matchMode,
            @ForAll String playerEmail,
            @ForAll String playerName,
            @ForAll String team1Name,
            @ForAll String team2Name,
            @ForAll String positionName) {
        
        Assume.that(!team1Name.equals(team2Name));
        
        // Property: For any match participation, the team must be one of the teams participating in the match
        
        // Create entities
        Athlete athlete = new Athlete(playerEmail, "passwordHash", playerName);
        athlete.setAtletaUuid(UUID.randomUUID());
        PlayerProfile player = new PlayerProfile(athlete);
        
        Team team1 = new Team(team1Name, player);
        team1.setId(1L);
        Team team2 = new Team(team2Name, player);
        team2.setId(2L);
        
        Position position = new Position(positionName);
        position.setId(1L);
        
        Match match = new Match(matchMode, LocalDateTime.now().plusDays(1), player);
        match.setId(1L);
        
        // Add both teams to the match
        MatchTeam matchTeam1 = new MatchTeam(match, team1, true); // local
        MatchTeam matchTeam2 = new MatchTeam(match, team2, false); // visitante
        match.addMatchTeam(matchTeam1);
        match.addMatchTeam(matchTeam2);
        
        // Create MatchPlayer with team1
        MatchPlayer matchPlayer = new MatchPlayer(match, team1, player, position, rol);
        
        // Validate that the player's team is one of the match teams
        assertThat(match.getMatchTeams()).hasSize(2);
        assertThat(match.getMatchTeams().stream()
            .anyMatch(mt -> mt.getTeam().equals(matchPlayer.getTeam())))
            .isTrue();
        
        // Validate match has exactly two teams
        assertThat(match.hasExactlyTwoTeams()).isTrue();
        
        // Validate player belongs to one of the participating teams
        boolean playerTeamParticipates = match.getMatchTeams().stream()
            .anyMatch(mt -> mt.getTeam().getId().equals(matchPlayer.getTeam().getId()));
        assertThat(playerTeamParticipates).isTrue();
    }

    @Property(tries = 100)
    @DisplayName("MatchPlayer should validate role-specific constraints")
    void matchPlayerShouldValidateRoleSpecificConstraints(
            @ForAll("validPlayerRole") PlayerRole rol,
            @ForAll("validMatchMode") MatchMode matchMode,
            @ForAll String playerEmail,
            @ForAll String playerName,
            @ForAll String teamName,
            @ForAll String positionName) {
        
        // Property: For any match participation, role-specific business rules should be enforced
        
        // Create entities
        Athlete athlete = new Athlete(playerEmail, "passwordHash", playerName);
        athlete.setAtletaUuid(UUID.randomUUID());
        PlayerProfile player = new PlayerProfile(athlete);
        
        Team team = new Team(teamName, player);
        team.setId(1L);
        
        Position position = new Position(positionName);
        position.setId(1L);
        
        Match match = new Match(matchMode, LocalDateTime.now().plusDays(1), player);
        match.setId(1L);
        
        // Create MatchPlayer
        MatchPlayer matchPlayer = new MatchPlayer(match, team, player, position, rol);
        
        // Validate role-specific methods work correctly
        switch (rol) {
            case JUGADOR:
                assertThat(matchPlayer.isJugadorRegular()).isTrue();
                assertThat(matchPlayer.isCapitan()).isFalse();
                assertThat(matchPlayer.isDT()).isFalse();
                break;
            case CAPITAN:
                assertThat(matchPlayer.isJugadorRegular()).isFalse();
                assertThat(matchPlayer.isCapitan()).isTrue();
                assertThat(matchPlayer.isDT()).isFalse();
                break;
            case DT:
                assertThat(matchPlayer.isJugadorRegular()).isFalse();
                assertThat(matchPlayer.isCapitan()).isFalse();
                assertThat(matchPlayer.isDT()).isTrue();
                break;
        }
        
        // Validate role is properly set
        assertThat(matchPlayer.getRol()).isEqualTo(rol);
    }

    @Property(tries = 100)
    @DisplayName("MatchPlayer confirmation status should be properly managed")
    void matchPlayerConfirmationStatusShouldBeProperlyManaged(
            @ForAll("validPlayerRole") PlayerRole rol,
            @ForAll("validMatchMode") MatchMode matchMode,
            @ForAll String playerEmail,
            @ForAll String playerName,
            @ForAll String teamName,
            @ForAll String positionName) {
        
        // Property: For any match participation, confirmation status should be properly managed
        
        // Create entities
        Athlete athlete = new Athlete(playerEmail, "passwordHash", playerName);
        athlete.setAtletaUuid(UUID.randomUUID());
        PlayerProfile player = new PlayerProfile(athlete);
        
        Team team = new Team(teamName, player);
        team.setId(1L);
        
        Position position = new Position(positionName);
        position.setId(1L);
        
        Match match = new Match(matchMode, LocalDateTime.now().plusDays(1), player);
        match.setId(1L);
        
        // Create MatchPlayer - should start unconfirmed
        MatchPlayer matchPlayer = new MatchPlayer(match, team, player, position, rol);
        
        // Initial state should be unconfirmed
        assertThat(matchPlayer.getConfirmado()).isFalse();
        assertThat(matchPlayer.isConfirmado()).isFalse();
        
        // Confirm participation
        matchPlayer.confirmarParticipacion();
        assertThat(matchPlayer.getConfirmado()).isTrue();
        assertThat(matchPlayer.isConfirmado()).isTrue();
        
        // Cancel confirmation
        matchPlayer.cancelarConfirmacion();
        assertThat(matchPlayer.getConfirmado()).isFalse();
        assertThat(matchPlayer.isConfirmado()).isFalse();
        
        // Test direct setter
        matchPlayer.setConfirmado(true);
        assertThat(matchPlayer.getConfirmado()).isTrue();
        assertThat(matchPlayer.isConfirmado()).isTrue();
        
        matchPlayer.setConfirmado(false);
        assertThat(matchPlayer.getConfirmado()).isFalse();
        assertThat(matchPlayer.isConfirmado()).isFalse();
    }

    @Provide
    Arbitrary<PlayerRole> validPlayerRole() {
        return Arbitraries.of(PlayerRole.values());
    }

    @Provide
    Arbitrary<MatchMode> validMatchMode() {
        return Arbitraries.of(MatchMode.values());
    }
}