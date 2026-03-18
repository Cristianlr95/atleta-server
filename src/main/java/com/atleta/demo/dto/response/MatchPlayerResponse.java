package com.atleta.demo.dto.response;

import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.enums.MatchTeamSide;

/**
 * DTO de respuesta para la información de un jugador en un partido.
 * Contiene la información de la participación de un jugador en un partido específico.
 */
public class MatchPlayerResponse {

    /**
     * ID de la participación del jugador
     */
    private Long id;

    /**
     * Información del jugador
     */
    private PlayerProfileResponse player;

    /**
     * Información del equipo con el que participa
     */
    private TeamResponse team;

    /**
     * Posición en la que juega
     */
    private PositionResponse position;

    /**
     * Rol del jugador en el partido
     */
    private PlayerRole rol;

    /**
     * Estado de confirmación de la participación
     */
    private Boolean confirmado;

    private MatchTeamSide teamSide;

    // Constructors
    public MatchPlayerResponse() {
    }

    public MatchPlayerResponse(Long id, PlayerProfileResponse player, TeamResponse team,
                               PositionResponse position, PlayerRole rol, Boolean confirmado, MatchTeamSide teamSide) {
        this.id = id;
        this.player = player;
        this.team = team;
        this.position = position;
        this.rol = rol;
        this.confirmado = confirmado;
        this.teamSide = teamSide;
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

    public TeamResponse getTeam() {
        return team;
    }

    public void setTeam(TeamResponse team) {
        this.team = team;
    }

    public PositionResponse getPosition() {
        return position;
    }

    public void setPosition(PositionResponse position) {
        this.position = position;
    }

    public PlayerRole getRol() {
        return rol;
    }

    public void setRol(PlayerRole rol) {
        this.rol = rol;
    }

    public Boolean getConfirmado() {
        return confirmado;
    }

    public void setConfirmado(Boolean confirmado) {
        this.confirmado = confirmado;
    }

    public MatchTeamSide getTeamSide() {
        return teamSide;
    }

    public void setTeamSide(MatchTeamSide teamSide) {
        this.teamSide = teamSide;
    }

    @Override
    public String toString() {
        return "MatchPlayerResponse{" +
                "id=" + id +
                ", rol=" + rol +
                ", confirmado=" + confirmado +
                ", teamSide=" + teamSide +
                '}';
    }
}
