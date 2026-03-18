package com.atleta.demo.dto.request;

import com.atleta.demo.enums.EventType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO para crear un evento de partido (gol, asistencia).
 * Contiene las validaciones necesarias para los eventos de partido.
 */
public class CreateMatchEventRequest {

    /**
     * ID del partido donde ocurre el evento
     */
    @NotNull(message = "El ID del partido es obligatorio")
    private Long matchId;

    /**
     * UUID del jugador que realiza el evento
     */
    @NotNull(message = "El UUID del jugador es obligatorio")
    private UUID playerUuid;

    /**
     * ID del equipo del jugador
     */
    @NotNull(message = "El ID del equipo es obligatorio")
    private Long teamId;

    /**
     * Tipo de evento (GOL, ASISTENCIA)
     */
    @NotNull(message = "El tipo de evento es obligatorio")
    private EventType eventType;

    /**
     * UUID del jugador que asiste (opcional, solo para goles)
     */
    private UUID assistPlayerUuid;

    /**
     * UUID del usuario responsable que registra el evento.
     * Si no se informa, se asume playerUuid para mantener compatibilidad.
     */
    private UUID registeredByUuid;

    /**
     * Minuto del partido en que ocurre el evento
     */
    private Integer minuto;

    // Constructors
    public CreateMatchEventRequest() {
    }

    public CreateMatchEventRequest(Long matchId, UUID playerUuid, Long teamId, EventType eventType) {
        this.matchId = matchId;
        this.playerUuid = playerUuid;
        this.teamId = teamId;
        this.eventType = eventType;
    }

    public CreateMatchEventRequest(Long matchId, UUID playerUuid, Long teamId, EventType eventType,
                                   UUID assistPlayerUuid, Integer minuto) {
        this.matchId = matchId;
        this.playerUuid = playerUuid;
        this.teamId = teamId;
        this.eventType = eventType;
        this.assistPlayerUuid = assistPlayerUuid;
        this.minuto = minuto;
    }

    // Getters and Setters
    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public UUID getAssistPlayerUuid() {
        return assistPlayerUuid;
    }

    public void setAssistPlayerUuid(UUID assistPlayerUuid) {
        this.assistPlayerUuid = assistPlayerUuid;
    }

    public UUID getRegisteredByUuid() {
        return registeredByUuid;
    }

    public void setRegisteredByUuid(UUID registeredByUuid) {
        this.registeredByUuid = registeredByUuid;
    }

    public Integer getMinuto() {
        return minuto;
    }

    public void setMinuto(Integer minuto) {
        this.minuto = minuto;
    }

    @Override
    public String toString() {
        return "CreateMatchEventRequest{" +
                "matchId=" + matchId +
                ", playerUuid=" + playerUuid +
                ", teamId=" + teamId +
                ", eventType=" + eventType +
                ", assistPlayerUuid=" + assistPlayerUuid +
                ", registeredByUuid=" + registeredByUuid +
                ", minuto=" + minuto +
                '}';
    }
}
