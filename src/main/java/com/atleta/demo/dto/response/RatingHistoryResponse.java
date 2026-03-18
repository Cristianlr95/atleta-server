package com.atleta.demo.dto.response;

import com.atleta.demo.enums.MatchResultType;
import com.atleta.demo.enums.PriorityLevel;
import com.atleta.demo.enums.RoleType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para el historial de calificaciones.
 * Contiene información detallada sobre un cambio de calificación específico.
 */
public class RatingHistoryResponse {

    private Long id;
    private UUID playerProfileId;
    private String playerAlias;
    private RoleType roleType;
    private PriorityLevel priorityLevel;
    private Long matchId;
    private BigDecimal previousRating;
    private BigDecimal newRating;
    private BigDecimal ratingDelta;
    private Integer goalsScored;
    private Integer assistsMade;
    private Integer goalsConceded;
    private Boolean wasMvp;
    private MatchResultType matchResult;
    private Boolean rotativeGoalkeeperMode;
    private LocalDateTime createdAt;

    // Componentes detallados del cálculo
    private BigDecimal resultPoints;
    private BigDecimal weightedGoalPoints;
    private BigDecimal weightedAssistPoints;
    private BigDecimal defensiveBonus;
    private BigDecimal mvpBonus;
    private BigDecimal priorityMultiplier;

    // Constructors
    public RatingHistoryResponse() {
    }

    public RatingHistoryResponse(Long id, UUID playerProfileId, String playerAlias,
                                RoleType roleType, PriorityLevel priorityLevel, Long matchId,
                                BigDecimal previousRating, BigDecimal newRating, BigDecimal ratingDelta,
                                Integer goalsScored, Integer assistsMade, Integer goalsConceded,
                                Boolean wasMvp, MatchResultType matchResult, Boolean rotativeGoalkeeperMode,
                                LocalDateTime createdAt) {
        this.id = id;
        this.playerProfileId = playerProfileId;
        this.playerAlias = playerAlias;
        this.roleType = roleType;
        this.priorityLevel = priorityLevel;
        this.matchId = matchId;
        this.previousRating = previousRating;
        this.newRating = newRating;
        this.ratingDelta = ratingDelta;
        this.goalsScored = goalsScored;
        this.assistsMade = assistsMade;
        this.goalsConceded = goalsConceded;
        this.wasMvp = wasMvp;
        this.matchResult = matchResult;
        this.rotativeGoalkeeperMode = rotativeGoalkeeperMode;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getPlayerProfileId() {
        return playerProfileId;
    }

    public void setPlayerProfileId(UUID playerProfileId) {
        this.playerProfileId = playerProfileId;
    }

    public String getPlayerAlias() {
        return playerAlias;
    }

    public void setPlayerAlias(String playerAlias) {
        this.playerAlias = playerAlias;
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
    }

    public PriorityLevel getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(PriorityLevel priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public BigDecimal getPreviousRating() {
        return previousRating;
    }

    public void setPreviousRating(BigDecimal previousRating) {
        this.previousRating = previousRating;
    }

    public BigDecimal getNewRating() {
        return newRating;
    }

    public void setNewRating(BigDecimal newRating) {
        this.newRating = newRating;
    }

    public BigDecimal getRatingDelta() {
        return ratingDelta;
    }

    public void setRatingDelta(BigDecimal ratingDelta) {
        this.ratingDelta = ratingDelta;
    }

    public Integer getGoalsScored() {
        return goalsScored;
    }

    public void setGoalsScored(Integer goalsScored) {
        this.goalsScored = goalsScored;
    }

    public Integer getAssistsMade() {
        return assistsMade;
    }

    public void setAssistsMade(Integer assistsMade) {
        this.assistsMade = assistsMade;
    }

    public Integer getGoalsConceded() {
        return goalsConceded;
    }

    public void setGoalsConceded(Integer goalsConceded) {
        this.goalsConceded = goalsConceded;
    }

    public Boolean getWasMvp() {
        return wasMvp;
    }

    public void setWasMvp(Boolean wasMvp) {
        this.wasMvp = wasMvp;
    }

    public MatchResultType getMatchResult() {
        return matchResult;
    }

    public void setMatchResult(MatchResultType matchResult) {
        this.matchResult = matchResult;
    }

    public Boolean getRotativeGoalkeeperMode() {
        return rotativeGoalkeeperMode;
    }

    public void setRotativeGoalkeeperMode(Boolean rotativeGoalkeeperMode) {
        this.rotativeGoalkeeperMode = rotativeGoalkeeperMode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getResultPoints() {
        return resultPoints;
    }

    public void setResultPoints(BigDecimal resultPoints) {
        this.resultPoints = resultPoints;
    }

    public BigDecimal getWeightedGoalPoints() {
        return weightedGoalPoints;
    }

    public void setWeightedGoalPoints(BigDecimal weightedGoalPoints) {
        this.weightedGoalPoints = weightedGoalPoints;
    }

    public BigDecimal getWeightedAssistPoints() {
        return weightedAssistPoints;
    }

    public void setWeightedAssistPoints(BigDecimal weightedAssistPoints) {
        this.weightedAssistPoints = weightedAssistPoints;
    }

    public BigDecimal getDefensiveBonus() {
        return defensiveBonus;
    }

    public void setDefensiveBonus(BigDecimal defensiveBonus) {
        this.defensiveBonus = defensiveBonus;
    }

    public BigDecimal getMvpBonus() {
        return mvpBonus;
    }

    public void setMvpBonus(BigDecimal mvpBonus) {
        this.mvpBonus = mvpBonus;
    }

    public BigDecimal getPriorityMultiplier() {
        return priorityMultiplier;
    }

    public void setPriorityMultiplier(BigDecimal priorityMultiplier) {
        this.priorityMultiplier = priorityMultiplier;
    }

    @Override
    public String toString() {
        return "RatingHistoryResponse{" +
                "id=" + id +
                ", playerProfileId=" + playerProfileId +
                ", playerAlias='" + playerAlias + '\'' +
                ", roleType=" + roleType +
                ", priorityLevel=" + priorityLevel +
                ", matchId=" + matchId +
                ", previousRating=" + previousRating +
                ", newRating=" + newRating +
                ", ratingDelta=" + ratingDelta +
                ", goalsScored=" + goalsScored +
                ", assistsMade=" + assistsMade +
                ", goalsConceded=" + goalsConceded +
                ", wasMvp=" + wasMvp +
                ", matchResult=" + matchResult +
                ", rotativeGoalkeeperMode=" + rotativeGoalkeeperMode +
                ", createdAt=" + createdAt +
                '}';
    }
}