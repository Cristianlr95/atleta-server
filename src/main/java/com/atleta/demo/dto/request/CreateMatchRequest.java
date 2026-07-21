package com.atleta.demo.dto.request;

import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchGenderCategory;
import com.atleta.demo.enums.MatchType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para la creación de un nuevo partido.
 * Contiene las validaciones de negocio necesarias para la creación de partidos.
 */
public class CreateMatchRequest {

    /**
     * Modalidad del partido (5v5, 6v6, 7v7)
     */
    @NotNull(message = "La modalidad del partido es obligatoria")
    private MatchMode modalidad;

    /**
     * Categoria de convocatoria por genero.
     */
    private MatchGenderCategory categoriaGenero = MatchGenderCategory.MIXTO;

    /**
     * Competitive context. Defaults to FRIENDLY for backwards compatibility.
     */
    private MatchType matchType = MatchType.FRIENDLY;

    /**
     * Fecha y hora programada para el partido
     */
    @NotNull(message = "La fecha y hora del partido son obligatorias")
    private LocalDateTime fechaHoraProgramada;

    /**
     * Latitud de la ubicación del partido
     */
    @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90 grados")
    @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90 grados")
    private BigDecimal latitud;

    /**
     * Longitud de la ubicación del partido
     */
    @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180 grados")
    @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180 grados")
    private BigDecimal longitud;

    /**
     * Cuota económica para participar en el partido
     */
    @DecimalMin(value = "0.0", message = "La cuota no puede ser negativa")
    private BigDecimal cuota;

    /**
     * UUID del jugador que crea el partido
     */
    @NotNull(message = "El creador del partido es obligatorio")
    private UUID creadorUuid;

    // Constructors
    public CreateMatchRequest() {
    }

    public CreateMatchRequest(MatchMode modalidad, LocalDateTime fechaHoraProgramada, UUID creadorUuid) {
        this.modalidad = modalidad;
        this.fechaHoraProgramada = fechaHoraProgramada;
        this.creadorUuid = creadorUuid;
    }

    public CreateMatchRequest(MatchMode modalidad, LocalDateTime fechaHoraProgramada, UUID creadorUuid,
                              BigDecimal latitud, BigDecimal longitud, BigDecimal cuota) {
        this.modalidad = modalidad;
        this.fechaHoraProgramada = fechaHoraProgramada;
        this.creadorUuid = creadorUuid;
        this.latitud = latitud;
        this.longitud = longitud;
        this.cuota = cuota;
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
        this.categoriaGenero = categoriaGenero;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(MatchType matchType) {
        this.matchType = matchType;
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

    public UUID getCreadorUuid() {
        return creadorUuid;
    }

    public void setCreadorUuid(UUID creadorUuid) {
        this.creadorUuid = creadorUuid;
    }

    @Override
    public String toString() {
        return "CreateMatchRequest{" +
                "modalidad=" + modalidad +
                ", categoriaGenero=" + categoriaGenero +
                ", matchType=" + matchType +
                ", fechaHoraProgramada=" + fechaHoraProgramada +
                ", latitud=" + latitud +
                ", longitud=" + longitud +
                ", cuota=" + cuota +
                ", creadorUuid=" + creadorUuid +
                '}';
    }
}
