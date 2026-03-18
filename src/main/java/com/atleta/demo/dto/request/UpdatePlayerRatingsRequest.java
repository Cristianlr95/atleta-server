package com.atleta.demo.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * DTO para solicitar la actualización manual de calificaciones de jugadores.
 * Contiene el ID del partido y la lista de rendimientos de jugadores.
 */
public class UpdatePlayerRatingsRequest {

    /**
     * ID del partido para el cual se actualizan las calificaciones
     */
    @NotNull(message = "El ID del partido es obligatorio")
    private Long matchId;

    /**
     * Lista de rendimientos de jugadores en el partido
     */
    @NotNull(message = "La lista de rendimientos es obligatoria")
    @NotEmpty(message = "La lista de rendimientos no puede estar vacía")
    @Valid
    private List<PlayerPerformanceDto> performances;

    // Constructors
    public UpdatePlayerRatingsRequest() {
    }

    public UpdatePlayerRatingsRequest(Long matchId, List<PlayerPerformanceDto> performances) {
        this.matchId = matchId;
        this.performances = performances;
    }

    // Getters and Setters
    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public List<PlayerPerformanceDto> getPerformances() {
        return performances;
    }

    public void setPerformances(List<PlayerPerformanceDto> performances) {
        this.performances = performances;
    }

    @Override
    public String toString() {
        return "UpdatePlayerRatingsRequest{" +
                "matchId=" + matchId +
                ", performances=" + performances +
                '}';
    }
}