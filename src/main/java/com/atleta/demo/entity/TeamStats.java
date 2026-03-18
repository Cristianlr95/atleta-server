package com.atleta.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

/**
 * Entidad que representa las estadísticas de un equipo.
 * Mantiene una relación uno-a-uno con Team y almacena contadores de rendimiento.
 */
@Entity
@Table(name = "team_stats")
public class TeamStats extends BaseEntity {

    /**
     * Relación uno-a-uno con Team
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /**
     * Número total de partidos jugados
     */
    @Column(name = "partidos_jugados", nullable = false)
    @Min(value = 0, message = "Los partidos jugados no pueden ser negativos")
    private Integer partidosJugados = 0;

    /**
     * Número de partidos ganados
     */
    @Column(name = "partidos_ganados", nullable = false)
    @Min(value = 0, message = "Los partidos ganados no pueden ser negativos")
    private Integer partidosGanados = 0;

    /**
     * Número de partidos empatados
     */
    @Column(name = "partidos_empatados", nullable = false)
    @Min(value = 0, message = "Los partidos empatados no pueden ser negativos")
    private Integer partidosEmpatados = 0;

    /**
     * Número de partidos perdidos
     */
    @Column(name = "partidos_perdidos", nullable = false)
    @Min(value = 0, message = "Los partidos perdidos no pueden ser negativos")
    private Integer partidosPerdidos = 0;

    /**
     * Total de goles anotados por el equipo
     */
    @Column(name = "goles_favor", nullable = false)
    @Min(value = 0, message = "Los goles a favor no pueden ser negativos")
    private Integer golesFavor = 0;

    /**
     * Total de goles recibidos por el equipo
     */
    @Column(name = "goles_contra", nullable = false)
    @Min(value = 0, message = "Los goles en contra no pueden ser negativos")
    private Integer golesContra = 0;

    /**
     * Puntos totales del equipo (3 por victoria, 1 por empate)
     */
    @Column(name = "puntos", nullable = false)
    @Min(value = 0, message = "Los puntos no pueden ser negativos")
    private Integer puntos = 0;

    // Constructors
    public TeamStats() {
        // Constructor por defecto para JPA
    }

    public TeamStats(Team team) {
        this.team = team;
    }

    // Getters and Setters
    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public Integer getPartidosJugados() {
        return partidosJugados;
    }

    public void setPartidosJugados(Integer partidosJugados) {
        this.partidosJugados = partidosJugados;
    }

    public Integer getPartidosGanados() {
        return partidosGanados;
    }

    public void setPartidosGanados(Integer partidosGanados) {
        this.partidosGanados = partidosGanados;
    }

    public Integer getPartidosEmpatados() {
        return partidosEmpatados;
    }

    public void setPartidosEmpatados(Integer partidosEmpatados) {
        this.partidosEmpatados = partidosEmpatados;
    }

    public Integer getPartidosPerdidos() {
        return partidosPerdidos;
    }

    public void setPartidosPerdidos(Integer partidosPerdidos) {
        this.partidosPerdidos = partidosPerdidos;
    }

    public Integer getGolesFavor() {
        return golesFavor;
    }

    public void setGolesFavor(Integer golesFavor) {
        this.golesFavor = golesFavor;
    }

    public Integer getGolesContra() {
        return golesContra;
    }

    public void setGolesContra(Integer golesContra) {
        this.golesContra = golesContra;
    }

    public Integer getPuntos() {
        return puntos;
    }

    public void setPuntos(Integer puntos) {
        this.puntos = puntos;
    }

    // Utility methods
    /**
     * Calcula la diferencia de goles (goles a favor - goles en contra)
     */
    public Integer getDiferenciaGoles() {
        return golesFavor - golesContra;
    }

    /**
     * Calcula el porcentaje de victorias
     */
    public Double getPorcentajeVictorias() {
        if (partidosJugados == 0) {
            return 0.0;
        }
        return (partidosGanados.doubleValue() / partidosJugados.doubleValue()) * 100.0;
    }

    /**
     * Actualiza las estadísticas después de un partido ganado
     */
    public void registrarVictoria(Integer golesFavor, Integer golesContra) {
        this.partidosJugados++;
        this.partidosGanados++;
        this.golesFavor += golesFavor;
        this.golesContra += golesContra;
        this.puntos += 3;
    }

    /**
     * Actualiza las estadísticas después de un empate
     */
    public void registrarEmpate(Integer golesFavor, Integer golesContra) {
        this.partidosJugados++;
        this.partidosEmpatados++;
        this.golesFavor += golesFavor;
        this.golesContra += golesContra;
        this.puntos += 1;
    }

    /**
     * Actualiza las estadísticas después de una derrota
     */
    public void registrarDerrota(Integer golesFavor, Integer golesContra) {
        this.partidosJugados++;
        this.partidosPerdidos++;
        this.golesFavor += golesFavor;
        this.golesContra += golesContra;
        // No se suman puntos por derrota
    }

    @Override
    public String toString() {
        return "TeamStats{" +
                "id=" + getId() +
                ", partidosJugados=" + partidosJugados +
                ", partidosGanados=" + partidosGanados +
                ", partidosEmpatados=" + partidosEmpatados +
                ", partidosPerdidos=" + partidosPerdidos +
                ", golesFavor=" + golesFavor +
                ", golesContra=" + golesContra +
                ", puntos=" + puntos +
                '}';
    }
}