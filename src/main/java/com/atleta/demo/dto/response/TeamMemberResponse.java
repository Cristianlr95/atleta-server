package com.atleta.demo.dto.response;

import com.atleta.demo.enums.PlayerRole;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para la información de un miembro de equipo.
 * Contiene la información de la membresía de un jugador en un equipo.
 */
public class TeamMemberResponse {

    /**
     * ID de la membresía
     */
    private Long id;

    /**
     * Información del jugador
     */
    private PlayerProfileResponse player;

    /**
     * Rol del jugador en el equipo
     */
    private PlayerRole rol;

    /**
     * Estado activo del miembro
     */
    private Boolean activo;

    /**
     * Fecha de ingreso al equipo
     */
    private LocalDateTime joinedAt;

    // Constructors
    public TeamMemberResponse() {
    }

    public TeamMemberResponse(Long id, PlayerProfileResponse player, PlayerRole rol, 
                              Boolean activo, LocalDateTime joinedAt) {
        this.id = id;
        this.player = player;
        this.rol = rol;
        this.activo = activo;
        this.joinedAt = joinedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PlayerProfileResponse getPlayer() {
        return player;
    }

    public void setPlayer(PlayerProfileResponse player) {
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

    @Override
    public String toString() {
        return "TeamMemberResponse{" +
                "id=" + id +
                ", rol=" + rol +
                ", activo=" + activo +
                ", joinedAt=" + joinedAt +
                '}';
    }
}