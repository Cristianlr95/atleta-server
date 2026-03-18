package com.atleta.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entidad que representa las posiciones de fútbol disponibles en el sistema.
 * Mantiene un catálogo fijo de posiciones que pueden ser asignadas a los jugadores.
 * Las posiciones incluyen: Portero, Defensa, Carrilero, Mediocampista, Delantero, DT.
 */
@Entity
@Table(name = "positions")
public class Position extends BaseEntity {

    /**
     * Nombre de la posición de fútbol
     */
    @Column(name = "nombre", nullable = false, unique = true)
    @NotBlank(message = "El nombre de la posición es obligatorio")
    @Size(max = 50, message = "El nombre de la posición no puede exceder 50 caracteres")
    private String nombre;

    // Constructors
    public Position() {
        // Constructor por defecto para JPA
    }

    public Position(String nombre) {
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
        return "Position{" +
                "id=" + getId() +
                ", nombre='" + nombre + '\'' +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}