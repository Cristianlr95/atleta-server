package com.atleta.demo.service;

import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchEvent;
import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.entity.MatchTeam;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Team;
import com.atleta.demo.enums.EventType;
import com.atleta.demo.enums.MatchTeamSide;
import com.atleta.demo.repository.MatchEventRepository;
import com.atleta.demo.repository.MatchPlayerRepository;
import com.atleta.demo.repository.MatchTeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchFinalScoreServiceTest {

    @Mock
    private MatchEventRepository matchEventRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private MatchTeamRepository matchTeamRepository;

    private MatchFinalScoreService service;

    @BeforeEach
    void setUp() {
        service = new MatchFinalScoreService(
                matchEventRepository,
                matchPlayerRepository,
                matchTeamRepository
        );
    }

    @Test
    void applyFinalScoreSnapshot_CountsOnlyConfirmedGoalsBySide() {
        Match match = new Match();
        match.setId(42L);

        Team localTeam = team(1L);
        Team awayTeam = team(2L);
        MatchTeam localMatchTeam = new MatchTeam(match, localTeam, true);
        MatchTeam awayMatchTeam = new MatchTeam(match, awayTeam, false);

        PlayerProfile localPlayer = player();
        PlayerProfile awayPlayer = player();
        PlayerProfile ignoredPlayer = player();

        MatchPlayer localMatchPlayer = matchPlayer(match, localTeam, localPlayer, MatchTeamSide.LOCAL);
        MatchPlayer awayMatchPlayer = matchPlayer(match, awayTeam, awayPlayer, MatchTeamSide.VISITA);

        MatchEvent localGoal = goal(match, localTeam, localPlayer, true);
        MatchEvent awayGoal = goal(match, awayTeam, awayPlayer, true);
        MatchEvent pendingGoal = goal(match, localTeam, ignoredPlayer, false);
        MatchEvent assistOnly = new MatchEvent(match, EventType.ASISTENCIA, localPlayer, localTeam, localPlayer);
        assistOnly.confirmByHome();
        assistOnly.confirmByAway();

        when(matchEventRepository.findByMatchOrderByCreatedAt(match))
                .thenReturn(List.of(localGoal, awayGoal, pendingGoal, assistOnly));
        when(matchPlayerRepository.findByMatchAndPlayer(match, localPlayer)).thenReturn(Optional.of(localMatchPlayer));
        when(matchPlayerRepository.findByMatchAndPlayer(match, awayPlayer)).thenReturn(Optional.of(awayMatchPlayer));
        when(matchTeamRepository.findByMatch(match)).thenReturn(List.of(localMatchTeam, awayMatchTeam));

        service.applyFinalScoreSnapshot(match);

        assertEquals(1, match.getFinalScoreLocal());
        assertEquals(1, match.getFinalScoreAway());
        assertEquals(1, localMatchTeam.getGoles());
        assertEquals(1, awayMatchTeam.getGoles());
        verify(matchTeamRepository).saveAll(anyList());
    }

    private MatchEvent goal(Match match, Team team, PlayerProfile player, boolean confirmed) {
        MatchEvent event = new MatchEvent(match, EventType.GOL, player, team, player);
        if (confirmed) {
            event.confirmByHome();
            event.confirmByAway();
        }
        return event;
    }

    private MatchPlayer matchPlayer(Match match, Team team, PlayerProfile player, MatchTeamSide side) {
        MatchPlayer matchPlayer = new MatchPlayer();
        matchPlayer.setMatch(match);
        matchPlayer.setTeam(team);
        matchPlayer.setPlayer(player);
        matchPlayer.setTeamSide(side);
        return matchPlayer;
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
}
