package com.atleta.demo.dto.response;

import com.atleta.demo.enums.PriorityLevel;
import com.atleta.demo.enums.RoleType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para las calificaciones de jugadores.
 * Contiene la información de calificación actual de un jugador para un rol específico.
 */
public class PlayerRatingResponse {

    private Long id;
    private UUID playerProfileId;
    private String playerAlias;
    private RoleType roleType;
    private PriorityLevel priorityLevel;
    private BigDecimal currentRating;
    private Integer matchesPlayed;
    private LocalDateTime lastUpdated;

    // Constructors
    public PlayerRatingResponse() {
    }

    public PlayerRatingResponse(Long id, UUID playerProfileId, String playerAlias, 
                               RoleType roleType, PriorityLevel priorityLevel,
                               BigDecimal currentRating, Integer matchesPlayed, 
                               LocalDateTime lastUpdated) {
        this.id = id;
        this.playerProfileId = playerProfileId;
        this.playerAlias = playerAlias;
        this.roleType = roleType;
        this.priorityLevel = priorityLevel;
        this.currentRating = currentRating;
        this.matchesPlayed = matchesPlayed;
        this.lastUpdated = lastUpdated;
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

    public BigDecimal getCurrentRating() {
        return currentRating;
    }

    public void setCurrentRating(BigDecimal currentRating) {
        this.currentRating = currentRating;
    }

    public Integer getMatchesPlayed() {
        return matchesPlayed;
    }

    public void setMatchesPlayed(Integer matchesPlayed) {
        this.matchesPlayed = matchesPlayed;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "PlayerRatingResponse{" +
                "id=" + id +
                ", playerProfileId=" + playerProfileId +
                ", playerAlias='" + playerAlias + '\'' +
                ", roleType=" + roleType +
                ", priorityLevel=" + priorityLevel +
                ", currentRating=" + currentRating +
                ", matchesPlayed=" + matchesPlayed +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}