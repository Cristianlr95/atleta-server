package com.atleta.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Entidad que representa la relación many-to-many entre PlayerProfile y Position.
 * Incluye información adicional como prioridad (1, 2, 3) y experiencia (XP) del jugador
 * en cada posición específica.
 */
@Entity
@Table(name = "player_positions", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"player_id", "position_id"}),
           @UniqueConstraint(columnNames = {"player_id", "prioridad"})
       })
public class PlayerPosition extends BaseEntity {

    /**
     * Relación con el perfil del jugador
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerProfile player;

    /**
     * Relación con la posición
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", nullable = false)
    private Position position;

    /**
     * Prioridad de la posición para el jugador (1, 2, 3)
     * 1 = Primera preferencia, 2 = Segunda preferencia, 3 = Tercera preferencia
     */
    @Column(name = "prioridad", nullable = false)
    @Min(value = 1, message = "La prioridad debe ser mínimo 1")
    @Max(value = 3, message = "La prioridad debe ser máximo 3")
    private Integer prioridad;

    /**
     * Experiencia (XP) del jugador en esta posición
     */
    @Column(name = "xp", nullable = false)
    @Min(value = 0, message = "La experiencia no puede ser negativa")
    private Integer xp = 0;

    // Constructors
    public PlayerPosition() {
        // Constructor por defecto para JPA
    }

    public PlayerPosition(PlayerProfile player, Position position, Integer prioridad) {
        this.player = player;
        this.position = position;
        this.prioridad = prioridad;
    }

    public PlayerPosition(PlayerProfile player, Position position, Integer prioridad, Integer xp) {
        this.player = player;
        this.position = position;
        this.prioridad = prioridad;
        this.xp = xp;
    }

    // Getters and Setters
    public PlayerProfile getPlayer() {
        return player;
    }

    public void setPlayer(PlayerProfile player) {
        this.player = player;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Integer getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(Integer prioridad) {
        this.prioridad = prioridad;
    }

    public Integer getXp() {
        return xp;
    }

    public void setXp(Integer xp) {
        this.xp = xp;
    }

    // Utility methods
    public void addXp(Integer additionalXp) {
        if (additionalXp != null && additionalXp > 0) {
            this.xp += additionalXp;
        }
    }

    @Override
    public String toString() {
        return "PlayerPosition{" +
                "id=" + getId() +
                ", player=" + (player != null ? player.getAtletaUuid() : null) +
                ", position=" + (position != null ? position.getNombre() : null) +
                ", prioridad=" + prioridad +
                ", xp=" + xp +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}