package com.atleta.demo.dto.response;

import com.atleta.demo.enums.GenderType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para la información de un atleta.
 * Contiene la información pública del atleta sin datos sensibles.
 */
public class AthleteResponse {

    /**
     * UUID único del atleta
     */
    private UUID atletaUuid;

    /**
     * Email del atleta
     */
    private String email;

    /**
     * Nombre completo del atleta
     */
    private String nombre;

    private GenderType genero;

    /**
     * Fecha de creación del registro
     */
    private LocalDateTime createdAt;

    /**
     * Información del perfil de jugador (si existe)
     */
    private PlayerProfileResponse playerProfile;

    // Constructors
    public AthleteResponse() {
    }

    public AthleteResponse(UUID atletaUuid, String email, String nombre, GenderType genero, LocalDateTime createdAt) {
        this.atletaUuid = atletaUuid;
        this.email = email;
        this.nombre = nombre;
        this.genero = genero;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public UUID getAtletaUuid() {
        return atletaUuid;
    }

    public void setAtletaUuid(UUID atletaUuid) {
        this.atletaUuid = atletaUuid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public GenderType getGenero() {
        return genero;
    }

    public void setGenero(GenderType genero) {
        this.genero = genero;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public PlayerProfileResponse getPlayerProfile() {
        return playerProfile;
    }

    public void setPlayerProfile(PlayerProfileResponse playerProfile) {
        this.playerProfile = playerProfile;
    }

    @Override
    public String toString() {
        return "AthleteResponse{" +
                "atletaUuid=" + atletaUuid +
                ", email='" + email + '\'' +
                ", nombre='" + nombre + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
