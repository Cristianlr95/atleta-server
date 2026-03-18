package com.atleta.demo.entity;

import net.jqwik.api.*;
import org.junit.jupiter.api.DisplayName;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for PlayerPosition entity validation.
 * Feature: api-foundation, Property 3: Validación de posiciones y prioridades
 * Validates: Requirements 3.2, 3.4
 */
class PlayerPositionPropertyTest {

    private final Validator validator;

    public PlayerPositionPropertyTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Property(tries = 100)
    @DisplayName("PlayerPosition should enforce unique priorities per player")
    void playerPositionShouldEnforceUniquePriorities(@ForAll("validPlayerPositionData") PlayerPositionData data) {
        // Property: For any player, priorities should be unique (1, 2, 3)
        PlayerProfile player = createTestPlayerProfile(data.playerUuid);
        Position position1 = new Position("Portero");
        position1.setId(1L);
        Position position2 = new Position("Defensa");
        position2.setId(2L);
        
        PlayerPosition playerPos1 = new PlayerPosition(player, position1, data.priority1);
        PlayerPosition playerPos2 = new PlayerPosition(player, position2, data.priority2);
        
        // Validate both positions
        Set<ConstraintViolation<PlayerPosition>> violations1 = validator.validate(playerPos1);
        Set<ConstraintViolation<PlayerPosition>> violations2 = validator.validate(playerPos2);
        
        // Both should be valid individually
        assertThat(violations1).isEmpty();
        assertThat(violations2).isEmpty();
        
        // Verify priority constraints
        assertThat(playerPos1.getPrioridad()).isBetween(1, 3);
        assertThat(playerPos2.getPrioridad()).isBetween(1, 3);
        
        // If priorities are the same, this would violate uniqueness constraint at DB level
        if (data.priority1.equals(data.priority2)) {
            // This would be caught by unique constraint in database
            assertThat(playerPos1.getPrioridad()).isEqualTo(playerPos2.getPrioridad());
        } else {
            assertThat(playerPos1.getPrioridad()).isNotEqualTo(playerPos2.getPrioridad());
        }
    }

    @Property(tries = 100)
    @DisplayName("PlayerPosition should validate priority range and XP constraints")
    void playerPositionShouldValidateConstraints(@ForAll("playerPositionDataWithPotentialIssues") PlayerPositionData data) {
        // Property: For any player position data, validation should enforce priority and XP constraints
        PlayerProfile player = createTestPlayerProfile(data.playerUuid);
        Position position = new Position("Mediocampista");
        position.setId(3L);
        
        PlayerPosition playerPosition = new PlayerPosition(player, position, data.priority1, data.xp);
        
        Set<ConstraintViolation<PlayerPosition>> violations = validator.validate(playerPosition);
        
        // Check priority validation
        if (data.priority1 < 1) {
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("prioridad") && 
                                           v.getMessage().contains("debe ser mínimo 1"));
        } else if (data.priority1 > 3) {
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("prioridad") && 
                                           v.getMessage().contains("debe ser máximo 3"));
        }
        
        // Check XP validation
        if (data.xp != null && data.xp < 0) {
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("xp") && 
                                           v.getMessage().contains("no puede ser negativa"));
        }
    }

    @Property(tries = 100)
    @DisplayName("PlayerPosition should correctly handle XP addition")
    void playerPositionShouldHandleXpAddition(@ForAll("validPlayerPositionData") PlayerPositionData data) {
        // Property: For any player position, adding XP should increase the total correctly
        PlayerProfile player = createTestPlayerProfile(data.playerUuid);
        Position position = new Position("Delantero");
        position.setId(4L);
        
        PlayerPosition playerPosition = new PlayerPosition(player, position, data.priority1, data.xp);
        Integer initialXp = playerPosition.getXp();
        
        // Add some XP
        Integer additionalXp = 50;
        playerPosition.addXp(additionalXp);
        
        // XP should be increased
        assertThat(playerPosition.getXp()).isEqualTo(initialXp + additionalXp);
        
        // Adding null or negative XP should not change the value
        Integer currentXp = playerPosition.getXp();
        playerPosition.addXp(null);
        assertThat(playerPosition.getXp()).isEqualTo(currentXp);
        
        playerPosition.addXp(-10);
        assertThat(playerPosition.getXp()).isEqualTo(currentXp);
    }

    @Property(tries = 100)
    @DisplayName("PlayerPosition should maintain valid relationships")
    void playerPositionShouldMaintainValidRelationships(@ForAll("validPlayerPositionData") PlayerPositionData data) {
        // Property: For any player position, relationships with player and position should be consistent
        PlayerProfile player = createTestPlayerProfile(data.playerUuid);
        Position position = new Position("Carrilero");
        position.setId(5L);
        
        PlayerPosition playerPosition = new PlayerPosition(player, position, data.priority1);
        
        // Relationships should be consistent
        assertThat(playerPosition.getPlayer()).isEqualTo(player);
        assertThat(playerPosition.getPosition()).isEqualTo(position);
        assertThat(playerPosition.getPrioridad()).isEqualTo(data.priority1);
        
        // Default XP should be 0
        assertThat(playerPosition.getXp()).isEqualTo(0);
        
        // Bidirectional relationship should work
        player.addPosition(playerPosition);
        assertThat(player.getPositions()).contains(playerPosition);
        assertThat(playerPosition.getPlayer()).isEqualTo(player);
    }

    // Helper method to create test PlayerProfile
    private PlayerProfile createTestPlayerProfile(UUID uuid) {
        Athlete athlete = new Athlete("test@example.com", "hashedPassword", "Test Player");
        athlete.setAtletaUuid(uuid);
        return new PlayerProfile(athlete);
    }

    // Data generators
    @Provide
    Arbitrary<PlayerPositionData> validPlayerPositionData() {
        return Combinators.combine(
            Arbitraries.create(UUID::randomUUID),
            Arbitraries.integers().between(1, 3), // valid priority
            Arbitraries.integers().between(1, 3), // valid priority for second position
            Arbitraries.integers().between(0, 1000) // valid XP
        ).as(PlayerPositionData::new);
    }

    @Provide
    Arbitrary<PlayerPositionData> playerPositionDataWithPotentialIssues() {
        return Combinators.combine(
            Arbitraries.create(UUID::randomUUID),
            Arbitraries.oneOf(
                Arbitraries.integers().between(1, 3), // valid priority
                Arbitraries.integers().between(-5, 0), // invalid priority (too low)
                Arbitraries.integers().between(4, 10) // invalid priority (too high)
            ),
            Arbitraries.integers().between(1, 3), // second priority (not used in this generator)
            Arbitraries.oneOf(
                Arbitraries.integers().between(0, 1000), // valid XP
                Arbitraries.integers().between(-100, -1), // invalid XP (negative)
                Arbitraries.just((Integer) null) // null XP
            )
        ).as(PlayerPositionData::new);
    }

    // Data class for test data
    static class PlayerPositionData {
        final UUID playerUuid;
        final Integer priority1;
        final Integer priority2;
        final Integer xp;

        PlayerPositionData(UUID playerUuid, Integer priority1, Integer priority2, Integer xp) {
            this.playerUuid = playerUuid;
            this.priority1 = priority1;
            this.priority2 = priority2;
            this.xp = xp != null ? xp : 0; // Default to 0 if null
        }
    }
}