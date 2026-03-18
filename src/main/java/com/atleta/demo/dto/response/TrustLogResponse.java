package com.atleta.demo.dto.response;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para la información de un log de confianza.
 * Contiene el registro de cambios en el trust score de un jugador.
 */
public class TrustLogResponse {

    /**
     * ID del log de confianza
     */
    private Long id;

    /**
     * Información del jugador
     */
    private PlayerProfileResponse player;

    /**
     * Información del partido relacionado (opcional)
     */
    private MatchResponse match;

    /**
     * Cambio en el trust score
     */
    private Integer cambio;

    /**
     * Motivo del cambio
     */
    private String motivo;

    /**
     * Fecha de creación del log
     */
    private LocalDateTime createdAt;

    // Constructors
    public TrustLogResponse() {
    }

    public TrustLogResponse(Long id, PlayerProfileResponse player, MatchResponse match,
                            Integer cambio, String motivo, LocalDateTime createdAt) {
        this.id = id;
        this.player = player;
        this.match = match;
        this.cambio = cambio;
        this.motivo = motivo;
        this.createdAt = createdAt;
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

    public MatchResponse getMatch() {
        return match;
    }

    public void setMatch(MatchResponse match) {
        this.match = match;
    }

    public Integer getCambio() {
        return cambio;
    }

    public void setCambio(Integer cambio) {
        this.cambio = cambio;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "TrustLogResponse{" +
                "id=" + id +
                ", cambio=" + cambio +
                ", motivo='" + motivo + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}