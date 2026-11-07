package com.atleta.demo.service;

import com.atleta.demo.dto.response.MatchEventResponse;
import com.atleta.demo.dto.response.MatchResponse;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchEvent;
import com.atleta.demo.entity.MatchTeam;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Position;
import com.atleta.demo.entity.Team;
import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.repository.MatchEventRepository;
import com.atleta.demo.repository.MatchPlayerRepository;
import com.atleta.demo.repository.MatchRepository;
import com.atleta.demo.repository.MatchTeamRepository;
import com.atleta.demo.repository.PlayerPositionRepository;
import com.atleta.demo.repository.PlayerProfileRepository;
import com.atleta.demo.repository.PositionRepository;
import com.atleta.demo.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchQueryServiceTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchTeamRepository matchTeamRepository;
    @Mock
    private MatchPlayerRepository matchPlayerRepository;
    @Mock
    private MatchEventRepository matchEventRepository;
    @Mock
    private PlayerProfileRepository playerProfileRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private PositionRepository positionRepository;
    @Mock
    private PlayerPositionRepository playerPositionRepository;
    @Mock
    private MatchAutomatedStatusService matchAutomatedStatusService;
    @Mock
    private MatchResponseMapper matchResponseMapper;

    private MatchQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new MatchQueryService(
                matchRepository,
                matchTeamRepository,
                matchPlayerRepository,
                matchEventRepository,
                playerProfileRepository,
                teamRepository,
                positionRepository,
                playerPositionRepository,
                matchAutomatedStatusService,
                matchResponseMapper
        );
    }

    @Test
    void getMatchesByPlayerOrCreator_MergesDuplicatesAndEnsuresCreatorParticipation() {
        PlayerProfile creator = player("Creador");
        Team team = team(1L, "Local");
        Position position = new Position("Delantero");
        Match match = match(10L, creator);
        Match creatorOnlyMatch = match(11L, creator);

        MatchTeam matchTeam = new MatchTeam(match, team, true);
        MatchTeam creatorOnlyMatchTeam = new MatchTeam(creatorOnlyMatch, team, true);
        MatchResponse firstResponse = response(10L);
        MatchResponse secondResponse = response(11L);

        when(playerProfileRepository.findById(creator.getAtletaUuid())).thenReturn(Optional.of(creator));
        when(matchRepository.findByPlayer(creator)).thenReturn(List.of(match));
        when(matchRepository.findByCreador(creator)).thenReturn(List.of(match, creatorOnlyMatch));
        when(matchTeamRepository.findLocalTeamByMatch(match)).thenReturn(Optional.of(matchTeam));
        when(matchTeamRepository.findLocalTeamByMatch(creatorOnlyMatch)).thenReturn(Optional.of(creatorOnlyMatchTeam));
        when(matchPlayerRepository.existsByMatchAndPlayer(match, creator)).thenReturn(false);
        when(matchPlayerRepository.existsByMatchAndPlayer(creatorOnlyMatch, creator)).thenReturn(false);
        when(playerPositionRepository.findPrimaryPositionByPlayer(creator)).thenReturn(Optional.empty());
        when(positionRepository.findAllOrderByNombre()).thenReturn(List.of(position));
        when(matchResponseMapper.toMatchResponse(match)).thenReturn(firstResponse);
        when(matchResponseMapper.toMatchResponse(creatorOnlyMatch)).thenReturn(secondResponse);

        List<MatchResponse> responses = queryService.getMatchesByPlayerOrCreator(creator.getAtletaUuid());

        assertEquals(List.of(firstResponse, secondResponse), responses);
        verify(matchAutomatedStatusService).refreshAutomatedMatchStates();
        verify(matchPlayerRepository).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getMatch().equals(match)
                        && saved.getTeam().equals(team)
                        && saved.getPlayer().equals(creator)
                        && saved.getConfirmado()
        ));
        verify(matchPlayerRepository).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getMatch().equals(creatorOnlyMatch)
                        && saved.getTeam().equals(team)
                        && saved.getPlayer().equals(creator)
                        && saved.getConfirmado()
        ));
    }

    @Test
    void getMatchEvents_MapsEventsWithoutRefreshingAutomatedStates() {
        Match match = match(10L, player("Creador"));
        MatchEvent event = new MatchEvent();
        MatchEventResponse response = new MatchEventResponse();

        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));
        when(matchEventRepository.findByMatchOrderByCreatedAt(match)).thenReturn(List.of(event));
        when(matchResponseMapper.toMatchEventResponse(event)).thenReturn(response);

        List<MatchEventResponse> responses = queryService.getMatchEvents(10L);

        assertEquals(List.of(response), responses);
        verify(matchAutomatedStatusService, never()).refreshAutomatedMatchStates();
    }

    private PlayerProfile player(String alias) {
        PlayerProfile player = new PlayerProfile();
        player.setAtletaUuid(UUID.randomUUID());
        player.setAlias(alias);
        return player;
    }

    private Team team(Long id, String name) {
        Team team = new Team();
        team.setId(id);
        team.setNombre(name);
        return team;
    }

    private Match match(Long id, PlayerProfile creator) {
        Match match = new Match(MatchMode.CINCO_VS_CINCO, LocalDateTime.now().plusDays(1), creator);
        match.setId(id);
        return match;
    }

    private MatchResponse response(Long id) {
        MatchResponse response = new MatchResponse();
        response.setId(id);
        return response;
    }
}
