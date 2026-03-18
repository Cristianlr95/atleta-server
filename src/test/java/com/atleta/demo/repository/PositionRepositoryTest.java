package com.atleta.demo.repository;

import com.atleta.demo.entity.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para PositionRepository.
 * Verifica operaciones CRUD y consultas personalizadas.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PositionRepositoryTest {

    @Autowired
    private PositionRepository positionRepository;

    private Position testPosition1;
    private Position testPosition2;
    private Position testPosition3;

    @BeforeEach
    void setUp() {
        // Limpiar datos existentes
        positionRepository.deleteAll();

        // Crear posiciones de prueba
        testPosition1 = new Position();
        testPosition1.setNombre("Portero");

        testPosition2 = new Position();
        testPosition2.setNombre("Defensa");

        testPosition3 = new Position();
        testPosition3.setNombre("Delantero");

        testPosition1 = positionRepository.save(testPosition1);
        testPosition2 = positionRepository.save(testPosition2);
        testPosition3 = positionRepository.save(testPosition3);
    }

    @Test
    void testFindByNombre_ExistingName_ReturnsPosition() {
        // When
        Optional<Position> result = positionRepository.findByNombre("Portero");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getNombre()).isEqualTo("Portero");
    }

    @Test
    void testFindByNombre_NonExistingName_ReturnsEmpty() {
        // When
        Optional<Position> result = positionRepository.findByNombre("Mediocampista");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testExistsByNombre_ExistingName_ReturnsTrue() {
        // When
        boolean exists = positionRepository.existsByNombre("Defensa");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByNombre_NonExistingName_ReturnsFalse() {
        // When
        boolean exists = positionRepository.existsByNombre("Carrilero");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void testFindByNombreContainingIgnoreCase_PartialMatch_ReturnsMatches() {
        // When
        List<Position> results = positionRepository.findByNombreContainingIgnoreCase("def");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getNombre()).isEqualTo("Defensa");
    }

    @Test
    void testFindByNombreContainingIgnoreCase_CaseInsensitive_ReturnsMatches() {
        // When
        List<Position> results = positionRepository.findByNombreContainingIgnoreCase("PORTERO");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getNombre()).isEqualTo("Portero");
    }

    @Test
    void testFindAllOrderByNombre_ReturnsPositionsOrderedAlphabetically() {
        // When
        List<Position> results = positionRepository.findAllOrderByNombre();

        // Then
        assertThat(results).hasSize(3);
        assertThat(results).extracting(Position::getNombre)
                .containsExactly("Defensa", "Delantero", "Portero");
    }

    @Test
    void testFindPositionsInUse_WithoutPlayerPositions_ReturnsEmpty() {
        // When
        List<Position> results = positionRepository.findPositionsInUse();

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void testFindPositionsNotInUse_WithoutPlayerPositions_ReturnsAllPositions() {
        // When
        List<Position> results = positionRepository.findPositionsNotInUse();

        // Then
        assertThat(results).hasSize(3);
        assertThat(results).extracting(Position::getNombre)
                .containsExactlyInAnyOrder("Portero", "Defensa", "Delantero");
    }

    @Test
    void testCountPlayersByPositionId_WithoutPlayers_ReturnsZero() {
        // When
        long count = positionRepository.countPlayersByPositionId(testPosition1.getId());

        // Then
        assertThat(count).isEqualTo(0);
    }

    @Test
    void testFindPositionsOrderByPopularity_WithoutPlayers_ReturnsAllPositions() {
        // When
        List<Position> results = positionRepository.findPositionsOrderByPopularity();

        // Then
        assertThat(results).hasSize(3);
        // All positions should have 0 players, so order might vary
        assertThat(results).extracting(Position::getNombre)
                .containsExactlyInAnyOrder("Portero", "Defensa", "Delantero");
    }

    @Test
    void testSaveAndFindById_ValidPosition_SavesAndRetrievesCorrectly() {
        // Given
        Position newPosition = new Position();
        newPosition.setNombre("Mediocampista");

        // When
        Position saved = positionRepository.save(newPosition);
        Optional<Position> found = positionRepository.findById(saved.getId());

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getNombre()).isEqualTo("Mediocampista");
    }

    @Test
    void testDeleteById_ExistingPosition_DeletesSuccessfully() {
        // Given
        Long positionId = testPosition1.getId();

        // When
        positionRepository.deleteById(positionId);

        // Then
        Optional<Position> found = positionRepository.findById(positionId);
        assertThat(found).isEmpty();
        assertThat(positionRepository.count()).isEqualTo(2);
    }

    @Test
    void testFindAll_ReturnsAllPositions() {
        // When
        List<Position> results = positionRepository.findAll();

        // Then
        assertThat(results).hasSize(3);
        assertThat(results).extracting(Position::getNombre)
                .containsExactlyInAnyOrder("Portero", "Defensa", "Delantero");
    }

    @Test
    void testUpdate_ExistingPosition_UpdatesSuccessfully() {
        // Given
        testPosition1.setNombre("Arquero");

        // When
        Position updated = positionRepository.save(testPosition1);

        // Then
        assertThat(updated.getNombre()).isEqualTo("Arquero");
        
        Optional<Position> found = positionRepository.findById(testPosition1.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getNombre()).isEqualTo("Arquero");
    }

    @Test
    void testUniqueConstraint_DuplicateName_ShouldHandleAppropriately() {
        // Given
        Position duplicatePosition = new Position();
        duplicatePosition.setNombre("Portero"); // Same name as testPosition1

        // When/Then
        // Note: This test depends on database constraints
        // If unique constraint exists, this should throw an exception
        // If not, it will save successfully
        Position saved = positionRepository.save(duplicatePosition);
        assertThat(saved.getId()).isNotNull();
        
        // Verify we now have 4 positions (including duplicate)
        assertThat(positionRepository.count()).isEqualTo(4);
    }
}