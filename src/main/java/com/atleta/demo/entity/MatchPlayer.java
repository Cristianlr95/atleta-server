package com.atleta.demo.entity;

import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.enums.MatchTeamSide;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * Entidad que representa la participación de un jugador en un partido específico.
 * Mantiene la relación entre Match, Team, PlayerProfile y Position,
 * incluyendo el rol del jugador y su confirmación de participación.
 */
@Entity
@Table(name = "match_players",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"match_id", "user_id"})
       })
public class MatchPlayer extends BaseEntity {

    /**
     * Referencia al partido
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    @NotNull(message = "El partido es obligatorio")
    private Match match;

    /**
     * Referencia al equipo en el que participa el jugador
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    @NotNull(message = "El equipo es obligatorio")
    private Team team;

    /**
     * Referencia al perfil del jugador
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "El jugador es obligatorio")
    private PlayerProfile player;

    /**
     * Posición en la que jugará el jugador en este partido
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", nullable = false)
    @NotNull(message = "La posición es obligatoria")
    private Position position;

    /**
     * Rol del jugador en este partido específico
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    @NotNull(message = "El rol del jugador es obligatorio")
    private PlayerRole rol;

    /**
     * Indica si el jugador ha confirmado su participación
     */
    @Column(name = "confirmado", nullable = false)
    private Boolean confirmado = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_side", length = 16)
    private MatchTeamSide teamSide;

    // Constructors
    public MatchPlayer() {
        // Constructor por defecto para JPA
    }

    public MatchPlayer(Match match, Team team, PlayerProfile player, Position position, PlayerRole rol) {
        this.match = match;
        this.team = team;
        this.player = player;
        this.position = position;
        this.rol = rol;
        this.confirmado = false;
    }

    // Getters and Setters
    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

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

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
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

    // Utility methods
    /**
     * Confirma la participación del jugador
     */
    public void confirmarParticipacion() {
        this.confirmado = true;
    }

    /**
     * Cancela la confirmación de participación
     */
    public void cancelarConfirmacion() {
        this.confirmado = false;
    }

    /**
     * Verifica si el jugador ha confirmado su participación
     */
    public boolean isConfirmado() {
        return Boolean.TRUE.equals(confirmado);
    }

    /**
     * Verifica si el jugador es capitán en este partido
     */
    public boolean isCapitan() {
        return PlayerRole.CAPITAN.equals(rol);
    }

    /**
     * Verifica si el jugador es DT en este partido
     */
    public boolean isDT() {
        return PlayerRole.DT.equals(rol);
    }

    /**
     * Verifica si el jugador es jugador regular en este partido
     */
    public boolean isJugadorRegular() {
        return PlayerRole.JUGADOR.equals(rol);
    }

    @Override
    public String toString() {
        return "MatchPlayer{" +
                "id=" + getId() +
                ", match=" + (match != null ? match.getId() : null) +
                ", team=" + (team != null ? team.getId() : null) +
                ", player=" + (player != null ? player.getAtletaUuid() : null) +
                ", position=" + (position != null ? position.getId() : null) +
                ", rol=" + rol +
                ", confirmado=" + confirmado +
                ", teamSide=" + teamSide +
                '}';
    }
}
