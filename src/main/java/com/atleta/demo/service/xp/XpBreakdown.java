package com.atleta.demo.service.xp;

/**
 * Desglose de XP por regla aplicada para trazabilidad.
 *
 * @param playXp XP por jugar el partido
 * @param resultXp XP por resultado (victoria/derrota/empate)
 * @param goalXp XP por goles ponderados por posicion
 * @param assistXp XP por asistencias
 * @param goalkeeperXp XP especial para portero
 * @param totalXp XP total final
 */
public record XpBreakdown(
        int playXp,
        int resultXp,
        int goalXp,
        int assistXp,
        int goalkeeperXp,
        int totalXp
) {
}
