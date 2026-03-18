package com.atleta.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad que representa el perfil específico de fútbol de un atleta.
 * Mantiene una relación uno-a-uno con Athlete y contiene información
 * específica del contexto deportivo como alias, trust score y posiciones.
 */
@Entity
@Table(name = "player_profiles")
public class PlayerProfile {

    /**
     * UUID del atleta - usado como PK y FK hacia Athlete
     */
    @Id
    @Column(name = "atleta_uuid", nullable = false)
    private UUID atletaUuid;

    /**
     * Relación uno-a-uno con Athlete usando @MapsId
     */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "atleta_uuid")
    private Athlete athlete;

    /**
     * Alias del jugador en el contexto de fútbol (opcional)
     */
    @Column(name = "alias")
    @Size(max = 50, message = "El alias no puede exceder 50 caracteres")
    private String alias;

    /**
     * Puntuación de confianza del jugador (valor por defecto: 100)
     */
    @Column(name = "trust_score", nullable = false)
    @Min(value = 0, message = "El trust score no puede ser menor a 0")
    @Max(value = 1000, message = "El trust score no puede ser mayor a 1000")
    private Integer trustScore = 100;

    /**
     * Timestamp de creación del perfil
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp de última actualización
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Versión para optimistic locking
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Relación con las posiciones del jugador
     */
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PlayerPosition> positions = new ArrayList<>();

    /**
     * Relación con los logs de cambios de confianza
     */
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TrustLog> trustLogs = new ArrayList<>();

    /**
     * Relación con las calificaciones del jugador por rol y prioridad
     */
    @OneToMany(mappedBy = "playerProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PlayerRating> playerRatings = new ArrayList<>();

    // Constructors
    public PlayerProfile() {
        // Constructor por defecto para JPA
    }

    public PlayerProfile(Athlete athlete) {
        this.athlete = athlete;
        this.atletaUuid = athlete.getAtletaUuid();
    }

    public PlayerProfile(Athlete athlete, String alias) {
        this.athlete = athlete;
        this.atletaUuid = athlete.getAtletaUuid();
        this.alias = alias;
    }

    // Getters and Setters
    public UUID getAtletaUuid() {
        return atletaUuid;
    }

    public void setAtletaUuid(UUID atletaUuid) {
        this.atletaUuid = atletaUuid;
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public void setAthlete(Athlete athlete) {
        this.athlete = athlete;
        if (athlete != null) {
            this.atletaUuid = athlete.getAtletaUuid();
        }
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Integer getTrustScore() {
        return trustScore;
    }

    public void setTrustScore(Integer trustScore) {
        this.trustScore = trustScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<PlayerPosition> getPositions() {
        return positions;
    }

    public void setPositions(List<PlayerPosition> positions) {
        this.positions = positions;
    }

    public List<TrustLog> getTrustLogs() {
        return trustLogs;
    }

    public void setTrustLogs(List<TrustLog> trustLogs) {
        this.trustLogs = trustLogs;
    }

    public List<PlayerRating> getPlayerRatings() {
        return playerRatings;
    }

    public void setPlayerRatings(List<PlayerRating> playerRatings) {
        this.playerRatings = playerRatings;
    }

    // Utility methods
    public void addPosition(PlayerPosition position) {
        positions.add(position);
        position.setPlayer(this);
    }

    public void removePosition(PlayerPosition position) {
        positions.remove(position);
        position.setPlayer(null);
    }

    public void addTrustLog(TrustLog trustLog) {
        trustLogs.add(trustLog);
        trustLog.setPlayer(this);
    }

    public void removeTrustLog(TrustLog trustLog) {
        trustLogs.remove(trustLog);
        trustLog.setPlayer(null);
    }

    public void addPlayerRating(PlayerRating playerRating) {
        playerRatings.add(playerRating);
        playerRating.setPlayerProfile(this);
    }

    public void removePlayerRating(PlayerRating playerRating) {
        playerRatings.remove(playerRating);
        playerRating.setPlayerProfile(null);
    }

    // Convenience methods for working with player ratings
    
    /**
     * Obtiene la calificación del jugador para un rol y prioridad específicos.
     * 
     * @param roleType Tipo de rol
     * @param priorityLevel Nivel de prioridad
     * @return Optional con la calificación si existe
     */
    public java.util.Optional<PlayerRating> getRatingByRoleAndPriority(com.atleta.demo.enums.RoleType roleType, 
                                                                       com.atleta.demo.enums.PriorityLevel priorityLevel) {
        return playerRatings.stream()
                .filter(rating -> rating.getRoleType() == roleType && rating.getPriorityLevel() == priorityLevel)
                .findFirst();
    }

    /**
     * Obtiene todas las calificaciones del jugador para un rol específico.
     * 
     * @param roleType Tipo de rol
     * @return Lista de calificaciones para el rol especificado
     */
    public List<PlayerRating> getRatingsByRole(com.atleta.demo.enums.RoleType roleType) {
        return playerRatings.stream()
                .filter(rating -> rating.getRoleType() == roleType)
                .toList();
    }

    /**
     * Obtiene todas las calificaciones del jugador para un nivel de prioridad específico.
     * 
     * @param priorityLevel Nivel de prioridad
     * @return Lista de calificaciones para el nivel de prioridad especificado
     */
    public List<PlayerRating> getRatingsByPriority(com.atleta.demo.enums.PriorityLevel priorityLevel) {
        return playerRatings.stream()
                .filter(rating -> rating.getPriorityLevel() == priorityLevel)
                .toList();
    }

    /**
     * Obtiene la calificación más alta del jugador independientemente del rol.
     * 
     * @return Optional con la calificación más alta
     */
    public java.util.Optional<PlayerRating> getHighestRating() {
        return playerRatings.stream()
                .max((r1, r2) -> r1.getCurrentRating().compareTo(r2.getCurrentRating()));
    }

    /**
     * Verifica si el jugador tiene calificaciones para un rol específico.
     * 
     * @param roleType Tipo de rol
     * @return true si tiene calificaciones para el rol, false en caso contrario
     */
    public boolean hasRatingForRole(com.atleta.demo.enums.RoleType roleType) {
        return playerRatings.stream()
                .anyMatch(rating -> rating.getRoleType() == roleType);
    }

    /**
     * Cuenta el número de roles diferentes para los que el jugador tiene calificaciones.
     * 
     * @return Número de roles diferentes
     */
    public long getDistinctRoleCount() {
        return playerRatings.stream()
                .map(PlayerRating::getRoleType)
                .distinct()
                .count();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        PlayerProfile that = (PlayerProfile) obj;
        return atletaUuid != null && atletaUuid.equals(that.atletaUuid);
    }

    @Override
    public int hashCode() {
        return atletaUuid != null ? atletaUuid.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "PlayerProfile{" +
                "atletaUuid=" + atletaUuid +
                ", alias='" + alias + '\'' +
                ", trustScore=" + trustScore +
                ", createdAt=" + createdAt +
                '}';
    }
}