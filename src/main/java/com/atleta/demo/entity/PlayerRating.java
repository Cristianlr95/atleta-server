package com.atleta.demo.entity;

import com.atleta.demo.enums.PriorityLevel;
import com.atleta.demo.enums.RoleType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa la calificación actual de un jugador para un rol y nivel de prioridad específicos.
 * Mantiene la calificación actual, estadísticas de partidos jugados y metadatos de actualización.
 */
@Entity
@Table(name = "player_ratings", 
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"player_profile_id", "role_type", "priority_level"},
           name = "uk_player_role_priority"
       ))
public class PlayerRating extends BaseEntity {

    /**
     * Relación con el perfil del jugador
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_profile_id", nullable = false)
    @NotNull(message = "El perfil del jugador es requerido")
    private PlayerProfile playerProfile;

    /**
     * Tipo de rol para esta calificación
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 20)
    @NotNull(message = "El tipo de rol es requerido")
    private RoleType roleType;

    /**
     * Nivel de prioridad para este rol
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", nullable = false, length = 20)
    @NotNull(message = "El nivel de prioridad es requerido")
    private PriorityLevel priorityLevel;

    /**
     * Calificación actual del jugador para este rol y prioridad
     */
    @Column(name = "current_rating", nullable = false, precision = 5, scale = 2)
    @NotNull(message = "La calificación actual es requerida")
    @DecimalMin(value = "0.00", message = "La calificación no puede ser negativa")
    @DecimalMax(value = "999.99", message = "La calificación no puede exceder 999.99")
    private BigDecimal currentRating;

    /**
     * Número de partidos jugados con este rol y prioridad
     */
    @Column(name = "matches_played", nullable = false)
    @NotNull(message = "El número de partidos jugados es requerido")
    @Min(value = 0, message = "El número de partidos jugados no puede ser negativo")
    private Integer matchesPlayed = 0;

    /**
     * Timestamp de la última actualización de calificación
     */
    @Column(name = "last_updated", nullable = false)
    @NotNull(message = "La fecha de última actualización es requerida")
    private LocalDateTime lastUpdated;

    /**
     * Relación con el historial de calificaciones
     */
    @OneToMany(mappedBy = "playerRating", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RatingHistory> ratingHistory = new ArrayList<>();

    // Constructors
    public PlayerRating() {
        // Constructor por defecto para JPA
        this.lastUpdated = LocalDateTime.now();
    }

    public PlayerRating(PlayerProfile playerProfile, RoleType roleType, PriorityLevel priorityLevel) {
        this.playerProfile = playerProfile;
        this.roleType = roleType;
        this.priorityLevel = priorityLevel;
        this.currentRating = BigDecimal.valueOf(priorityLevel.getBaseRating());
        this.matchesPlayed = 0;
        this.lastUpdated = LocalDateTime.now();
    }

    public PlayerRating(PlayerProfile playerProfile, RoleType roleType, PriorityLevel priorityLevel, BigDecimal initialRating) {
        this.playerProfile = playerProfile;
        this.roleType = roleType;
        this.priorityLevel = priorityLevel;
        this.currentRating = initialRating;
        this.matchesPlayed = 0;
        this.lastUpdated = LocalDateTime.now();
    }

    // Getters and Setters
    public PlayerProfile getPlayerProfile() {
        return playerProfile;
    }

    public void setPlayerProfile(PlayerProfile playerProfile) {
        // Remove from old player profile if exists
        if (this.playerProfile != null && this.playerProfile != playerProfile) {
            this.playerProfile.getPlayerRatings().remove(this);
        }
        
        this.playerProfile = playerProfile;
        
        // Add to new player profile if not null and not already present
        if (playerProfile != null && !playerProfile.getPlayerRatings().contains(this)) {
            playerProfile.getPlayerRatings().add(this);
        }
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
        this.lastUpdated = LocalDateTime.now();
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

    public List<RatingHistory> getRatingHistory() {
        return ratingHistory;
    }

    public void setRatingHistory(List<RatingHistory> ratingHistory) {
        this.ratingHistory = ratingHistory;
    }

    // Utility methods
    public void addRatingHistory(RatingHistory history) {
        ratingHistory.add(history);
        history.setPlayerRating(this);
    }

    public void removeRatingHistory(RatingHistory history) {
        ratingHistory.remove(history);
        history.setPlayerRating(null);
    }

    /**
     * Incrementa el contador de partidos jugados
     */
    public void incrementMatchesPlayed() {
        this.matchesPlayed++;
    }

    /**
     * Actualiza la calificación y marca la fecha de actualización
     */
    public void updateRating(BigDecimal newRating) {
        this.currentRating = newRating;
        this.lastUpdated = LocalDateTime.now();
        this.incrementMatchesPlayed();
    }

    /**
     * Verifica si la calificación está por debajo del mínimo base
     */
    public boolean isBelowMinimumRating() {
        return currentRating.compareTo(BigDecimal.valueOf(priorityLevel.getBaseRating())) < 0;
    }

    /**
     * Aplica la calificación mínima base si es necesario
     */
    public void enforceMinimumRating() {
        BigDecimal minimumRating = BigDecimal.valueOf(priorityLevel.getBaseRating());
        if (currentRating.compareTo(minimumRating) < 0) {
            this.currentRating = minimumRating;
            this.lastUpdated = LocalDateTime.now();
        }
    }

    @Override
    public String toString() {
        return "PlayerRating{" +
                "id=" + getId() +
                ", playerProfile=" + (playerProfile != null ? playerProfile.getAtletaUuid() : null) +
                ", roleType=" + roleType +
                ", priorityLevel=" + priorityLevel +
                ", currentRating=" + currentRating +
                ", matchesPlayed=" + matchesPlayed +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}