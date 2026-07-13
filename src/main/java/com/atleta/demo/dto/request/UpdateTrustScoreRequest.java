package com.atleta.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO para actualizar el trust score de un jugador.
 * Contiene las validaciones necesarias para los cambios de confianza.
 */
public class UpdateTrustScoreRequest {

    /**
     * UUID del jugador cuyo trust score se actualizará
     */
    private UUID playerUuid;

    /**
     * Cambio en el trust score (puede ser positivo o negativo)
     */
    @NotNull(message = "El cambio en el trust score es obligatorio")
    private Integer cambio;

    /**
     * Motivo del cambio en el trust score
     */
    @NotBlank(message = "El motivo del cambio es obligatorio")
    @Size(max = 255, message = "El motivo no puede exceder 255 caracteres")
    private String motivo;

    /**
     * ID del partido relacionado con el cambio (opcional)
     */
    private Long matchId;

    // Constructors
    public UpdateTrustScoreRequest() {
    }

    public UpdateTrustScoreRequest(UUID playerUuid, Integer cambio, String motivo) {
        this.playerUuid = playerUuid;
        this.cambio = cambio;
        this.motivo = motivo;
    }

    public UpdateTrustScoreRequest(UUID playerUuid, Integer cambio, String motivo, Long matchId) {
        this.playerUuid = playerUuid;
        this.cambio = cambio;
        this.motivo = motivo;
        this.matchId = matchId;
    }

    // Getters and Setters
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
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

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    @Override
    public String toString() {
        return "UpdateTrustScoreRequest{" +
                "playerUuid=" + playerUuid +
                ", cambio=" + cambio +
                ", motivo='" + motivo + '\'' +
                ", matchId=" + matchId +
                '}';
    }
}
