package com.atleta.demo.dto.request;

import jakarta.validation.constraints.Size;

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

    @Override
    public String toString() {
        return "UpdatePlayerProfileRequest{" +
                "alias='" + alias + '\'' +
                '}';
    }
}