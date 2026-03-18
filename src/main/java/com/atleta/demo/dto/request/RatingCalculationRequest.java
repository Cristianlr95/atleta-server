package com.atleta.demo.dto.request;

import com.atleta.demo.enums.MatchResultType;
import com.atleta.demo.enums.PriorityLevel;
import com.atleta.demo.enums.RoleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO para solicitar el cálculo de una nueva calificación.
 * Contiene todos los parámetros necesarios para el motor de cálculo.
 */
public class RatingCalculationRequest {

    /**
     * Calificación actual del jugador para el rol específico
     */
    @NotNull(message = "La calificación actual es obligatoria")
    @DecimalMin(value = "0.0", message = "La calificación actual no puede ser negativa")
    private BigDecimal currentRating;

    /**
     * Rol del jugador durante el partido
     */
    @NotNull(message = "El tipo de rol es obligatorio")
    private RoleType roleType;

    /**
     * Nivel de prioridad del rol para este jugador
     */
    @NotNull(message = "El nivel de prioridad es obligatorio")
    private PriorityLevel priorityLevel;

    /**
     * Resultado del partido desde la perspectiva del jugador
     */
    @NotNull(message = "El resultado del partido es obligatorio")
    private MatchResultType matchResult;

    /**
     * Número de goles anotados por el jugador
     */
    @NotNull(message = "Los goles anotados son obligatorios")
    @Min(value = 0, message = "Los goles anotados no pueden ser negativos")
    private Integer goalsScored;

    /**
     * Número de asistencias realizadas por el jugador
     */
    @NotNull(message = "Las asistencias realizadas son obligatorias")
    @Min(value = 0, message = "Las asistencias realizadas no pueden ser negativas")
    private Integer assistsMade;

    /**
     * Número de goles recibidos (solo aplicable para DEFENSA y ARQUERO)
     */
    @Min(value = 0, message = "Los goles recibidos no pueden ser negativos")
    private Integer goalsConceded;

    /**
     * Indica si el jugador fue designado como MVP del partido
     */
    @NotNull(message = "El estatus MVP es obligatorio")
    private Boolean wasMvp;

    // Constructors
    public RatingCalculationRequest() {
    }

    public RatingCalculationRequest(BigDecimal currentRating, RoleType roleType, PriorityLevel priorityLevel,
                                   MatchResultType matchResult, Integer goalsScored, Integer assistsMade,
                                   Integer goalsConceded, Boolean wasMvp) {
        this.currentRating = currentRating;
        this.roleType = roleType;
        this.priorityLevel = priorityLevel;
        this.matchResult = matchResult;
        this.goalsScored = goalsScored;
        this.assistsMade = assistsMade;
        this.goalsConceded = goalsConceded;
        this.wasMvp = wasMvp;
    }

    // Getters and Setters
    public BigDecimal getCurrentRating() {
        return currentRating;
    }

    public void setCurrentRating(BigDecimal currentRating) {
        this.currentRating = currentRating;
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
    }

    public PriorityLevel getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(PriorityLevel priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public MatchResultType getMatchResult() {
        return matchResult;
    }

    public void setMatchResult(MatchResultType matchResult) {
        this.matchResult = matchResult;
    }

    public Integer getGoalsScored() {
        return goalsScored;
    }

    public void setGoalsScored(Integer goalsScored) {
        this.goalsScored = goalsScored;
    }

    public Integer getAssistsMade() {
        return assistsMade;
    }

    public void setAssistsMade(Integer assistsMade) {
        this.assistsMade = assistsMade;
    }

    public Integer getGoalsConceded() {
        return goalsConceded;
    }

    public void setGoalsConceded(Integer goalsConceded) {
        this.goalsConceded = goalsConceded;
    }

    public Boolean getWasMvp() {
        return wasMvp;
    }

    public void setWasMvp(Boolean wasMvp) {
        this.wasMvp = wasMvp;
    }

    @Override
    public String toString() {
        return "RatingCalculationRequest{" +
                "currentRating=" + currentRating +
                ", roleType=" + roleType +
                ", priorityLevel=" + priorityLevel +
                ", matchResult=" + matchResult +
                ", goalsScored=" + goalsScored +
                ", assistsMade=" + assistsMade +
                ", goalsConceded=" + goalsConceded +
                ", wasMvp=" + wasMvp +
                '}';
    }
}