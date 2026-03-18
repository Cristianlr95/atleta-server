package com.atleta.demo.entity;

import com.atleta.demo.enums.MatchResultType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Entidad que mantiene el historial de cambios de calificación de un jugador.
 * Registra todos los detalles de rendimiento y cálculo para auditoría y análisis.
 */
@Entity
@Table(name = "rating_history")
public class RatingHistory extends BaseEntity {

    /**
     * Relación con la calificación del jugador
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_rating_id", nullable = false)
    @NotNull(message = "La calificación del jugador es requerida")
    private PlayerRating playerRating;

    /**
     * Relación con el partido que generó este cambio de calificación
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    @NotNull(message = "El partido es requerido")
    private Match match;

    /**
     * Calificación anterior antes del cálculo
     */
    @Column(name = "previous_rating", nullable = false, precision = 5, scale = 2)
    @NotNull(message = "La calificación anterior es requerida")
    @DecimalMin(value = "0.00", message = "La calificación anterior no puede ser negativa")
    @DecimalMax(value = "999.99", message = "La calificación anterior no puede exceder 999.99")
    private BigDecimal previousRating;

    /**
     * Nueva calificación después del cálculo
     */
    @Column(name = "new_rating", nullable = false, precision = 5, scale = 2)
    @NotNull(message = "La nueva calificación es requerida")
    @DecimalMin(value = "0.00", message = "La nueva calificación no puede ser negativa")
    @DecimalMax(value = "999.99", message = "La nueva calificación no puede exceder 999.99")
    private BigDecimal newRating;

    /**
     * Delta de calificación aplicado (puede ser positivo o negativo)
     */
    @Column(name = "rating_delta", nullable = false, precision = 6, scale = 2)
    @NotNull(message = "El delta de calificación es requerido")
    @DecimalMin(value = "-999.99", message = "El delta de calificación no puede ser menor a -999.99")
    @DecimalMax(value = "999.99", message = "El delta de calificación no puede exceder 999.99")
    private BigDecimal ratingDelta;

    /**
     * Número de goles anotados por el jugador en este partido
     */
    @Column(name = "goals_scored", nullable = false)
    @NotNull(message = "Los goles anotados son requeridos")
    @Min(value = 0, message = "Los goles anotados no pueden ser negativos")
    private Integer goalsScored;

    /**
     * Número de asistencias realizadas por el jugador en este partido
     */
    @Column(name = "assists_made", nullable = false)
    @NotNull(message = "Las asistencias realizadas son requeridas")
    @Min(value = 0, message = "Las asistencias realizadas no pueden ser negativas")
    private Integer assistsMade;

    /**
     * Número de goles recibidos (solo aplicable para roles DEFENSA y ARQUERO)
     */
    @Column(name = "goals_conceded")
    @Min(value = 0, message = "Los goles recibidos no pueden ser negativos")
    private Integer goalsConceded;

    /**
     * Indica si el jugador fue MVP en este partido
     */
    @Column(name = "was_mvp", nullable = false)
    @NotNull(message = "El estatus MVP es requerido")
    private Boolean wasMvp;

    /**
     * Resultado del partido desde la perspectiva del jugador
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "match_result", nullable = false, length = 20)
    @NotNull(message = "El resultado del partido es requerido")
    private MatchResultType matchResult;

    /**
     * Indica si este cálculo fue realizado en modo arquero rotativo
     */
    @Column(name = "rotative_goalkeeper_mode", nullable = false)
    @NotNull(message = "El modo arquero rotativo es requerido")
    private Boolean rotativeGoalkeeperMode = false;

    /**
     * Puntos de resultado aplicados (basados en el resultado del partido)
     */
    @Column(name = "result_points", precision = 4, scale = 2)
    @DecimalMin(value = "-99.99", message = "Los puntos de resultado no pueden ser menores a -99.99")
    @DecimalMax(value = "99.99", message = "Los puntos de resultado no pueden exceder 99.99")
    private BigDecimal resultPoints;

    /**
     * Puntos de goles ponderados aplicados
     */
    @Column(name = "weighted_goal_points", precision = 4, scale = 2)
    @DecimalMin(value = "0.00", message = "Los puntos de goles ponderados no pueden ser negativos")
    @DecimalMax(value = "99.99", message = "Los puntos de goles ponderados no pueden exceder 99.99")
    private BigDecimal weightedGoalPoints;

    /**
     * Puntos de asistencias ponderadas aplicados
     */
    @Column(name = "weighted_assist_points", precision = 4, scale = 2)
    @DecimalMin(value = "0.00", message = "Los puntos de asistencias ponderadas no pueden ser negativos")
    @DecimalMax(value = "99.99", message = "Los puntos de asistencias ponderadas no pueden exceder 99.99")
    private BigDecimal weightedAssistPoints;

    /**
     * Bono defensivo aplicado (solo para roles DEFENSA y ARQUERO)
     */
    @Column(name = "defensive_bonus", precision = 4, scale = 2)
    @DecimalMin(value = "0.00", message = "El bono defensivo no puede ser negativo")
    @DecimalMax(value = "99.99", message = "El bono defensivo no puede exceder 99.99")
    private BigDecimal defensiveBonus;

