package com.atleta.demo.dto.response;

import com.atleta.demo.enums.MatchResult;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para la información del historial de un jugador.
 * Contiene el registro inmutable de la participación de un jugador en un partido.
 */
public class PlayerHistoryResponse {

    /**
     * ID del registro de historial
     */
    private Long id;

    /**
     * Información del partido
     */
    private MatchResponse match;

    /**
     * Información del jugador
     */
    private PlayerProfileResponse player;

    /**
     * Información del equipo con el que jugó
     */
    private TeamResponse team;

    /**
     * Posición en la que jugó
     */
    private PositionResponse position;

    /**
     * Goles anotados en el partido
     */
    private Integer goles;

    /**
     * Asistencias realizadas en el partido
     */
    private Integer asistencias;

    /**
     * Resultado del partido para el jugador
     */
    private MatchResult resultado;

    /**
     * XP ganada en el partido
     */
    private Integer xpGanada;

    /**
     * Fecha de creación del registro
     */
    private LocalDateTime createdAt;

    // Constructors
    public PlayerHistoryResponse() {
    }

    public PlayerHistoryResponse(Long id, MatchResponse match, PlayerProfileResponse player,
                                 TeamResponse team, PositionResponse position, Integer goles,
                                 Integer asistencias, MatchResult resultado, Integer xpGanada,
                                 LocalDateTime createdAt) {
        this.id = id;
        this.match = match;
        this.player = player;
        this.team = team;
        this.position = position;
        this.goles = goles;
        this.asistencias = asistencias;
        this.resultado = resultado;
        this.xpGanada = xpGanada;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MatchResponse getMatch() {
        return match;
    }

    public void setMatch(MatchResponse match) {
        this.match = match;
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

    public Integer getGoles() {
        return goles;
    }

    public void setGoles(Integer goles) {
        this.goles = goles;
    }

    public Integer getAsistencias() {
        return asistencias;
    }

    public void setAsistencias(Integer asistencias) {
        this.asistencias = asistencias;
    }

    public MatchResult getResultado() {
        return resultado;
    }

    public void setResultado(MatchResult resultado) {
        this.resultado = resultado;
    }

    public Integer getXpGanada() {
        return xpGanada;
    }

    public void setXpGanada(Integer xpGanada) {
        this.xpGanada = xpGanada;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "PlayerHistoryResponse{" +
                "id=" + id +
                ", goles=" + goles +
                ", asistencias=" + asistencias +
                ", resultado=" + resultado +
                ", xpGanada=" + xpGanada +
                ", createdAt=" + createdAt +
                '}';
    }
}