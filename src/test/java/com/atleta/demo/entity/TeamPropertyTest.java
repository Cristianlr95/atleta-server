package com.atleta.demo.entity;

import net.jqwik.api.*;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Team entity integrity and validation.
 * Feature: api-foundation, Property 4: Integridad de equipos
 * Validates: Requirements 4.1, 4.3, 4.4
 */
class TeamPropertyTest {

    @Property(tries = 100)
    @DisplayName("Team should have valid creator, unique name, and initialized stats")
    void teamShouldHaveValidCreatorAndUniqueNameAndInitializedStats(
            @ForAll String teamName, 
            @ForAll String creatorEmail,
            @ForAll String logoUrl,
            @ForAll("validYear") Integer anioFundacion) {
        
        // Property: For any team created, it must have a valid creator, unique name, and stats initialized to zero
        
        // Create a valid creator (PlayerProfile)
        Athlete athlete = new Athlete(creatorEmail, "passwordHash", "Creator Name");
        athlete.setAtletaUuid(UUID.randomUUID()); // Simulate persisted athlete
        PlayerProfile creator = new PlayerProfile(athlete);
        
        // Create team with creator
        Team team = new Team(teamName, creator, logoUrl, anioFundacion);
        
        // Validate team integrity
        assertThat(team.getNombre()).isEqualTo(teamName);
        assertThat(team.getCreador()).isEqualTo(creator);
        assertThat(team.getLogoUrl()).isEqualTo(logoUrl);
        assertThat(team.getAnioFundacion()).isEqualTo(anioFundacion);
        
        // Team should have empty members list initially
        assertThat(team.getMembers()).isNotNull();
        assertThat(team.getMembers()).isEmpty();
        
        // Create and associate stats (simulating what would happen in service layer)
        TeamStats stats = new TeamStats(team);
        team.setStats(stats);
        
        // Validate stats are initialized to zero
        assertThat(team.getStats()).isNotNull();
        assertThat(team.getStats().getPartidosJugados()).isEqualTo(0);
        assertThat(team.getStats().getPartidosGanados()).isEqualTo(0);
        assertThat(team.getStats().getPartidosEmpatados()).isEqualTo(0);
        assertThat(team.getStats().getPartidosPerdidos()).isEqualTo(0);
        assertThat(team.getStats().getGolesFavor()).isEqualTo(0);
        assertThat(team.getStats().getGolesContra()).isEqualTo(0);
        assertThat(team.getStats().getPuntos()).isEqualTo(0);
    }

    @Property(tries = 100)
    @DisplayName("Teams with different names should be different entities")
    void teamsWithDifferentNamesShouldBeDifferent(
            @ForAll String teamName1, 
            @ForAll String teamName2,
            @ForAll String creatorEmail) {
        
        Assume.that(!teamName1.equals(teamName2));
        
        // Property: For any two teams with different names, they should be different entities
        
        // Create a valid creator
        Athlete athlete = new Athlete(creatorEmail, "passwordHash", "Creator Name");
        athlete.setAtletaUuid(UUID.randomUUID());
        PlayerProfile creator = new PlayerProfile(athlete);
        
        // Create two teams with different names
        Team team1 = new Team(teamName1, creator);
        Team team2 = new Team(teamName2, creator);
        
        // They should not be equal (based on BaseEntity equals implementation)
        assertThat(team1).isNotEqualTo(team2);
        assertThat(team1.getNombre()).isNotEqualTo(team2.getNombre());
        
        // Both should have the same creator
        assertThat(team1.getCreador()).isEqualTo(team2.getCreador());
    }

    @Property(tries = 100)
    @DisplayName("Team stats should maintain consistency after operations")
    void teamStatsShouldMaintainConsistencyAfterOperations(
            @ForAll String teamName,
            @ForAll String creatorEmail,
            @ForAll("validGoals") Integer golesFavor,
            @ForAll("validGoals") Integer golesContra) {
        
        // Property: For any team stats operations, consistency should be maintained
        
        // Create team with stats
        Athlete athlete = new Athlete(creatorEmail, "passwordHash", "Creator Name");
        athlete.setAtletaUuid(UUID.randomUUID());
        PlayerProfile creator = new PlayerProfile(athlete);
        Team team = new Team(teamName, creator);
        TeamStats stats = new TeamStats(team);
        team.setStats(stats);
        
        // Test victory operation
        stats.registrarVictoria(golesFavor, golesContra);
        
        // Validate consistency after victory
        assertThat(stats.getPartidosJugados()).isEqualTo(1);
        assertThat(stats.getPartidosGanados()).isEqualTo(1);
        assertThat(stats.getPartidosEmpatados()).isEqualTo(0);
        assertThat(stats.getPartidosPerdidos()).isEqualTo(0);
        assertThat(stats.getGolesFavor()).isEqualTo(golesFavor);
        assertThat(stats.getGolesContra()).isEqualTo(golesContra);
        assertThat(stats.getPuntos()).isEqualTo(3);
        assertThat(stats.getDiferenciaGoles()).isEqualTo(golesFavor - golesContra);
        
        // Test that played games equals sum of results
        assertThat(stats.getPartidosJugados())
            .isEqualTo(stats.getPartidosGanados() + stats.getPartidosEmpatados() + stats.getPartidosPerdidos());
    }

    @Provide
    Arbitrary<Integer> validYear() {
        return Arbitraries.integers().between(1800, 2030);
    }

    @Provide
    Arbitrary<Integer> validGoals() {
        return Arbitraries.integers().between(0, 20);
    }
}