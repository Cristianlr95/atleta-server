package com.atleta.demo.enums;

/**
 * Estados posibles de un partido durante su ciclo de vida.
 */
public enum MatchStatus {
    /**
     * Partido recién creado, esperando confirmaciones
     */
    CREADO,
    
    /**
     * Partido en progreso
     */
    INICIADO,
    
    /**
     * Partido completado normalmente
     */
    FINALIZADO,
    
    /**
     * Partido cancelado o invalidado
     */
    INVALIDO
}