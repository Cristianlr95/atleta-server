package com.atleta.demo.entity;

import com.atleta.demo.enums.PlayerRole;
import net.jqwik.api.*;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for TeamMember entity consistency and validation.
 * Feature: api-foundation, Property 5: Consistencia de membresía
 * Validates: Requirements 5.1, 5.2
 */
class TeamMemberPropertyTest {

    @Property(tries = 100)
    @DisplayName("Team membership should have valid player, team, and role")
    void teamMembershipShouldHaveValidPlayerTeamAndRole(
            @ForAll String teamName,
            @ForAll String playerEmail,
            @ForAll("validPlayerRole") PlayerRole role) {
        
        // Property: For any team membership, the player must exist, team must exist, and role must be valid
        
        // Create valid entities
        Athlete athlete = new Athlete(playerEmail, "passwordHash", "Player Name");
        athlete.setAtletaUuid(UUID.randomUUID()); // Simulate persisted athlete
        PlayerProfile player = new PlayerProfile(athlete);
        
        Athlete creatorAthlete = new Athlete("creator@example.com", "passwordHash", "Creator Name");
        creatorAthlete.setAtletaUuid(UUID.randomUUID());
        PlayerProfile creator = new PlayerProfile(creatorAthlete);
        Team team = new Team(teamName, creator);
        
        // Create team membership
        TeamMember membership = new TeamMember(team, player, role);
        
        // Validate membership consistency
        assertThat(membership.getTeam()).isNotNull();
        assertThat(membership.getPlayer()).isNotNull();
        assertThat(membership.getRol()).isNotNull();
        
        // Validate relationships
        assertThat(membership.getTeam()).isEqualTo(team);
        assertThat(membership.getPlayer()).isEqualTo(player);
        assertThat(membership.getRol()).isEqualTo(role);
        
        // Validate default values
        assertThat(membership.getActivo()).isTrue();
        assertThat(membership.getJoinedAt()).isNull(); // Will be set by @CreationTimestamp
        
        // Validate role-specific methods
        if (role == PlayerRole.CAPITAN) {
            assertThat(membership.isCapitan()).isTrue();
            assertThat(membership.isDT()).isFalse();
        } else if (role == PlayerRole.DT) {
            assertThat(membership.isDT()).isTrue();
            assertThat(membership.isCapitan()).isFalse();
        } else {
            assertThat(membership.isCapitan()).isFalse();
            assertThat(membership.isDT()).isFalse();
        }
    }

    @Property(tries = 100)
    @DisplayName("Team membership should allow multiple players per team")
    void teamMembershipShouldAllowMultiplePlayersPerTeam(
            @ForAll String teamName,
            @ForAll String player1Email,
            @ForAll String player2Email) {
        
        Assume.that(!player1Email.equals(player2Email));
        
        // Property: For any team, multiple players should be able to join with different memberships
        
        // Create team
        Athlete creatorAthlete = new Athlete("creator@example.com", "passwordHash", "Creator Name");
        creatorAthlete.setAtletaUuid(UUID.randomUUID());
        PlayerProfile creator = new PlayerProfile(creatorAthlete);
        Team team = new Team(teamName, creator);
        
        // Create two different players
        Athlete athlete1 = new Athlete(player1Email, "passwordHash", "Player 1");
        athlete1.setAtletaUuid(UUID.randomUUID());
        PlayerProfile player1 = new PlayerProfile(athlete1);
        
        Athlete athlete2 = new Athlete(player2Email, "passwordHash", "Player 2");
        athlete2.setAtletaUuid(UUID.randomUUID());
        PlayerProfile player2 = new PlayerProfile(athlete2);
        
        // Create memberships
        TeamMember membership1 = new TeamMember(team, player1, PlayerRole.JUGADOR);
        TeamMember membership2 = new TeamMember(team, player2, PlayerRole.CAPITAN);
        
        // Both memberships should be valid and different
        assertThat(membership1.getTeam()).isEqualTo(team);
        assertThat(membership2.getTeam()).isEqualTo(team);
        assertThat(membership1.getPlayer()).isNotEqualTo(membership2.getPlayer());
        assertThat(membership1.getRol()).isNotEqualTo(membership2.getRol());
        
        // Both should be active by default
        assertThat(membership1.isActivo()).isTrue();
        assertThat(membership2.isActivo()).isTrue();
    }

    @Property(tries = 100)
    @DisplayName("Team membership status should be manageable")
    void teamMembershipStatusShouldBeManageable(
            @ForAll String teamName,
            @ForAll String playerEmail) {
        
        // Property: For any team membership, the active status should be manageable
        
        // Create entities
        Athlete athlete = new Athlete(playerEmail, "passwordHash", "Player Name");
        athlete.setAtletaUuid(UUID.randomUUID());
        PlayerProfile player = new PlayerProfile(athlete);
        
        Athlete creatorAthlete = new Athlete("creator@example.com", "passwordHash", "Creator Name");
        creatorAthlete.setAtletaUuid(UUID.randomUUID());
        PlayerProfile creator = new PlayerProfile(creatorAthlete);
        Team team = new Team(teamName, creator);
        
        TeamMember membership = new TeamMember(team, player);
        
        // Initially should be active
        assertThat(membership.isActivo()).isTrue();
        
        // Should be able to deactivate
        membership.desactivar();
        assertThat(membership.getActivo()).isFalse();
        assertThat(membership.isActivo()).isFalse();
        
        // Should be able to reactivate
        membership.reactivar();
        assertThat(membership.getActivo()).isTrue();
        assertThat(membership.isActivo()).isTrue();
    }

    @Provide
    Arbitrary<PlayerRole> validPlayerRole() {
        return Arbitraries.of(PlayerRole.values());
    }
}