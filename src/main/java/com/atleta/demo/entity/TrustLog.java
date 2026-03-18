package com.atleta.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que registra todos los cambios en el trust score de los jugadores.
 * Proporciona trazabilidad completa de los cambios de confianza en el sistema.
 * 
 * Requisitos implementados:
 * - 10.1: Registro de cambios en trust_score con referencia a trust_logs
 * - 10.2: Asociación de cambios de confianza a partidos específicos (opcional)
 * - 10.3: Inclusión del motivo del cambio
 * - 10.4: Mantenimiento de historial completo de cambios de confianza
 * - 10.5: Actualización automática del perfil del jugador al calcular confianza
 */
@Entity
@Table(name = "trust_logs")
public class TrustLog extends BaseEntity {

    /**
     * Referencia al jugador cuyo trust score cambió
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "El jugador es obligatorio")
    private PlayerProfile player;

    /**
     * Referencia al partido relacionado con el cambio (opcional)
     * Puede ser null si el cambio no está relacionado con un partido específico
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    /**
     * Valor del cambio en el trust score
     * Puede ser positivo (incremento) o negativo (decremento)
     */
    @Column(name = "cambio", nullable = false)
    @NotNull(message = "El valor del cambio es obligatorio")
    private Integer cambio;

    /**
     * Trust score anterior antes del cambio
     */
    @Column(name = "trust_score_anterior", nullable = false)
    @NotNull(message = "El trust score anterior es obligatorio")
    private Integer trustScoreAnterior;

    /**
     * Trust score nuevo después del cambio
     */
    @Column(name = "trust_score_nuevo", nullable = false)
    @NotNull(message = "El trust score nuevo es obligatorio")
    private Integer trustScoreNuevo;

    /**
     * Motivo del cambio en el trust score
     */
    @Column(name = "motivo", nullable = false)
    @NotBlank(message = "El motivo del cambio es obligatorio")
    @Size(max = 255, message = "El motivo no puede exceder 255 caracteres")
    private String motivo;

    /**
     * Jugador o sistema que realizó el cambio (opcional)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private PlayerProfile changedBy;

    /**
     * Timestamp de cuando se realizó el cambio
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public TrustLog() {
        // Constructor por defecto para JPA
    }

    /**
     * Constructor para cambios relacionados con partidos
     */
    public TrustLog(PlayerProfile player, Match match, Integer cambio, Integer trustScoreAnterior, 
                   Integer trustScoreNuevo, String motivo, PlayerProfile changedBy) {
        this.player = player;
        this.match = match;
        this.cambio = cambio;
        this.trustScoreAnterior = trustScoreAnterior;
        this.trustScoreNuevo = trustScoreNuevo;
        this.motivo = motivo;
        this.changedBy = changedBy;
    }

    /**
     * Constructor para cambios no relacionados con partidos
     */
    public TrustLog(PlayerProfile player, Integer cambio, Integer trustScoreAnterior, 
                   Integer trustScoreNuevo, String motivo, PlayerProfile changedBy) {
        this.player = player;
        this.cambio = cambio;
        this.trustScoreAnterior = trustScoreAnterior;
        this.trustScoreNuevo = trustScoreNuevo;
        this.motivo = motivo;
        this.changedBy = changedBy;
    }

    /**
     * Constructor para cambios automáticos del sistema
     */
    public TrustLog(PlayerProfile player, Integer cambio, Integer trustScoreAnterior, 
                   Integer trustScoreNuevo, String motivo) {
        this.player = player;
        this.cambio = cambio;
        this.trustScoreAnterior = trustScoreAnterior;
        this.trustScoreNuevo = trustScoreNuevo;
        this.motivo = motivo;
    }

    // Getters and Setters
    public PlayerProfile getPlayer() {
        return player;
    }

    public void setPlayer(PlayerProfile player) {
        this.player = player;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public Integer getCambio() {
        return cambio;
    }

    public void setCambio(Integer cambio) {
        this.cambio = cambio;
    }

    public Integer getTrustScoreAnterior() {
        return trustScoreAnterior;
    }

    public void setTrustScoreAnterior(Integer trustScoreAnterior) {
        this.trustScoreAnterior = trustScoreAnterior;
    }

    public Integer getTrustScoreNuevo() {
        return trustScoreNuevo;
    }

    public void setTrustScoreNuevo(Integer trustScoreNuevo) {
        this.trustScoreNuevo = trustScoreNuevo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public PlayerProfile getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(PlayerProfile changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Utility methods
    /**
     * Verifica si el cambio fue un incremento en el trust score
     */
    public boolean isIncremento() {
        return cambio != null && cambio > 0;
    }

    /**
     * Verifica si el cambio fue un decremento en el trust score
     */
    public boolean isDecremento() {
        return cambio != null && cambio < 0;
    }

    /**
     * Verifica si el cambio está relacionado con un partido
     */
    public boolean isRelatedToMatch() {
        return match != null;
    }

    /**
     * Verifica si el cambio fue realizado por el sistema automáticamente
     */
    public boolean isSystemChange() {
        return changedBy == null;
    }

    /**
     * Verifica si el cambio fue realizado manualmente por un usuario
     */
    public boolean isManualChange() {
        return changedBy != null;
    }

    /**
     * Obtiene el valor absoluto del cambio
     */
    public Integer getAbsoluteCambio() {
        return cambio != null ? Math.abs(cambio) : 0;
    }

    @Override
    public String toString() {
        return "TrustLog{" +
                "id=" + getId() +
                ", player=" + (player != null ? player.getAtletaUuid() : null) +
                ", match=" + (match != null ? match.getId() : null) +
                ", cambio=" + cambio +
                ", trustScoreAnterior=" + trustScoreAnterior +
                ", trustScoreNuevo=" + trustScoreNuevo +
                ", motivo='" + motivo + '\'' +
                ", changedBy=" + (changedBy != null ? changedBy.getAtletaUuid() : null) +
                ", createdAt=" + createdAt +
                '}';
    }
}