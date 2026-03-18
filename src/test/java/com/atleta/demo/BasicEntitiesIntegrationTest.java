package com.atleta.demo;

import com.atleta.demo.config.BaseIntegrationTest;
import com.atleta.demo.entity.*;
import com.atleta.demo.enums.*;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to verify basic entities are working correctly
 */
class BasicEntitiesIntegrationTest extends BaseIntegrationTest {

    @Test
    void testAthleteEntityCreation() {
        // Test Athlete entity creation and basic functionality
        Athlete athlete = new Athlete("test@example.com", "hashedPassword", "Test Athlete");
        
        assertThat(athlete.getEmail()).isEqualTo("test@example.com");
        assertThat(athlete.getPasswordHash()).isEqualTo("hashedPassword");
        assertThat(athlete.getNombre()).isEqualTo("Test Athlete");
        assertThat(athlete.getAtletaUuid()).isNull(); // Should be null before persistence
        assertThat(athlete.getCreatedAt()).isNull(); // Should be null before persistence
        
        // Test UUID assignment
        UUID testUuid = UUID.randomUUID();
        athlete.setAtletaUuid(testUuid);
        assertThat(athlete.getAtletaUuid()).isEqualTo(testUuid);
    }

    @Test
    void testPlayerProfileEntityCreation() {
        // Test PlayerProfile entity creation and relationships
        Athlete athlete = new Athlete("profile@example.com", "hashedPassword", "Profile Test");
        athlete.setAtletaUuid(UUID.randomUUID());
        
        PlayerProfile profile = new PlayerProfile(athlete, "TestAlias");
        
        assertThat(profile.getAthlete()).isEqualTo(athlete);
        assertThat(profile.getAtletaUuid()).isEqualTo(athlete.getAtletaUuid());
        assertThat(profile.getAlias()).isEqualTo("TestAlias");
        assertThat(profile.getTrustScore()).isEqualTo(100); // Default value
        assertThat(profile.getPositions()).isNotNull();
        assertThat(profile.getPositions()).isEmpty();
    }

    @Test
    void testPositionEntityCreation() {
        // Test Position entity creation
        Position position = new Position("Portero");
        
        assertThat(position.getNombre()).isEqualTo("Portero");
        assertThat(position.getId()).isNull(); // Should be null before persistence
        assertThat(position.getCreatedAt()).isNull(); // Should be null before persistence
    }

    @Test
    void testPlayerPositionEntityCreation() {
        // Test PlayerPosition entity creation and relationships
        Athlete athlete = new Athlete("position@example.com", "hashedPassword", "Position Test");
        athlete.setAtletaUuid(UUID.randomUUID());
        PlayerProfile player = new PlayerProfile(athlete);
        Position position = new Position("Mediocampista");
        position.setId(1L);
        
        PlayerPosition playerPosition = new PlayerPosition(player, position, 1, 50);
        
        assertThat(playerPosition.getPlayer()).isEqualTo(player);
        assertThat(playerPosition.getPosition()).isEqualTo(position);
        assertThat(playerPosition.getPrioridad()).isEqualTo(1);
        assertThat(playerPosition.getXp()).isEqualTo(50);
        
        // Test XP addition
        playerPosition.addXp(25);
        assertThat(playerPosition.getXp()).isEqualTo(75);
        
        // Test invalid XP addition
        playerPosition.addXp(-10);
        assertThat(playerPosition.getXp()).isEqualTo(75); // Should remain unchanged
        
        playerPosition.addXp(null);
        assertThat(playerPosition.getXp()).isEqualTo(75); // Should remain unchanged
    }

    @Test
    void testEnumerations() {
        // Test all enumerations are working correctly
        
        // MatchMode
        MatchMode mode = MatchMode.CINCO_VS_CINCO;
        assertThat(mode.getDisplayName()).isEqualTo("5v5");
        assertThat(mode.toString()).isEqualTo("5v5");
        
        // MatchStatus
        MatchStatus status = MatchStatus.CREADO;
        assertThat(status.name()).isEqualTo("CREADO");
        
        // PlayerRole
        PlayerRole role = PlayerRole.CAPITAN;
        assertThat(role.name()).isEqualTo("CAPITAN");
        
        // EventType
        EventType eventType = EventType.GOL;
        assertThat(eventType.name()).isEqualTo("GOL");
        
        // MatchResult
        MatchResult result = MatchResult.VICTORIA;
        assertThat(result.name()).isEqualTo("VICTORIA");
    }

    @Test
    void testBidirectionalRelationships() {
        // Test bidirectional relationships work correctly
        Athlete athlete = new Athlete("bidirectional@example.com", "hashedPassword", "Bidirectional Test");
        athlete.setAtletaUuid(UUID.randomUUID());
        
        PlayerProfile profile = new PlayerProfile(athlete);
        athlete.setPlayerProfile(profile);
        
        // Test bidirectional relationship
        assertThat(profile.getAthlete()).isEqualTo(athlete);
        assertThat(athlete.getPlayerProfile()).isEqualTo(profile);
        assertThat(profile.getAtletaUuid()).isEqualTo(athlete.getAtletaUuid());
        
        // Test position relationships
        Position position = new Position("Delantero");
        position.setId(2L);
        PlayerPosition playerPosition = new PlayerPosition(profile, position, 2);
        
        profile.addPosition(playerPosition);
        assertThat(profile.getPositions()).contains(playerPosition);
        assertThat(playerPosition.getPlayer()).isEqualTo(profile);
    }
}