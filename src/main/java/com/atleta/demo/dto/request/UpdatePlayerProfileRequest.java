package com.atleta.demo.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * DTO para la actualización de información de un perfil de jugador.
 * Permite actualizar el alias del jugador.
 */
public class UpdatePlayerProfileRequest {

    /**
     * Nuevo alias del jugador (opcional)
     */
    @Size(max = 50, message = "El alias no puede exceder 50 caracteres")
    private String alias;

    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;

    @Size(min = 3, max = 3, message = "Debes seleccionar exactamente 3 posiciones")
    private List<@NotNull Long> positionIds;

    // Constructors
    public UpdatePlayerProfileRequest() {
    }

    public UpdatePlayerProfileRequest(String alias) {
        this.alias = alias;
    }

    // Getters and Setters
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

    public List<Long> getPositionIds() {
        return positionIds;
    }

    public void setPositionIds(List<Long> positionIds) {
        this.positionIds = positionIds;
    }

    @Override
    public String toString() {
        return "UpdatePlayerProfileRequest{" +
                "alias='" + alias + '\'' +
                ", nombre='" + nombre + '\'' +
                ", positionIds=" + positionIds +
                '}';
    }
}
