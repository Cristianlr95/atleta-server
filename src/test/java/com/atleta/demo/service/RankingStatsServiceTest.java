package com.atleta.demo.service;

import com.atleta.demo.enums.RoleType;
import com.atleta.demo.repository.PlayerHistoryRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RankingStatsServiceTest {
    @Test
    void separatesCareerAndPositionAndDoesNotInventMissingHistory() {
        var history = mock(PlayerHistoryRepository.class);
        var id = UUID.randomUUID();
        var other = UUID.randomUUID();
        var forward = group(id, "Delantero", 3L);
        var defence = group(id, "Defensa", 2L);
        when(forward.getMvps()).thenReturn(1L);
        when(history.summarizeRankingGoals(List.of(id, other), -1L)).thenReturn(List.of(forward, defence));
        var result = new RankingStatsService(history).calculate(List.of(id, other), null, RoleType.ATAQUE);
        assertEquals(5, result.get(id).total());
        assertEquals(3L, result.get(id).role());
        assertEquals(1L, result.get(id).mvpTotal());
        assertEquals(1L, result.get(id).mvpRole());
        assertFalse(result.containsKey(other));
    }

    @Test
    void unknownPositionLeavesRoleUnknownAndPreservesCareer() {
        var history = mock(PlayerHistoryRepository.class);
        var id = UUID.randomUUID();
        var unknown = group(id, "Sin clasificar", 2L);
        when(history.summarizeRankingGoals(List.of(id), 77L)).thenReturn(List.of(unknown));
        var goals = new RankingStatsService(history).calculate(List.of(id), 77L, RoleType.ATAQUE).get(id);
        assertEquals(2, goals.total());
        assertNull(goals.role());
    }

    @Test
    void separatesZeroGoalsFromNoMatchesInTheSelectedPosition() {
        var history = mock(PlayerHistoryRepository.class);
        var id = UUID.randomUUID();
        var zero = group(id, "Delantero", 0L);
        var other = group(id, "Defensa", 2L);
        when(history.summarizeRankingGoals(List.of(id), -1L)).thenReturn(List.of(zero, other)).thenReturn(List.of(other));
        var service = new RankingStatsService(history);
        assertEquals(0L, service.calculate(List.of(id), null, RoleType.ATAQUE).get(id).role());
        assertNull(service.calculate(List.of(id), null, RoleType.ATAQUE).get(id).role());
    }

    @Test
    void returnsLastFiveChronologicallyForCareerAndPosition() {
        var history = mock(PlayerHistoryRepository.class);
        var id = UUID.randomUUID();
        var forward = group(id, "Delantero", 1L);
        var defence = group(id, "Defensa", 0L);
        when(history.summarizeRankingGoals(List.of(id), -1L)).thenReturn(List.of(forward, defence));
        var matches = new java.util.ArrayList<PlayerHistoryRepository.RankingResult>();
        for (int i = 1; i <= 6; i++) {
            var item = mock(PlayerHistoryRepository.RankingResult.class);
            when(item.getPlayerUuid()).thenReturn(id.toString());
            when(item.getPositionName()).thenReturn(i == 6 ? "Defensa" : "Delantero");
            when(item.getMatchDate()).thenReturn(java.time.LocalDateTime.of(2026, 1, i, 12, 0));
            when(item.getMatchId()).thenReturn((long) i);
            when(item.getResult()).thenReturn(i == 6 ? "DERROTA" : "VICTORIA");
            matches.add(item);
        }
        when(history.findRecentRankingResults(List.of(id))).thenReturn(matches);
        var result = new RankingStatsService(history).calculate(List.of(id), null, RoleType.ATAQUE).get(id);
        assertEquals(List.of("VICTORIA", "VICTORIA", "VICTORIA", "VICTORIA", "DERROTA"), result.recentResults());
        assertEquals(List.of("VICTORIA", "VICTORIA", "VICTORIA", "VICTORIA", "VICTORIA"), result.roleResults());
    }

    @Test
    void ordersResultsWithEqualDatesByMatchId() {
        var history = mock(PlayerHistoryRepository.class);
        var id = UUID.randomUUID();
        var forward = group(id, "Delantero", 0L);
        when(history.summarizeRankingGoals(List.of(id), -1L)).thenReturn(List.of(forward));
        var first = mock(PlayerHistoryRepository.RankingResult.class);
        var second = mock(PlayerHistoryRepository.RankingResult.class);
        for (var item : List.of(first, second)) {
            when(item.getPlayerUuid()).thenReturn(id.toString());
            when(item.getPositionName()).thenReturn("Delantero");
            when(item.getMatchDate()).thenReturn(java.time.LocalDateTime.of(2026, 1, 1, 12, 0));
        }
        when(first.getMatchId()).thenReturn(1L);
        when(second.getMatchId()).thenReturn(2L);
        when(first.getResult()).thenReturn("VICTORIA");
        when(second.getResult()).thenReturn("DERROTA");
        when(history.findRecentRankingResults(List.of(id))).thenReturn(List.of(second, first));
        var result = new RankingStatsService(history).calculate(List.of(id), null, RoleType.ATAQUE).get(id);
        assertEquals(List.of("VICTORIA", "DERROTA"), result.recentResults());
    }

    private PlayerHistoryRepository.RankingGoals group(UUID id, String position, long goals) {
        var result = mock(PlayerHistoryRepository.RankingGoals.class);
        when(result.getPlayerUuid()).thenReturn(id);
        when(result.getPositionName()).thenReturn(position);
        when(result.getGoals()).thenReturn(goals);
        when(result.getMvps()).thenReturn(0L);
        return result;
    }
}
