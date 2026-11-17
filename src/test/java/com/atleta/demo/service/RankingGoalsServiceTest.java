package com.atleta.demo.service;

import com.atleta.demo.enums.RoleType;
import com.atleta.demo.repository.PlayerHistoryRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RankingGoalsServiceTest {
    @Test
    void separatesCareerAndPositionAndDoesNotInventMissingHistory() {
        var history = mock(PlayerHistoryRepository.class);
        var id = UUID.randomUUID();
        var other = UUID.randomUUID();
        var forward = group(id, "Delantero", 3L);
        var defence = group(id, "Defensa", 2L);
        when(history.summarizeRankingGoals(List.of(id, other), null)).thenReturn(List.of(forward, defence));
        var result = new RankingGoalsService(history).calculate(List.of(id, other), null, RoleType.ATAQUE);
        assertEquals(5, result.get(id).total());
        assertEquals(3L, result.get(id).role());
        assertFalse(result.containsKey(other));
    }

    @Test
    void unknownPositionLeavesRoleUnknownAndPreservesCareer() {
        var history = mock(PlayerHistoryRepository.class);
        var id = UUID.randomUUID();
        var unknown = group(id, "Sin clasificar", 2L);
        when(history.summarizeRankingGoals(List.of(id), 77L)).thenReturn(List.of(unknown));
        var goals = new RankingGoalsService(history).calculate(List.of(id), 77L, RoleType.ATAQUE).get(id);
        assertEquals(2, goals.total());
        assertNull(goals.role());
    }

    @Test
    void separatesZeroGoalsFromNoMatchesInTheSelectedPosition() {
        var history = mock(PlayerHistoryRepository.class);
        var id = UUID.randomUUID();
        var zero = group(id, "Delantero", 0L);
        var other = group(id, "Defensa", 2L);
        when(history.summarizeRankingGoals(List.of(id), null)).thenReturn(List.of(zero, other)).thenReturn(List.of(other));
        var service = new RankingGoalsService(history);
        assertEquals(0L, service.calculate(List.of(id), null, RoleType.ATAQUE).get(id).role());
        assertNull(service.calculate(List.of(id), null, RoleType.ATAQUE).get(id).role());
    }

    private PlayerHistoryRepository.RankingGoals group(UUID id, String position, long goals) {
        var result = mock(PlayerHistoryRepository.RankingGoals.class);
        when(result.getPlayerUuid()).thenReturn(id);
        when(result.getPositionName()).thenReturn(position);
        when(result.getGoals()).thenReturn(goals);
        return result;
    }
}
