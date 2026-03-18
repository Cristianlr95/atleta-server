package com.atleta.demo.enums;

/**
 * Modalidades de partido disponibles en el sistema.
 * Define el número de jugadores por equipo en cada modalidad.
 */
public enum MatchMode {
    CINCO_VS_CINCO("5v5"),
    SEIS_VS_SEIS("6v6"),
    SIETE_VS_SIETE("7v7");

    private final String displayName;

    MatchMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}