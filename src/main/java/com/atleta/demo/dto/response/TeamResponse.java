package com.atleta.demo.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta para la información de un equipo.
 * Contiene la información completa del equipo incluyendo estadísticas y miembros.
 */
public class TeamResponse {

    /**
     * ID del equipo
     */
    private Long id;

    /**
     * Nombre del equipo
     */
    private String nombre;

    /**
     * URL del logo del equipo
     */
    private String logoUrl;

    /**
     * Año de fundación del equipo
     */
    private Integer anioFundacion;

    /**
     * Información del creador del equipo
     */
    private PlayerProfileResponse creador;

    /**
     * Fecha de creación del equipo
     */
    private LocalDateTime createdAt;

    /**
     * Estadísticas del equipo
     */
    private TeamStatsResponse stats;

    /**
     * Lista de miembros del equipo
     */
    private List<TeamMemberResponse> members;

    // Constructors
    public TeamResponse() {
    }

    public TeamResponse(Long id, String nombre, String logoUrl, Integer anioFundacion, 
                        PlayerProfileResponse creador, LocalDateTime createdAt) {
        this.id = id;
        this.nombre = nombre;
        this.logoUrl = logoUrl;
        this.anioFundacion = anioFundacion;
        this.creador = creador;
        this.createdAt = createdAt;
    }

    // Getters and Setters
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

    public PlayerProfileResponse getCreador() {
        return creador;
    }

    public void setCreador(PlayerProfileResponse creador) {
        this.creador = creador;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public TeamStatsResponse getStats() {
        return stats;
    }

    public void setStats(TeamStatsResponse stats) {
        this.stats = stats;
    }

    public List<TeamMemberResponse> getMembers() {
        return members;
    }

    public void setMembers(List<TeamMemberResponse> members) {
        this.members = members;
    }

    @Override
    public String toString() {
        return "TeamResponse{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", logoUrl='" + logoUrl + '\'' +
                ", anioFundacion=" + anioFundacion +
                ", createdAt=" + createdAt +
                '}';
    }
}