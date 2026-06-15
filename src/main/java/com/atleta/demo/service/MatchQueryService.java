package com.atleta.demo.service;

import com.atleta.demo.dto.response.MatchEventResponse;
import com.atleta.demo.dto.response.MatchResponse;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchEvent;
import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.entity.MatchTeam;
import com.atleta.demo.entity.PlayerPosition;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Position;
import com.atleta.demo.entity.Team;
import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.repository.MatchEventRepository;
import com.atleta.demo.repository.MatchPlayerRepository;
import com.atleta.demo.repository.MatchRepository;
import com.atleta.demo.repository.MatchTeamRepository;
import com.atleta.demo.repository.PlayerPositionRepository;
import com.atleta.demo.repository.PlayerProfileRepository;
import com.atleta.demo.repository.PositionRepository;
import com.atleta.demo.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MatchQueryService {

    private final MatchRepository matchRepository;
    private final MatchTeamRepository matchTeamRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchEventRepository matchEventRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final TeamRepository teamRepository;
    private final PositionRepository positionRepository;
    private final PlayerPositionRepository playerPositionRepository;
    private final MatchAutomatedStatusService matchAutomatedStatusService;
    private final MatchResponseMapper matchResponseMapper;

    public MatchQueryService(MatchRepository matchRepository,
                             MatchTeamRepository matchTeamRepository,
                             MatchPlayerRepository matchPlayerRepository,
                             MatchEventRepository matchEventRepository,
                             PlayerProfileRepository playerProfileRepository,
                             TeamRepository teamRepository,
                             PositionRepository positionRepository,
                             PlayerPositionRepository playerPositionRepository,
                             MatchAutomatedStatusService matchAutomatedStatusService,
                             MatchResponseMapper matchResponseMapper) {
        this.matchRepository = matchRepository;
        this.matchTeamRepository = matchTeamRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.matchEventRepository = matchEventRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.teamRepository = teamRepository;
        this.positionRepository = positionRepository;
        this.playerPositionRepository = playerPositionRepository;
        this.matchAutomatedStatusService = matchAutomatedStatusService;
        this.matchResponseMapper = matchResponseMapper;
    }

    @Transactional
    public MatchResponse getMatchById(Long matchId) {
        matchAutomatedStatusService.refreshAutomatedMatchStates();
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + matchId));
        ensureCreatorParticipation(match, resolveDefaultTeamForCreator(match));
        return matchResponseMapper.toMatchResponse(match);
    }

    @Transactional
    public List<MatchResponse> getAllMatches() {
        matchAutomatedStatusService.refreshAutomatedMatchStates();
        return matchRepository.findAll().stream()
                .peek(match -> ensureCreatorParticipation(match, resolveDefaultTeamForCreator(match)))
                .map(matchResponseMapper::toMatchResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<MatchResponse> getUpcomingMatches() {
        matchAutomatedStatusService.refreshAutomatedMatchStates();
        return matchRepository.findUpcomingMatches().stream()
                .peek(match -> ensureCreatorParticipation(match, resolveDefaultTeamForCreator(match)))
                .map(matchResponseMapper::toMatchResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<MatchResponse> getMatchesByPlayer(UUID playerUuid) {
        matchAutomatedStatusService.refreshAutomatedMatchStates();
        PlayerProfile player = playerProfileRepository.findById(playerUuid)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + playerUuid));

        return matchRepository.findByPlayer(player).stream()
                .peek(match -> ensureCreatorParticipation(match, resolveDefaultTeamForCreator(match)))
                .map(matchResponseMapper::toMatchResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<MatchResponse> getMatchesByPlayerOrCreator(UUID playerUuid) {
        matchAutomatedStatusService.refreshAutomatedMatchStates();
        PlayerProfile player = playerProfileRepository.findById(playerUuid)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + playerUuid));

        List<Match> byPlayer = matchRepository.findByPlayer(player);
        List<Match> byCreator = matchRepository.findByCreador(player);

        LinkedHashMap<Long, Match> mergedById = new LinkedHashMap<>();
        for (Match match : byPlayer) {
            mergedById.put(match.getId(), match);
        }
        for (Match match : byCreator) {
            mergedById.put(match.getId(), match);
        }

        return mergedById.values().stream()
                .peek(match -> ensureCreatorParticipation(match, resolveDefaultTeamForCreator(match)))
                .map(matchResponseMapper::toMatchResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<MatchResponse> getMatchesByTeam(Long teamId) {
        matchAutomatedStatusService.refreshAutomatedMatchStates();
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado: " + teamId));

        return matchRepository.findByTeam(team).stream()
                .peek(match -> ensureCreatorParticipation(match, resolveDefaultTeamForCreator(match)))
                .map(matchResponseMapper::toMatchResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MatchEventResponse> getMatchEvents(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + matchId));

        return matchEventRepository.findByMatchOrderByCreatedAt(match).stream()
                .map(matchResponseMapper::toMatchEventResponse)
                .collect(Collectors.toList());
    }

    private void ensureCreatorParticipation(Match match, Team preferredTeam) {
        if (match == null || match.getCreador() == null) {
            return;
        }

        if (matchPlayerRepository.existsByMatchAndPlayer(match, match.getCreador())) {
            return;
        }

        Team team = preferredTeam != null ? preferredTeam : resolveDefaultTeamForCreator(match);
        if (team == null) {
            return;
        }

        Position position = resolveDefaultPositionForPlayer(match.getCreador());
        if (position == null) {
            throw new IllegalStateException("No se pudo resolver una posicion para el creador del partido");
        }

        MatchPlayer creatorParticipation = new MatchPlayer(match, team, match.getCreador(), position, PlayerRole.CAPITAN);
        creatorParticipation.setConfirmado(true);
        matchPlayerRepository.save(creatorParticipation);
    }

    private Team resolveDefaultTeamForCreator(Match match) {
        MatchTeam localTeam = matchTeamRepository.findLocalTeamByMatch(match).orElse(null);
        if (localTeam != null && localTeam.getTeam() != null) {
            return localTeam.getTeam();
        }

        MatchTeam awayTeam = matchTeamRepository.findVisitingTeamByMatch(match).orElse(null);
        if (awayTeam != null && awayTeam.getTeam() != null) {
            return awayTeam.getTeam();
        }

        List<MatchTeam> teams = matchTeamRepository.findByMatch(match);
        if (!teams.isEmpty() && teams.get(0).getTeam() != null) {
            return teams.get(0).getTeam();
        }

        return null;
    }

    private Position resolveDefaultPositionForPlayer(PlayerProfile player) {
        if (player == null) {
            return null;
        }

        PlayerPosition primaryPosition = playerPositionRepository.findPrimaryPositionByPlayer(player).orElse(null);
        if (primaryPosition != null && primaryPosition.getPosition() != null) {
            return primaryPosition.getPosition();
        }

        return positionRepository.findAllOrderByNombre().stream().findFirst().orElse(null);
    }
}
