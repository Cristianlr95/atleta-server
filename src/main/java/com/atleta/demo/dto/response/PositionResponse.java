package com.atleta.demo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta para posiciones de fútbol.
 * Representa una posición del catálogo fijo del sistema.
 */
@Schema(description = "Información de una posición de fútbol")
public class PositionResponse {

    @Schema(description = "ID único de la posición", example = "1")
    private Long id;

    @Schema(description = "Nombre de la posición", example = "Portero")
    private String nombre;

    public PositionResponse() {}

    public PositionResponse(Long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}