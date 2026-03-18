package com.atleta.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO para la creación de un nuevo equipo.
 * Contiene las validaciones de negocio necesarias para la creación de equipos.
 */
public class CreateTeamRequest {

    /**
     * Nombre del equipo (único en el sistema)
     */
    @NotBlank(message = "El nombre del equipo es obligatorio")
    @Size(max = 100, message = "El nombre del equipo no puede exceder 100 caracteres")
    private String nombre;

    /**
     * URL del logo del equipo (opcional)
     */
    private String logoUrl;

    /**
     * Año de fundación del equipo (opcional)
     */
    private Integer anioFundacion;

    /**
     * UUID del jugador que crea el equipo
     */
    @NotNull(message = "El creador del equipo es obligatorio")
    private UUID creadorUuid;

    // Constructors
    public CreateTeamRequest() {
    }

    public CreateTeamRequest(String nombre, UUID creadorUuid) {
        this.nombre = nombre;
        this.creadorUuid = creadorUuid;
    }

    public CreateTeamRequest(String nombre, UUID creadorUuid, String logoUrl, Integer anioFundacion) {
        this.nombre = nombre;
        this.creadorUuid = creadorUuid;
        this.logoUrl = logoUrl;
        this.anioFundacion = anioFundacion;
    }

    // Getters and Setters
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public Integer getAnioFundacion() {
        return anioFundacion;
    }

    public void setAnioFundacion(Integer anioFundacion) {
        this.anioFundacion = anioFundacion;
    }

    public UUID getCreadorUuid() {
        return creadorUuid;
    }

    public void setCreadorUuid(UUID creadorUuid) {
        this.creadorUuid = creadorUuid;
    }

    @Override
    public String toString() {
        return "CreateTeamRequest{" +
                "nombre='" + nombre + '\'' +
                ", logoUrl='" + logoUrl + '\'' +
                ", anioFundacion=" + anioFundacion +
                ", creadorUuid=" + creadorUuid +
                '}';
    }
}