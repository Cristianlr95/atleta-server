package com.atleta.demo.repository;

import com.atleta.demo.entity.Athlete;
import com.atleta.demo.entity.PlayerProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para PlayerProfileRepository.
 * Verifica operaciones CRUD y consultas personalizadas.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlayerProfileRepositoryTest {

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private AthleteRepository athleteRepository;

    private Athlete testAthlete1;
    private Athlete testAthlete2;
    private Athlete testAthlete3;
    private PlayerProfile testProfile1;
    private PlayerProfile testProfile2;
    private PlayerProfile testProfile3;

    @BeforeEach
    void setUp() {
        // Limpiar datos existentes
        playerProfileRepository.deleteAll();
        athleteRepository.deleteAll();
        
        // Crear atletas de prueba
        testAthlete1 = new Athlete("test1@example.com", "hash1", "Juan Pérez");
        testAthlete2 = new Athlete("test2@example.com", "hash2", "María García");
        testAthlete3 = new Athlete("test3@example.com", "hash3", "Carlos López");

        testAthlete1 = athleteRepository.save(testAthlete1);
        testAthlete2 = athleteRepository.save(testAthlete2);
        testAthlete3 = athleteRepository.save(testAthlete3);

        // Crear perfiles de jugador
        testProfile1 = new PlayerProfile(testAthlete1, "JuanP");
        testProfile1.setTrustScore(150);
        
        testProfile2 = new PlayerProfile(testAthlete2, "MariaG");
        testProfile2.setTrustScore(80);
        
        testProfile3 = new PlayerProfile(testAthlete3, "CarlosL");
        testProfile3.setTrustScore(120);

        testProfile1 = playerProfileRepository.save(testProfile1);
        testProfile2 = playerProfileRepository.save(testProfile2);
        testProfile3 = playerProfileRepository.save(testProfile3);
    }

    @Test
    void testFindByAlias_ExistingAlias_ReturnsProfile() {
        // When
        Optional<PlayerProfile> result = playerProfileRepository.findByAlias("JuanP");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getAlias()).isEqualTo("JuanP");
        assertThat(result.get().getAthlete().getNombre()).isEqualTo("Juan Pérez");
    }

    @Test
    void testFindByAlias_NonExistingAlias_ReturnsEmpty() {
        // When
        Optional<PlayerProfile> result = playerProfileRepository.findByAlias("NonExistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testExistsByAlias_ExistingAlias_ReturnsTrue() {
        // When
        boolean exists = playerProfileRepository.existsByAlias("MariaG");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByAlias_NonExistingAlias_ReturnsFalse() {
        // When
        boolean exists = playerProfileRepository.existsByAlias("NonExistent");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void testFindByTrustScoreBetween_ValidRange_ReturnsProfilesInRange() {
        // When
        List<PlayerProfile> results = playerProfileRepository.findByTrustScoreBetween(100, 150);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(PlayerProfile::getAlias)
                .containsExactlyInAnyOrder("JuanP", "CarlosL");
    }

    @Test
    void testFindByTrustScoreBetween_NoMatches_ReturnsEmpty() {
        // When
        List<PlayerProfile> results = playerProfileRepository.findByTrustScoreBetween(200, 300);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void testFindByTrustScoreGreaterThanEqual_ValidThreshold_ReturnsMatchingProfiles() {
        // When
        List<PlayerProfile> results = playerProfileRepository.findByTrustScoreGreaterThanEqual(120);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(PlayerProfile::getAlias)
                .containsExactlyInAnyOrder("JuanP", "CarlosL");
        // Verificar que están ordenados por trust score descendente
        assertThat(results.get(0).getTrustScore()).isGreaterThanOrEqualTo(results.get(1).getTrustScore());
    }

    @Test
    void testFindByAliasContainingIgnoreCase_PartialMatch_ReturnsMatches() {
        // When
        List<PlayerProfile> results = playerProfileRepository.findByAliasContainingIgnoreCase("juan");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAlias()).isEqualTo("JuanP");
    }

    @Test
    void testFindByAliasContainingIgnoreCase_CaseInsensitive_ReturnsMatches() {
        // When
        List<PlayerProfile> results = playerProfileRepository.findByAliasContainingIgnoreCase("MARIA");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAlias()).isEqualTo("MariaG");
    }

    @Test
    void testFindByCreatedAtAfter_RecentDate_ReturnsRecentProfiles() {
        // Given
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        // When
        List<PlayerProfile> results = playerProfileRepository.findByCreatedAtAfter(oneHourAgo);

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void testFindByCreatedAtAfter_FutureDate_ReturnsEmpty() {
        // Given
        LocalDateTime futureDate = LocalDateTime.now().plusHours(1);

        // When
        List<PlayerProfile> results = playerProfileRepository.findByCreatedAtAfter(futureDate);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void testFindProfilesWithPositions_WithoutPositions_ReturnsEmpty() {
        // When
        List<PlayerProfile> results = playerProfileRepository.findProfilesWithPositions();

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void testFindProfilesWithoutPositions_WithoutPositions_ReturnsAllProfiles() {
        // When
        List<PlayerProfile> results = playerProfileRepository.findProfilesWithoutPositions();

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void testFindByAthleteNombreContainingIgnoreCase_PartialMatch_ReturnsMatches() {
        // When
        List<PlayerProfile> results = playerProfileRepository.findByAthleteNombreContainingIgnoreCase("juan");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAthlete().getNombre()).isEqualTo("Juan Pérez");
    }

    @Test
    void testGetTrustScoreStatistics_ReturnsCorrectStatistics() {
        // When
        Object[] stats = playerProfileRepository.getTrustScoreStatistics();

        // Then
        assertThat(stats).hasSize(3);
        assertThat(stats[0]).isEqualTo(80); // MIN
        assertThat(stats[1]).isEqualTo(150); // MAX
        assertThat((Double) stats[2]).isEqualTo(116.66666666666667); // AVG
    }

    @Test
    void testCountByTrustScoreBetween_ValidRange_ReturnsCorrectCount() {
        // When
        long count = playerProfileRepository.countByTrustScoreBetween(100, 150);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testCountByTrustScoreBetween_NoMatches_ReturnsZero() {
        // When
        long count = playerProfileRepository.countByTrustScoreBetween(200, 300);

        // Then
        assertThat(count).isEqualTo(0);
    }

    @Test
    void testSaveAndFindById_ValidProfile_SavesAndRetrievesCorrectly() {
        // Given
        Athlete newAthlete = new Athlete("new@example.com", "newhash", "Nuevo Atleta");
        newAthlete = athleteRepository.save(newAthlete);
        
        PlayerProfile newProfile = new PlayerProfile(newAthlete, "NuevoP");
        newProfile.setTrustScore(90);

        // When
        PlayerProfile saved = playerProfileRepository.save(newProfile);
        Optional<PlayerProfile> found = playerProfileRepository.findById(saved.getAtletaUuid());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getAlias()).isEqualTo("NuevoP");
        assertThat(found.get().getTrustScore()).isEqualTo(90);
    }

    @Test
    void testDeleteById_ExistingProfile_DeletesSuccessfully() {
        // Given
        var profileId = testProfile1.getAtletaUuid();

        // When
        playerProfileRepository.deleteById(profileId);

        // Then
        Optional<PlayerProfile> found = playerProfileRepository.findById(profileId);
        assertThat(found).isEmpty();
        assertThat(playerProfileRepository.count()).isEqualTo(2);
    }

    @Test
    void testFindAll_ReturnsAllProfiles() {
        // When
        List<PlayerProfile> results = playerProfileRepository.findAll();

        // Then
        assertThat(results).hasSize(3);
        assertThat(results).extracting(PlayerProfile::getAlias)
                .containsExactlyInAnyOrder("JuanP", "MariaG", "CarlosL");
    }

    @Test
    void testDefaultTrustScore_NewProfile_HasDefaultValue() {
        // Given
        Athlete newAthlete = new Athlete("default@example.com", "hash", "Default User");
        newAthlete = athleteRepository.save(newAthlete);
        
        PlayerProfile newProfile = new PlayerProfile(newAthlete, "DefaultP");

        // When
        PlayerProfile saved = playerProfileRepository.save(newProfile);

        // Then
        assertThat(saved.getTrustScore()).isEqualTo(100);
    }
}