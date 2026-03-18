package com.atleta.demo.service.xp;

import java.util.List;

/**
 * Snapshot inmutable de entrada para el motor de XP.
 *
 * @param validMatch indica si el partido cumple reglas de validez para otorgar XP
 * @param players jugadores participantes a evaluar
 */
public record MatchXpInput(
        boolean validMatch,
        List<PlayerXpSnapshot> players
) {
}
