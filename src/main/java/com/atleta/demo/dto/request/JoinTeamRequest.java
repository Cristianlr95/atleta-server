package com.atleta.demo.dto.request;

import com.atleta.demo.enums.PlayerRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO para que un jugador se una a un equipo.
 * Contiene las validaciones necesarias para la membresía de equipos.
 */
public class JoinTeamRequest {

    /**
     * UUID del jugador que se une al equipo
     */
    @NotNull(message = "El UUID del jugador es obligatorio")
    private UUID playerUuid;

    /**
     * ID del equipo al que se une
     */
    @NotNull(message = "El ID del equipo es obligatorio")
    private Long teamId;

    /**
     * Rol del jugador en el equipo
     */
    @NotNull(message = "El rol del jugador es obligatorio")
    private PlayerRole rol;

    // Constructors
    public JoinTeamRequest() {
    }

    public JoinTeamRequest(UUID playerUuid, Long teamId, PlayerRole rol) {
        this.playerUuid = playerUuid;
        this.teamId = teamId;
        this.rol = rol;
    }

    // Getters and Setters
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public PlayerRole getRol() {
        return rol;
    }

    public void setRol(PlayerRole rol) {
        this.rol = rol;
    }

    @Override
    public String toString() {
        return "JoinTeamRequest{" +
                "playerUuid=" + playerUuid +
                ", teamId=" + teamId +
                ", rol=" + rol +
                '}';
    }
}