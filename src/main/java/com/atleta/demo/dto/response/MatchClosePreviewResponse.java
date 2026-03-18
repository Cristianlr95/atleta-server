package com.atleta.demo.dto.response;

import java.util.ArrayList;
import java.util.List;

public class MatchClosePreviewResponse {
    private Long matchId;
    private Integer finalScoreLocal;
    private Integer finalScoreAway;
    private List<MatchClosePreviewPlayerResponse> players = new ArrayList<>();

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public Integer getFinalScoreLocal() {
        return finalScoreLocal;
    }

    public void setFinalScoreLocal(Integer finalScoreLocal) {
        this.finalScoreLocal = finalScoreLocal;
    }

    public Integer getFinalScoreAway() {
        return finalScoreAway;
    }

    public void setFinalScoreAway(Integer finalScoreAway) {
        this.finalScoreAway = finalScoreAway;
    }

    public List<MatchClosePreviewPlayerResponse> getPlayers() {
        return players;
    }

    public void setPlayers(List<MatchClosePreviewPlayerResponse> players) {
        this.players = players;
    }
}

