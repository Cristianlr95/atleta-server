package com.atleta.demo.service;

import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchEvent;
import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.entity.MatchTeam;
import com.atleta.demo.entity.Team;
import com.atleta.demo.enums.EventType;
import com.atleta.demo.enums.MatchTeamSide;
import com.atleta.demo.repository.MatchEventRepository;
import com.atleta.demo.repository.MatchPlayerRepository;
import com.atleta.demo.repository.MatchTeamRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MatchFinalScoreService {

    private final MatchEventRepository matchEventRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchTeamRepository matchTeamRepository;

    public MatchFinalScoreService(
            MatchEventRepository matchEventRepository,
            MatchPlayerRepository matchPlayerRepository,
            MatchTeamRepository matchTeamRepository
    ) {
        this.matchEventRepository = matchEventRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.matchTeamRepository = matchTeamRepository;
    }

    public void applyFinalScoreSnapshot(Match match) {
        int localGoals = 0;
        int awayGoals = 0;

        List<MatchEvent> events = matchEventRepository.findByMatchOrderByCreatedAt(match);
        for (MatchEvent event : events) {
            if (event.getTipoEvento() != EventType.GOL || event.getPlayer() == null || !event.isFullyConfirmed()) {
                continue;
            }

            MatchTeamSide eventSide = resolveEventTeamSide(match, event);
            if (eventSide == MatchTeamSide.VISITA) {
                awayGoals += 1;
            } else {
                localGoals += 1;
            }
        }

        List<MatchTeam> teams = matchTeamRepository.findByMatch(match);
        MatchTeam localTeam = teams.stream()
                .filter(MatchTeam::getEsLocal)
                .findFirst()
                .orElse(null);
        MatchTeam awayTeam = teams.stream()
                .filter(team -> !team.getEsLocal())
                .findFirst()
                .orElse(null);

        if (localTeam != null) {
            localTeam.setGoles(localGoals);
        }

        if (awayTeam != null) {
            awayTeam.setGoles(awayGoals);
        }

        if (localTeam != null || awayTeam != null) {
            matchTeamRepository.saveAll(
                    teams.stream()
                            .filter(team -> team == localTeam || team == awayTeam)
                            .collect(Collectors.toList())
            );
        }

        match.setFinalScoreLocal(localGoals);
        match.setFinalScoreAway(awayGoals);
    }

    private MatchTeamSide resolveEventTeamSide(Match match, MatchEvent event) {
        if (event == null || event.getPlayer() == null) {
            return MatchTeamSide.LOCAL;
        }

        MatchPlayer matchPlayer = matchPlayerRepository.findByMatchAndPlayer(match, event.getPlayer()).orElse(null);
        if (matchPlayer != null) {
            if (matchPlayer.getTeamSide() != null) {
                return matchPlayer.getTeamSide();
            }
            if (matchPlayer.getTeam() != null) {
                MatchTeamSide side = resolveTeamSide(match, matchPlayer.getTeam());
                if (side != null) {
                    return side;
                }
            }
        }

        if (event.getTeam() != null) {
            MatchTeamSide side = resolveTeamSide(match, event.getTeam());
            if (side != null) {
                return side;
            }
        }

        return MatchTeamSide.LOCAL;
    }

    private MatchTeamSide resolveTeamSide(Match match, Team team) {
        MatchTeam localTeam = matchTeamRepository.findLocalTeamByMatch(match).orElse(null);
        MatchTeam awayTeam = matchTeamRepository.findVisitingTeamByMatch(match).orElse(null);

        if (localTeam != null && localTeam.getTeam() != null && localTeam.getTeam().getId().equals(team.getId())) {
            return MatchTeamSide.LOCAL;
        }

        if (awayTeam != null && awayTeam.getTeam() != null && awayTeam.getTeam().getId().equals(team.getId())) {
            return MatchTeamSide.VISITA;
        }

        return null;
    }
}
