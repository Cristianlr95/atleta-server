package com.atleta.demo.dto.request;

import com.atleta.demo.enums.MatchResultType;
import com.atleta.demo.enums.PriorityLevel;
import com.atleta.demo.enums.RoleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO para capturar el rendimiento de un jugador en un partido.
 * Contiene todas las métricas necesarias para calcular la nueva calificación.
 */
public class PlayerPerformanceDto {

    /**
     * ID único del perfil del jugador
     */
    @NotNull(message = "El ID del perfil del jugador es obligatorio")
    private UUID playerProfileId;

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

    /**
     * Resultado del partido desde la perspectiva del jugador
     */
    @NotNull(message = "El resultado del partido es obligatorio")
    private MatchResultType matchResult;

    // Constructors
    public PlayerPerformanceDto() {
    }

    public PlayerPerformanceDto(UUID playerProfileId, RoleType roleType, PriorityLevel priorityLevel,
                               Integer goalsScored, Integer assistsMade, Integer goalsConceded,
                               Boolean wasMvp, MatchResultType matchResult) {
        this.playerProfileId = playerProfileId;
        this.roleType = roleType;
        this.priorityLevel = priorityLevel;
        this.goalsScored = goalsScored;
        this.assistsMade = assistsMade;
        this.goalsConceded = goalsConceded;
        this.wasMvp = wasMvp;
        this.matchResult = matchResult;
    }

    // Getters and Setters
    public UUID getPlayerProfileId() {
        return playerProfileId;
    }

    public void setPlayerProfileId(UUID playerProfileId) {
        this.playerProfileId = playerProfileId;
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

    public MatchResultType getMatchResult() {
        return matchResult;
    }

    public void setMatchResult(MatchResultType matchResult) {
        this.matchResult = matchResult;
    }

    @Override
    public String toString() {
        return "PlayerPerformanceDto{" +
                "playerProfileId=" + playerProfileId +
                ", roleType=" + roleType +
                ", priorityLevel=" + priorityLevel +
                ", goalsScored=" + goalsScored +
                ", assistsMade=" + assistsMade +
                ", goalsConceded=" + goalsConceded +
                ", wasMvp=" + wasMvp +
                ", matchResult=" + matchResult +
                '}';
    }
}