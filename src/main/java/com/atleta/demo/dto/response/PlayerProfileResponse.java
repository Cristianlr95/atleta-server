package com.atleta.demo.dto.response;

import com.atleta.demo.enums.GenderType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta para la información del perfil de jugador.
 * Contiene la información específica del contexto de fútbol.
 */
public class PlayerProfileResponse {

    /**
     * UUID del atleta
     */
    private UUID atletaUuid;

    /**
     * Alias del jugador en el contexto de fútbol
     */
    private String alias;

    private String nombre;

    /** Foto elegida por el jugador o importada desde su proveedor de acceso. */
    private String pictureUrl;

    private GenderType genero;

    /**
     * Puntuación de confianza del jugador
     */
    private Integer trustScore;

    /**
     * Fecha de creación del perfil
     */
    private LocalDateTime createdAt;

    /**
     * Posiciones del jugador con sus prioridades
     */
    private List<PlayerPositionResponse> positions;

    // Constructors
    public PlayerProfileResponse() {
    }

    public PlayerProfileResponse(UUID atletaUuid, String alias, GenderType genero, Integer trustScore, LocalDateTime createdAt) {
        this.atletaUuid = atletaUuid;
        this.alias = alias;
        this.genero = genero;
        this.trustScore = trustScore;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public UUID getAtletaUuid() {
        return atletaUuid;
    }

    public void setAtletaUuid(UUID atletaUuid) {
        this.atletaUuid = atletaUuid;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public GenderType getGenero() {
        return genero;
    }

    public void setGenero(GenderType genero) {
        this.genero = genero;
    }

    public Integer getTrustScore() {
        return trustScore;
    }

    public void setTrustScore(Integer trustScore) {
        this.trustScore = trustScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<PlayerPositionResponse> getPositions() {
        return positions;
    }

    public void setPositions(List<PlayerPositionResponse> positions) {
        this.positions = positions;
    }

    @Override
    public String toString() {
        return "PlayerProfileResponse{" +
                "atletaUuid=" + atletaUuid +
                ", alias='" + alias + '\'' +
                ", nombre='" + nombre + '\'' +
                ", trustScore=" + trustScore +
                ", createdAt=" + createdAt +
                '}';
    }
}
