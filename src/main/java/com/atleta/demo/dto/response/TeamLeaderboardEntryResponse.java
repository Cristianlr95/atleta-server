package com.atleta.demo.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record TeamLeaderboardEntryResponse(
        int rank,
        UUID playerProfileId,
        String alias,
        BigDecimal score,
        int matchesPlayed,
        boolean rated
) {
}
