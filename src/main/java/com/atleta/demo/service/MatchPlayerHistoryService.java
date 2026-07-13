package com.atleta.demo.service;

import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchEvent;
import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.entity.PlayerHistory;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Team;
import com.atleta.demo.enums.EventType;
import com.atleta.demo.enums.MatchResult;
import com.atleta.demo.enums.MatchTeamSide;
import com.atleta.demo.repository.MatchEventRepository;
import com.atleta.demo.repository.MatchPlayerRepository;
import com.atleta.demo.repository.MatchTeamRepository;
import com.atleta.demo.repository.PlayerHistoryRepository;
import com.atleta.demo.repository.PlayerPositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MatchPlayerHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(MatchPlayerHistoryService.class);

    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchEventRepository matchEventRepository;
    private final MatchTeamRepository matchTeamRepository;
    private final PlayerHistoryRepository playerHistoryRepository;
    private final PlayerPositionRepository playerPositionRepository;

    public MatchPlayerHistoryService(
            MatchPlayerRepository matchPlayerRepository,
            MatchEventRepository matchEventRepository,
            MatchTeamRepository matchTeamRepository,
            PlayerHistoryRepository playerHistoryRepository,
            PlayerPositionRepository playerPositionRepository
    ) {
        this.matchPlayerRepository = matchPlayerRepository;
        this.matchEventRepository = matchEventRepository;
        this.matchTeamRepository = matchTeamRepository;
        this.playerHistoryRepository = playerHistoryRepository;
        this.playerPositionRepository = playerPositionRepository;
    }

    public void persistForFinalization(Match match) {
        List<MatchPlayer> players = matchPlayerRepository.findByMatch(match);
        if (players.isEmpty()) {
            return;
        }
        int createdHistoryRows = 0;

        int finalLocal = match.getFinalScoreLocal() != null ? match.getFinalScoreLocal() : 0;
        int finalAway = match.getFinalScoreAway() != null ? match.getFinalScoreAway() : 0;

        Map<UUID, Integer> goalsByPlayer = new HashMap<>();
        Map<UUID, Integer> assistsByPlayer = new HashMap<>();
        List<MatchEvent> events = matchEventRepository.findByMatchOrderByCreatedAt(match);
        for (MatchEvent event : events) {
            if (event.getTipoEvento() != EventType.GOL || event.getPlayer() == null || !event.isFullyConfirmed()) {
                continue;
            }

            UUID scorerUuid = event.getPlayer().getAtletaUuid();
            goalsByPlayer.merge(scorerUuid, 1, Integer::sum);

            if (event.getAssistPlayer() != null) {
                UUID assistUuid = event.getAssistPlayer().getAtletaUuid();
                assistsByPlayer.merge(assistUuid, 1, Integer::sum);
            }
        }

        for (MatchPlayer matchPlayer : players) {
            if (!Boolean.TRUE.equals(matchPlayer.getConfirmado()) || matchPlayer.getPlayer() == null) {
                continue;
            }

            PlayerProfile player = matchPlayer.getPlayer();
            if (playerHistoryRepository.findByMatchAndPlayer(match, player).isPresent()) {
                continue;
            }

            if (matchPlayer.getTeam() == null || matchPlayer.getPosition() == null) {
                logger.warn("Se omite historial del jugador {} en partido {} por datos incompletos",
                        player.getAtletaUuid(), match.getId());
                continue;
            }

            MatchTeamSide side = matchPlayer.getTeamSide() != null
                    ? matchPlayer.getTeamSide()
                    : resolveTeamSide(match, matchPlayer.getTeam());

            MatchResult result = resolvePlayerResult(side, finalLocal, finalAway);
            int goals = goalsByPlayer.getOrDefault(player.getAtletaUuid(), 0);
            int assists = assistsByPlayer.getOrDefault(player.getAtletaUuid(), 0);
            int xp = estimateXpForPlayer(matchPlayer, goals, finalLocal, finalAway,
                    side != null ? side : MatchTeamSide.LOCAL);

            PlayerHistory history = new PlayerHistory(
                    match,
                    player,
                    matchPlayer.getTeam(),
                    matchPlayer.getPosition(),
                    goals,
                    assists,
                    result,
                    xp
            );
            playerHistoryRepository.save(history);
            createdHistoryRows += 1;

            playerPositionRepository.findByPlayerAndPosition(player, matchPlayer.getPosition())
                    .ifPresent(playerPosition -> {
                        playerPosition.addXp(xp);
                        playerPositionRepository.save(playerPosition);
                    });
        }
        logger.info("Historial de jugadores persistido para partido {}: {} filas nuevas",
                match.getId(), createdHistoryRows);
    }

    public boolean hasHistoryRows(Match match) {
        return !playerHistoryRepository.findByMatch(match).isEmpty();
    }

    private MatchResult resolvePlayerResult(MatchTeamSide side, int finalLocal, int finalAway) {
        if (finalLocal == finalAway) {
            return MatchResult.EMPATE;
        }

        boolean localWon = finalLocal > finalAway;
        if (side == MatchTeamSide.VISITA) {
            return localWon ? MatchResult.DERROTA : MatchResult.VICTORIA;
        }

        return localWon ? MatchResult.VICTORIA : MatchResult.DERROTA;
    }

    private int estimateXpForPlayer(MatchPlayer player, int goals, int finalLocal, int finalAway, MatchTeamSide side) {
        int xp = 10;
        if (finalLocal != finalAway) {
            boolean won = (side == MatchTeamSide.LOCAL && finalLocal > finalAway)
                    || (side == MatchTeamSide.VISITA && finalAway > finalLocal);
            xp += won ? 10 : 5;
        }

        String position = player.getPosition() != null && player.getPosition().getNombre() != null
                ? player.getPosition().getNombre().toUpperCase()
                : "";
        int goalXp;
        if (position.contains("DEF")) {
            goalXp = 15;
        } else if (position.contains("MED")) {
            goalXp = 12;
        } else {
            goalXp = 10;
        }
        xp += goals * goalXp;
        return xp;
    }

    private MatchTeamSide resolveTeamSide(Match match, Team team) {
        return matchTeamRepository.findLocalTeamByMatch(match)
                .filter(localTeam -> localTeam.getTeam() != null && localTeam.getTeam().getId().equals(team.getId()))
                .map(localTeam -> MatchTeamSide.LOCAL)
                .or(() -> matchTeamRepository.findVisitingTeamByMatch(match)
                        .filter(awayTeam -> awayTeam.getTeam() != null && awayTeam.getTeam().getId().equals(team.getId()))
                        .map(awayTeam -> MatchTeamSide.VISITA))
                .orElse(null);
    }
}
