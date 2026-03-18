package com.atleta.demo.dto.response;

import com.atleta.demo.enums.RoleType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * DTO de respuesta para las estadísticas generales (OVR) de un jugador.
 * Incluye múltiples métricas calculadas a partir de las calificaciones por rol.
 */
public class PlayerOverallStatsResponse {

    private UUID playerProfileId;
    private String alias;
    
    // Calificaciones OVR
    private BigDecimal hybridOVR;
    private BigDecimal weightedOVR;
    private BigDecimal simpleOVR;
    
    // Clasificación
    private String classification;
    
    // Métricas adicionales
    private BigDecimal versatilityIndex;
    private BigDecimal consistencyScore;
    private RoleType bestRole;
    private BigDecimal bestRoleRating;
    
    // Desglose por rol
    private Map<RoleType, BigDecimal> roleBreakdown;
    
    // Estadísticas generales
    private Integer totalRatings;
    private Integer totalMatchesPlayed;

    // Constructors
    public PlayerOverallStatsResponse() {
    }

    public PlayerOverallStatsResponse(UUID playerProfileId, String alias, BigDecimal hybridOVR, 
                                     BigDecimal weightedOVR, String classification) {
        this.playerProfileId = playerProfileId;
        this.alias = alias;
        this.hybridOVR = hybridOVR;
        this.weightedOVR = weightedOVR;
        this.classification = classification;
    }

    // Getters and Setters
    public UUID getPlayerProfileId() {
        return playerProfileId;
    }

    public void setPlayerProfileId(UUID playerProfileId) {
        this.playerProfileId = playerProfileId;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public BigDecimal getHybridOVR() {
        return hybridOVR;
    }

    public void setHybridOVR(BigDecimal hybridOVR) {
        this.hybridOVR = hybridOVR;
    }

    public BigDecimal getWeightedOVR() {
        return weightedOVR;
    }

    public void setWeightedOVR(BigDecimal weightedOVR) {
        this.weightedOVR = weightedOVR;
    }

    public BigDecimal getSimpleOVR() {
        return simpleOVR;
    }

    public void setSimpleOVR(BigDecimal simpleOVR) {
        this.simpleOVR = simpleOVR;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public BigDecimal getVersatilityIndex() {
        return versatilityIndex;
    }

    public void setVersatilityIndex(BigDecimal versatilityIndex) {
        this.versatilityIndex = versatilityIndex;
    }

    public BigDecimal getConsistencyScore() {
        return consistencyScore;
    }

    public void setConsistencyScore(BigDecimal consistencyScore) {
        this.consistencyScore = consistencyScore;
    }

    public RoleType getBestRole() {
        return bestRole;
    }

    public void setBestRole(RoleType bestRole) {
        this.bestRole = bestRole;
    }

    public BigDecimal getBestRoleRating() {
        return bestRoleRating;
    }

    public void setBestRoleRating(BigDecimal bestRoleRating) {
        this.bestRoleRating = bestRoleRating;
    }

    public Map<RoleType, BigDecimal> getRoleBreakdown() {
        return roleBreakdown;
    }

    public void setRoleBreakdown(Map<RoleType, BigDecimal> roleBreakdown) {
        this.roleBreakdown = roleBreakdown;
    }

    public Integer getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(Integer totalRatings) {
        this.totalRatings = totalRatings;
    }

    public Integer getTotalMatchesPlayed() {
        return totalMatchesPlayed;
    }

    public void setTotalMatchesPlayed(Integer totalMatchesPlayed) {
        this.totalMatchesPlayed = totalMatchesPlayed;
    }

    @Override
    public String toString() {
        return "PlayerOverallStatsResponse{" +
                "playerProfileId=" + playerProfileId +
                ", alias='" + alias + '\'' +
                ", hybridOVR=" + hybridOVR +
                ", weightedOVR=" + weightedOVR +
                ", classification='" + classification + '\'' +
                ", bestRole=" + bestRole +
                ", versatilityIndex=" + versatilityIndex +
                '}';
    }
}
