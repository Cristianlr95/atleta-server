package com.atleta.demo.entity;

import com.atleta.demo.enums.PlayerRole;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa la membresía de un jugador en un equipo.
 * Implementa la relación N-M entre PlayerProfile y Team con información adicional.
 */
@Entity
@Table(name = "team_members")
public class TeamMember extends BaseEntity {

    /**
     * Equipo al que pertenece el miembro
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /**
     * Jugador que es miembro del equipo
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private PlayerProfile player;

    /**
     * Rol del jugador en el equipo
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private PlayerRole rol = PlayerRole.JUGADOR;

    /**
     * Estado activo/inactivo del miembro en el equipo
     */
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /**
     * Fecha de ingreso al equipo
     */
    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    // Constructors
    public TeamMember() {
        // Constructor por defecto para JPA
    }

    public TeamMember(Team team, PlayerProfile player) {
        this.team = team;
        this.player = player;
    }

    public TeamMember(Team team, PlayerProfile player, PlayerRole rol) {
        this.team = team;
        this.player = player;
        this.rol = rol;
    }

    // Getters and Setters
    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public PlayerProfile getPlayer() {
        return player;
    }

    public void setPlayer(PlayerProfile player) {
        this.player = player;
    }

    public PlayerRole getRol() {
        return rol;
    }

    public void setRol(PlayerRole rol) {
        this.rol = rol;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    // Utility methods
    /**
     * Verifica si el miembro es capitán del equipo
     */
    public boolean isCapitan() {
        return PlayerRole.CAPITAN.equals(rol);
    }

    /**
     * Verifica si el miembro es director técnico del equipo
     */
    public boolean isDT() {
        return PlayerRole.DT.equals(rol);
    }

    /**
     * Verifica si el miembro está activo en el equipo
     */
    public boolean isActivo() {
        return Boolean.TRUE.equals(activo);
    }

    /**
     * Desactiva al miembro del equipo
     */
    public void desactivar() {
        this.activo = false;
    }

    /**
     * Reactiva al miembro del equipo
     */
    public void reactivar() {
        this.activo = true;
    }

    @Override
    public String toString() {
        return "TeamMember{" +
                "id=" + getId() +
                ", team=" + (team != null ? team.getNombre() : null) +
                ", player=" + (player != null ? player.getAlias() : null) +
                ", rol=" + rol +
                ", activo=" + activo +
                ", joinedAt=" + joinedAt +
                '}';
    }
}