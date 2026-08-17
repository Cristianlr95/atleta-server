package com.atleta.demo.service;

import com.atleta.demo.dto.response.MatchEventResponse;
import com.atleta.demo.dto.response.MatchPlayerResponse;
import com.atleta.demo.dto.response.MatchResponse;
import com.atleta.demo.dto.response.MatchTeamResponse;
import com.atleta.demo.dto.response.PlayerProfileResponse;
import com.atleta.demo.dto.response.PositionResponse;
import com.atleta.demo.dto.response.TeamResponse;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchEvent;
import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.entity.MatchTeam;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Position;
import com.atleta.demo.entity.Team;
import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.repository.MatchEventRepository;
import com.atleta.demo.repository.MatchPlayerRepository;
import com.atleta.demo.repository.MatchTeamRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MatchResponseMapper {

    private static final long MATCH_PLAY_WINDOW_HOURS = 1;

    private final MatchTeamRepository matchTeamRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchEventRepository matchEventRepository;
    private final MatchStatusPolicy matchStatusPolicy;

    public MatchResponseMapper(MatchTeamRepository matchTeamRepository,
                               MatchPlayerRepository matchPlayerRepository,
                               MatchEventRepository matchEventRepository,
                               MatchStatusPolicy matchStatusPolicy) {
        this.matchTeamRepository = matchTeamRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.matchEventRepository = matchEventRepository;
        this.matchStatusPolicy = matchStatusPolicy;
    }

    public MatchResponse toMatchResponse(Match match) {
        MatchResponse response = new MatchResponse();
        response.setId(match.getId());
        response.setModalidad(match.getModalidad());
        response.setCategoriaGenero(match.getCategoriaGenero());
        response.setMatchType(match.getMatchType());
        response.setFechaHoraProgramada(match.getFechaHoraProgramada());
        response.setLatitud(match.getLatitud());
        response.setLongitud(match.getLongitud());
        response.setCuota(match.getCuota());
        response.setEstado(match.getEstado());
        response.setStartedAt(match.getStartedAt());
        response.setFinalizedAt(match.getFinalizedAt());
        response.setValidationStatus(match.getValidationStatus());
        response.setValidationReason(match.getValidationReason());
        response.setFinalScoreLocal(match.getFinalScoreLocal());
        response.setFinalScoreAway(match.getFinalScoreAway());
        response.setClosePending(isClosePending(match));
        response.setMvpVotingClosedAt(match.getMvpVotingClosedAt());
        response.setCreatedAt(match.getCreatedAt());

        if (match.getCreador() != null) {
            response.setCreador(toPlayerProfileResponse(match.getCreador()));
        }

        if (match.getMvpUser() != null) {
            response.setMvpUser(toPlayerProfileResponse(match.getMvpUser()));
        }

        List<MatchTeam> matchTeams = matchTeamRepository.findByMatch(match);
        response.setMatchTeams(matchTeams.stream()
                .map(this::toMatchTeamResponse)
                .collect(Collectors.toList()));

        List<MatchPlayer> players = matchPlayerRepository.findByMatch(match);
        List<MatchPlayerResponse> playerResponses = players.stream()
                .map(this::toMatchPlayerResponse)
                .collect(Collectors.toCollection(ArrayList::new));

        if (match.getCreador() != null) {
            boolean creatorIncluded = playerResponses.stream()
                    .anyMatch(player -> player.getPlayer() != null
                            && match.getCreador().getAtletaUuid().equals(player.getPlayer().getAtletaUuid()));

            if (!creatorIncluded) {
                MatchPlayerResponse creatorResponse = new MatchPlayerResponse();
                creatorResponse.setId(-match.getId());
                creatorResponse.setPlayer(toPlayerProfileResponse(match.getCreador()));
                creatorResponse.setRol(PlayerRole.CAPITAN);
                creatorResponse.setConfirmado(true);
                playerResponses.add(creatorResponse);
            }
        }

        response.setPlayers(playerResponses);

        List<MatchEvent> events = matchEventRepository.findByMatchOrderByCreatedAt(match);
        response.setEvents(events.stream()
                .map(this::toMatchEventResponse)
                .collect(Collectors.toList()));

        return response;
    }

    public MatchPlayerResponse toMatchPlayerResponse(MatchPlayer matchPlayer) {
        MatchPlayerResponse response = new MatchPlayerResponse();
        response.setId(matchPlayer.getId());
        response.setRol(matchPlayer.getRol());
        response.setConfirmado(matchPlayer.getConfirmado());
        response.setTeamSide(matchPlayer.getTeamSide());

        if (matchPlayer.getPlayer() != null) {
            response.setPlayer(toPlayerProfileResponse(matchPlayer.getPlayer()));
        }

        if (matchPlayer.getTeam() != null) {
            response.setTeam(toTeamResponse(matchPlayer.getTeam()));
        }

        if (matchPlayer.getPosition() != null) {
            response.setPosition(toPositionResponse(matchPlayer.getPosition()));
        }

        return response;
    }

    private MatchTeamResponse toMatchTeamResponse(MatchTeam matchTeam) {
        MatchTeamResponse response = new MatchTeamResponse();
        response.setId(matchTeam.getId());
        response.setEsLocal(matchTeam.getEsLocal());
        response.setGoles(matchTeam.getGoles());

        if (matchTeam.getTeam() != null) {
            response.setTeam(toTeamResponse(matchTeam.getTeam()));
        }

        return response;
    }

    public MatchEventResponse toMatchEventResponse(MatchEvent event) {
        MatchEventResponse response = new MatchEventResponse();
        response.setSchemaVersion(1);
        response.setId(event.getId());
        response.setEventType(event.getTipoEvento());
        response.setConfirmedByLocal(event.getConfirmedByHome());
        response.setConfirmedByVisitante(event.getConfirmedByAway());
        response.setCreatedAt(event.getRegisteredAt());

        if (event.getPlayer() != null) {
            response.setPlayer(toPlayerProfileResponse(event.getPlayer()));
        }

        if (event.getTeam() != null) {
            response.setTeam(toTeamResponse(event.getTeam()));
        }

        if (event.getAssistPlayer() != null) {
            response.setAssistPlayer(toPlayerProfileResponse(event.getAssistPlayer()));
        }

        return response;
    }

    private PlayerProfileResponse toPlayerProfileResponse(PlayerProfile profile) {
        PlayerProfileResponse response = new PlayerProfileResponse();
        response.setAtletaUuid(profile.getAtletaUuid());
        response.setAlias(profile.getAlias());
        response.setGenero(profile.getAthlete() != null ? profile.getAthlete().getGenero() : null);
        response.setTrustScore(profile.getTrustScore());
        response.setCreatedAt(profile.getCreatedAt());

        return response;
    }

    private TeamResponse toTeamResponse(Team team) {
        TeamResponse response = new TeamResponse();
        response.setId(team.getId());
        response.setNombre(team.getNombre());
        response.setLogoUrl(team.getLogoUrl());
        response.setAnioFundacion(team.getAnioFundacion());
        response.setCreatedAt(team.getCreatedAt());

        if (team.getCreador() != null) {
            response.setCreador(toPlayerProfileResponse(team.getCreador()));
        }

        return response;
    }

    private PositionResponse toPositionResponse(Position position) {
        PositionResponse response = new PositionResponse();
        response.setId(position.getId());
        response.setNombre(position.getNombre());
        return response;
    }

    private boolean isClosePending(Match match) {
        return matchStatusPolicy.isClosePending(match, MATCH_PLAY_WINDOW_HOURS, LocalDateTime.now());
    }
}
