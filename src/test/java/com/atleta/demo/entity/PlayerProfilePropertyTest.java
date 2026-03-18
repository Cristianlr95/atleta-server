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
 * Property-based tests for PlayerProfile entity integrity.
 * Feature: api-foundation, Property 2: Integridad de perfiles de jugador
 * Validates: Requirements 2.1, 2.2
 */
class PlayerProfilePropertyTest {

    private final Validator validator;

    public PlayerProfilePropertyTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Property(tries = 100)
    @DisplayName("PlayerProfile should be associated to existing athlete and have initial trust score of 100")
    void playerProfileShouldHaveValidAthleteAndInitialTrustScore(@ForAll("validPlayerProfileData") PlayerProfileData data) {
        // Property: For any player profile created, it must be associated to an athlete and have trust score 100
        Athlete athlete = new Athlete(data.athleteEmail, data.athletePasswordHash, data.athleteNombre);
        athlete.setAtletaUuid(UUID.randomUUID()); // Simulate persisted athlete
        
        PlayerProfile profile = new PlayerProfile(athlete, data.alias);
        
        // Must be associated to existing athlete
        assertThat(profile.getAthlete()).isNotNull();
        assertThat(profile.getAthlete()).isEqualTo(athlete);
        assertThat(profile.getAtletaUuid()).isEqualTo(athlete.getAtletaUuid());
        
        // Must have initial trust score of 100
        assertThat(profile.getTrustScore()).isEqualTo(100);
        
        // Alias should be set correctly
        assertThat(profile.getAlias()).isEqualTo(data.alias);
        
        // Created timestamp should be null before persistence
        assertThat(profile.getCreatedAt()).isNull();
        
        // Positions list should be initialized but empty
        assertThat(profile.getPositions()).isNotNull();
        assertThat(profile.getPositions()).isEmpty();
    }

    @Property(tries = 100)
    @DisplayName("PlayerProfile should maintain one-to-one relationship with Athlete")
    void playerProfileShouldMaintainOneToOneRelationship(@ForAll("validPlayerProfileData") PlayerProfileData data1,
                                                        @ForAll("validPlayerProfileData") PlayerProfileData data2) {
        // Property: For any two player profiles, they should have different athletes (one-to-one relationship)
        Athlete athlete1 = new Athlete(data1.athleteEmail, data1.athletePasswordHash, data1.athleteNombre);
        Athlete athlete2 = new Athlete(data2.athleteEmail, data2.athletePasswordHash, data2.athleteNombre);
        
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        athlete1.setAtletaUuid(uuid1);
        athlete2.setAtletaUuid(uuid2);
        
        PlayerProfile profile1 = new PlayerProfile(athlete1, data1.alias);
        PlayerProfile profile2 = new PlayerProfile(athlete2, data2.alias);
        
        // Each profile should have different athlete UUID
        assertThat(profile1.getAtletaUuid()).isNotEqualTo(profile2.getAtletaUuid());
        assertThat(profile1.getAthlete()).isNotEqualTo(profile2.getAthlete());
        
        // Profiles should be different entities
        assertThat(profile1).isNotEqualTo(profile2);
    }

    @Property(tries = 100)
    @DisplayName("PlayerProfile validation should enforce trust score constraints and alias length")
    void playerProfileValidationShouldEnforceConstraints(@ForAll("playerProfileDataWithPotentialIssues") PlayerProfileData data) {
        // Property: For any player profile data, validation should properly enforce constraints
        Athlete athlete = new Athlete(data.athleteEmail, data.athletePasswordHash, data.athleteNombre);
        athlete.setAtletaUuid(UUID.randomUUID());
        
        PlayerProfile profile = new PlayerProfile(athlete, data.alias);
        if (data.trustScore != null) {
            profile.setTrustScore(data.trustScore);
        }
        
        Set<ConstraintViolation<PlayerProfile>> violations = validator.validate(profile);
        
        // Check trust score validation
        if (data.trustScore != null) {
            if (data.trustScore < 0) {
                assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("trustScore") && 
                                               v.getMessage().contains("no puede ser menor a 0"));
            } else if (data.trustScore > 1000) {
                assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("trustScore") && 
                                               v.getMessage().contains("no puede ser mayor a 1000"));
            }
        }
        
        // Check alias length validation
        if (data.alias != null && data.alias.length() > 50) {
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("alias") && 
                                           v.getMessage().contains("50 caracteres"));
        }
    }

    @Property(tries = 100)
    @DisplayName("PlayerProfile should correctly handle bidirectional relationship with Athlete")
    void playerProfileShouldHandleBidirectionalRelationship(@ForAll("validPlayerProfileData") PlayerProfileData data) {
        // Property: For any player profile, the bidirectional relationship with athlete should be consistent
        Athlete athlete = new Athlete(data.athleteEmail, data.athletePasswordHash, data.athleteNombre);
        athlete.setAtletaUuid(UUID.randomUUID());
        
        PlayerProfile profile = new PlayerProfile(athlete, data.alias);
        athlete.setPlayerProfile(profile);
        
        // Bidirectional relationship should be consistent
        assertThat(profile.getAthlete()).isEqualTo(athlete);
        assertThat(athlete.getPlayerProfile()).isEqualTo(profile);
        assertThat(profile.getAtletaUuid()).isEqualTo(athlete.getAtletaUuid());
    }

    // Data generators
    @Provide
    Arbitrary<PlayerProfileData> validPlayerProfileData() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20)
                .map(s -> s.toLowerCase() + "@example.com"),
            Arbitraries.strings().alpha().ofMinLength(8).ofMaxLength(50)
                .map(s -> "hash_" + s),
            Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(50)
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase()),
            Arbitraries.oneOf(
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(30)
                    .map(s -> "alias_" + s),
                Arbitraries.just((String) null) // alias is optional
            )
        ).as(PlayerProfileData::new);
    }

    @Provide
    Arbitrary<PlayerProfileData> playerProfileDataWithPotentialIssues() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20)
                .map(s -> s.toLowerCase() + "@example.com"),
            Arbitraries.strings().alpha().ofMinLength(8).ofMaxLength(50)
                .map(s -> "hash_" + s),
            Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(50),
            Arbitraries.oneOf(
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(30), // valid alias
                Arbitraries.strings().alpha().ofMinLength(51).ofMaxLength(100), // too long alias
                Arbitraries.just((String) null) // null alias
            ),
            Arbitraries.oneOf(
                Arbitraries.integers().between(0, 1000), // valid trust score
                Arbitraries.integers().between(-100, -1), // negative trust score
                Arbitraries.integers().between(1001, 2000), // too high trust score
                Arbitraries.just((Integer) null) // null trust score (should use default)
            )
        ).as(PlayerProfileData::new);
    }

    // Data class for test data
    static class PlayerProfileData {
        final String athleteEmail;
        final String athletePasswordHash;
        final String athleteNombre;
        final String alias;
        final Integer trustScore;

        PlayerProfileData(String athleteEmail, String athletePasswordHash, String athleteNombre, String alias) {
            this.athleteEmail = athleteEmail;
            this.athletePasswordHash = athletePasswordHash;
            this.athleteNombre = athleteNombre;
            this.alias = alias;
            this.trustScore = null; // Use default
        }

        PlayerProfileData(String athleteEmail, String athletePasswordHash, String athleteNombre, String alias, Integer trustScore) {
            this.athleteEmail = athleteEmail;
            this.athletePasswordHash = athletePasswordHash;
            this.athleteNombre = athleteNombre;
            this.alias = alias;
            this.trustScore = trustScore;
        }
    }
}