package com.atleta.demo.dto.response;

import java.util.List;

public record MatchAiSummaryResponse(
        Long matchId,
        String promptVersion,
        String source,
        String title,
        String summary,
        List<String> highlights,
        String mvpComment
) { }
