package com.atleta.demo.dto.request;

import jakarta.validation.constraints.Min;

import java.util.Map;
import java.util.UUID;

public class MatchClosePreviewRequest {

    private Map<UUID, Integer> goalsByPlayer;

    @Min(0)
    private Integer finalScoreLocal;

    @Min(0)
    private Integer finalScoreAway;

    public Map<UUID, Integer> getGoalsByPlayer() {
        return goalsByPlayer;
    }

    public void setGoalsByPlayer(Map<UUID, Integer> goalsByPlayer) {
        this.goalsByPlayer = goalsByPlayer;
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
}

