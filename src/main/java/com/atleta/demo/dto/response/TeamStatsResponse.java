package com.atleta.demo.dto.response;

/**
 * DTO de respuesta para las estadísticas de un equipo.
 * Contiene todas las métricas estadísticas del equipo.
 */
public class TeamStatsResponse {

    /**
     * ID de las estadísticas
     */
    private Long id;

    /**
     * Número de partidos jugados
     */
    private Integer partidosJugados;

    /**
     * Número de partidos ganados
     */
    private Integer partidosGanados;

    /**
     * Número de partidos empatados
     */
    private Integer partidosEmpatados;

    /**
     * Número de partidos perdidos
     */
    private Integer partidosPerdidos;

    /**
     * Total de goles anotados
     */
    private Integer golesAnotados;

    /**
     * Total de goles recibidos
     */
    private Integer golesRecibidos;

    /**
     * Diferencia de goles
     */
    private Integer diferenciagoles;

    /**
     * Total de puntos
     */
    private Integer puntos;

    // Constructors
    public TeamStatsResponse() {
    }

    public TeamStatsResponse(Long id, Integer partidosJugados, Integer partidosGanados, 
                             Integer partidosEmpatados, Integer partidosPerdidos,
                             Integer golesAnotados, Integer golesRecibidos, 
                             Integer diferenciagoles, Integer puntos) {
        this.id = id;
        this.partidosJugados = partidosJugados;
        this.partidosGanados = partidosGanados;
        this.partidosEmpatados = partidosEmpatados;
        this.partidosPerdidos = partidosPerdidos;
        this.golesAnotados = golesAnotados;
        this.golesRecibidos = golesRecibidos;
        this.diferenciagoles = diferenciagoles;
        this.puntos = puntos;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getGolesAnotados() {
        return golesAnotados;
    }

    public void setGolesAnotados(Integer golesAnotados) {
        this.golesAnotados = golesAnotados;
    }

    public Integer getGolesRecibidos() {
        return golesRecibidos;
    }

    public void setGolesRecibidos(Integer golesRecibidos) {
        this.golesRecibidos = golesRecibidos;
    }

    public Integer getDiferenciagoles() {
        return diferenciagoles;
    }

    public void setDiferenciagoles(Integer diferenciagoles) {
        this.diferenciagoles = diferenciagoles;
    }

    public Integer getPuntos() {
        return puntos;
    }

    public void setPuntos(Integer puntos) {
        this.puntos = puntos;
    }

    @Override
    public String toString() {
        return "TeamStatsResponse{" +
                "id=" + id +
                ", partidosJugados=" + partidosJugados +
                ", partidosGanados=" + partidosGanados +
                ", partidosEmpatados=" + partidosEmpatados +
                ", partidosPerdidos=" + partidosPerdidos +
                ", golesAnotados=" + golesAnotados +
                ", golesRecibidos=" + golesRecibidos +
                ", diferenciagoles=" + diferenciagoles +
                ", puntos=" + puntos +
                '}';
    }
}