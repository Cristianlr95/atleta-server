package com.atleta.demo.service;

import com.atleta.demo.dto.request.UpdateTrustScoreRequest;
import com.atleta.demo.dto.response.TrustLogResponse;
import com.atleta.demo.dto.response.PlayerProfileResponse;
import com.atleta.demo.entity.*;
import com.atleta.demo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentMatchers;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para TrustScoreService.
 * Valida la lógica de negocio para cálculo y actualización de confianza.
 */
@ExtendWith(MockitoExtension.class)
class TrustScoreServiceTest {

    @Mock
    private TrustLogRepository trustLogRepository;

    @Mock
    private PlayerProfileRepository playerProfileRepository;

    @Mock
    private MatchRepository matchRepository;

    private TrustScoreService trustScoreService;

    private UpdateTrustScoreRequest validRequest;
    private PlayerProfile samplePlayer;
    private PlayerProfile changedByPlayer;
    private Match sampleMatch;
    private TrustLog sampleTrustLog;

    @BeforeEach
    void setUp() {
        trustScoreService = new TrustScoreService(
                trustLogRepository,
                playerProfileRepository,
                matchRepository
        );

        UUID playerId = UUID.randomUUID();
        UUID changedById = UUID.randomUUID();
        
        samplePlayer = new PlayerProfile();
        samplePlayer.setAtletaUuid(playerId);
        samplePlayer.setAlias("TestPlayer");
        samplePlayer.setTrustScore(100);

        changedByPlayer = new PlayerProfile();
        changedByPlayer.setAtletaUuid(changedById);
        changedByPlayer.setAlias("AdminPlayer");
        changedByPlayer.setTrustScore(150);

        sampleMatch = new Match();
        sampleMatch.setId(1L);

        validRequest = new UpdateTrustScoreRequest();
        validRequest.setPlayerUuid(playerId);
        validRequest.setCambio(10);
        validRequest.setMotivo("Buen comportamiento en el partido");
        validRequest.setMatchId(1L);

        sampleTrustLog = new TrustLog();
        sampleTrustLog.setId(1L);
        sampleTrustLog.setPlayer(samplePlayer);
        sampleTrustLog.setMatch(sampleMatch);
        sampleTrustLog.setCambio(10);
        sampleTrustLog.setTrustScoreAnterior(100);
        sampleTrustLog.setTrustScoreNuevo(110);
        sampleTrustLog.setMotivo("Buen comportamiento en el partido");
        sampleTrustLog.setChangedBy(changedByPlayer);
    }

    @Test
    void updateTrustScore_ValidRequest_ShouldUpdateScore() {
        // Arrange
        UUID changedByUuid = changedByPlayer.getAtletaUuid();
        when(playerProfileRepository.findById(validRequest.getPlayerUuid())).thenReturn(Optional.of(samplePlayer));
        when(playerProfileRepository.findById(changedByUuid)).thenReturn(Optional.of(changedByPlayer));
        when(matchRepository.findById(validRequest.getMatchId())).thenReturn(Optional.of(sampleMatch));
        when(trustLogRepository.save(any(TrustLog.class))).thenReturn(sampleTrustLog);
        when(playerProfileRepository.save(any(PlayerProfile.class))).thenReturn(samplePlayer);

        // Act
        TrustLogResponse response = trustScoreService.updateTrustScore(validRequest, changedByUuid);

        // Assert
        assertNotNull(response);
        assertEquals(sampleTrustLog.getCambio(), response.getCambio());
        assertEquals(sampleTrustLog.getMotivo(), response.getMotivo());

        verify(playerProfileRepository).findById(validRequest.getPlayerUuid());
        verify(playerProfileRepository).findById(changedByUuid);
        verify(matchRepository).findById(validRequest.getMatchId());
        verify(trustLogRepository).save(any(TrustLog.class));
        verify(playerProfileRepository).save(samplePlayer);
    }

    @Test
    void updateTrustScore_PlayerNotFound_ShouldThrowException() {
        // Arrange
        UUID changedByUuid = changedByPlayer.getAtletaUuid();
        when(playerProfileRepository.findById(validRequest.getPlayerUuid())).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> trustScoreService.updateTrustScore(validRequest, changedByUuid)
        );

