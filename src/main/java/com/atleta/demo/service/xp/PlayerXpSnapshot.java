package com.atleta.demo.service.xp;

import com.atleta.demo.enums.MatchResult;
import com.atleta.demo.enums.PlayerRole;

import java.util.UUID;

/**
 * Snapshot de performance y contexto de un jugador en el match finalizado.
 *
 * @param userUuid uuid del jugador
 * @param positionId posicion jugada (fuente para asignar XP por posicion)
 * @param positionName nombre de posicion jugada
 * @param role rol en el match (JUGADOR/CAPITAN/DT)
 * @param result resultado del partido para el jugador
 * @param goals goles anotados
 * @param assists asistencias realizadas
 * @param goalsConceded goles recibidos (aplica para portero)
 * @param confirmedParticipation si confirmo participacion efectiva del match
 */
public record PlayerXpSnapshot(
        UUID userUuid,
        Long positionId,
        String positionName,
        PlayerRole role,
        MatchResult result,
        int goals,
        int assists,
        int goalsConceded,
        boolean confirmedParticipation
) {
}
