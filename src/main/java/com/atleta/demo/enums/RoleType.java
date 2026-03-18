package com.atleta.demo.enums;

/**
 * Tipos de roles de jugador en el campo con sus respectivos pesos para goles y asistencias.
 * Los laterales izquierdo y derecho se clasifican como CARRILERO.
 */
public enum RoleType {
    /**
     * Jugador de ataque - máximo peso para goles, peso medio para asistencias
     */
    ATAQUE(1.0, 0.6),
    
    /**
     * Jugador de defensa - peso bajo para goles y asistencias
     */
    DEFENSA(0.3, 0.3),
    
    /**
     * Jugador de mediocampo - peso medio para goles, máximo para asistencias
     */
    MEDIOCAMPO(0.6, 1.0),
    
    /**
     * Jugador carrilero (incluye laterales izquierdo y derecho) - peso medio-bajo para goles, peso medio-alto para asistencias
     */
    CARRILERO(0.5, 0.7),
    
    /**
     * Arquero - peso mínimo para goles, sin peso para asistencias
     */
    ARQUERO(0.1, 0.0),
    
    /**
     * Director técnico - peso bajo para goles y asistencias
     */
    DT(0.2, 0.2);
    
    private final double goalWeight;
    private final double assistWeight;
    
    RoleType(double goalWeight, double assistWeight) {
        this.goalWeight = goalWeight;
        this.assistWeight = assistWeight;
    }
    
    /**
     * Obtiene el peso aplicado a los goles para este rol
     * @return peso de goles como double
     */
    public double getGoalWeight() {
        return goalWeight;
    }
    
    /**
     * Obtiene el peso aplicado a las asistencias para este rol
     * @return peso de asistencias como double
     */
    public double getAssistWeight() {
        return assistWeight;
    }
}