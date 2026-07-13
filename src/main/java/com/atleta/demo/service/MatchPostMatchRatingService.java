package com.atleta.demo.service;

import com.atleta.demo.dto.request.PlayerPerformanceDto;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchEvent;
import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.entity.MatchTeam;
import com.atleta.demo.entity.Position;
import com.atleta.demo.entity.Team;
import com.atleta.demo.enums.EventType;
import com.atleta.demo.enums.MatchResultType;
import com.atleta.demo.enums.PriorityLevel;
import com.atleta.demo.enums.RoleType;
import com.atleta.demo.repository.MatchEventRepository;
import com.atleta.demo.repository.MatchPlayerRepository;
import com.atleta.demo.repository.MatchTeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MatchPostMatchRatingService {

    private static final Logger logger = LoggerFactory.getLogger(MatchPostMatchRatingService.class);

    private final MatchTeamRepository matchTeamRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchEventRepository matchEventRepository;
    private final RatingService ratingService;

    public MatchPostMatchRatingService(MatchTeamRepository matchTeamRepository,
                                       MatchPlayerRepository matchPlayerRepository,
                                       MatchEventRepository matchEventRepository,
                                       RatingService ratingService) {
        this.matchTeamRepository = matchTeamRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.matchEventRepository = matchEventRepository;
        this.ratingService = ratingService;
    }

    public void updatePlayerRatingsAfterMatch(Match match) {
        logger.debug("Iniciando actualizacion de calificaciones para el partido {}", match.getId());

        List<MatchTeam> matchTeams = matchTeamRepository.findByMatch(match);
        if (matchTeams.size() != 2) {
            logger.warn("El partido {} no tiene exactamente 2 equipos, saltando actualizacion de calificaciones", match.getId());
            return;
        }

        MatchResultType matchResult = determineMatchResult(matchTeams);
        logger.debug("Resultado del partido {}: {}", match.getId(), matchResult);

        List<MatchPlayer> matchPlayers = matchPlayerRepository.findByMatch(match);
        if (matchPlayers.isEmpty()) {
            logger.warn("No hay jugadores registrados en el partido {}", match.getId());
            return;
        }

        List<MatchEvent> matchEvents = matchEventRepository.findByMatchOrderByCreatedAt(match);
        List<PlayerPerformanceDto> performanceData = new ArrayList<>();

        for (MatchPlayer matchPlayer : matchPlayers) {
            if (!Boolean.TRUE.equals(matchPlayer.getConfirmado())) {
                logger.debug(
                        "Saltando jugador {} - no confirmo participacion",
                        matchPlayer.getPlayer().getAtletaUuid()
                );
                continue;
            }

            PlayerPerformanceDto performance = collectPlayerPerformance(matchPlayer, matchEvents, matchResult, matchTeams);
            performanceData.add(performance);

            logger.debug(
                    "Datos recopilados para jugador {}: {} goles, {} asistencias, MVP: {}",
                    performance.getPlayerProfileId(),
                    performance.getGoalsScored(),
                    performance.getAssistsMade(),
                    performance.getWasMvp()
            );
        }

        if (!performanceData.isEmpty()) {
            ratingService.updatePlayerRatings(match.getId(), performanceData);
            logger.info(
                    "Calificaciones actualizadas para {} jugadores del partido {}",
                    performanceData.size(),
                    match.getId()
            );
        }
    }

    private MatchResultType determineMatchResult(List<MatchTeam> matchTeams) {
        MatchTeam localTeam = matchTeams.stream()
                .filter(MatchTeam::getEsLocal)
                .findFirst()
                .orElse(null);

        MatchTeam visitingTeam = matchTeams.stream()
                .filter(mt -> !mt.getEsLocal())
                .findFirst()
                .orElse(null);

        if (localTeam == null || visitingTeam == null) {
            return MatchResultType.EMPATE;
        }

        int localGoals = localTeam.getGoles();
        int visitingGoals = visitingTeam.getGoles();

        if (localGoals > visitingGoals) {
            return MatchResultType.GANADO;
        }
        if (localGoals < visitingGoals) {
            return MatchResultType.PERDIDO;
        }
        return MatchResultType.EMPATE;
    }

    private PlayerPerformanceDto collectPlayerPerformance(MatchPlayer matchPlayer,
                                                          List<MatchEvent> matchEvents,
                                                          MatchResultType matchResult,
                                                          List<MatchTeam> matchTeams) {
        UUID playerUuid = matchPlayer.getPlayer().getAtletaUuid();

        int goalsScored = (int) matchEvents.stream()
                .filter(event -> event.getTipoEvento() == EventType.GOL)
                .filter(MatchEvent::isFullyConfirmed)
                .filter(event -> playerUuid.equals(event.getPlayer().getAtletaUuid()))
                .count();

        int assistsMade = (int) matchEvents.stream()
                .filter(event -> event.getTipoEvento() == EventType.GOL)
                .filter(MatchEvent::isFullyConfirmed)
                .filter(event -> event.getAssistPlayer() != null)
                .filter(event -> playerUuid.equals(event.getAssistPlayer().getAtletaUuid()))
                .count();

        boolean wasMvp = false;
        int playerContributions = goalsScored + assistsMade;
        if (playerContributions > 0) {
            wasMvp = isPlayerMvp(playerUuid, matchEvents);
        }

        RoleType roleType = mapPositionToRole(matchPlayer.getPosition());
        PriorityLevel priorityLevel = determinePriorityLevel();

        Integer goalsConceded = null;
        if (roleType == RoleType.DEFENSA || roleType == RoleType.ARQUERO) {
            goalsConceded = calculateGoalsConceded(matchPlayer, matchTeams);
        }

        return new PlayerPerformanceDto(
                playerUuid,
                roleType,
                priorityLevel,
                goalsScored,
                assistsMade,
                goalsConceded,
                wasMvp,
                matchResult
        );
    }

    private boolean isPlayerMvp(UUID playerUuid, List<MatchEvent> matchEvents) {
        Map<UUID, Integer> playerContributions = new HashMap<>();

        for (MatchEvent event : matchEvents) {
            if (event.getTipoEvento() == EventType.GOL && event.isFullyConfirmed()) {
                UUID goalScorer = event.getPlayer().getAtletaUuid();
                playerContributions.merge(goalScorer, 1, Integer::sum);

                if (event.getAssistPlayer() != null) {
                    UUID assistPlayer = event.getAssistPlayer().getAtletaUuid();
                    playerContributions.merge(assistPlayer, 1, Integer::sum);
                }
            }
        }

        int maxContributions = playerContributions.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        return maxContributions > 0
                && playerContributions.getOrDefault(playerUuid, 0) == maxContributions;
    }

    private RoleType mapPositionToRole(Position position) {
        if (position == null || position.getNombre() == null) {
            return RoleType.MEDIOCAMPO;
        }

        String positionName = position.getNombre().toLowerCase();

        if (positionName.contains("arquero") || positionName.contains("portero")) {
            return RoleType.ARQUERO;
        }
        if (positionName.contains("defensa") || positionName.contains("defensor")) {
            return RoleType.DEFENSA;
        }
        if (positionName.contains("delantero") || positionName.contains("atacante")) {
            return RoleType.ATAQUE;
        }
        return RoleType.MEDIOCAMPO;
    }

    private PriorityLevel determinePriorityLevel() {
        return PriorityLevel.PRINCIPAL;
    }

    private Integer calculateGoalsConceded(MatchPlayer matchPlayer, List<MatchTeam> matchTeams) {
        Team playerTeam = matchPlayer.getTeam();

        MatchTeam opposingTeam = matchTeams.stream()
                .filter(mt -> !mt.getTeam().getId().equals(playerTeam.getId()))
                .findFirst()
                .orElse(null);

        return opposingTeam != null ? opposingTeam.getGoles() : 0;
    }
}
