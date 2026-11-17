package com.atleta.demo.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record TeamLeaderboardEntryResponse(
        int rank,
        UUID playerProfileId,
        String alias,
        BigDecimal score,
        int matchesPlayed,
        boolean rated,
        Long goalsScored,
        Long mvpCount,
        java.util.List<String> recentResults
) {
    public TeamLeaderboardEntryResponse(int rank, UUID playerProfileId, String alias, BigDecimal score, int matchesPlayed, boolean rated) {
        this(rank, playerProfileId, alias, score, matchesPlayed, rated, null, null, null);
    }
}
