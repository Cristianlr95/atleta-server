package com.atleta.demo.enums;

/**
 * Niveles de prioridad para los roles de jugador con calificaciones base mínimas y multiplicadores.
 */
public enum PriorityLevel {
    /**
     * Prioridad principal - calificación base más alta y multiplicador completo
     */
    PRINCIPAL(70, 1.0),
    
    /**
     * Prioridad secundaria - calificación base media y multiplicador reducido
     */
    SECUNDARIA(60, 0.7),
    
    /**
     * Prioridad terciaria - calificación base más baja y multiplicador mínimo
     */
    TERCIARIA(50, 0.4);
    
    private final int baseRating;
    private final double multiplier;
    
    PriorityLevel(int baseRating, double multiplier) {
        this.baseRating = baseRating;
        this.multiplier = multiplier;
    }
    
    /**
     * Obtiene la calificación base mínima para este nivel de prioridad
     * @return calificación base como entero
     */
    public int getBaseRating() {
        return baseRating;
    }
    
    /**
     * Obtiene el multiplicador aplicado al delta de calificación para este nivel de prioridad
     * @return multiplicador como double
     */
    public double getMultiplier() {
        return multiplier;
    }
}