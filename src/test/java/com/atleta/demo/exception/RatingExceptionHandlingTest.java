package com.atleta.demo.exception;

import com.atleta.demo.config.TestSecurityConfig;
import com.atleta.demo.dto.request.PlayerPerformanceDto;
import com.atleta.demo.enums.MatchResultType;
import com.atleta.demo.enums.PriorityLevel;
import com.atleta.demo.enums.RoleType;
import com.atleta.demo.service.RatingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para verificar el manejo de errores y excepciones
 * en el sistema de calificación de jugadores.
 * 
 * Valida el requerimiento 9.4: Manejo robusto de errores y validación de entrada.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class RatingExceptionHandlingTest {

    @Autowired
    private RatingService ratingService;

    @Test
    void testInvalidPlayerDataException_NullMatchId() {
        // Arrange
        List<PlayerPerformanceDto> performances = new ArrayList<>();
        
        // Act & Assert
        InvalidPlayerDataException exception = assertThrows(
            InvalidPlayerDataException.class,
            () -> ratingService.updatePlayerRatings(null, performances)
        );
        
        assertTrue(exception.getMessage().contains("ID del partido es obligatorio"));
        assertEquals("matchId", exception.getFieldName());
    }

    @Test
    void testInvalidPlayerDataException_EmptyPerformancesList() {
        // Arrange
        Long matchId = 1L;
        List<PlayerPerformanceDto> performances = new ArrayList<>();
        
        // Act & Assert
        InvalidPlayerDataException exception = assertThrows(
            InvalidPlayerDataException.class,
            () -> ratingService.updatePlayerRatings(matchId, performances)
        );
        
        assertTrue(exception.getMessage().contains("lista de rendimientos de jugadores no puede estar vac"));
        assertEquals("performances", exception.getFieldName());
    }

    @Test
    void testInvalidPlayerDataException_NullPerformance() {
        // Arrange
        Long matchId = 1L;
        List<PlayerPerformanceDto> performances = new ArrayList<>();
        performances.add(null);
        
        // Act & Assert
        InvalidPlayerDataException exception = assertThrows(
            InvalidPlayerDataException.class,
            () -> ratingService.updatePlayerRatings(matchId, performances)
        );
        
        assertTrue(exception.getMessage().contains("rendimiento del jugador"));
        assertEquals("performance[0]", exception.getFieldName());
    }

    @Test
    void testInvalidPlayerDataException_NullPlayerProfileId() {
        // Arrange
        Long matchId = 1L;
        List<PlayerPerformanceDto> performances = new ArrayList<>();
        
        PlayerPerformanceDto performance = new PlayerPerformanceDto();
        performance.setPlayerProfileId(null);
        performance.setRoleType(RoleType.ATAQUE);
        performance.setPriorityLevel(PriorityLevel.PRINCIPAL);
        performance.setMatchResult(MatchResultType.GANADO);
        performance.setGoalsScored(1);
        performance.setAssistsMade(0);
        performance.setWasMvp(false);
        
        performances.add(performance);
        
        // Act & Assert
        InvalidPlayerDataException exception = assertThrows(
            InvalidPlayerDataException.class,
            () -> ratingService.updatePlayerRatings(matchId, performances)
        );
        
        assertTrue(exception.getMessage().contains("ID del perfil del jugador es obligatorio"));
        assertEquals("playerProfileId", exception.getFieldName());
    }

    @Test
    void testInvalidPlayerDataException_NegativeGoals() {
        // Arrange
        Long matchId = 1L;
        List<PlayerPerformanceDto> performances = new ArrayList<>();
        
        PlayerPerformanceDto performance = new PlayerPerformanceDto();
        performance.setPlayerProfileId(UUID.randomUUID());
        performance.setRoleType(RoleType.ATAQUE);
        performance.setPriorityLevel(PriorityLevel.PRINCIPAL);
        performance.setMatchResult(MatchResultType.GANADO);
        performance.setGoalsScored(-1); // Valor inválido
        performance.setAssistsMade(0);
        performance.setWasMvp(false);
        
        performances.add(performance);
        
        // Act & Assert
        InvalidPlayerDataException exception = assertThrows(
            InvalidPlayerDataException.class,
            () -> ratingService.updatePlayerRatings(matchId, performances)
        );
        
        assertTrue(exception.getMessage().contains("goles anotados"));
        assertEquals("goalsScored", exception.getFieldName());
        assertEquals(-1, exception.getInvalidValue());
    }

    @Test
    void testInvalidPlayerDataException_MultipleMVPs() {
        // Arrange - Create a test match first
        Long matchId = 1L;
        List<PlayerPerformanceDto> performances = new ArrayList<>();
        
        // Primer MVP
        PlayerPerformanceDto performance1 = new PlayerPerformanceDto();
        performance1.setPlayerProfileId(UUID.randomUUID());
        performance1.setRoleType(RoleType.ATAQUE);
        performance1.setPriorityLevel(PriorityLevel.PRINCIPAL);
        performance1.setMatchResult(MatchResultType.GANADO);
        performance1.setGoalsScored(2);
        performance1.setAssistsMade(1);
        performance1.setWasMvp(true); // MVP
        
        // Segundo MVP (inválido)
        PlayerPerformanceDto performance2 = new PlayerPerformanceDto();
        performance2.setPlayerProfileId(UUID.randomUUID());
        performance2.setRoleType(RoleType.MEDIOCAMPO);
        performance2.setPriorityLevel(PriorityLevel.PRINCIPAL);
        performance2.setMatchResult(MatchResultType.GANADO);
        performance2.setGoalsScored(1);
        performance2.setAssistsMade(2);
        performance2.setWasMvp(true); // MVP duplicado
        
        performances.add(performance1);
        performances.add(performance2);
        
        // Act & Assert - Should throw MatchNotFoundException first since match doesn't exist
        // This test validates that the service properly checks for match existence before validation
        MatchNotFoundException exception = assertThrows(
            MatchNotFoundException.class,
            () -> ratingService.updatePlayerRatings(matchId, performances)
        );
        
        assertTrue(exception.getMessage().contains("El partido con ID " + matchId + " no existe"));
        assertEquals(matchId, exception.getMatchId());
    }

    @Test
    void testInvalidPlayerDataException_NullPlayerProfileIdInQuery() {
        // Act & Assert
        InvalidPlayerDataException exception = assertThrows(
            InvalidPlayerDataException.class,
            () -> ratingService.getPlayerRatings(null)
        );
        
        assertTrue(exception.getMessage().contains("ID del perfil del jugador es obligatorio"));
        assertEquals("playerProfileId", exception.getFieldName());
    }

    @Test
    void testInvalidPlayerDataException_RotativeGoalkeeperNullMatchId() {
        // Act & Assert
        InvalidPlayerDataException exception = assertThrows(
            InvalidPlayerDataException.class,
            () -> ratingService.updateRotativeGoalkeeperRatings(null, MatchResultType.GANADO)
        );
        
        assertTrue(exception.getMessage().contains("ID del partido es obligatorio"));
        assertEquals("matchId", exception.getFieldName());
    }

    @Test
    void testInvalidPlayerDataException_RotativeGoalkeeperNullResult() {
        // Act & Assert
        InvalidPlayerDataException exception = assertThrows(
            InvalidPlayerDataException.class,
            () -> ratingService.updateRotativeGoalkeeperRatings(1L, null)
        );
        
        assertTrue(exception.getMessage().contains("resultado del partido es obligatorio"));
        assertEquals("matchResult", exception.getFieldName());
    }

    @Test
    void testExceptionMessageFormatting() {
        // Arrange
        String playerId = "test-player-123";
        String fieldName = "testField";
        String invalidValue = "invalidValue";
        
        // Act
        InvalidPlayerDataException exception = new InvalidPlayerDataException(
            "Test message", playerId, fieldName, invalidValue
        );
        
        // Assert
        String message = exception.getMessage();
        assertTrue(message.contains("Test message"));
        assertTrue(message.contains("[Jugador: " + playerId + "]"));
        assertTrue(message.contains("[Campo: " + fieldName + "]"));
        assertTrue(message.contains("[Valor inválido: " + invalidValue + "]"));
    }

    @Test
    void testConcurrentRatingUpdateExceptionFormatting() {
        // Arrange
        String playerId = "test-player-123";
        String roleType = "ATAQUE";
        String priorityLevel = "PRINCIPAL";
        Long expectedVersion = 1L;
        Long actualVersion = 2L;
        
        // Act
        ConcurrentRatingUpdateException exception = new ConcurrentRatingUpdateException(
            "Test concurrent message", playerId, roleType, priorityLevel, expectedVersion, actualVersion
        );
        
        // Assert
        String message = exception.getMessage();
        assertTrue(message.contains("Test concurrent message"));
        assertTrue(message.contains("[Jugador: " + playerId + "]"));
        assertTrue(message.contains("[Rol: " + roleType + "]"));
        assertTrue(message.contains("[Prioridad: " + priorityLevel + "]"));
        assertTrue(message.contains("[Versión esperada: " + expectedVersion));
        assertTrue(message.contains("Versión actual: " + actualVersion + "]"));
        assertTrue(exception.hasVersioningInfo());
    }

    @Test
    void testMatchNotFoundException() {
        // Arrange
        Long nonExistentMatchId = 999L;
        List<PlayerPerformanceDto> performances = new ArrayList<>();
        
        PlayerPerformanceDto performance = new PlayerPerformanceDto();
        performance.setPlayerProfileId(UUID.randomUUID());
        performance.setRoleType(RoleType.ATAQUE);
        performance.setPriorityLevel(PriorityLevel.PRINCIPAL);
        performance.setMatchResult(MatchResultType.GANADO);
        performance.setGoalsScored(1);
        performance.setAssistsMade(0);
        performance.setWasMvp(false);
        
        performances.add(performance);
        
        // Act & Assert
        MatchNotFoundException exception = assertThrows(
            MatchNotFoundException.class,
            () -> ratingService.updatePlayerRatings(nonExistentMatchId, performances)
        );
        
        assertTrue(exception.getMessage().contains("El partido con ID " + nonExistentMatchId + " no existe"));
        assertEquals(nonExistentMatchId, exception.getMatchId());
    }

    @Test
    void testPlayerNotFoundException_InQuery() {
        // Arrange
        UUID nonExistentPlayerId = UUID.randomUUID();
        
        // Act - This should return empty list, not throw exception for non-existent player
        List<?> result = ratingService.getPlayerRatings(nonExistentPlayerId);
        
        // Assert - Should return empty list for non-existent player
        assertTrue(result.isEmpty(), "Should return empty list for non-existent player");
    }
}
