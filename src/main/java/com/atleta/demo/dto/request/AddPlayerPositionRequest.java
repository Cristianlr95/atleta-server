package com.atleta.demo.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO para agregar una posición a un jugador.
 * Contiene las validaciones necesarias para las posiciones y prioridades.
 */
public class AddPlayerPositionRequest {

    /**
     * UUID del jugador
     */
    @NotNull(message = "El UUID del jugador es obligatorio")
    private UUID playerUuid;

    /**
     * ID de la posición
     */
    @NotNull(message = "La posición es obligatoria")
    private Long positionId;

    /**
     * Prioridad de la posición (1, 2, 3)
     */
    @NotNull(message = "La prioridad es obligatoria")
    @Min(value = 1, message = "La prioridad debe ser entre 1 y 3")
    @Max(value = 3, message = "La prioridad debe ser entre 1 y 3")
    private Integer prioridad;

    // Constructors
    public AddPlayerPositionRequest() {
    }

    public AddPlayerPositionRequest(UUID playerUuid, Long positionId, Integer prioridad) {
        this.playerUuid = playerUuid;
        this.positionId = positionId;
        this.prioridad = prioridad;
    }

    // Getters and Setters
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public Integer getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(Integer prioridad) {
        this.prioridad = prioridad;
    }

    @Override
    public String toString() {
        return "AddPlayerPositionRequest{" +
                "playerUuid=" + playerUuid +
                ", positionId=" + positionId +
                ", prioridad=" + prioridad +
                '}';
    }
}