        assertEquals("Jugador no encontrado: " + validRequest.getPlayerUuid(), exception.getMessage());
        verify(playerProfileRepository).findById(validRequest.getPlayerUuid());
        verify(trustLogRepository, never()).save(any(TrustLog.class));
        verify(playerProfileRepository, never()).save(any(PlayerProfile.class));
    }

    @Test
    void updateTrustScore_ChangedByNotFound_ShouldThrowException() {
        // Arrange
        UUID nonExistingChangedBy = UUID.randomUUID();
        when(playerProfileRepository.findById(validRequest.getPlayerUuid())).thenReturn(Optional.of(samplePlayer));
        when(playerProfileRepository.findById(nonExistingChangedBy)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> trustScoreService.updateTrustScore(validRequest, nonExistingChangedBy)
        );

        assertEquals("Usuario que realiza el cambio no encontrado: " + nonExistingChangedBy, exception.getMessage());
        verify(playerProfileRepository).findById(validRequest.getPlayerUuid());
        verify(playerProfileRepository).findById(nonExistingChangedBy);
        verify(trustLogRepository, never()).save(any(TrustLog.class));
    }

    @Test
    void updateTrustScore_MatchNotFound_ShouldThrowException() {
        // Arrange
        UUID changedByUuid = changedByPlayer.getAtletaUuid();
        when(playerProfileRepository.findById(validRequest.getPlayerUuid())).thenReturn(Optional.of(samplePlayer));
        when(playerProfileRepository.findById(changedByUuid)).thenReturn(Optional.of(changedByPlayer));
        when(matchRepository.findById(validRequest.getMatchId())).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> trustScoreService.updateTrustScore(validRequest, changedByUuid)
        );

        assertEquals("Partido no encontrado: " + validRequest.getMatchId(), exception.getMessage());
        verify(matchRepository).findById(validRequest.getMatchId());
        verify(trustLogRepository, never()).save(any(TrustLog.class));
    }

    @Test
    void updateTrustScore_WithoutChangedBy_ShouldWork() {
        // Arrange
        when(playerProfileRepository.findById(validRequest.getPlayerUuid())).thenReturn(Optional.of(samplePlayer));
        when(matchRepository.findById(validRequest.getMatchId())).thenReturn(Optional.of(sampleMatch));
        when(trustLogRepository.save(any(TrustLog.class))).thenReturn(sampleTrustLog);
        when(playerProfileRepository.save(any(PlayerProfile.class))).thenReturn(samplePlayer);

        // Act
        TrustLogResponse response = trustScoreService.updateTrustScore(validRequest, null);

        // Assert
        assertNotNull(response);
        assertEquals(sampleTrustLog.getCambio(), response.getCambio());

        verify(playerProfileRepository).findById(validRequest.getPlayerUuid());
        verify(playerProfileRepository, never()).findById(ArgumentMatchers.isNull());
        verify(matchRepository).findById(validRequest.getMatchId());
        verify(trustLogRepository).save(any(TrustLog.class));
        verify(playerProfileRepository).save(samplePlayer);
    }

    @Test
    void updateTrustScoreAutomatic_ValidRequest_ShouldUpdateScore() {
        // Arrange
        UUID playerUuid = samplePlayer.getAtletaUuid();
        Long matchId = 1L;
        Integer cambio = -5;
        String motivo = "Llegada tardía";

        when(playerProfileRepository.findById(playerUuid)).thenReturn(Optional.of(samplePlayer));
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(sampleMatch));
        when(trustLogRepository.save(any(TrustLog.class))).thenReturn(sampleTrustLog);
        when(playerProfileRepository.save(any(PlayerProfile.class))).thenReturn(samplePlayer);

        // Act
        TrustLogResponse response = trustScoreService.updateTrustScoreAutomatic(playerUuid, matchId, cambio, motivo);

        // Assert
        assertNotNull(response);
        assertEquals(sampleTrustLog.getCambio(), response.getCambio());
        assertEquals(sampleTrustLog.getMotivo(), response.getMotivo());

        verify(playerProfileRepository).findById(playerUuid);
        verify(matchRepository).findById(matchId);
        verify(trustLogRepository).save(any(TrustLog.class));
        verify(playerProfileRepository).save(samplePlayer);
    }

    @Test
    void getTrustHistory_ValidPlayer_ShouldReturnHistory() {
        // Arrange
        UUID playerUuid = samplePlayer.getAtletaUuid();
        List<TrustLog> trustLogs = Arrays.asList(sampleTrustLog);
        
        when(playerProfileRepository.findById(playerUuid)).thenReturn(Optional.of(samplePlayer));
        when(trustLogRepository.findByPlayerOrderByCreatedAtDesc(samplePlayer)).thenReturn(trustLogs);

        // Act
        List<TrustLogResponse> responses = trustScoreService.getTrustHistory(playerUuid);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(sampleTrustLog.getCambio(), responses.get(0).getCambio());
        assertEquals(sampleTrustLog.getMotivo(), responses.get(0).getMotivo());

        verify(playerProfileRepository).findById(playerUuid);
        verify(trustLogRepository).findByPlayerOrderByCreatedAtDesc(samplePlayer);
    }

    @Test
    void getTrustChangesByMatch_ValidMatch_ShouldReturnChanges() {
        // Arrange
        Long matchId = 1L;
        List<TrustLog> trustLogs = Arrays.asList(sampleTrustLog);
        
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(sampleMatch));
        when(trustLogRepository.findByMatch(sampleMatch)).thenReturn(trustLogs);

        // Act
        List<TrustLogResponse> responses = trustScoreService.getTrustChangesByMatch(matchId);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(sampleTrustLog.getCambio(), responses.get(0).getCambio());

        verify(matchRepository).findById(matchId);
        verify(trustLogRepository).findByMatch(sampleMatch);
    }

    @Test
    void getPositiveTrustChanges_ValidPlayer_ShouldReturnPositiveChanges() {
        // Arrange
        UUID playerUuid = samplePlayer.getAtletaUuid();
        List<TrustLog> positiveLogs = Arrays.asList(sampleTrustLog);
        
        when(playerProfileRepository.findById(playerUuid)).thenReturn(Optional.of(samplePlayer));
        when(trustLogRepository.findPositiveChangesByPlayer(samplePlayer)).thenReturn(positiveLogs);

        // Act
        List<TrustLogResponse> responses = trustScoreService.getPositiveTrustChanges(playerUuid);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertTrue(responses.get(0).getCambio() > 0);

        verify(playerProfileRepository).findById(playerUuid);
        verify(trustLogRepository).findPositiveChangesByPlayer(samplePlayer);
    }

    @Test
    void getTrustScoreStats_ValidPlayer_ShouldReturnStats() {
        // Arrange
        UUID playerUuid = samplePlayer.getAtletaUuid();
        Object[] statsData = {5L, 3L, 2L, 15L}; // total, positive, negative, sum
        
        when(playerProfileRepository.findById(playerUuid)).thenReturn(Optional.of(samplePlayer));
        when(trustLogRepository.getTrustChangeStatsByPlayer(samplePlayer)).thenReturn(statsData);

        // Act
        TrustScoreService.TrustScoreStats stats = trustScoreService.getTrustScoreStats(playerUuid);

        // Assert
        assertNotNull(stats);
        assertEquals(samplePlayer.getTrustScore(), stats.getCurrentTrustScore());
        assertEquals(5, stats.getTotalChanges());
        assertEquals(3, stats.getPositiveChanges());
        assertEquals(2, stats.getNegativeChanges());
        assertEquals(15, stats.getTotalChangeValue());

        verify(playerProfileRepository).findById(playerUuid);
        verify(trustLogRepository).getTrustChangeStatsByPlayer(samplePlayer);
    }

    @Test
    void recalculateTrustScore_ValidPlayer_ShouldRecalculate() {
        // Arrange
        UUID playerUuid = samplePlayer.getAtletaUuid();
        Long totalChanges = 15L; // This would make the score 115 (100 + 15)
        samplePlayer.setTrustScore(110); // Current score is different from calculated
        
        when(playerProfileRepository.findById(playerUuid)).thenReturn(Optional.of(samplePlayer));
        when(trustLogRepository.getTotalChangesByPlayer(samplePlayer)).thenReturn(totalChanges);
        when(playerProfileRepository.save(any(PlayerProfile.class))).thenReturn(samplePlayer);
        when(trustLogRepository.save(any(TrustLog.class))).thenReturn(sampleTrustLog);

        // Act
        PlayerProfileResponse response = trustScoreService.recalculateTrustScore(playerUuid);

        // Assert
        assertNotNull(response);
        assertEquals(playerUuid, response.getAtletaUuid());

        verify(playerProfileRepository).findById(playerUuid);
        verify(trustLogRepository).getTotalChangesByPlayer(samplePlayer);
        verify(playerProfileRepository).save(samplePlayer);
        verify(trustLogRepository).save(any(TrustLog.class)); // Correction log should be created
    }

    @Test
    void applyAutomaticPenalty_NoShow_ShouldApplyCorrectPenalty() {
        // Arrange
        UUID playerUuid = samplePlayer.getAtletaUuid();
        Long matchId = 1L;
        TrustScoreService.PenaltyType penaltyType = TrustScoreService.PenaltyType.NO_SHOW;
        
        when(playerProfileRepository.findById(playerUuid)).thenReturn(Optional.of(samplePlayer));
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(sampleMatch));
        when(trustLogRepository.save(any(TrustLog.class))).thenReturn(sampleTrustLog);
        when(playerProfileRepository.save(any(PlayerProfile.class))).thenReturn(samplePlayer);

        // Act
        TrustLogResponse response = trustScoreService.applyAutomaticPenalty(playerUuid, matchId, penaltyType);

        // Assert
        assertNotNull(response);
        assertEquals(sampleTrustLog.getCambio(), response.getCambio());

        verify(playerProfileRepository).findById(playerUuid);
        verify(matchRepository).findById(matchId);
        verify(trustLogRepository).save(any(TrustLog.class));
        verify(playerProfileRepository).save(samplePlayer);
    }

    @Test
    void applyAutomaticBonus_GoodSportsmanship_ShouldApplyCorrectBonus() {
        // Arrange
        UUID playerUuid = samplePlayer.getAtletaUuid();
        Long matchId = 1L;
        TrustScoreService.BonusType bonusType = TrustScoreService.BonusType.GOOD_SPORTSMANSHIP;
        
        when(playerProfileRepository.findById(playerUuid)).thenReturn(Optional.of(samplePlayer));
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(sampleMatch));
        when(trustLogRepository.save(any(TrustLog.class))).thenReturn(sampleTrustLog);
        when(playerProfileRepository.save(any(PlayerProfile.class))).thenReturn(samplePlayer);

        // Act
        TrustLogResponse response = trustScoreService.applyAutomaticBonus(playerUuid, matchId, bonusType);

        // Assert
        assertNotNull(response);
        assertEquals(sampleTrustLog.getCambio(), response.getCambio());

        verify(playerProfileRepository).findById(playerUuid);
        verify(matchRepository).findById(matchId);
        verify(trustLogRepository).save(any(TrustLog.class));
        verify(playerProfileRepository).save(samplePlayer);
    }

    @Test
    void trustScoreStats_CalculatePercentages_ShouldReturnCorrectPercentages() {
        // Arrange & Act
        TrustScoreService.TrustScoreStats stats = new TrustScoreService.TrustScoreStats(
                100, 10, 7, 3, 25
        );

        // Assert
        assertEquals(70.0, stats.getPositiveChangePercentage(), 0.01);
        assertEquals(30.0, stats.getNegativeChangePercentage(), 0.01);
    }

    @Test
    void trustScoreStats_ZeroTotalChanges_ShouldReturnZeroPercentages() {
        // Arrange & Act
        TrustScoreService.TrustScoreStats stats = new TrustScoreService.TrustScoreStats(
                100, 0, 0, 0, 0
        );

        // Assert
        assertEquals(0.0, stats.getPositiveChangePercentage(), 0.01);
        assertEquals(0.0, stats.getNegativeChangePercentage(), 0.01);
    }
}
