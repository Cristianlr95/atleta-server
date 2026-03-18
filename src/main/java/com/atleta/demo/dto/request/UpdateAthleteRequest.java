package com.atleta.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para la actualización de información de un atleta.
 */
public class UpdateAthleteRequest {

    /**
     * Nuevo nombre del atleta
     */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;

    // Constructors
    public UpdateAthleteRequest() {
    }

    public UpdateAthleteRequest(String nombre) {
        this.nombre = nombre;
    }

    // Getters and Setters
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return "UpdateAthleteRequest{" +
                "nombre='" + nombre + '\'' +
                '}';
    }
}