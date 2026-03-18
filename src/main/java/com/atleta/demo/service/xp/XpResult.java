package com.atleta.demo.service.xp;

import java.util.UUID;

/**
 * Resultado de XP final para persistencia en historial/posiciones.
 *
 * @param userUuid jugador evaluado
 * @param positionId posicion a la que se debe acreditar la XP
 * @param positionName nombre de posicion
 * @param breakdown desglose de puntos aplicados
 */
public record XpResult(
        UUID userUuid,
        Long positionId,
        String positionName,
        XpBreakdown breakdown
) {
}
