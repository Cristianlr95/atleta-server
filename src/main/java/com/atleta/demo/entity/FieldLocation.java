package com.atleta.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Entity
@Table(name = "field_locations")
public class FieldLocation extends BaseEntity {

    @Column(name = "nombre", nullable = false, length = 150)
    @NotBlank(message = "El nombre de la cancha es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    private String nombre;

    @Column(name = "direccion", nullable = false, length = 255)
    @NotBlank(message = "La direccion de la cancha es obligatoria")
    @Size(max = 255, message = "La direccion no puede exceder 255 caracteres")
    private String direccion;

    @Column(name = "ciudad", nullable = false, length = 120)
    @NotBlank(message = "La ciudad es obligatoria")
    @Size(max = 120, message = "La ciudad no puede exceder 120 caracteres")
    private String ciudad;

    @Column(name = "latitud", nullable = false, precision = 10, scale = 8)
    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
    private BigDecimal latitud;

    @Column(name = "longitud", nullable = false, precision = 11, scale = 8)
    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
    private BigDecimal longitud;

    @Column(name = "activo", nullable = false)
    @NotNull(message = "El estado activo es obligatorio")
    private Boolean activo = true;

    public FieldLocation() {
    }

    public FieldLocation(String nombre, String direccion, String ciudad, BigDecimal latitud, BigDecimal longitud, Boolean activo) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.ciudad = ciudad;
        this.latitud = latitud;
        this.longitud = longitud;
        this.activo = activo == null ? true : activo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
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

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
