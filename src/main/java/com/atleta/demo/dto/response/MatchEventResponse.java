package com.atleta.demo.dto.response;

import com.atleta.demo.enums.EventType;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para la información de un evento de partido.
 * Contiene la información de los eventos que ocurren durante un partido.
 */
public class MatchEventResponse {

    /** Version of the event payload contract for additive client evolution. */
    private Integer schemaVersion = 1;

    /**
     * ID del evento
     */
    private Long id;

    /**
     * Información del jugador que realiza el evento
     */
    private PlayerProfileResponse player;

    /**
     * Información del equipo del jugador
     */
    private TeamResponse team;

    /**
     * Tipo de evento
     */
    private EventType eventType;

    /**
     * Jugador que asiste (opcional)
     */
    private PlayerProfileResponse assistPlayer;

    /**
     * Minuto del partido en que ocurre el evento
     */
    private Integer minuto;

    /**
     * Estado de confirmación del equipo local
     */
    private Boolean confirmedByLocal;

    /**
     * Estado de confirmación del equipo visitante
     */
    private Boolean confirmedByVisitante;

    /**
     * Fecha de creación del evento
     */
    private LocalDateTime createdAt;

    // Constructors
    public MatchEventResponse() {
    }

    public MatchEventResponse(Long id, PlayerProfileResponse player, TeamResponse team,
                              EventType eventType, PlayerProfileResponse assistPlayer,
                              Integer minuto, Boolean confirmedByLocal, Boolean confirmedByVisitante,
                              LocalDateTime createdAt) {
        this.id = id;
        this.player = player;
        this.team = team;
        this.eventType = eventType;
        this.assistPlayer = assistPlayer;
        this.minuto = minuto;
        this.confirmedByLocal = confirmedByLocal;
        this.confirmedByVisitante = confirmedByVisitante;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
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

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public PlayerProfileResponse getAssistPlayer() {
        return assistPlayer;
    }

    public void setAssistPlayer(PlayerProfileResponse assistPlayer) {
        this.assistPlayer = assistPlayer;
    }

    public Integer getMinuto() {
        return minuto;
    }

    public void setMinuto(Integer minuto) {
        this.minuto = minuto;
    }

    public Boolean getConfirmedByLocal() {
        return confirmedByLocal;
    }

    public void setConfirmedByLocal(Boolean confirmedByLocal) {
        this.confirmedByLocal = confirmedByLocal;
    }

    public Boolean getConfirmedByVisitante() {
        return confirmedByVisitante;
    }

    public void setConfirmedByVisitante(Boolean confirmedByVisitante) {
        this.confirmedByVisitante = confirmedByVisitante;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "MatchEventResponse{" +
                "id=" + id +
                ", eventType=" + eventType +
                ", minuto=" + minuto +
                ", confirmedByLocal=" + confirmedByLocal +
                ", confirmedByVisitante=" + confirmedByVisitante +
                ", createdAt=" + createdAt +
                '}';
    }
}
