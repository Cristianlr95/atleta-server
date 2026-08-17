package com.atleta.demo.service;

import com.atleta.demo.dto.response.TeamLeaderboardEntryResponse;
import com.atleta.demo.entity.PlayerRating;
import com.atleta.demo.entity.Team;
import com.atleta.demo.entity.TeamMember;
import com.atleta.demo.repository.PlayerRatingRepository;
import com.atleta.demo.repository.TeamMemberRepository;
import com.atleta.demo.repository.TeamRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TeamLeaderboardService {
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final PlayerRatingRepository playerRatingRepository;

    public TeamLeaderboardService(
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            PlayerRatingRepository playerRatingRepository
    ) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.playerRatingRepository = playerRatingRepository;
    }

    @Transactional(readOnly = true)
    public List<TeamLeaderboardEntryResponse> getLeaderboard(Long teamId, UUID viewerUuid) {
        Team team = teamRepository.findById(teamId)
                .filter(candidate -> !Boolean.TRUE.equals(candidate.getArchived()))
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado: " + teamId));

        List<TeamMember> members = teamMemberRepository.findActiveByTeam(team);
        boolean isCreator = team.getCreador() != null
                && team.getCreador().getAtletaUuid().equals(viewerUuid);
        boolean isMember = members.stream()
                .anyMatch(member -> member.getPlayer().getAtletaUuid().equals(viewerUuid));
        if (!isCreator && !isMember) {
            throw new AccessDeniedException("El ranking del equipo es visible solo para sus miembros");
        }

        if (members.isEmpty()) {
            return List.of();
        }

        List<UUID> playerIds = members.stream()
                .map(member -> member.getPlayer().getAtletaUuid())
                .distinct()
                .toList();
        Map<UUID, List<PlayerRating>> ratingsByPlayer = new HashMap<>();
        for (PlayerRating rating : playerRatingRepository.findByPlayerProfileIds(playerIds)) {
            ratingsByPlayer.computeIfAbsent(
                    rating.getPlayerProfile().getAtletaUuid(),
                    ignored -> new ArrayList<>()
            ).add(rating);
        }

        List<UnrankedEntry> entries = members.stream()
                .map(member -> toUnranked(member, ratingsByPlayer.getOrDefault(
                        member.getPlayer().getAtletaUuid(), List.of())))
                .sorted(Comparator
                        .comparing(UnrankedEntry::rated).reversed()
                        .thenComparing(UnrankedEntry::score, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(UnrankedEntry::alias, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(entry -> entry.playerProfileId().toString()))
                .toList();

        List<TeamLeaderboardEntryResponse> result = new ArrayList<>();
        BigDecimal previousScore = null;
        int currentRank = 0;
        for (int index = 0; index < entries.size(); index++) {
            UnrankedEntry entry = entries.get(index);
            if (entry.score() == null || previousScore == null || entry.score().compareTo(previousScore) != 0) {
                currentRank = index + 1;
            }
            result.add(new TeamLeaderboardEntryResponse(
                    currentRank,
                    entry.playerProfileId(),
                    entry.alias(),
                    entry.score(),
                    entry.matchesPlayed(),
                    entry.rated()
            ));
            previousScore = entry.score();
        }
        return result;
    }

    private UnrankedEntry toUnranked(TeamMember member, List<PlayerRating> ratings) {
        if (ratings.isEmpty()) {
            return new UnrankedEntry(
                    member.getPlayer().getAtletaUuid(), member.getPlayer().getAlias(), null, 0, false);
        }
        List<BigDecimal> scores = ratings.stream()
                .map(PlayerRating::getCurrentRating)
                .sorted(Comparator.reverseOrder())
                .toList();
        BigDecimal best = scores.get(0);
        int topCount = Math.min(3, scores.size());
        BigDecimal topAverage = average(scores.subList(0, topCount));
        BigDecimal allAverage = average(scores);
        BigDecimal hybrid = best.multiply(BigDecimal.valueOf(0.4))
                .add(topAverage.multiply(BigDecimal.valueOf(0.4)))
                .add(allAverage.multiply(BigDecimal.valueOf(0.2)))
                .setScale(2, RoundingMode.HALF_UP);
        int matches = ratings.stream().mapToInt(PlayerRating::getMatchesPlayed).sum();
        return new UnrankedEntry(
                member.getPlayer().getAtletaUuid(), member.getPlayer().getAlias(), hybrid, matches, true);
    }

    private BigDecimal average(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private record UnrankedEntry(
            UUID playerProfileId,
            String alias,
            BigDecimal score,
            int matchesPlayed,
            boolean rated
    ) {
    }
}
