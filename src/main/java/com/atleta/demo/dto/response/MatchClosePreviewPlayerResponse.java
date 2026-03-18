package com.atleta.demo.dto.response;

import com.atleta.demo.enums.MatchTeamSide;

import java.math.BigDecimal;
import java.util.UUID;

public class MatchClosePreviewPlayerResponse {
    private UUID playerUuid;
    private String alias;
    private String position;
    private MatchTeamSide teamSide;
    private Integer goals;
    private Integer estimatedXp;
    private BigDecimal currentHybridOvr;

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public MatchTeamSide getTeamSide() {
        return teamSide;
    }

    public void setTeamSide(MatchTeamSide teamSide) {
        this.teamSide = teamSide;
    }

    public Integer getGoals() {
        return goals;
    }

    public void setGoals(Integer goals) {
        this.goals = goals;
    }

    public Integer getEstimatedXp() {
        return estimatedXp;
    }

    public void setEstimatedXp(Integer estimatedXp) {
        this.estimatedXp = estimatedXp;
    }

    public BigDecimal getCurrentHybridOvr() {
        return currentHybridOvr;
    }

    public void setCurrentHybridOvr(BigDecimal currentHybridOvr) {
        this.currentHybridOvr = currentHybridOvr;
    }
}

