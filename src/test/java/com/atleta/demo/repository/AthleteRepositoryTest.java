package com.atleta.demo.repository;

import com.atleta.demo.entity.Athlete;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para AthleteRepository.
 * Verifica operaciones CRUD y consultas personalizadas.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AthleteRepositoryTest {

    @Autowired
    private AthleteRepository athleteRepository;

    private Athlete testAthlete1;
    private Athlete testAthlete2;
    private Athlete testAthlete3;
    private long initialAthleteCount;

    @BeforeEach
    void setUp() {
        initialAthleteCount = athleteRepository.countAllAthletes();
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        testAthlete1 = athleteRepository.save(new Athlete(
                "repo-" + suffix + "-1@example.com",
                "hash1",
                "Repo Juan " + suffix
        ));
        testAthlete2 = athleteRepository.save(new Athlete(
                "repo-" + suffix + "-2@example.com",
                "hash2",
                "Repo Maria " + suffix
        ));
        testAthlete3 = athleteRepository.save(new Athlete(
                "repo-" + suffix + "-3@example.com",
                "hash3",
                "Repo Carlos " + suffix
        ));
    }

    @Test
    void testFindByEmail_ExistingEmail_ReturnsAthlete() {
        Optional<Athlete> result = athleteRepository.findByEmail(testAthlete1.getEmail());

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(testAthlete1.getEmail());
        assertThat(result.get().getNombre()).isEqualTo(testAthlete1.getNombre());
    }

    @Test
    void testFindByEmail_NonExistingEmail_ReturnsEmpty() {
        Optional<Athlete> result = athleteRepository.findByEmail("nonexistent@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void testExistsByEmail_ExistingEmail_ReturnsTrue() {
        boolean exists = athleteRepository.existsByEmail(testAthlete2.getEmail());

        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByEmail_NonExistingEmail_ReturnsFalse() {
        boolean exists = athleteRepository.existsByEmail("nonexistent@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    void testFindByNombreContainingIgnoreCase_PartialMatch_ReturnsMatches() {
        List<Athlete> results = athleteRepository.findByNombreContainingIgnoreCase("Repo Juan");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getNombre()).isEqualTo(testAthlete1.getNombre());
    }

    @Test
    void testFindByNombreContainingIgnoreCase_CaseInsensitive_ReturnsMatches() {
        List<Athlete> results = athleteRepository.findByNombreContainingIgnoreCase("repo maria");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getNombre()).isEqualTo(testAthlete2.getNombre());
    }

    @Test
    void testFindByCreatedAtAfter_RecentDate_ReturnsRecentAthletes() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        List<Athlete> results = athleteRepository.findByCreatedAtAfter(oneHourAgo);

        assertThat(results).extracting(Athlete::getEmail)
                .contains(testAthlete1.getEmail(), testAthlete2.getEmail(), testAthlete3.getEmail());
    }

    @Test
    void testFindByCreatedAtAfter_FutureDate_ReturnsEmpty() {
        LocalDateTime futureDate = LocalDateTime.now().plusHours(1);

        List<Athlete> results = athleteRepository.findByCreatedAtAfter(futureDate);

        assertThat(results).isEmpty();
    }

    @Test
    void testFindAthletesWithPlayerProfile_WithoutProfiles_ReturnsNoNewAthletes() {
        List<Athlete> results = athleteRepository.findAthletesWithPlayerProfile();

        assertThat(results).extracting(Athlete::getEmail)
                .doesNotContain(testAthlete1.getEmail(), testAthlete2.getEmail(), testAthlete3.getEmail());
    }

    @Test
    void testFindAthletesWithPlayerProfile_WithProfiles_ReturnsExistingProfiledAthletes() {
        List<Athlete> results = athleteRepository.findAthletesWithPlayerProfile();

        assertThat(results).extracting(Athlete::getEmail)
                .doesNotContain(testAthlete1.getEmail(), testAthlete2.getEmail(), testAthlete3.getEmail());
    }

    @Test
    void testFindAthletesWithoutPlayerProfile_WithoutProfiles_ReturnsNewAthletes() {
        List<Athlete> results = athleteRepository.findAthletesWithoutPlayerProfile();

        assertThat(results).extracting(Athlete::getEmail)
                .contains(testAthlete1.getEmail(), testAthlete2.getEmail(), testAthlete3.getEmail());
    }

    @Test
    void testFindAthletesWithoutPlayerProfile_WithSomeProfiles_ReturnsAthletesWithoutProfiles() {
        List<Athlete> results = athleteRepository.findAthletesWithoutPlayerProfile();

        assertThat(results).extracting(Athlete::getEmail)
                .contains(testAthlete1.getEmail(), testAthlete2.getEmail(), testAthlete3.getEmail());
    }

    @Test
    void testCountAllAthletes_ReturnsCorrectCount() {
        long count = athleteRepository.countAllAthletes();

        assertThat(count).isEqualTo(initialAthleteCount + 3);
    }

    @Test
    void testFindByCreatedAtBetween_ValidRange_ReturnsAthletesInRange() {
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        List<Athlete> results = athleteRepository.findByCreatedAtBetween(start, end);

        assertThat(results).extracting(Athlete::getEmail)
                .contains(testAthlete1.getEmail(), testAthlete2.getEmail(), testAthlete3.getEmail());
    }

    @Test
    void testFindByCreatedAtBetween_InvalidRange_ReturnsEmpty() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        List<Athlete> results = athleteRepository.findByCreatedAtBetween(start, end);

        assertThat(results).isEmpty();
    }

    @Test
    void testSaveAndFindById_ValidAthlete_SavesAndRetrievesCorrectly() {
        Athlete newAthlete = new Athlete("new@example.com", "newhash", "Nuevo Atleta");

        Athlete saved = athleteRepository.save(newAthlete);
        Optional<Athlete> found = athleteRepository.findById(saved.getAtletaUuid());

        assertThat(saved.getAtletaUuid()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("new@example.com");
        assertThat(found.get().getNombre()).isEqualTo("Nuevo Atleta");
    }

    @Test
    void testDeleteById_ExistingAthlete_DeletesSuccessfully() {
        UUID athleteId = testAthlete1.getAtletaUuid();

        athleteRepository.deleteById(athleteId);

        Optional<Athlete> found = athleteRepository.findById(athleteId);
        assertThat(found).isEmpty();
        assertThat(athleteRepository.count()).isEqualTo(initialAthleteCount + 2);
    }

    @Test
    void testFindAll_ReturnsAllAthletes() {
        List<Athlete> results = athleteRepository.findAll();

        assertThat(results).extracting(Athlete::getEmail)
                .contains(testAthlete1.getEmail(), testAthlete2.getEmail(), testAthlete3.getEmail());
    }
}
