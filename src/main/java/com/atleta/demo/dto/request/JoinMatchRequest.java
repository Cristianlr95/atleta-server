package com.atleta.demo.dto.request;

import com.atleta.demo.enums.PlayerRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO para que un jugador se una a un partido.
 * Contiene las validaciones necesarias para la participación en partidos.
 */
public class JoinMatchRequest {

    /**
     * UUID del jugador que se une al partido
     */
    @NotNull(message = "El UUID del jugador es obligatorio")
    private UUID playerUuid;

    /**
     * ID del partido al que se une
     */
    @NotNull(message = "El ID del partido es obligatorio")
    private Long matchId;

    /**
     * ID del equipo con el que participa
     */
    @NotNull(message = "El ID del equipo es obligatorio")
    private Long teamId;

    /**
     * ID de la posición en la que jugará
     */
    @NotNull(message = "La posición es obligatoria")
    private Long positionId;

    /**
     * Rol del jugador en el partido
     */
    @NotNull(message = "El rol del jugador es obligatorio")
    private PlayerRole rol;

    // Constructors
    public JoinMatchRequest() {
    }

    public JoinMatchRequest(UUID playerUuid, Long matchId, Long teamId, Long positionId, PlayerRole rol) {
        this.playerUuid = playerUuid;
        this.matchId = matchId;
        this.teamId = teamId;
        this.positionId = positionId;
        this.rol = rol;
    }

    // Getters and Setters
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public PlayerRole getRol() {
        return rol;
    }

    public void setRol(PlayerRole rol) {
        this.rol = rol;
    }

    @Override
    public String toString() {
        return "JoinMatchRequest{" +
                "playerUuid=" + playerUuid +
                ", matchId=" + matchId +
                ", teamId=" + teamId +
                ", positionId=" + positionId +
                ", rol=" + rol +
                '}';
    }
}