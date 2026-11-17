package com.atleta.demo.service;

import com.atleta.demo.enums.RoleType;
import com.atleta.demo.repository.PlayerHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RankingGoalsService {
    public record Goals(long total, Long role) {}
    private static class Accumulator {
        long total;
        long roleGoals;
        boolean hasRole;
        boolean unknownPosition;
    }
    private final PlayerHistoryRepository history;

    public RankingGoalsService(PlayerHistoryRepository history) { this.history = history; }

    @Transactional(readOnly = true)
    public Map<UUID, Goals> calculate(List<UUID> playerIds, Long teamId, RoleType role) {
        if (playerIds.isEmpty()) return Map.of();
        Map<UUID, Accumulator> totals = new HashMap<>();
        for (var group : history.summarizeRankingGoals(playerIds, teamId)) {
            RoleType positionRole = RatingService.mapPositionToRole(group.getPositionName());
            Accumulator entry = totals.computeIfAbsent(group.getPlayerUuid(), ignored -> new Accumulator());
            entry.total += group.getGoals();
            if (positionRole == null) entry.unknownPosition = true;
            if (role != null && positionRole == role) {
                entry.hasRole = true;
                entry.roleGoals += group.getGoals();
            }
        }
        Map<UUID, Goals> result = new HashMap<>();
        totals.forEach((id, entry) -> result.put(id, new Goals(entry.total,
                role != null && entry.hasRole && !entry.unknownPosition ? entry.roleGoals : null)));
        return result;
    }
}
