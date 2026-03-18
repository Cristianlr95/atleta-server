package com.atleta.demo.dto.request;

import com.atleta.demo.enums.MatchResultType;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para solicitar la actualización de calificaciones en modo arquero rotativo.
 * Contiene el ID del partido y el resultado del partido.
 */
public class UpdateRotativeGoalkeeperRequest {

    /**
     * ID del partido en modo arquero rotativo
     */
    @NotNull(message = "El ID del partido es obligatorio")
    private Long matchId;

    /**
     * Resultado del partido desde la perspectiva del equipo
     */
    @NotNull(message = "El resultado del partido es obligatorio")
    private MatchResultType matchResult;

    // Constructors
    public UpdateRotativeGoalkeeperRequest() {
    }

    public UpdateRotativeGoalkeeperRequest(Long matchId, MatchResultType matchResult) {
        this.matchId = matchId;
        this.matchResult = matchResult;
    }

    // Getters and Setters
    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public MatchResultType getMatchResult() {
        return matchResult;
    }

    public void setMatchResult(MatchResultType matchResult) {
        this.matchResult = matchResult;
    }

    @Override
    public String toString() {
        return "UpdateRotativeGoalkeeperRequest{" +
                "matchId=" + matchId +
                ", matchResult=" + matchResult +
                '}';
    }
}