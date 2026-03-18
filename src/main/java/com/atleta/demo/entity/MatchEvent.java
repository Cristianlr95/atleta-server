package com.atleta.demo.entity;

import com.atleta.demo.enums.EventType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa un evento ocurrido durante un partido.
 * Los eventos incluyen goles y asistencias registrados por los jugadores.
 * Requiere confirmación de ambos equipos para ser válido.
 * 
 * Requisitos implementados:
 * - 8.1: Registro de goles y asistencias
 * - 8.2: Confirmación de ambos equipos (local y visitante)
 * - 8.3: Asociación de asistente opcional para goles
 * - 8.4: Trazabilidad de quién registró el evento
 * - 8.5: Actualización automática de estadísticas al confirmar
 */
@Entity
@Table(name = "match_events")
public class MatchEvent extends BaseEntity {

    /**
     * Referencia al partido donde ocurrió el evento
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    @NotNull(message = "El partido es obligatorio")
    private Match match;

    /**
     * Tipo de evento (GOL, ASISTENCIA)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false)
    @NotNull(message = "El tipo de evento es obligatorio")
    private EventType tipoEvento;

    /**
     * Jugador que realizó el evento (goleador o asistente)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    @NotNull(message = "El jugador es obligatorio")
    private PlayerProfile player;

    /**
     * Equipo del jugador que realizó el evento
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    @NotNull(message = "El equipo es obligatorio")
    private Team team;

    /**
     * Jugador que asistió en el gol (opcional, solo para eventos tipo GOL)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assist_player_id")
    private PlayerProfile assistPlayer;

    /**
     * Jugador que registró el evento en el sistema
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by_id", nullable = false)
    @NotNull(message = "El registrador del evento es obligatorio")
    private PlayerProfile registeredBy;

    /**
     * Indica si el equipo local ha confirmado el evento
     */
    @Column(name = "confirmed_by_home", nullable = false)
    private Boolean confirmedByHome = false;

    /**
     * Indica si el equipo visitante ha confirmado el evento
     */
    @Column(name = "confirmed_by_away", nullable = false)
    private Boolean confirmedByAway = false;

    /**
     * Timestamp de cuando se registró el evento
     */
    @CreationTimestamp
    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    /**
     * Timestamp de cuando se confirmó completamente el evento
     */
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    // Constructors
    public MatchEvent() {
        // Constructor por defecto para JPA
    }

    public MatchEvent(Match match, EventType tipoEvento, PlayerProfile player, Team team, PlayerProfile registeredBy) {
        this.match = match;
        this.tipoEvento = tipoEvento;
        this.player = player;
        this.team = team;
        this.registeredBy = registeredBy;
        this.confirmedByHome = false;
        this.confirmedByAway = false;
    }

    public MatchEvent(Match match, EventType tipoEvento, PlayerProfile player, Team team, 
                     PlayerProfile assistPlayer, PlayerProfile registeredBy) {
        this.match = match;
        this.tipoEvento = tipoEvento;
        this.player = player;
        this.team = team;
        this.assistPlayer = assistPlayer;
        this.registeredBy = registeredBy;
        this.confirmedByHome = false;
        this.confirmedByAway = false;
    }

    // Getters and Setters
    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public EventType getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(EventType tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public PlayerProfile getPlayer() {
        return player;
    }

    public void setPlayer(PlayerProfile player) {
        this.player = player;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public PlayerProfile getAssistPlayer() {
        return assistPlayer;
    }

    public void setAssistPlayer(PlayerProfile assistPlayer) {
        this.assistPlayer = assistPlayer;
    }

    public PlayerProfile getRegisteredBy() {
        return registeredBy;
    }

    public void setRegisteredBy(PlayerProfile registeredBy) {
        this.registeredBy = registeredBy;
    }

    public Boolean getConfirmedByHome() {
        return confirmedByHome;
    }

    public void setConfirmedByHome(Boolean confirmedByHome) {
        this.confirmedByHome = confirmedByHome;
    }

    public Boolean getConfirmedByAway() {
        return confirmedByAway;
    }

    public void setConfirmedByAway(Boolean confirmedByAway) {
        this.confirmedByAway = confirmedByAway;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    // Utility methods
    /**
     * Confirma el evento por parte del equipo local
     */
    public void confirmByHome() {
        this.confirmedByHome = true;
        checkAndSetFullConfirmation();
    }

    /**
     * Confirma el evento por parte del equipo visitante
     */
    public void confirmByAway() {
        this.confirmedByAway = true;
        checkAndSetFullConfirmation();
    }

    /**
     * Verifica si el evento está completamente confirmado por ambos equipos
     */
    public boolean isFullyConfirmed() {
        return Boolean.TRUE.equals(confirmedByHome) && Boolean.TRUE.equals(confirmedByAway);
    }

    /**
     * Verifica si el evento es un gol
     */
    public boolean isGol() {
        return EventType.GOL.equals(tipoEvento);
    }

    /**
     * Verifica si el evento es una asistencia
     */
    public boolean isAsistencia() {
        return EventType.ASISTENCIA.equals(tipoEvento);
    }

    /**
     * Verifica si el evento tiene asistente
     */
    public boolean hasAssist() {
        return assistPlayer != null;
    }

    /**
     * Método privado para establecer la confirmación completa
     */
    private void checkAndSetFullConfirmation() {
        if (isFullyConfirmed() && confirmedAt == null) {
            this.confirmedAt = LocalDateTime.now();
        }
    }

    @Override
    public String toString() {
        return "MatchEvent{" +
                "id=" + getId() +
                ", match=" + (match != null ? match.getId() : null) +
                ", tipoEvento=" + tipoEvento +
                ", player=" + (player != null ? player.getAtletaUuid() : null) +
                ", team=" + (team != null ? team.getId() : null) +
                ", assistPlayer=" + (assistPlayer != null ? assistPlayer.getAtletaUuid() : null) +
                ", registeredBy=" + (registeredBy != null ? registeredBy.getAtletaUuid() : null) +
                ", confirmedByHome=" + confirmedByHome +
                ", confirmedByAway=" + confirmedByAway +
                ", registeredAt=" + registeredAt +
                ", confirmedAt=" + confirmedAt +
                '}';
    }
}