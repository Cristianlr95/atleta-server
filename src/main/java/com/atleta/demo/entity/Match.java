package com.atleta.demo.entity;

import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchGenderCategory;
import com.atleta.demo.enums.MatchStatus;
import com.atleta.demo.enums.MatchType;
import com.atleta.demo.enums.MatchValidationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa un partido de fútbol.
 * Contiene información sobre modalidad, fecha/hora, ubicación, cuota y estado.
 * Mantiene relaciones con equipos participantes, jugadores y eventos del partido.
 */
@Entity
@Table(name = "matches")
public class Match extends BaseEntity {

    /**
     * Modalidad del partido (5v5, 6v6, 7v7)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", nullable = false)
    @NotNull(message = "La modalidad del partido es obligatoria")
    private MatchMode modalidad;

    /**
     * Categoria de convocatoria por genero.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_genero", nullable = false, length = 20)
    private MatchGenderCategory categoriaGenero = MatchGenderCategory.MIXTO;

    /**
     * Competitive context selected when the match is created.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 20)
    private MatchType matchType = MatchType.FRIENDLY;

    /**
     * Fecha y hora programada para el partido
     */
    @Column(name = "fecha_hora_programada", nullable = false)
    @NotNull(message = "La fecha y hora del partido son obligatorias")
    private LocalDateTime fechaHoraProgramada;

    /**
     * Latitud de la ubicación del partido
     */
    @Column(name = "latitud", precision = 10, scale = 8)
    @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90 grados")
    @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90 grados")
    private BigDecimal latitud;

    /**
     * Longitud de la ubicación del partido
     */
    @Column(name = "longitud", precision = 11, scale = 8)
    @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180 grados")
    @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180 grados")
    private BigDecimal longitud;

    /**
     * Cuota económica para participar en el partido
     */
    @Column(name = "cuota", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "La cuota no puede ser negativa")
    private BigDecimal cuota;

    /**
     * Jugador que creó el partido
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creador_user_id", nullable = false)
    @NotNull(message = "El creador del partido es obligatorio")
    private PlayerProfile creador;

    /**
     * Estado actual del partido
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private MatchStatus estado = MatchStatus.CREADO;

    /**
     * Timestamp de cuando se inició el partido
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * Timestamp de cierre del partido.
     */
    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    /**
     * Timestamp de cierre de ventana de votacion MVP.
     */
    @Column(name = "mvp_voting_closed_at")
    private LocalDateTime mvpVotingClosedAt;

    /**
     * Jugador ganador MVP del partido.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mvp_user_id")
    private PlayerProfile mvpUser;

    /**
     * Estado de validacion del partido para habilitar XP.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false)
    private MatchValidationStatus validationStatus = MatchValidationStatus.PENDING;

    /**
     * Motivo de invalidacion/observacion de validacion.
     */
    @Column(name = "validation_reason", length = 255)
    private String validationReason;

    /**
     * Marcador final local del partido.
     */
    @Column(name = "final_score_local", nullable = false)
    private Integer finalScoreLocal = 0;

    /**
     * Marcador final visitante del partido.
     */
    @Column(name = "final_score_away", nullable = false)
    private Integer finalScoreAway = 0;

    /**
     * Timestamp de creación del partido
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Relación con los equipos del partido - EXACTAMENTE 2 equipos por partido
     */
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Size(max = 2, message = "Un partido no puede tener mas de 2 equipos")
    private List<MatchTeam> matchTeams = new ArrayList<>();

    /**
     * Relación con los jugadores participantes del partido
     */
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MatchPlayer> players = new ArrayList<>();

    /**
     * Relación con los eventos del partido
     */
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MatchEvent> events = new ArrayList<>();

    // Constructors
    public Match() {
        // Constructor por defecto para JPA
    }

    public Match(MatchMode modalidad, LocalDateTime fechaHoraProgramada, PlayerProfile creador) {
        this.modalidad = modalidad;
        this.fechaHoraProgramada = fechaHoraProgramada;
        this.creador = creador;
        this.estado = MatchStatus.CREADO;
    }

    public Match(MatchMode modalidad, LocalDateTime fechaHoraProgramada, PlayerProfile creador, 
                 BigDecimal latitud, BigDecimal longitud, BigDecimal cuota) {
        this.modalidad = modalidad;
        this.fechaHoraProgramada = fechaHoraProgramada;
        this.creador = creador;
        this.latitud = latitud;
        this.longitud = longitud;
        this.cuota = cuota;
        this.estado = MatchStatus.CREADO;
    }

    // Getters and Setters
    public MatchMode getModalidad() {
        return modalidad;
    }

    public void setModalidad(MatchMode modalidad) {
        this.modalidad = modalidad;
    }

    public MatchGenderCategory getCategoriaGenero() {
        return categoriaGenero;
    }

