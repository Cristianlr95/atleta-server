package com.atleta.demo.dto.response;

import com.atleta.demo.enums.RoleType;

import java.math.BigDecimal;
import java.util.UUID;

public class LeaderboardEntryResponse {

    private UUID playerProfileId;
    private String playerId;
    private String alias;
    private String name;
    private BigDecimal score;
    private BigDecimal rating;
    private RoleType roleType;
    private Integer matchesPlayed;
    private Long wins;
    private Long losses;
    private Long draws;
    private Long goalsScored;
    private Long goalsForRole;
    private Long mvpCount;
    private Long mvpForRole;
    private java.util.List<String> recentResults;
    private java.util.List<String> resultsForRole;
    public java.util.List<String> getRecentResults() { return recentResults; }
    public void setRecentResults(java.util.List<String> value) { recentResults = value; }
    public java.util.List<String> getResultsForRole() { return resultsForRole; }
    public void setResultsForRole(java.util.List<String> value) { resultsForRole = value; }
    public Long getMvpCount() { return mvpCount; }
    public void setMvpCount(Long value) { this.mvpCount = value; }
    public Long getMvpForRole() { return mvpForRole; }
    public void setMvpForRole(Long value) { this.mvpForRole = value; }
    public Long getGoalsScored() { return goalsScored; }
    public void setGoalsScored(Long goalsScored) { this.goalsScored = goalsScored; }
    public Long getGoalsForRole() { return goalsForRole; }
    public void setGoalsForRole(Long goalsForRole) { this.goalsForRole = goalsForRole; }

    public LeaderboardEntryResponse() {
    }

    public UUID getPlayerProfileId() {
        return playerProfileId;
    }

    public void setPlayerProfileId(UUID playerProfileId) {
        this.playerProfileId = playerProfileId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
    }

    public Integer getMatchesPlayed() {
        return matchesPlayed;
    }

    public void setMatchesPlayed(Integer matchesPlayed) {
        this.matchesPlayed = matchesPlayed;
    }

    public Long getWins() {
        return wins;
    }

    public void setWins(Long wins) {
        this.wins = wins;
    }

    public Long getLosses() {
        return losses;
    }

    public void setLosses(Long losses) {
        this.losses = losses;
    }

    public Long getDraws() {
        return draws;
    }

    public void setDraws(Long draws) {
        this.draws = draws;
    }
}
