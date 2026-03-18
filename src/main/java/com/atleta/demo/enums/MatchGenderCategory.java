package com.atleta.demo.enums;

/**
 * Categoria de convocatoria por genero para un partido.
 */
public enum MatchGenderCategory {
    MIXTO("Mixto"),
    SOLO_MUJERES("Solo mujeres"),
    SOLO_HOMBRES("Solo hombres");

    private final String displayName;

    MatchGenderCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
