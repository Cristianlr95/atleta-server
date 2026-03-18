package com.atleta.demo.dto.response;

/**
 * DTO de respuesta para la información de posición de un jugador.
 * Contiene la posición, prioridad y experiencia del jugador.
 */
public class PlayerPositionResponse {

    /**
     * ID de la posición del jugador
     */
    private Long id;

    /**
     * Información de la posición
     */
    private PositionResponse position;

    /**
     * Prioridad de la posición (1, 2, 3)
     */
    private Integer prioridad;

    /**
     * Experiencia (XP) en esta posición
     */
    private Integer xp;

    // Constructors
    public PlayerPositionResponse() {
    }

    public PlayerPositionResponse(Long id, PositionResponse position, Integer prioridad, Integer xp) {
        this.id = id;
        this.position = position;
        this.prioridad = prioridad;
        this.xp = xp;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PositionResponse getPosition() {
        return position;
    }

    public void setPosition(PositionResponse position) {
        this.position = position;
    }

    public Integer getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(Integer prioridad) {
        this.prioridad = prioridad;
    }

    public Integer getXp() {
        return xp;
    }

    public void setXp(Integer xp) {
        this.xp = xp;
    }

    @Override
    public String toString() {
        return "PlayerPositionResponse{" +
                "id=" + id +
                ", position=" + position +
                ", prioridad=" + prioridad +
                ", xp=" + xp +
                '}';
    }
}