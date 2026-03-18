package com.atleta.demo.enums;

/**
 * Estado de validacion del match para habilitar o bloquear el cierre con XP.
 */
public enum MatchValidationStatus {
    /**
     * Aun no se evaluo la validez del partido.
     */
    PENDING,

    /**
     * Match valido para generar XP/estadisticas.
     */
    VALID,

    /**
     * Match invalido por ventana horaria.
     */
    INVALID_TIME_WINDOW,

    /**
     * Match invalido por porcentaje de confirmaciones insuficiente.
     */
    INVALID_CONFIRMATION_THRESHOLD,

    /**
     * Match invalido por estado/precondiciones del flujo.
     */
    INVALID_STATE
}
