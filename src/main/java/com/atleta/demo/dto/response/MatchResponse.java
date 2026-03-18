package com.atleta.demo.dto.response;

import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchGenderCategory;
import com.atleta.demo.enums.MatchStatus;
import com.atleta.demo.enums.MatchValidationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta para la información de un partido.
 * Contiene la información completa del partido incluyendo equipos, jugadores y eventos.
 */
public class MatchResponse {

    /**
     * ID del partido
     */
    private Long id;

    /**
     * Modalidad del partido
     */
    private MatchMode modalidad;

    /**
     * Categoria de convocatoria por genero.
     */
    private MatchGenderCategory categoriaGenero;

    /**
     * Fecha y hora programada del partido
     */
    private LocalDateTime fechaHoraProgramada;

    /**
     * Latitud de la ubicación
     */
    private BigDecimal latitud;

    /**
     * Longitud de la ubicación
     */
    private BigDecimal longitud;

    /**
     * Cuota económica del partido
     */
    private BigDecimal cuota;

    /**
     * Información del creador del partido
     */
    private PlayerProfileResponse creador;

    /**
     * Estado actual del partido
     */
    private MatchStatus estado;

    /**
     * Fecha y hora de inicio del partido
     */
    private LocalDateTime startedAt;

    /**
     * Fecha y hora de finalizacion del partido.
     */
    private LocalDateTime finalizedAt;

    /**
     * Estado de validacion para cierre con XP.
     */
    private MatchValidationStatus validationStatus;

    /**
     * Motivo de validacion/invalidez.
     */
    private String validationReason;

    /**
     * Marcador final local.
     */
    private Integer finalScoreLocal;

    /**
     * Marcador final visitante.
     */
    private Integer finalScoreAway;

    /**
     * Indica que el partido supero 1 hora desde inicio y esta pendiente de cierre.
     */
    private Boolean closePending;

    /**
     * Fecha y hora de cierre de la ventana de votacion MVP.
     */
    private LocalDateTime mvpVotingClosedAt;

    /**
     * MVP definitivo del partido, si ya fue resuelto.
     */
    private PlayerProfileResponse mvpUser;

    /**
     * Fecha de creación del partido
     */
    private LocalDateTime createdAt;

    /**
     * Equipos participantes en el partido
     */
    private List<MatchTeamResponse> matchTeams;

    /**
     * Jugadores participantes en el partido
     */
    private List<MatchPlayerResponse> players;

    /**
     * Eventos del partido
     */
    private List<MatchEventResponse> events;

    // Constructors
    public MatchResponse() {
    }

    public MatchResponse(Long id, MatchMode modalidad, LocalDateTime fechaHoraProgramada,
                         BigDecimal latitud, BigDecimal longitud, BigDecimal cuota,
                         PlayerProfileResponse creador, MatchStatus estado,
                         LocalDateTime startedAt, LocalDateTime createdAt) {
        this.id = id;
        this.modalidad = modalidad;
        this.fechaHoraProgramada = fechaHoraProgramada;
        this.latitud = latitud;
        this.longitud = longitud;
        this.cuota = cuota;
        this.creador = creador;
        this.estado = estado;
        this.startedAt = startedAt;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
        this.categoriaGenero = categoriaGenero;
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

    public PlayerProfileResponse getCreador() {
        return creador;
    }

    public void setCreador(PlayerProfileResponse creador) {
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

    public Boolean getClosePending() {
        return closePending;
    }

    public void setClosePending(Boolean closePending) {
        this.closePending = closePending;
    }

    public LocalDateTime getMvpVotingClosedAt() {
        return mvpVotingClosedAt;
    }

    public void setMvpVotingClosedAt(LocalDateTime mvpVotingClosedAt) {
        this.mvpVotingClosedAt = mvpVotingClosedAt;
    }

    public PlayerProfileResponse getMvpUser() {
        return mvpUser;
    }

    public void setMvpUser(PlayerProfileResponse mvpUser) {
        this.mvpUser = mvpUser;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<MatchTeamResponse> getMatchTeams() {
        return matchTeams;
    }

    public void setMatchTeams(List<MatchTeamResponse> matchTeams) {
        this.matchTeams = matchTeams;
    }

    public List<MatchPlayerResponse> getPlayers() {
        return players;
    }

    public void setPlayers(List<MatchPlayerResponse> players) {
        this.players = players;
    }

    public List<MatchEventResponse> getEvents() {
        return events;
    }

    public void setEvents(List<MatchEventResponse> events) {
        this.events = events;
    }

    @Override
    public String toString() {
        return "MatchResponse{" +
                "id=" + id +
                ", modalidad=" + modalidad +
                ", categoriaGenero=" + categoriaGenero +
                ", fechaHoraProgramada=" + fechaHoraProgramada +
                ", estado=" + estado +
                ", createdAt=" + createdAt +
                '}';
    }
}