    public void setCategoriaGenero(MatchGenderCategory categoriaGenero) {
        this.categoriaGenero = categoriaGenero != null ? categoriaGenero : MatchGenderCategory.MIXTO;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(MatchType matchType) {
        this.matchType = matchType != null ? matchType : MatchType.FRIENDLY;
    }

    public LocalDateTime getFechaHoraProgramada() {
        return fechaHoraProgramada;
    }

    public void setFechaHoraProgramada(LocalDateTime fechaHoraProgramada) {
        this.fechaHoraProgramada = fechaHoraProgramada;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public void setLatitud(BigDecimal latitud) {
        this.latitud = latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    public void setLongitud(BigDecimal longitud) {
        this.longitud = longitud;
    }

    public BigDecimal getCuota() {
        return cuota;
    }

    public void setCuota(BigDecimal cuota) {
        this.cuota = cuota;
    }

    public PlayerProfile getCreador() {
        return creador;
    }

    public void setCreador(PlayerProfile creador) {
        this.creador = creador;
    }

    public MatchStatus getEstado() {
        return estado;
    }

    public void setEstado(MatchStatus estado) {
        this.estado = estado;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(LocalDateTime finalizedAt) {
        this.finalizedAt = finalizedAt;
    }

    public LocalDateTime getMvpVotingClosedAt() {
        return mvpVotingClosedAt;
    }

    public void setMvpVotingClosedAt(LocalDateTime mvpVotingClosedAt) {
        this.mvpVotingClosedAt = mvpVotingClosedAt;
    }

    public PlayerProfile getMvpUser() {
        return mvpUser;
    }

    public void setMvpUser(PlayerProfile mvpUser) {
        this.mvpUser = mvpUser;
    }

    public MatchValidationStatus getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(MatchValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getValidationReason() {
        return validationReason;
    }

    public void setValidationReason(String validationReason) {
        this.validationReason = validationReason;
    }

    public Integer getFinalScoreLocal() {
        return finalScoreLocal;
    }

    public void setFinalScoreLocal(Integer finalScoreLocal) {
        this.finalScoreLocal = finalScoreLocal;
    }

    public Integer getFinalScoreAway() {
        return finalScoreAway;
    }

    public void setFinalScoreAway(Integer finalScoreAway) {
        this.finalScoreAway = finalScoreAway;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<MatchTeam> getMatchTeams() {
        return matchTeams;
    }

    public void setMatchTeams(List<MatchTeam> matchTeams) {
        this.matchTeams = matchTeams;
    }

    public List<MatchPlayer> getPlayers() {
        return players;
    }

    public void setPlayers(List<MatchPlayer> players) {
        this.players = players;
    }

    public List<MatchEvent> getEvents() {
        return events;
    }

    public void setEvents(List<MatchEvent> events) {
        this.events = events;
    }

    // Utility methods
    /**
     * Verifica si el partido tiene exactamente 2 equipos
     */
    public boolean hasExactlyTwoTeams() {
        return matchTeams != null && matchTeams.size() == 2;
    }

    /**
     * Agrega un equipo al partido
     */
    public void addMatchTeam(MatchTeam matchTeam) {
        matchTeams.add(matchTeam);
        matchTeam.setMatch(this);
    }

    /**
     * Remueve un equipo del partido
     */
    public void removeMatchTeam(MatchTeam matchTeam) {
        matchTeams.remove(matchTeam);
        matchTeam.setMatch(null);
    }

    /**
     * Agrega un jugador al partido
     */
    public void addPlayer(MatchPlayer matchPlayer) {
        players.add(matchPlayer);
        matchPlayer.setMatch(this);
    }

    /**
     * Remueve un jugador del partido
     */
    public void removePlayer(MatchPlayer matchPlayer) {
        players.remove(matchPlayer);
        matchPlayer.setMatch(null);
    }

    /**
     * Agrega un evento al partido
     */
    public void addEvent(MatchEvent matchEvent) {
        events.add(matchEvent);
        matchEvent.setMatch(this);
    }

    /**
     * Remueve un evento del partido
     */
    public void removeEvent(MatchEvent matchEvent) {
        events.remove(matchEvent);
        matchEvent.setMatch(null);
    }

    @Override
    public String toString() {
        return "Match{" +
                "id=" + getId() +
                ", modalidad=" + modalidad +
                ", categoriaGenero=" + categoriaGenero +
                ", matchType=" + matchType +
                ", fechaHoraProgramada=" + fechaHoraProgramada +
                ", latitud=" + latitud +
                ", longitud=" + longitud +
                ", cuota=" + cuota +
                ", estado=" + estado +
                ", startedAt=" + startedAt +
                ", finalizedAt=" + finalizedAt +
                ", mvpVotingClosedAt=" + mvpVotingClosedAt +
                ", mvpUser=" + (mvpUser != null ? mvpUser.getAtletaUuid() : null) +
                ", validationStatus=" + validationStatus +
                ", finalScoreLocal=" + finalScoreLocal +
                ", finalScoreAway=" + finalScoreAway +
                ", createdAt=" + createdAt +
                '}';
    }
}
