package com.atleta.demo.dto.request;

import com.atleta.demo.enums.MatchResultType;
import com.atleta.demo.enums.PriorityLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO para solicitar el cálculo de calificación en modo arquero rotativo.
 * En este modo especial, todos los jugadores reciben actualizaciones de calificación de arquero.
 */
public class RotativeGoalkeeperRequest {

    /**
     * Calificación actual del jugador para el rol de arquero
     */
    @NotNull(message = "La calificación actual de arquero es obligatoria")
    @DecimalMin(value = "0.0", message = "La calificación actual de arquero no puede ser negativa")
    private BigDecimal currentGoalkeeperRating;

    /**
     * Nivel de prioridad del rol de arquero para este jugador
     */
    @NotNull(message = "El nivel de prioridad de arquero es obligatorio")
    private PriorityLevel goalkeeperPriority;

    /**
     * Resultado del partido desde la perspectiva del jugador
     */
    @NotNull(message = "El resultado del partido es obligatorio")
    private MatchResultType matchResult;

    // Constructors
    public RotativeGoalkeeperRequest() {
    }

    public RotativeGoalkeeperRequest(BigDecimal currentGoalkeeperRating, PriorityLevel goalkeeperPriority,
                                    MatchResultType matchResult) {
        this.currentGoalkeeperRating = currentGoalkeeperRating;
        this.goalkeeperPriority = goalkeeperPriority;
        this.matchResult = matchResult;
    }

    // Getters and Setters
    public BigDecimal getCurrentGoalkeeperRating() {
        return currentGoalkeeperRating;
    }

    public void setCurrentGoalkeeperRating(BigDecimal currentGoalkeeperRating) {
        this.currentGoalkeeperRating = currentGoalkeeperRating;
    }

    public PriorityLevel getGoalkeeperPriority() {
        return goalkeeperPriority;
    }

    public void setGoalkeeperPriority(PriorityLevel goalkeeperPriority) {
        this.goalkeeperPriority = goalkeeperPriority;
    }

    public MatchResultType getMatchResult() {
        return matchResult;
    }

    public void setMatchResult(MatchResultType matchResult) {
        this.matchResult = matchResult;
    }

    @Override
    public String toString() {
        return "RotativeGoalkeeperRequest{" +
                "currentGoalkeeperRating=" + currentGoalkeeperRating +
                ", goalkeeperPriority=" + goalkeeperPriority +
                ", matchResult=" + matchResult +
                '}';
    }
}