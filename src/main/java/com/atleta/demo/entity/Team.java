package com.atleta.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa un equipo de fútbol.
 * Contiene información básica del equipo y mantiene relaciones con sus miembros y estadísticas.
 */
@Entity
@Table(name = "teams")
public class Team extends BaseEntity {

    /**
     * Nombre del equipo (único en el sistema)
     */
    @Column(name = "nombre", nullable = false, unique = true)
    @NotBlank(message = "El nombre del equipo es obligatorio")
    @Size(max = 100, message = "El nombre del equipo no puede exceder 100 caracteres")
    private String nombre;

    /**
     * URL del logo del equipo (opcional)
     */
    @Column(name = "logo_url")
    private String logoUrl;

    /**
     * Año de fundación del equipo (opcional)
     */
    @Column(name = "anio_fundacion")
    private Integer anioFundacion;

    /**
     * Jugador que creó el equipo
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creador_user_id", nullable = false)
    private PlayerProfile creador;

    /**
     * Timestamp de creación del equipo
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Indica si el equipo fue archivado (eliminacion logica).
     */
    @Column(name = "archived", nullable = false)
    private Boolean archived = false;

    /**
     * Fecha de archivado del equipo.
     */
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    /**
     * Relación uno-a-uno con las estadísticas del equipo
     */
    @OneToOne(mappedBy = "team", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private TeamStats stats;

    /**
     * Relación con los miembros del equipo
     */
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TeamMember> members = new ArrayList<>();

    // Constructors
    public Team() {
        // Constructor por defecto para JPA
    }

    public Team(String nombre, PlayerProfile creador) {
        this.nombre = nombre;
        this.creador = creador;
    }

    public Team(String nombre, PlayerProfile creador, String logoUrl, Integer anioFundacion) {
        this.nombre = nombre;
        this.creador = creador;
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

    public PlayerProfile getCreador() {
        return creador;
    }

    public void setCreador(PlayerProfile creador) {
        this.creador = creador;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }

    public TeamStats getStats() {
        return stats;
    }

    public void setStats(TeamStats stats) {
        this.stats = stats;
        if (stats != null) {
            stats.setTeam(this);
        }
    }

    public List<TeamMember> getMembers() {
        return members;
    }

    public void setMembers(List<TeamMember> members) {
        this.members = members;
    }

    // Utility methods
    public void addMember(TeamMember member) {
        members.add(member);
        member.setTeam(this);
    }

    public void removeMember(TeamMember member) {
        members.remove(member);
        member.setTeam(null);
    }

    @Override
    public String toString() {
        return "Team{" +
                "id=" + getId() +
                ", nombre='" + nombre + '\'' +
                ", logoUrl='" + logoUrl + '\'' +
                ", anioFundacion=" + anioFundacion +
                ", archived=" + archived +
                ", archivedAt=" + archivedAt +
                ", createdAt=" + createdAt +
                '}';
    }
}
