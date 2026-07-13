package com.atleta.demo.service;

import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.entity.MatchTeam;
import com.atleta.demo.enums.MatchStatus;
import com.atleta.demo.enums.MatchValidationStatus;
import com.atleta.demo.repository.MatchPlayerRepository;
import com.atleta.demo.repository.MatchRepository;
import com.atleta.demo.repository.MatchTeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MatchAutomatedStatusService {

    private static final Logger logger = LoggerFactory.getLogger(MatchAutomatedStatusService.class);
    private static final long DATA_CAPTURE_WINDOW_HOURS = 3;
    private static final long MATCH_PLAY_WINDOW_HOURS = 1;

    private final MatchRepository matchRepository;
    private final MatchTeamRepository matchTeamRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchPlayerHistoryService matchPlayerHistoryService;
    private final MatchRosterPolicy matchRosterPolicy;

    public MatchAutomatedStatusService(MatchRepository matchRepository,
                                       MatchTeamRepository matchTeamRepository,
                                       MatchPlayerRepository matchPlayerRepository,
                                       MatchPlayerHistoryService matchPlayerHistoryService,
                                       MatchRosterPolicy matchRosterPolicy) {
        this.matchRepository = matchRepository;
        this.matchTeamRepository = matchTeamRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.matchPlayerHistoryService = matchPlayerHistoryService;
        this.matchRosterPolicy = matchRosterPolicy;
    }

    @Transactional
    public void refreshAutomatedMatchStates() {
        autoStartReadyMatches();
        autoInvalidateExpiredMatches();
    }

    @Transactional
    void autoStartReadyMatches() {
        LocalDateTime now = LocalDateTime.now();
        List<Match> readyMatches = matchRepository.findCreatedMatchesReadyToStart(now);
        if (readyMatches.isEmpty()) {
            return;
        }

        List<Match> toStart = new ArrayList<>();
        for (Match match : readyMatches) {
            if (match.getEstado() != MatchStatus.CREADO || !canAutoStart(match, now)) {
                continue;
            }

            match.setEstado(MatchStatus.INICIADO);
            if (match.getStartedAt() == null) {
                LocalDateTime scheduledAt = match.getFechaHoraProgramada();
                match.setStartedAt(scheduledAt != null ? scheduledAt : now);
            }
            match.setValidationStatus(MatchValidationStatus.PENDING);
            match.setValidationReason(null);
            toStart.add(match);
        }

        if (!toStart.isEmpty()) {
            matchRepository.saveAll(toStart);
            logger.info("Partidos iniciados automaticamente por hora/cupo completo: {}", toStart.size());
        }
    }

    @Transactional
    void autoInvalidateExpiredMatches() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdCutoff = now.minusHours(MATCH_PLAY_WINDOW_HOURS);
        LocalDateTime startedCutoff = now.minusHours(DATA_CAPTURE_WINDOW_HOURS);

        List<Match> expiredCreated = matchRepository.findExpiredCreatedMatches(createdCutoff);
        List<Match> expiredStarted = matchRepository.findExpiredStartedMatches(startedCutoff);
        List<Match> finalized = matchRepository.findByEstado(MatchStatus.FINALIZADO);
        if (expiredCreated.isEmpty() && expiredStarted.isEmpty() && finalized.isEmpty()) {
            return;
        }

        int invalidated = 0;

        for (Match match : expiredCreated) {
            if (match.getEstado() != MatchStatus.CREADO) {
                continue;
            }
            match.setEstado(MatchStatus.INVALIDO);
            match.setValidationStatus(MatchValidationStatus.INVALID_TIME_WINDOW);
            match.setValidationReason("Partido vencido automaticamente por no iniciar a tiempo");
            invalidated += 1;
        }

        for (Match match : expiredStarted) {
            if (match.getEstado() != MatchStatus.INICIADO) {
                continue;
            }
            match.setEstado(MatchStatus.INVALIDO);
            match.setValidationStatus(MatchValidationStatus.INVALID_TIME_WINDOW);
            match.setValidationReason("Partido vencido automaticamente por no cerrarse en la ventana permitida");
            invalidated += 1;
        }

        for (Match match : finalized) {
            if (match.getEstado() != MatchStatus.FINALIZADO
                    || hasMinimumConfirmedPlayers(match)
                    || matchPlayerHistoryService.hasHistoryRows(match)) {
                continue;
            }
            match.setEstado(MatchStatus.INVALIDO);
            match.setValidationStatus(MatchValidationStatus.INVALID_CONFIRMATION_THRESHOLD);
            match.setValidationReason("Partido invalidado automaticamente por cierre inconsistente sin minimo de confirmados");
            invalidated += 1;
        }

        if (invalidated > 0) {
            List<Match> toSave = new ArrayList<>(expiredCreated.size() + expiredStarted.size() + finalized.size());
            toSave.addAll(expiredCreated);
            toSave.addAll(expiredStarted);
            toSave.addAll(finalized);
            matchRepository.saveAll(toSave);
            logger.info("Partidos invalidados automaticamente por tiempo: {}", invalidated);
        }
    }

    private boolean canAutoStart(Match match, LocalDateTime now) {
        if (match == null || match.getEstado() != MatchStatus.CREADO) {
            return false;
        }

        if (match.getFechaHoraProgramada() == null || match.getFechaHoraProgramada().isAfter(now)) {
            return false;
        }

        List<MatchTeam> teams = matchTeamRepository.findByMatch(match);
        if (teams.isEmpty()) {
            return false;
        }

        return hasMinimumConfirmedPlayers(match);
    }

    private boolean hasMinimumConfirmedPlayers(Match match) {
        List<MatchPlayer> players = matchPlayerRepository.findByMatch(match);
        return matchRosterPolicy.hasMinimumConfirmedPlayers(match, players);
    }
}