    /**
     * Bono MVP aplicado
     */
    @Column(name = "mvp_bonus", precision = 4, scale = 2)
    @DecimalMin(value = "0.00", message = "El bono MVP no puede ser negativo")
    @DecimalMax(value = "99.99", message = "El bono MVP no puede exceder 99.99")
    private BigDecimal mvpBonus;

    /**
     * Multiplicador de prioridad aplicado
     */
    @Column(name = "priority_multiplier", precision = 3, scale = 2)
    @DecimalMin(value = "0.00", message = "El multiplicador de prioridad no puede ser negativo")
    @DecimalMax(value = "9.99", message = "El multiplicador de prioridad no puede exceder 9.99")
    private BigDecimal priorityMultiplier;

    // Constructors
    public RatingHistory() {
        // Constructor por defecto para JPA
    }

    public RatingHistory(PlayerRating playerRating, Match match, BigDecimal previousRating, 
                        BigDecimal newRating, BigDecimal ratingDelta, Integer goalsScored, 
                        Integer assistsMade, Boolean wasMvp, MatchResultType matchResult) {
        this.playerRating = playerRating;
        this.match = match;
        this.previousRating = previousRating;
        this.newRating = newRating;
        this.ratingDelta = ratingDelta;
        this.goalsScored = goalsScored;
        this.assistsMade = assistsMade;
        this.wasMvp = wasMvp;
        this.matchResult = matchResult;
        this.rotativeGoalkeeperMode = false;
    }

    // Getters and Setters
    public PlayerRating getPlayerRating() {
        return playerRating;
    }

    public void setPlayerRating(PlayerRating playerRating) {
        this.playerRating = playerRating;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public BigDecimal getPreviousRating() {
        return previousRating;
    }

    public void setPreviousRating(BigDecimal previousRating) {
        this.previousRating = previousRating;
    }

    public BigDecimal getNewRating() {
        return newRating;
    }

    public void setNewRating(BigDecimal newRating) {
        this.newRating = newRating;
    }

    public BigDecimal getRatingDelta() {
        return ratingDelta;
    }

    public void setRatingDelta(BigDecimal ratingDelta) {
        this.ratingDelta = ratingDelta;
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

    public Boolean getRotativeGoalkeeperMode() {
        return rotativeGoalkeeperMode;
    }

    public void setRotativeGoalkeeperMode(Boolean rotativeGoalkeeperMode) {
        this.rotativeGoalkeeperMode = rotativeGoalkeeperMode;
    }

    public BigDecimal getResultPoints() {
        return resultPoints;
    }

    public void setResultPoints(BigDecimal resultPoints) {
        this.resultPoints = resultPoints;
    }

    public BigDecimal getWeightedGoalPoints() {
        return weightedGoalPoints;
    }

    public void setWeightedGoalPoints(BigDecimal weightedGoalPoints) {
        this.weightedGoalPoints = weightedGoalPoints;
    }

    public BigDecimal getWeightedAssistPoints() {
        return weightedAssistPoints;
    }

    public void setWeightedAssistPoints(BigDecimal weightedAssistPoints) {
        this.weightedAssistPoints = weightedAssistPoints;
    }

    public BigDecimal getDefensiveBonus() {
        return defensiveBonus;
    }

    public void setDefensiveBonus(BigDecimal defensiveBonus) {
        this.defensiveBonus = defensiveBonus;
    }

    public BigDecimal getMvpBonus() {
        return mvpBonus;
    }

    public void setMvpBonus(BigDecimal mvpBonus) {
        this.mvpBonus = mvpBonus;
    }

    public BigDecimal getPriorityMultiplier() {
        return priorityMultiplier;
    }

    public void setPriorityMultiplier(BigDecimal priorityMultiplier) {
        this.priorityMultiplier = priorityMultiplier;
    }

    // Utility methods
    /**
     * Calcula el delta total antes de aplicar el multiplicador de prioridad
     */
    public BigDecimal calculateRawDelta() {
        BigDecimal rawDelta = BigDecimal.ZERO;
        
        if (resultPoints != null) {
            rawDelta = rawDelta.add(resultPoints);
        }
        if (weightedGoalPoints != null) {
            rawDelta = rawDelta.add(weightedGoalPoints);
        }
        if (weightedAssistPoints != null) {
            rawDelta = rawDelta.add(weightedAssistPoints);
        }
        if (defensiveBonus != null) {
            rawDelta = rawDelta.add(defensiveBonus);
        }
        if (mvpBonus != null) {
            rawDelta = rawDelta.add(mvpBonus);
        }
        
        return rawDelta;
    }

    /**
     * Verifica si este registro corresponde a un rol defensivo
     */
    public boolean isDefensiveRole() {
        return goalsConceded != null;
    }

    @Override
    public String toString() {
        return "RatingHistory{" +
                "id=" + getId() +
                ", playerRating=" + (playerRating != null ? playerRating.getId() : null) +
                ", match=" + (match != null ? match.getId() : null) +
                ", previousRating=" + previousRating +
                ", newRating=" + newRating +
                ", ratingDelta=" + ratingDelta +
                ", goalsScored=" + goalsScored +
                ", assistsMade=" + assistsMade +
                ", goalsConceded=" + goalsConceded +
                ", wasMvp=" + wasMvp +
                ", matchResult=" + matchResult +
                ", rotativeGoalkeeperMode=" + rotativeGoalkeeperMode +
                '}';
    }
}