package com.atleta.demo.enums;

/**
 * Tipos de resultado de partido con puntos normales y puntos para modo arquero rotativo.
 */
public enum MatchResultType {
    /**
     * Partido ganado - puntos positivos altos
     */
    GANADO(2.0, 1.5),
    
    /**
     * Partido empatado - puntos positivos bajos
     */
    EMPATE(0.5, 0.3),
    
    /**
     * Partido perdido - puntos negativos
     */
    PERDIDO(-1.5, -1.2);
    
    private final double normalPoints;
    private final double rotativeGoalkeeperPoints;
    
    MatchResultType(double normalPoints, double rotativeGoalkeeperPoints) {
        this.normalPoints = normalPoints;
        this.rotativeGoalkeeperPoints = rotativeGoalkeeperPoints;
    }
    
    /**
     * Obtiene los puntos aplicados en modo normal para este resultado
     * @return puntos normales como double
     */
    public double getNormalPoints() {
        return normalPoints;
    }
    
    /**
     * Obtiene los puntos aplicados en modo arquero rotativo para este resultado
     * @return puntos de arquero rotativo como double
     */
    public double getRotativeGoalkeeperPoints() {
        return rotativeGoalkeeperPoints;
    }
}