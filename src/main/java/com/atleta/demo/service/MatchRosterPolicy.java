package com.atleta.demo.service;

import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.enums.GenderType;
import com.atleta.demo.enums.MatchGenderCategory;
import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class MatchRosterPolicy {

    public int playersPerTeamByModality(MatchMode modality) {
        if (modality == MatchMode.SEIS_VS_SEIS) {
            return 6;
        }
        if (modality == MatchMode.SIETE_VS_SIETE) {
            return 7;
        }
        return 5;
    }

    public void validateTeamAssignmentWindow(Match match) {
        if (match.getEstado() == MatchStatus.FINALIZADO) {
            throw new IllegalArgumentException("No se puede editar equipos en partido finalizado");
        }

        if (match.getStartedAt() != null || match.getEstado() == MatchStatus.INICIADO) {
            throw new IllegalArgumentException("Los equipos se bloquean al comenzar el partido");
        }
    }

    public boolean hasMinimumConfirmedPlayers(Match match, List<MatchPlayer> players) {
        long confirmed = confirmedPlayerCount(match, players);
        int minimum = playersPerTeamByModality(match.getModalidad()) * 2;
        return confirmed >= minimum;
    }

    /**
     * El creador ocupa un cupo desde que organiza el partido, aunque una fila
     * legacy de match_players exista todavía con confirmado=false. Contarlo por
     * presencia producía una diferencia entre el 10/10 mostrado y el 9/10 que
     * validaba el inicio.
     */
    public long confirmedPlayerCount(Match match, List<MatchPlayer> players) {
        long confirmed = players.stream()
                .filter(item -> Boolean.TRUE.equals(item.getConfirmado()))
                .count();
        boolean creatorConfirmed = players.stream()
                .anyMatch(item -> Boolean.TRUE.equals(item.getConfirmado())
                        && item.getPlayer() != null
                        && match.getCreador() != null
                        && match.getCreador().getAtletaUuid().equals(item.getPlayer().getAtletaUuid()));
        if (match.getCreador() != null && !creatorConfirmed) {
            confirmed += 1;
        }
        return confirmed;
    }

    public void validateGenderAssignmentRules(
            Match match,
            List<MatchPlayer> players,
            Set<UUID> homeSet,
            Set<UUID> awaySet
    ) {
        if (match == null || match.getCategoriaGenero() == null) {
            return;
        }

        GenderCounts counts = countAssignedGenders(players, homeSet, awaySet);

        if (match.getCategoriaGenero() == MatchGenderCategory.SOLO_MUJERES
                && (counts.homeMen > 0 || counts.awayMen > 0)) {
            throw new IllegalArgumentException("En convocatoria solo mujeres no se permiten hombres en los equipos");
        }

        if (match.getCategoriaGenero() == MatchGenderCategory.SOLO_HOMBRES
                && (counts.homeWomen > 0 || counts.awayWomen > 0)) {
            throw new IllegalArgumentException("En convocatoria solo hombres no se permiten mujeres en los equipos");
        }

        if (match.getCategoriaGenero() == MatchGenderCategory.MIXTO
                && (Math.abs(counts.homeWomen - counts.homeMen) > 1
                || Math.abs(counts.awayWomen - counts.awayMen) > 1)) {
            throw new IllegalArgumentException(
                    "En convocatoria mixta cada equipo debe quedar balanceado por genero (diferencia maxima de 1)"
            );
        }
    }

    private GenderCounts countAssignedGenders(List<MatchPlayer> players, Set<UUID> homeSet, Set<UUID> awaySet) {
        GenderCounts counts = new GenderCounts();

        for (MatchPlayer item : players) {
            if (item.getPlayer() == null
                    || item.getPlayer().getAthlete() == null
                    || item.getPlayer().getAtletaUuid() == null) {
                continue;
            }

            UUID playerUuid = item.getPlayer().getAtletaUuid();
            GenderType gender = item.getPlayer().getAthlete().getGenero();
            boolean assigned = homeSet.contains(playerUuid) || awaySet.contains(playerUuid);
            if (assigned && gender == null) {
                throw new IllegalArgumentException(
                        "Hay jugadores sin genero definido. Completa su perfil para poder asignar equipos por convocatoria"
                );
            }
            if (!assigned) {
                continue;
            }

            counts.add(playerUuid, gender, homeSet, awaySet);
        }

        return counts;
    }

    private static final class GenderCounts {
        private int homeWomen;
        private int homeMen;
        private int awayWomen;
        private int awayMen;

        private void add(UUID playerUuid, GenderType gender, Set<UUID> homeSet, Set<UUID> awaySet) {
            if (homeSet.contains(playerUuid)) {
                if (gender == GenderType.FEMENINO) {
                    homeWomen += 1;
                } else {
                    homeMen += 1;
                }
                return;
            }

            if (awaySet.contains(playerUuid)) {
                if (gender == GenderType.FEMENINO) {
                    awayWomen += 1;
                } else {
                    awayMen += 1;
                }
            }
        }
    }
}
