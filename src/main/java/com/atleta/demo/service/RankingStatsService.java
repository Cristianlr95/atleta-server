package com.atleta.demo.service;

import com.atleta.demo.enums.RoleType;
import com.atleta.demo.repository.PlayerHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RankingStatsService {
    private static final long ALL_TEAMS = -1L;
    public record Goals(long total, Long role, long mvpTotal, Long mvpRole,
                        List<String> recentResults, List<String> roleResults) {
        public Goals(long total, Long role) { this(total, role, 0L, null, List.of(), null); }
    }
    private static class Accumulator {
        long total;
        long roleGoals;
        long mvpTotal;
        long mvpRole;
        boolean hasRole;
        boolean unknownPosition;
        List<PlayerHistoryRepository.RankingResult> recent = new ArrayList<>();
        List<PlayerHistoryRepository.RankingResult> recentRole = new ArrayList<>();
    }
    private final PlayerHistoryRepository history;

    public RankingStatsService(PlayerHistoryRepository history) { this.history = history; }

    @Transactional(readOnly = true)
    public Map<UUID, Goals> calculate(List<UUID> playerIds, Long teamId, RoleType role) {
        if (playerIds.isEmpty()) return Map.of();
        Map<UUID, Accumulator> totals = new HashMap<>();
        for (var group : history.summarizeRankingGoals(playerIds, teamId == null ? ALL_TEAMS : teamId)) {
            RoleType positionRole = RatingService.mapPositionToRole(group.getPositionName());
            Accumulator entry = totals.computeIfAbsent(group.getPlayerUuid(), ignored -> new Accumulator());
            entry.total += group.getGoals();
            entry.mvpTotal += group.getMvps();
            if (positionRole == null) entry.unknownPosition = true;
            if (role != null && positionRole == role) {
                entry.hasRole = true;
                entry.roleGoals += group.getGoals();
                entry.mvpRole += group.getMvps();
            }
        }
        var recent = teamId == null ? history.findRecentRankingResults(playerIds)
                : history.findRecentRankingTeamResults(playerIds, teamId);
        for (var item : recent) {
            var entry = totals.get(UUID.fromString(item.getPlayerUuid()));
            if (entry == null) continue;
            entry.recent.add(item);
            if (role != null && RatingService.mapPositionToRole(item.getPositionName()) == role) entry.recentRole.add(item);
        }
        Map<UUID, Goals> result = new HashMap<>();
        totals.forEach((id, entry) -> result.put(id, new Goals(entry.total,
                role != null && entry.hasRole && !entry.unknownPosition ? entry.roleGoals : null,
                entry.mvpTotal, role != null && entry.hasRole && !entry.unknownPosition ? entry.mvpRole : null,
                topFive(entry.recent), role != null && entry.hasRole && !entry.unknownPosition ? topFive(entry.recentRole) : null)));
        return result;
    }

    private List<String> topFive(List<PlayerHistoryRepository.RankingResult> items) {
        Comparator<PlayerHistoryRepository.RankingResult> chronological = Comparator
                .comparing(PlayerHistoryRepository.RankingResult::getMatchDate)
                .thenComparing(PlayerHistoryRepository.RankingResult::getMatchId);
        return items.stream().sorted(chronological.reversed()).limit(5).sorted(chronological)
                .map(item -> com.atleta.demo.enums.MatchResult.valueOf(item.getResult()).name()).toList();
    }
}
