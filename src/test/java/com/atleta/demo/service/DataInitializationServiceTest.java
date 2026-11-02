package com.atleta.demo.service;

import com.atleta.demo.entity.Position;
import com.atleta.demo.repository.PositionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DataInitializationServiceTest {
    private final PositionRepository repository = mock(PositionRepository.class);
    private final DataInitializationService initializer = new DataInitializationService();

    @Test
    void initializesTheSameCatalogAsTheMigrationsWhenEmpty() throws Exception {
        ReflectionTestUtils.setField(initializer, "positionRepository", repository);
        when(repository.count()).thenReturn(0L);

        initializer.run(null);

        ArgumentCaptor<Position> positions = ArgumentCaptor.forClass(Position.class);
        verify(repository, times(7)).save(positions.capture());
        assertThat(positions.getAllValues()).extracting(Position::getNombre)
                .containsExactly("Portero", "Defensa", "Mediocampo", "Delantero",
                        "Lateral Derecho", "Lateral Izquierdo", "DT");
    }

    @Test
    void preservesAnExistingCatalog() throws Exception {
        ReflectionTestUtils.setField(initializer, "positionRepository", repository);
        when(repository.count()).thenReturn(1L);

        initializer.run(null);

        verify(repository, never()).save(any());
    }
}
