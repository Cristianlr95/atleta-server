package com.atleta.demo.service;

import com.atleta.demo.dto.request.PlayerPerformanceDto;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.PlayerRating;
import com.atleta.demo.entity.RatingHistory;
import com.atleta.demo.enums.MatchResultType;
import com.atleta.demo.enums.PriorityLevel;
import com.atleta.demo.enums.RoleType;
import com.atleta.demo.repository.MatchRepository;
import com.atleta.demo.repository.PlayerPositionRepository;
import com.atleta.demo.repository.PlayerProfileRepository;
import com.atleta.demo.repository.PlayerRatingRepository;
import com.atleta.demo.repository.RatingHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para RatingService.
 * Verifica la funcionalidad básica del servicio de calificaciones.
 */
@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingCalculationEngine calculationEngine;
    
    @Mock
    private PlayerRatingRepository playerRatingRepository;
    
    @Mock
    private RatingHistoryRepository ratingHistoryRepository;
    
    @Mock
    private PlayerProfileRepository playerProfileRepository;

    @Mock
    private PlayerPositionRepository playerPositionRepository;
    
    @Mock
    private MatchRepository matchRepository;

    private RatingService ratingService;

    @BeforeEach
    void setUp() {
        ratingService = new RatingService(
                calculationEngine,
                playerRatingRepository,
                ratingHistoryRepository,
                playerProfileRepository,
                playerPositionRepository,
                matchRepository
        );
    }

    @Test
    void testUpdatePlayerRatings_ValidInput_Success() {
        // Arrange
        Long matchId = 1L;
        UUID playerId = UUID.randomUUID();
        
        PlayerPerformanceDto performance = new PlayerPerformanceDto(
                playerId,
                RoleType.ATAQUE,
                PriorityLevel.PRINCIPAL,
                2, // goals
                1, // assists
                null, // goals conceded (not applicable for ATAQUE)
                false, // not MVP
                MatchResultType.GANADO
        );
        
        Match match = new Match();
        match.setId(matchId);
        
        PlayerProfile playerProfile = new PlayerProfile();
        playerProfile.setAtletaUuid(playerId);
        
        PlayerRating existingRating = new PlayerRating(playerProfile, RoleType.ATAQUE, PriorityLevel.PRINCIPAL);
        existingRating.setCurrentRating(BigDecimal.valueOf(75.0));
        
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(playerRatingRepository.findByPlayerProfileIdAndRoleTypeAndPriorityLevel(playerId, RoleType.ATAQUE, PriorityLevel.PRINCIPAL))
                .thenReturn(Optional.of(existingRating));
        when(calculationEngine.calculateNewRating(any())).thenReturn(BigDecimal.valueOf(78.5));
        when(playerRatingRepository.save(any())).thenReturn(existingRating);
        when(ratingHistoryRepository.save(any())).thenReturn(new RatingHistory());

        // Act
        assertDoesNotThrow(() -> ratingService.updatePlayerRatings(matchId, List.of(performance)));

        // Assert
        verify(matchRepository).findById(matchId);
        verify(calculationEngine).calculateNewRating(any());
        verify(playerRatingRepository).save(any());
        verify(ratingHistoryRepository).save(any());
    }

    @Test
    void testUpdatePlayerRatings_NullMatchId_ThrowsException() {
        // Arrange
        PlayerPerformanceDto performance = new PlayerPerformanceDto();
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ratingService.updatePlayerRatings(null, List.of(performance))
        );
        
        assertEquals("El ID del partido es obligatorio", exception.getMessage());
    }

    @Test
    void testUpdatePlayerRatings_EmptyPerformances_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ratingService.updatePlayerRatings(1L, List.of())
        );
        
        assertEquals("La lista de rendimientos de jugadores no puede estar vacía", exception.getMessage());
    }

    @Test
    void testUpdatePlayerRatings_MultipleMVPs_ThrowsException() {
        // Arrange
        Long matchId = 1L;
        UUID playerId1 = UUID.randomUUID();
        UUID playerId2 = UUID.randomUUID();
        
        PlayerPerformanceDto performance1 = new PlayerPerformanceDto(
                playerId1, RoleType.ATAQUE, PriorityLevel.PRINCIPAL,
                1, 0, null, true, MatchResultType.GANADO
        );
        
        PlayerPerformanceDto performance2 = new PlayerPerformanceDto(
                playerId2, RoleType.MEDIOCAMPO, PriorityLevel.PRINCIPAL,
                0, 1, null, true, MatchResultType.GANADO
        );
        
        Match match = new Match();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ratingService.updatePlayerRatings(matchId, List.of(performance1, performance2))
        );
        
        assertTrue(exception.getMessage().contains("Solo puede haber un MVP por partido"));
    }

    @Test
    void testGetPlayerRatings_ValidPlayerId_ReturnsRatings() {
        // Arrange
        UUID playerId = UUID.randomUUID();
        PlayerRating rating1 = new PlayerRating();
        PlayerRating rating2 = new PlayerRating();
        List<PlayerRating> expectedRatings = List.of(rating1, rating2);
        
        when(playerRatingRepository.findByPlayerProfileId(playerId)).thenReturn(expectedRatings);

        // Act
        List<PlayerRating> actualRatings = ratingService.getPlayerRatings(playerId);

        // Assert
        assertEquals(expectedRatings, actualRatings);
        verify(playerRatingRepository).findByPlayerProfileId(playerId);
    }

    @Test
    void testGetPlayerRatings_NullPlayerId_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ratingService.getPlayerRatings(null)
        );
        
        assertEquals("El ID del perfil del jugador es obligatorio", exception.getMessage());
    }

    @Test
    void testGetRatingHistory_ValidPlayerId_ReturnsHistory() {
        // Arrange
        UUID playerId = UUID.randomUUID();
        RatingHistory history1 = new RatingHistory();
        RatingHistory history2 = new RatingHistory();
        List<RatingHistory> expectedHistory = List.of(history1, history2);
        
        when(ratingHistoryRepository.findByPlayerProfileId(playerId)).thenReturn(expectedHistory);

        // Act
        List<RatingHistory> actualHistory = ratingService.getRatingHistory(playerId);

        // Assert
        assertEquals(expectedHistory, actualHistory);
        verify(ratingHistoryRepository).findByPlayerProfileId(playerId);
    }

    @Test
    void testUpdateRotativeGoalkeeperRatings_ValidInput_Success() {
        // Arrange
        Long matchId = 1L;
        MatchResultType matchResult = MatchResultType.GANADO;
        
        Match match = new Match();
        match.setId(matchId);
        
        PlayerProfile playerProfile = new PlayerProfile();
        playerProfile.setAtletaUuid(UUID.randomUUID());
        
        PlayerRating goalkeeperRating = new PlayerRating(playerProfile, RoleType.ARQUERO, PriorityLevel.PRINCIPAL);
        goalkeeperRating.setCurrentRating(BigDecimal.valueOf(70.0));
        
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(playerRatingRepository.findByRoleType(RoleType.ARQUERO)).thenReturn(List.of(goalkeeperRating));
        when(calculationEngine.calculateRotativeGoalkeeperRating(any())).thenReturn(BigDecimal.valueOf(72.0));
        when(playerRatingRepository.save(any())).thenReturn(goalkeeperRating);
        when(ratingHistoryRepository.save(any())).thenReturn(new RatingHistory());

        // Act
        assertDoesNotThrow(() -> ratingService.updateRotativeGoalkeeperRatings(matchId, matchResult));

        // Assert
        verify(matchRepository).findById(matchId);
        verify(playerRatingRepository).findByRoleType(RoleType.ARQUERO);
        verify(calculationEngine).calculateRotativeGoalkeeperRating(any());
        verify(playerRatingRepository).save(any());
        verify(ratingHistoryRepository).save(any());
    }

    @Test
    void testUpdateRotativeGoalkeeperRatings_NullMatchId_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ratingService.updateRotativeGoalkeeperRatings(null, MatchResultType.GANADO)
        );
        
        assertEquals("El ID del partido es obligatorio", exception.getMessage());
    }

    @Test
    void testUpdateRotativeGoalkeeperRatings_NullMatchResult_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ratingService.updateRotativeGoalkeeperRatings(1L, null)
        );
        
        assertEquals("El resultado del partido es obligatorio", exception.getMessage());
    }
}
