package com.atleta.demo.service;

import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchEvent;
import com.atleta.demo.entity.MatchTeam;
import com.atleta.demo.repository.MatchEventRepository;
import com.atleta.demo.repository.MatchTeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatchPendingEventClosureService {

    private static final Logger logger = LoggerFactory.getLogger(MatchPendingEventClosureService.class);

    private final MatchEventRepository matchEventRepository;
    private final MatchTeamRepository matchTeamRepository;

    public MatchPendingEventClosureService(
            MatchEventRepository matchEventRepository,
            MatchTeamRepository matchTeamRepository
    ) {
        this.matchEventRepository = matchEventRepository;
        this.matchTeamRepository = matchTeamRepository;
    }

    public void applyGoalToTeamScore(MatchEvent event) {
        MatchTeam matchTeam = matchTeamRepository.findByMatchAndTeam(event.getMatch(), event.getTeam())
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado en el partido"));

        matchTeam.incrementarGoles();
        matchTeamRepository.save(matchTeam);
    }

    public void closePendingEventsForFinalization(Match match) {
        List<MatchEvent> pendingEvents = matchEventRepository.findPendingEventsByMatch(match);
        if (pendingEvents.isEmpty()) {
            return;
        }

        for (MatchEvent event : pendingEvents) {
            boolean wasFullyConfirmed = event.isFullyConfirmed();

            if (!Boolean.TRUE.equals(event.getConfirmedByHome())) {
                event.confirmByHome();
            }
            if (!Boolean.TRUE.equals(event.getConfirmedByAway())) {
                event.confirmByAway();
            }

            if (!wasFullyConfirmed && event.isFullyConfirmed() && event.isGol()) {
                applyGoalToTeamScore(event);
            }
        }

        matchEventRepository.saveAll(pendingEvents);
        logger.info("Se cerraron {} eventos pendientes al finalizar partido {}", pendingEvents.size(), match.getId());
    }
}
