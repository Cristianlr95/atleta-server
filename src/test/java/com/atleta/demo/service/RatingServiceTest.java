package com.atleta.demo.service;

import com.atleta.demo.dto.request.PlayerPerformanceDto;
import com.atleta.demo.dto.request.RotativeGoalkeeperRequest;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.PlayerRating;
import com.atleta.demo.entity.RatingHistory;
import com.atleta.demo.enums.MatchResultType;
import com.atleta.demo.enums.PriorityLevel;
import com.atleta.demo.enums.RoleType;
import com.atleta.demo.exception.InvalidPlayerDataException;
import com.atleta.demo.repository.MatchRepository;
import com.atleta.demo.repository.PlayerPositionRepository;
import com.atleta.demo.repository.PlayerProfileRepository;
import com.atleta.demo.repository.PlayerRatingRepository;
import com.atleta.demo.repository.RatingHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para RatingService.
 * Verifica la funcionalidad basica del servicio de calificaciones.
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

    @Test
    void rotativeRatingsUseOnlyProvidedPlayersAndTheirOwnResults() {
        Match match = new Match();
        PlayerProfile home = new PlayerProfile();
        home.setAtletaUuid(UUID.randomUUID());
        PlayerProfile away = new PlayerProfile();
        away.setAtletaUuid(UUID.randomUUID());
        PlayerRating homeRating = new PlayerRating(home, RoleType.ARQUERO, PriorityLevel.PRINCIPAL);
        homeRating.setMatchesPlayed(1);
        PlayerRating awayRating = new PlayerRating(away, RoleType.ARQUERO, PriorityLevel.TERCIARIA);
        when(playerRatingRepository.findByPlayerProfileIdAndRoleType(home.getAtletaUuid(), RoleType.ARQUERO))
                .thenReturn(List.of(homeRating));
        when(playerRatingRepository.findByPlayerProfileIdAndRoleType(away.getAtletaUuid(), RoleType.ARQUERO))
                .thenReturn(List.of(awayRating));
        when(calculationEngine.calculateRotativeGoalkeeperRating(any())).thenReturn(BigDecimal.valueOf(75));
        PlayerPerformanceDto homePerformance = new PlayerPerformanceDto();
        homePerformance.setPlayerProfileId(home.getAtletaUuid());
        homePerformance.setRoleType(RoleType.ARQUERO);
        homePerformance.setPriorityLevel(PriorityLevel.PRINCIPAL);
        homePerformance.setMatchResult(MatchResultType.GANADO);
        PlayerPerformanceDto awayPerformance = new PlayerPerformanceDto();
        awayPerformance.setPlayerProfileId(away.getAtletaUuid());
        awayPerformance.setMatchResult(MatchResultType.PERDIDO);

        ratingService.updateConfirmedRotativeGoalkeeperRatings(match, List.of(homePerformance, awayPerformance));

        ArgumentCaptor<RotativeGoalkeeperRequest> calculations = ArgumentCaptor.forClass(RotativeGoalkeeperRequest.class);
        verify(calculationEngine, org.mockito.Mockito.times(2)).calculateRotativeGoalkeeperRating(calculations.capture());
        assertEquals(MatchResultType.GANADO, calculations.getAllValues().get(0).getMatchResult());
        assertEquals(MatchResultType.PERDIDO, calculations.getAllValues().get(1).getMatchResult());
        ArgumentCaptor<RatingHistory> histories = ArgumentCaptor.forClass(RatingHistory.class);
        verify(ratingHistoryRepository, org.mockito.Mockito.times(2)).save(histories.capture());
        assertEquals(homeRating, histories.getAllValues().get(0).getPlayerRating());
        assertEquals(awayRating, histories.getAllValues().get(1).getPlayerRating());
        assertTrue(histories.getAllValues().stream().allMatch(RatingHistory::getRotativeGoalkeeperMode));
        assertEquals(1, homeRating.getMatchesPlayed());
        assertEquals(1, awayRating.getMatchesPlayed());
    }

    @Test
    void rotativeRatingCreatesTheGoalkeeperRoleWhenMissing() {
        PlayerProfile player = new PlayerProfile();
        player.setAtletaUuid(UUID.randomUUID());
        PlayerPerformanceDto performance = new PlayerPerformanceDto();
        performance.setPlayerProfileId(player.getAtletaUuid());
        performance.setMatchResult(MatchResultType.EMPATE);
        when(playerRatingRepository.findByPlayerProfileIdAndRoleType(player.getAtletaUuid(), RoleType.ARQUERO))
                .thenReturn(List.of());
        when(playerProfileRepository.findById(player.getAtletaUuid())).thenReturn(Optional.of(player));
        when(playerRatingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(calculationEngine.calculateRotativeGoalkeeperRating(any())).thenReturn(BigDecimal.valueOf(50));

        ratingService.updateConfirmedRotativeGoalkeeperRatings(new Match(), List.of(performance));

        ArgumentCaptor<RatingHistory> history = ArgumentCaptor.forClass(RatingHistory.class);
        verify(ratingHistoryRepository).save(history.capture());
        assertEquals(RoleType.ARQUERO, history.getValue().getPlayerRating().getRoleType());
        assertEquals(PriorityLevel.TERCIARIA, history.getValue().getPlayerRating().getPriorityLevel());
    }

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
    void calculateHybridOvrBatchUsesOneRepositoryQueryAndGroupsPlayers() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        PlayerProfile first = new PlayerProfile();
        first.setAtletaUuid(firstId);
        PlayerProfile second = new PlayerProfile();
        second.setAtletaUuid(secondId);

        PlayerRating firstHigh = new PlayerRating(first, RoleType.ATAQUE, PriorityLevel.PRINCIPAL, BigDecimal.valueOf(80));
        PlayerRating firstLow = new PlayerRating(first, RoleType.DEFENSA, PriorityLevel.SECUNDARIA, BigDecimal.valueOf(70));
        PlayerRating secondOnly = new PlayerRating(second, RoleType.MEDIOCAMPO, PriorityLevel.PRINCIPAL, BigDecimal.valueOf(60));
        List<UUID> requested = List.of(firstId, secondId);
        when(playerRatingRepository.findByPlayerProfileIds(requested))
                .thenReturn(List.of(firstHigh, firstLow, secondOnly));

        Map<UUID, BigDecimal> result = ratingService.calculateHybridOVRBatch(requested);

        assertEquals(new BigDecimal("77.00"), result.get(firstId));
        assertEquals(new BigDecimal("60.00"), result.get(secondId));
        verify(playerRatingRepository).findByPlayerProfileIds(requested);
    }

    @Test
    void testUpdatePlayerRatings_ValidInput_Success() {
        Long matchId = 1L;
        UUID playerId = UUID.randomUUID();

        PlayerPerformanceDto performance = new PlayerPerformanceDto(
                playerId,
                RoleType.ATAQUE,
                PriorityLevel.PRINCIPAL,
                2,
                1,
                null,
                false,
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

        assertDoesNotThrow(() -> ratingService.updatePlayerRatings(matchId, List.of(performance)));

        verify(matchRepository).findById(matchId);
        verify(calculationEngine).calculateNewRating(any());
        verify(playerRatingRepository).save(any());
        verify(ratingHistoryRepository).save(any());
    }

    @Test
    void testUpdatePlayerRatings_NullMatchId_ThrowsException() {
        PlayerPerformanceDto performance = new PlayerPerformanceDto();

        InvalidPlayerDataException exception = assertThrows(
                InvalidPlayerDataException.class,
                () -> ratingService.updatePlayerRatings(null, List.of(performance))
        );

        assertTrue(exception.getMessage().contains("El ID del partido es obligatorio"));
    }

    @Test
    void testUpdatePlayerRatings_EmptyPerformances_ThrowsException() {
        InvalidPlayerDataException exception = assertThrows(
                InvalidPlayerDataException.class,
                () -> ratingService.updatePlayerRatings(1L, List.of())
        );

        assertTrue(exception.getMessage().contains("La lista de rendimientos de jugadores no puede estar vac"));
    }

    @Test
    void testUpdatePlayerRatings_MultipleMVPs_ThrowsException() {
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

        InvalidPlayerDataException exception = assertThrows(
                InvalidPlayerDataException.class,
                () -> ratingService.updatePlayerRatings(matchId, List.of(performance1, performance2))
        );

        assertTrue(exception.getMessage().contains("Solo puede haber un MVP por partido"));
    }

    @Test
    void testGetPlayerRatings_ValidPlayerId_ReturnsRatings() {
        UUID playerId = UUID.randomUUID();
        PlayerRating rating1 = new PlayerRating();
        PlayerRating rating2 = new PlayerRating();
        List<PlayerRating> expectedRatings = List.of(rating1, rating2);

        when(playerRatingRepository.findByPlayerProfileId(playerId)).thenReturn(expectedRatings);

        List<PlayerRating> actualRatings = ratingService.getPlayerRatings(playerId);

        assertEquals(expectedRatings, actualRatings);
        verify(playerRatingRepository).findByPlayerProfileId(playerId);
    }

    @Test
    void testGetPlayerRatings_NullPlayerId_ThrowsException() {
        InvalidPlayerDataException exception = assertThrows(
                InvalidPlayerDataException.class,
                () -> ratingService.getPlayerRatings(null)
        );

        assertTrue(exception.getMessage().contains("El ID del perfil del jugador es obligatorio"));
    }

    @Test
    void testGetRatingHistory_ValidPlayerId_ReturnsHistory() {
        UUID playerId = UUID.randomUUID();
        RatingHistory history1 = new RatingHistory();
        RatingHistory history2 = new RatingHistory();
        List<RatingHistory> expectedHistory = List.of(history1, history2);

        when(ratingHistoryRepository.findByPlayerProfileId(playerId)).thenReturn(expectedHistory);

        List<RatingHistory> actualHistory = ratingService.getRatingHistory(playerId);

        assertEquals(expectedHistory, actualHistory);
        verify(ratingHistoryRepository).findByPlayerProfileId(playerId);
    }

    @Test
    void manualRotativeUpdateRemainsDisabled() {
        assertThrows(UnsupportedOperationException.class,
                () -> ratingService.updateRotativeGoalkeeperRatings(1L, MatchResultType.GANADO));
        org.mockito.Mockito.verify(playerRatingRepository, org.mockito.Mockito.never()).findByRoleType(any());
    }
    @Test
    void testUpdateRotativeGoalkeeperRatings_NullMatchId_ThrowsException() {
        InvalidPlayerDataException exception = assertThrows(
                InvalidPlayerDataException.class,
                () -> ratingService.updateRotativeGoalkeeperRatings(null, MatchResultType.GANADO)
        );

        assertTrue(exception.getMessage().contains("El ID del partido es obligatorio"));
    }

    @Test
    void testUpdateRotativeGoalkeeperRatings_NullMatchResult_ThrowsException() {
        InvalidPlayerDataException exception = assertThrows(
                InvalidPlayerDataException.class,
                () -> ratingService.updateRotativeGoalkeeperRatings(1L, null)
        );

        assertTrue(exception.getMessage().contains("El resultado del partido es obligatorio"));
    }
}
