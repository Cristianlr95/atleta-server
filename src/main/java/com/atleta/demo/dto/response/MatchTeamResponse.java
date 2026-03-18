package com.atleta.demo.dto.response;

/**
 * DTO de respuesta para la información de un equipo en un partido.
 * Contiene la información de la participación de un equipo en un partido específico.
 */
public class MatchTeamResponse {

    /**
     * ID de la relación match-team
     */
    private Long id;

    /**
     * Información del equipo
     */
    private TeamResponse team;

    /**
     * Indica si el equipo es local
     */
    private Boolean esLocal;

    /**
     * Goles anotados por el equipo en el partido
     */
    private Integer goles;

    // Constructors
    public MatchTeamResponse() {
    }

    public MatchTeamResponse(Long id, TeamResponse team, Boolean esLocal, Integer goles) {
        this.id = id;
        this.team = team;
        this.esLocal = esLocal;
        this.goles = goles;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TeamResponse getTeam() {
        return team;
    }

    public void setTeam(TeamResponse team) {
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

    @Override
    public String toString() {
        return "MatchTeamResponse{" +
                "id=" + id +
                ", esLocal=" + esLocal +
                ", goles=" + goles +
                '}';
    }
}