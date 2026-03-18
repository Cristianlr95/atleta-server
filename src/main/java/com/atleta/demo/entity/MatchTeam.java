package com.atleta.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Entidad que representa la relación entre un partido y un equipo.
 * Un partido debe tener exactamente 2 equipos: uno local y uno visitante.
 * Mantiene el marcador de goles para cada equipo.
 */
@Entity
@Table(name = "match_teams", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"match_id", "team_id"}),
           @UniqueConstraint(columnNames = {"match_id", "es_local"})
       })
public class MatchTeam extends BaseEntity {

    /**
     * Referencia al partido
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    @NotNull(message = "El partido es obligatorio")
    private Match match;

    /**
     * Referencia al equipo
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    @NotNull(message = "El equipo es obligatorio")
    private Team team;

    /**
     * Indica si el equipo es local (true) o visitante (false)
     * Solo puede haber un equipo local por partido
     */
    @Column(name = "es_local", nullable = false)
    @NotNull(message = "Se debe especificar si el equipo es local o visitante")
    private Boolean esLocal;

    /**
     * Número de goles marcados por este equipo en el partido
     */
    @Column(name = "goles", nullable = false)
    @Min(value = 0, message = "Los goles no pueden ser negativos")
    private Integer goles = 0;

    // Constructors
    public MatchTeam() {
        // Constructor por defecto para JPA
    }

    public MatchTeam(Match match, Team team, Boolean esLocal) {
        this.match = match;
        this.team = team;
        this.esLocal = esLocal;
        this.goles = 0;
    }

    // Getters and Setters
    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public Boolean getEsLocal() {
        return esLocal;
    }

    public void setEsLocal(Boolean esLocal) {
        this.esLocal = esLocal;
    }

    public Integer getGoles() {
        return goles;
    }

    public void setGoles(Integer goles) {
        this.goles = goles;
    }

    // Utility methods
    /**
     * Incrementa el número de goles del equipo
     */
    public void incrementarGoles() {
        this.goles++;
    }

    /**
     * Decrementa el número de goles del equipo (si es mayor a 0)
     */
    public void decrementarGoles() {
        if (this.goles > 0) {
            this.goles--;
        }
    }

    /**
     * Verifica si este equipo es el local
     */
    public boolean isLocal() {
        return Boolean.TRUE.equals(esLocal);
    }

    /**
     * Verifica si este equipo es el visitante
     */
    public boolean isVisitante() {
        return Boolean.FALSE.equals(esLocal);
    }

    @Override
    public String toString() {
        return "MatchTeam{" +
                "id=" + getId() +
                ", match=" + (match != null ? match.getId() : null) +
                ", team=" + (team != null ? team.getId() : null) +
                ", esLocal=" + esLocal +
                ", goles=" + goles +
                '}';
    }
}