package com.atleta.demo.dto.response;

/**
 * Competitive record for matches played against a different registered team.
 * Internal practices are deliberately excluded from every value.
 */
public record TeamExternalRecordResponse(
        Long teamId,
        String teamName,
        int matchesPlayed,
        int wins,
        int draws,
        int losses,
        int points
) {
}
