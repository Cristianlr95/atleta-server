package com.atleta.demo.service;

import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchEvent;
import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.entity.MatchTeam;
import com.atleta.demo.entity.PlayerHistory;
import com.atleta.demo.entity.PlayerPosition;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Position;
import com.atleta.demo.entity.Team;
import com.atleta.demo.enums.EventType;
import com.atleta.demo.enums.MatchResult;
import com.atleta.demo.enums.MatchTeamSide;
import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.repository.MatchEventRepository;
import com.atleta.demo.repository.MatchPlayerRepository;
import com.atleta.demo.repository.MatchTeamRepository;
import com.atleta.demo.repository.PlayerHistoryRepository;
import com.atleta.demo.repository.PlayerPositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchPlayerHistoryServiceTest {

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private MatchEventRepository matchEventRepository;

    @Mock
    private MatchTeamRepository matchTeamRepository;

    @Mock
    private PlayerHistoryRepository playerHistoryRepository;

    @Mock
    private PlayerPositionRepository playerPositionRepository;

    private MatchPlayerHistoryService service;

    @BeforeEach
    void setUp() {
        service = new MatchPlayerHistoryService(
                matchPlayerRepository,
                matchEventRepository,
                matchTeamRepository,
                playerHistoryRepository,
                playerPositionRepository
        );
    }

    @Test
    void persistForFinalization_CreatesHistoryAndAddsPositionXp() {
        Match match = new Match();
        match.setId(42L);
        match.setFinalScoreLocal(2);
        match.setFinalScoreAway(1);

        Team localTeam = team(1L);
        PlayerProfile scorer = player();
        PlayerProfile assistant = player();
        Position position = position("Delantero");
        MatchPlayer matchPlayer = new MatchPlayer(match, localTeam, scorer, position, PlayerRole.JUGADOR);
        matchPlayer.setConfirmado(true);
        matchPlayer.setTeamSide(MatchTeamSide.LOCAL);

        MatchEvent goal = new MatchEvent(match, EventType.GOL, scorer, localTeam, assistant, scorer);
        goal.confirmByHome();
        goal.confirmByAway();

        PlayerPosition playerPosition = new PlayerPosition(scorer, position, 1);

        when(matchPlayerRepository.findByMatch(match)).thenReturn(List.of(matchPlayer));
        when(matchEventRepository.findByMatchOrderByCreatedAt(match)).thenReturn(List.of(goal));
        when(playerHistoryRepository.findByMatchAndPlayer(match, scorer)).thenReturn(Optional.empty());
        when(playerPositionRepository.findByPlayerAndPosition(scorer, position)).thenReturn(Optional.of(playerPosition));

        service.persistForFinalization(match);

        ArgumentCaptor<PlayerHistory> historyCaptor = ArgumentCaptor.forClass(PlayerHistory.class);
        verify(playerHistoryRepository).save(historyCaptor.capture());
        verify(playerPositionRepository).save(playerPosition);

        PlayerHistory history = historyCaptor.getValue();
        assertEquals(scorer, history.getPlayer());
        assertEquals(localTeam, history.getTeam());
        assertEquals(position, history.getPosition());
        assertEquals(1, history.getGoles());
        assertEquals(0, history.getAsistencias());
        assertEquals(MatchResult.VICTORIA, history.getResultado());
        assertEquals(30, history.getXpGanada());
        assertEquals(30, playerPosition.getXp());
    }

    @Test
    void hasHistoryRows_ReturnsTrueWhenMatchAlreadyHasHistory() {
        Match match = new Match();
        PlayerHistory history = new PlayerHistory();

        when(playerHistoryRepository.findByMatch(match)).thenReturn(List.of(history));

        assertEquals(true, service.hasHistoryRows(match));
    }

    private PlayerProfile player() {
        PlayerProfile player = new PlayerProfile();
        player.setAtletaUuid(UUID.randomUUID());
        return player;
    }

    private Team team(Long id) {
        Team team = new Team();
        team.setId(id);
        return team;
    }

    private Position position(String name) {
        Position position = new Position();
        position.setId(1L);
        position.setNombre(name);
        return position;
    }
}
