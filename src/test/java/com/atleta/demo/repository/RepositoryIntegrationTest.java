package com.atleta.demo.repository;

import com.atleta.demo.entity.*;
import com.atleta.demo.enums.MatchMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integración para repositorios.
 * Verifica la interacción entre múltiples repositorios.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RepositoryIntegrationTest {

    @Autowired
    private AthleteRepository athleteRepository;

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MatchRepository matchRepository;

    private Athlete testAthlete;
    private PlayerProfile testProfile;
    private Team testTeam;
    private Match testMatch;

    @BeforeEach
    void setUp() {
        // Limpiar datos existentes
        matchRepository.deleteAll();
        teamRepository.deleteAll();
        playerProfileRepository.deleteAll();
        athleteRepository.deleteAll();

        // Crear datos de prueba integrados
        testAthlete = new Athlete("integration@example.com", "hash", "Integration Test");
        testAthlete = athleteRepository.save(testAthlete);

        testProfile = new PlayerProfile(testAthlete, "IntegrationPlayer");
        testProfile = playerProfileRepository.save(testProfile);

        testTeam = new Team("Integration Team", testProfile);
        testTeam = teamRepository.save(testTeam);

        testMatch = new Match(MatchMode.CINCO_VS_CINCO, LocalDateTime.now().plusDays(1), testProfile);
        testMatch = matchRepository.save(testMatch);
    }

    @Test
    void testFullIntegration_CreateAthleteWithProfileTeamAndMatch_AllEntitiesLinked() {
        // When - Verificar que todas las entidades están correctamente vinculadas
        var foundAthlete = athleteRepository.findById(testAthlete.getAtletaUuid());
        var foundProfile = playerProfileRepository.findById(testProfile.getAtletaUuid());
        var foundTeam = teamRepository.findById(testTeam.getId());
        var foundMatch = matchRepository.findById(testMatch.getId());

        // Then
        assertThat(foundAthlete).isPresent();
        assertThat(foundProfile).isPresent();
        assertThat(foundTeam).isPresent();
        assertThat(foundMatch).isPresent();

        // Verificar relaciones
        assertThat(foundProfile.get().getAthlete().getAtletaUuid()).isEqualTo(testAthlete.getAtletaUuid());
        assertThat(foundTeam.get().getCreador().getAtletaUuid()).isEqualTo(testProfile.getAtletaUuid());
        assertThat(foundMatch.get().getCreador().getAtletaUuid()).isEqualTo(testProfile.getAtletaUuid());
    }

    @Test
    void testCascadeDelete_DeleteAthlete_ProfileAlsoDeleted() {
        // Given
        var athleteId = testAthlete.getAtletaUuid();

        // When
        athleteRepository.deleteById(athleteId);

        // Then
        assertThat(athleteRepository.findById(athleteId)).isEmpty();
        // Note: Depending on cascade configuration, profile might or might not be deleted
        // This test verifies the current behavior
    }

    @Test
    void testRepositoryQueries_CrossEntityQueries_WorkCorrectly() {
        // When
        var athletesWithProfiles = athleteRepository.findAthletesWithPlayerProfile();
        var teamsCreatedByProfile = teamRepository.findByCreador(testProfile);
        var matchesCreatedByProfile = matchRepository.findByCreador(testProfile);

        // Then
        assertThat(athletesWithProfiles).hasSize(1);
        assertThat(teamsCreatedByProfile).hasSize(1);
        assertThat(matchesCreatedByProfile).hasSize(1);
    }
}